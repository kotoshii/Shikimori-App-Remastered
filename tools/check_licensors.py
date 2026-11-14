r"""Compare the licensor lists of shikimori's catalog pages with the app's «Лицензия» chips.

The api has no list of licensors, so the app keeps the names the site's catalog pages offer, in
app/src/main/res/values/licensors.xml (one string-array per type), and sends the chosen name as
`licensor`, which must match exactly. This check reads the licensor block of /animes, /mangas and /ranobe
(the `data-value` of each option in the `data-field="licensor"` list) and fails when a list stops
matching the app's: a name the site added (the app does not offer it) or one the site dropped (the app
still offers it). `any` («Лицензировано (любой)») is the app's own chip and is left out. The order is
not compared.

Usage:
    python tools/check_licensors.py

Exit 1 on a difference, or when a page has no licensor list (its layout changed, so the check would see
nothing). Shikimori unreachable is a warning, not a failure, as in check_enums.py. Anonymous requests.
When it fails: update the arrays in licensors.xml to the site's lists (this output names each change).
"""
import html, os, re, socket, sys, urllib.error, urllib.request

from refresh_graphql_schema import USER_AGENT, Unreachable

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
APP_FILE = "app/src/main/res/values/licensors.xml"
PAGES = [("anime", "https://shikimori.io/animes"), ("manga", "https://shikimori.io/mangas"),
         ("ranobe", "https://shikimori.io/ranobe")]


def request_text(url):
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept-Language": "ru"})
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        #a 4xx means the request itself no longer works - that must fail, not pass as an outage
        if e.code >= 500:
            raise Unreachable("%s answered %d" % (url, e.code))
        raise
    except (urllib.error.URLError, socket.timeout) as e:
        raise Unreachable("%s: %s" % (url, e))


def site_names(page):
    start = page.find('data-field="licensor"')
    if start < 0:
        return None
    end = page.find("</ul>", start)
    names = [html.unescape(v) for v in re.findall(r'data-value="([^"]*)"', page[start:end])]
    return [n for n in names if n != "any"]


def app_names():
    text = open(os.path.join(ROOT, APP_FILE), encoding="utf-8").read()
    lists = {}
    for key, body in re.findall(r'<string-array name="licensors_(\w+)"[^>]*>(.*?)</string-array>', text, re.S):
        lists[key] = [html.unescape(i.strip()) for i in re.findall(r"<item>(.*?)</item>", body, re.S)]
    return lists


def main():
    ours = app_names()
    try:
        pages = [(key, url, request_text(url)) for key, url in PAGES]
    except Unreachable as e:
        print("::warning::shikimori unreachable, the licensors were not compared: %s" % e)
        return

    failed = False
    for key, url, page in pages:
        theirs = site_names(page)
        mine = ours.get(key)
        if not theirs:
            print("::error::%s has no licensor list: the page changed, the check sees nothing" % url)
            failed = True
            continue
        if not mine:
            print("::error file=%s::no licensors_%s array" % (APP_FILE, key))
            failed = True
            continue
        added, dropped = set(theirs) - set(mine), set(mine) - set(theirs)
        for name in sorted(added):
            print("::error file=%s::%s offers the licensor \"%s\", which licensors_%s does not have" % (APP_FILE, url, name, key))
        for name in sorted(dropped):
            print("::error file=%s::%s no longer offers the licensor \"%s\" of licensors_%s" % (APP_FILE, url, name, key))
        if added or dropped:
            failed = True
        else:
            print("%s: ok, %d licensors" % (key, len(theirs)))

    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
