# Design documents

Self-contained HTML with inline SVG — open either file directly in a browser, no server needed.

| Document | Provider | Rendered |
|---|---|---|
| [`index.html`](index.html) | JobPosting | [preview](https://rawcdn.githack.com/sureshcoder/data-faker-custom-provider/v1.3.0/design/index.html) |
| [`candidate.html`](candidate.html) | Candidate | [preview](https://rawcdn.githack.com/sureshcoder/data-faker-custom-provider/v1.3.0/design/candidate.html) |

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

**Preview links.** Pinning the links to a tag means they go stale on every release, so the tag is
rewritten too, from the `<version>` in `pom.xml`. Six links across three files:

| File | Links |
|---|---|
| `README.md` | Design documents table |
| `design/README.md` | The table above |
| `CLAUDE.md` | Design documents table |

The release step is therefore **bump `pom.xml`, then run the script** — no URL is edited by hand.
A file that no longer contains any preview link is an error rather than a silent skip, so a link
that is moved or renamed gets reported.

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
