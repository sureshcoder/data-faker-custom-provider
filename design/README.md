# Design documents

Self-contained HTML with inline SVG — open either file directly in a browser, no server needed.

| Document | Provider |
|---|---|
| [`index.html`](index.html) | JobPosting |
| [`candidate.html`](candidate.html) | Candidate |

## Regenerating the data sections

Each document describes every configured industry. Those sections are **generated from the
YAML mappings**, because they drifted from the data as industries were added and renamed:

```bash
pip install pyyaml
python3 design/generate.py
```

Four regions are rewritten, each delimited by `<!-- GENERATED:… -->` comments:

| Marker | Document | Content |
|---|---|---|
| `job-industry-cards` | `index.html` | One card per industry |
| `job-salary-chart` | `index.html` | Salary range comparison |
| `candidate-industry-cards` | `candidate.html` | One card per industry |
| `candidate-cert-chart` | `candidate.html` | Certification pool sizes |

Everything else — architecture diagrams, algorithm walkthroughs, prose, usage examples — is
hand-written and left untouched. Edit those directly.

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
