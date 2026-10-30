r"""Fetch shikimori's GraphQL schema and compare it with tools/shikimori.graphql.

Shikimori caps query depth at 5, which a normal introspection query exceeds. A query named
`IntrospectionQuery` is let through whole (found 2026-10-04). This script sends one, prints the
result as SDL in the layout of tools/shikimori.graphql, and shows the difference:
- descriptions as `#` comments, with a blank line before a described member that is not the first;
- types sorted by name; built-in scalars, built-in directives and `__*` types left out;
- arguments on one line when the field fits in 80 columns and no argument has a description;
- `@deprecated(reason: …)` broken over lines when it doesn't fit, as prettier does.
On 2026-10-04 the output matched the file line for line, so an empty diff means "no change".

Usage:
    python tools/refresh_graphql_schema.py            # print a unified diff against the file
    python tools/refresh_graphql_schema.py --check    # the same, and exit 1 when they differ (CI)
    python tools/refresh_graphql_schema.py --write    # overwrite tools/shikimori.graphql

One anonymous request. With --check, shikimori being unreachable is a warning, not a failure: an outage
must not fail every push (the Live check workflow).
"""
import difflib, json, os, socket, sys, urllib.error, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
TARGET = os.path.join(HERE, "shikimori.graphql")
URL = "https://shikimori.io/api/graphql"
#GitHub Actions sets CI
USER_AGENT = "Shikimori-App-Remastered-ci" if os.environ.get("CI") else "Shikimori-App-Remastered-research"

QUERY = """query IntrospectionQuery {
  __schema {
    queryType { name } mutationType { name } subscriptionType { name }
    types { ...FullType }
    directives { name description locations isRepeatable args { ...InputValue } }
  }
}
fragment FullType on __Type {
  kind name description
  fields(includeDeprecated: true) { name description args { ...InputValue } type { ...TypeRef } isDeprecated deprecationReason }
  inputFields { ...InputValue }
  interfaces { ...TypeRef }
  enumValues(includeDeprecated: true) { name description isDeprecated deprecationReason }
  possibleTypes { ...TypeRef }
}
fragment InputValue on __InputValue { name description type { ...TypeRef } defaultValue }
fragment TypeRef on __Type { kind name ofType { kind name ofType { kind name ofType { kind name ofType { kind name ofType { kind name ofType { kind name ofType { kind name } } } } } } } }
"""

BUILTIN_SCALARS = {"String", "Int", "Float", "Boolean", "ID"}
BUILTIN_DIRECTIVES = {"deprecated", "include", "skip", "specifiedBy"}
WIDTH = 80


class Unreachable(Exception):
    """Shikimori did not answer, or answered with a server error: an outage, not a change."""


def request_json(url, body=None):
    headers = {"User-Agent": USER_AGENT}
    if body is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        #a 4xx means the request itself no longer works - that must fail, not pass as an outage
        if e.code >= 500:
            raise Unreachable("%s answered %d" % (url, e.code))
        raise
    except (urllib.error.URLError, socket.timeout) as e:
        raise Unreachable("%s: %s" % (url, e))


def fetch():
    body = json.dumps({"query": QUERY, "operationName": "IntrospectionQuery"}).encode()
    data = request_json(URL, body)
    if data.get("errors"):
        raise SystemExit("errors: %s" % data["errors"])
    return data["data"]["__schema"]


def tref(t):
    if t["kind"] == "NON_NULL":
        return tref(t["ofType"]) + "!"
    if t["kind"] == "LIST":
        return "[" + tref(t["ofType"]) + "]"
    return t["name"]


def desc(text, indent, first):
    if not text:
        return []
    out = [] if first else [""]
    for line in text.split("\n"):
        out.append(indent + ("# " + line if line else "#"))
    return out


def arg_str(a):
    s = a["name"] + ": " + tref(a["type"])
    if a["defaultValue"] is not None:
        s += " = " + a["defaultValue"]
    return s


def deprecated(item, head, indent):
    if not item.get("isDeprecated"):
        return [head]
    reason = item.get("deprecationReason")
    if reason is None or reason == "No longer supported":
        directive = "@deprecated"
    else:
        directive = "@deprecated(reason: " + json.dumps(reason, ensure_ascii=False) + ")"
    if len(head + " " + directive) <= WIDTH:
        return [head + " " + directive]
    if directive == "@deprecated":
        return [head, indent + "  @deprecated"]
    return [head, indent + "  @deprecated(",
            indent + "    reason: " + json.dumps(reason, ensure_ascii=False),
            indent + "  )"]


def field_lines(f, indent="  "):
    args = f.get("args") or []
    ret = ": " + tref(f["type"])
    if not args:
        return deprecated(f, indent + f["name"] + ret, indent)
    single = indent + f["name"] + "(" + ", ".join(arg_str(a) for a in args) + ")" + ret
    if len(single) <= WIDTH and not any(a["description"] for a in args):
        return deprecated(f, single, indent)
    out = [indent + f["name"] + "("]
    for i, a in enumerate(args):
        out += desc(a["description"], indent + "  ", i == 0)
        out += deprecated(a, indent + "  " + arg_str(a), indent + "  ")
    out += deprecated(f, indent + ")" + ret, indent)
    return out


def to_sdl(schema):
    blocks = []
    for d in schema["directives"]:
        if d["name"] in BUILTIN_DIRECTIVES:
            continue
        lines = desc(d["description"], "", True)
        args = "(" + ", ".join(arg_str(a) for a in d["args"]) + ")" if d["args"] else ""
        rep = " repeatable" if d.get("isRepeatable") else ""
        lines.append("directive @" + d["name"] + args + rep + " on " + " | ".join(d["locations"]))
        blocks.append(lines)

    for t in sorted(schema["types"], key=lambda x: x["name"]):
        name, kind = t["name"], t["kind"]
        if name.startswith("__") or (kind == "SCALAR" and name in BUILTIN_SCALARS):
            continue
        lines = desc(t["description"], "", True)
        if kind == "SCALAR":
            lines.append("scalar " + name)
        elif kind in ("OBJECT", "INTERFACE", "INPUT_OBJECT"):
            word = {"OBJECT": "type", "INTERFACE": "interface", "INPUT_OBJECT": "input"}[kind]
            impl = ""
            if t.get("interfaces"):
                impl = " implements " + " & ".join(i["name"] for i in t["interfaces"])
            lines.append(word + " " + name + impl + " {")
            members = t["inputFields"] if kind == "INPUT_OBJECT" else t["fields"]
            for i, f in enumerate(members):
                lines += desc(f["description"], "  ", i == 0)
                if kind == "INPUT_OBJECT":
                    lines += deprecated(f, "  " + arg_str(f), "  ")
                else:
                    lines += field_lines(f)
            lines.append("}")
        elif kind == "ENUM":
            lines.append("enum " + name + " {")
            for i, v in enumerate(t["enumValues"]):
                lines += desc(v["description"], "  ", i == 0)
                lines += deprecated(v, "  " + v["name"], "  ")
            lines.append("}")
        elif kind == "UNION":
            lines.append("union " + name + " = " + " | ".join(p["name"] for p in t["possibleTypes"]))
        blocks.append(lines)
    return "\n\n".join("\n".join(b) for b in blocks) + "\n"


def main():
    check = "--check" in sys.argv
    try:
        new = to_sdl(fetch())
    except Unreachable as e:
        if not check:
            raise SystemExit(str(e))
        print("::warning::shikimori unreachable, the schema was not compared: %s" % e)
        return
    if "--write" in sys.argv:
        open(TARGET, "w", encoding="utf-8", newline="\n").write(new)
        print("written", TARGET)
        return
    old = open(TARGET, encoding="utf-8").read()
    diff = list(difflib.unified_diff(old.splitlines(), new.splitlines(),
                                     "shikimori.graphql", "live", lineterm=""))
    if not diff:
        print("no change: the live schema matches the file")
        return
    print("\n".join(diff))
    if check:
        print("::error::the live GraphQL schema differs from tools/shikimori.graphql: check what changed, "
              "then refresh the file with --write")
        sys.exit(1)


if __name__ == "__main__":
    main()
