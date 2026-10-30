r"""Compare shikimori's live enum values with the app's Gson enums.

Gson turns a value an enum does not list into null, so a new value shikimori starts sending can blank a
status or break a whole screen (BUGS #9, #93; the forum tab, 2026-09-29). This check fails on such a
value before users meet it.

For each enum in CHECKS, the app's values come from its Kotlin file (every `SerializedName("…")` and its
`alternate` spellings); the live ones from the GraphQL schema (the request refresh_graphql_schema.py
sends) or from the forum list (`/api/forums`). A live value the app lacks fails, unless ACCEPTED names it
with the reason it is harmless. Values only the app has are printed and pass: filter values such as
`tv_13`, the app's own forums such as `my_clubs`.

Usage:
    python tools/check_enums.py

Exit 1 on a value the app lacks, or on an enum file that gives no values (its format changed, so the
check would see nothing). Shikimori unreachable is a warning, not a failure: an outage must not fail
every push (the Live check workflow). Anonymous requests only.
"""
import os, re, sys

from refresh_graphql_schema import Unreachable, fetch, request_json

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ENTITY = "app/src/main/java/com/gnoemes/shikimori/entity/"
FORUMS = "/api/forums"

#the app's enum file, and the live lists its values must cover
CHECKS = [
    ("anime/domain/AnimeType.kt", ["AnimeKindEnum"]),
    ("manga/domain/MangaType.kt", ["MangaKindEnum"]),
    #one enum for both: manga add paused and discontinued
    ("common/domain/Status.kt", ["AnimeStatusEnum", "MangaStatusEnum"]),
    ("common/domain/AgeRating.kt", ["AnimeRatingEnum"]),
    ("rates/domain/RateStatus.kt", ["UserRateStatusEnum"]),
    ("common/domain/RelationType.kt", ["RelationKindEnum"]),
    ("anime/domain/AnimeVideoType.kt", ["VideoKindEnum"]),
    ("forum/domain/ForumType.kt", [FORUMS]),
]

#live values the app does not list on purpose, and why that is harmless
ACCEPTED = {
    "RelationKindEnum": ({"adaptation", "character", "spin_off"},
                         "FranchiseRelationResponse falls back to RelationType.OTHER"),
    "VideoKindEnum": ({"character_trailer", "clip", "cm", "episode_preview", "op_ed_clip"},
                      "AnimeVideoResponse falls back to AnimeVideoType.OTHER"),
}

NAME = re.compile(r'SerializedName\(\s*"([^"]*)"(?:\s*,\s*alternate\s*=\s*\[([^\]]*)\])?')


def app_values(path):
    values = set()
    for name, alternates in NAME.findall(open(os.path.join(ROOT, path), encoding="utf-8").read()):
        values.add(name)
        values.update(re.findall(r'"([^"]*)"', alternates))
    #an empty name is the app's own "unknown", not a value shikimori sends
    values.discard("")
    return values


def live_values(schema, source):
    if source == FORUMS:
        return {forum["permalink"] for forum in request_json("https://shikimori.io" + FORUMS)}
    for t in schema["types"]:
        if t["name"] == source and t["kind"] == "ENUM":
            return {v["name"] for v in t["enumValues"]}
    #a missing enum is a renamed one: fail rather than compare against nothing
    raise SystemExit("::error::the GraphQL schema has no enum %s" % source)


def main():
    try:
        schema = fetch()
        live = {source: live_values(schema, source) for _, sources in CHECKS for source in sources}
    except Unreachable as e:
        print("::warning::shikimori unreachable, the enums were not compared: %s" % e)
        return

    failed = False
    for file, sources in CHECKS:
        path = ENTITY + file
        name = os.path.basename(file)[:-3]
        ours = app_values(path)
        if not ours:
            print("::error file=%s::no SerializedName values read from %s" % (path, name))
            failed = True
            continue

        theirs = set().union(*(live[source] for source in sources))
        accepted = set()
        for source in sources:
            if source in ACCEPTED:
                values, reason = ACCEPTED[source]
                found = values & (theirs - ours)
                if found:
                    print("%s: accepted %s (%s)" % (name, ", ".join(sorted(found)), reason))
                if values - found:
                    print("%s: no longer needed in ACCEPTED: %s" % (name, ", ".join(sorted(values - found))))
                accepted |= found

        new = theirs - ours - accepted
        for value in sorted(new):
            print("::error file=%s::shikimori sends %s \"%s\" (%s), which %s does not list"
                  % (path, name, value, " / ".join(sources), name))
        failed = failed or bool(new)
        if ours - theirs:
            print("%s: only in the app: %s" % (name, ", ".join(sorted(ours - theirs))))
        if not new:
            print("%s: ok, %d live values" % (name, len(theirs)))

    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
