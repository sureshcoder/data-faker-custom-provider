# Design documents

Self-contained HTML with inline SVG — open either file directly in a browser, no server needed.

| Document | Provider | Rendered |
|---|---|---|
| [`index.html`](index.html) | JobPosting | [preview](https://rawcdn.githack.com/sureshcoder/data-faker-custom-provider/v1.3.1/design/index.html) |
| [`candidate.html`](candidate.html) | Candidate | [preview](https://rawcdn.githack.com/sureshcoder/data-faker-custom-provider/v1.3.1/design/candidate.html) |

The preview links are pinned to the current release tag, so a reader on an old tag sees that
version's document rather than whatever `main` later became. `generate.py` rewrites the tag from
the `<version>` in `pom.xml` — see below.

## Regenerating

```bash
pip install pyyaml
python3 design/generate.py            # rewrite in place
python3 design/generate.py --check    # exit 1 if anything is out of date
```

One script, two kinds of drift.

**Industry sections.** Each document describes every configured industry. Those sections are
generated from the YAML mappings, because they drifted from the data as industries were added
and renamed. Four regions are rewritten, each delimited by `<!-- GENERATED:… -->` comments:

| Marker | Document | Content |
|---|---|---|
| `job-industry-cards` | `index.html` | One card per industry |
| `job-salary-chart` | `index.html` | Salary range comparison |
| `candidate-industry-cards` | `candidate.html` | One card per industry |
| `candidate-cert-chart` | `candidate.html` | Certification pool sizes |

Everything else — architecture diagrams, algorithm walkthroughs, prose, usage examples — is
hand-written and left untouched. Edit those directly.

**Version references.** The version is quoted all over the documentation, and every copy goes
stale on release, so they are all rewritten from the `<version>` in `pom.xml` — fourteen of them:

| Rule | Count | Where |
|---|---|---|
| Preview links | 6 | Design documents table in `README.md`, `design/README.md`, `CLAUDE.md` |
| Maven coordinates | 2 | `README.md` install snippets, local and JitPack |
| Gradle coordinates | 4 | `README.md` install snippets, Kotlin and Groovy DSL × local and JitPack |
| JitPack build log URL | 1 | `README.md` FAQ |
| JitPack tag hint | 1 | `README.md` Option B intro |

The local build uses a bare Maven version (`1.3.0`) while JitPack uses the tag (`v1.3.0`) for the
same release, so each rule carries whichever form it matched back through the rewrite.

The release step is therefore **bump `pom.xml`, then run the script** — nothing is edited by hand.

A **bare version in prose is deliberately left alone.** The upgrade notes discuss 1.2.1 and 1.3.0
as history and must keep saying so, which is why every rule anchors on an adjacent coordinate,
host or phrase rather than on the version alone.

A rule that matches **nothing** is an error rather than a silent skip, so a snippet that gets
reworded is reported instead of quietly staying on an old version — these are exactly the strings
nobody remembers to bump.

`--check` is the form worth running in CI. It fails when the YAML or the pom has moved on but the
documents have not, which is how these went stale in the first place.

## Checking for drift

```bash
python3 design/generate.py --check
```

Exits non-zero when the YAML has moved on but the documents have not. This is the useful one
in CI: silent drift is how these documents went stale in the first place.

## Adding an industry

`generate.py` holds a `META` table of per-industry presentation values — emoji, short chart
label, and colours — which cannot be derived from the YAML. Adding an industry to the YAML
without adding a `META` entry makes the script exit with a message naming the missing key,
rather than producing a document with a gap in it.
