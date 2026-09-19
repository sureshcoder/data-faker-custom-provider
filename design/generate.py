#!/usr/bin/env python3
"""Regenerate the data-driven sections of the design documents from the YAML mappings.

The design documents describe every configured industry: a card per industry, a salary
range chart, and a certification pool chart. Those sections were hand-written once and
then drifted from the YAML as industries were added and renamed, so they are generated
here instead.

Only the regions between the GENERATED markers are rewritten. Everything else in the
documents — architecture diagrams, algorithm walkthroughs, prose, usage examples — is
hand-written and left untouched.

    python3 design/generate.py            rewrite the generated regions in place
    python3 design/generate.py --check    exit 1 if a document is out of date

--check is the useful one in CI: it fails when the YAML has moved on but the design
documents have not, which is exactly how they drifted the first time.

Requires PyYAML (pip install pyyaml). Run from the repository root.
"""

import argparse
import html
import pathlib
import re
import sys

try:
    import yaml
except ImportError:
    sys.exit("PyYAML is required: pip install pyyaml")

ROOT = pathlib.Path(__file__).resolve().parent.parent
JOB_YAML = ROOT / "src/main/resources/job-posting-mappings.yml"
CAND_YAML = ROOT / "src/main/resources/candidate-mappings.yml"
JOB_DOC = ROOT / "design/index.html"
CAND_DOC = ROOT / "design/candidate.html"

# Per-industry presentation: emoji, short label for chart axes, text colour, bar fill,
# card background, card border. Keys must match the YAML industry keys exactly; the
# script fails loudly if they drift apart.
META = {
    "accommodation_services":                            ("🏨", "Accommodation",   "#9a3412", "#ea580c", "#fff7ed", "#fed7aa"),
    "administrative_and_support_services":               ("🤝", "Admin & Support", "#9d174d", "#db2777", "#fdf2f8", "#fbcfe8"),
    "construction":                                      ("🏗️", "Construction",    "#78350f", "#d97706", "#fffbeb", "#fde68a"),
    "consumer_services":                                 ("🛎️", "Consumer Svcs",   "#3730a3", "#4f46e5", "#eef2ff", "#c7d2fe"),
    "education":                                         ("🎓", "Education",       "#4c1d95", "#7c3aed", "#f5f3ff", "#ddd6fe"),
    "entertainment_providers":                           ("🎬", "Entertainment",   "#9a3412", "#f97316", "#fff7ed", "#fed7aa"),
    "farming_ranching_forestry":                         ("🌾", "Farming",         "#3f6212", "#65a30d", "#f7fee7", "#d9f99d"),
    "financial_services":                                ("📈", "Financial Svcs",  "#92400e", "#b45309", "#fffbeb", "#fde68a"),
    "government_administration":                         ("🏛️", "Government",      "#1e3a8a", "#1d4ed8", "#eff6ff", "#bfdbfe"),
    "holding_companies":                                 ("🏢", "Holding Cos",     "#334155", "#475569", "#f8fafc", "#cbd5e1"),
    "hospitals_and_health_care":                         ("🏥", "Health Care",     "#065f46", "#059669", "#ecfdf5", "#6ee7b7"),
    "manufacturing":                                     ("🏭", "Manufacturing",   "#334155", "#64748b", "#f8fafc", "#cbd5e1"),
    "oil_gas_and_mining":                                ("⛏️", "Oil, Gas, Mining", "#7c2d12", "#c2410c", "#fff7ed", "#fed7aa"),
    "professional_services":                             ("🧩", "Professional",    "#1e3a8a", "#2563eb", "#eff6ff", "#bfdbfe"),
    "real_estate_and_equipment_rental_services":         ("🏘️", "Real Estate",     "#155e75", "#0891b2", "#ecfeff", "#a5f3fc"),
    "retail":                                            ("🛍️", "Retail",          "#991b1b", "#dc2626", "#fef2f2", "#fecaca"),
    "technology_information_and_media":                  ("💻", "Technology",      "#1d4ed8", "#2563eb", "#eff6ff", "#bfdbfe"),
    "transportation_logistics_supply_chain_and_storage": ("🚚", "Transportation",  "#0e7490", "#0891b2", "#ecfeff", "#a5f3fc"),
    "utilities":                                         ("⚡", "Utilities",       "#854d0e", "#ca8a04", "#fefce8", "#fef08a"),
    "wholesale":                                         ("📦", "Wholesale",       "#6b21a8", "#9333ea", "#faf5ff", "#e9d5ff"),
}


def esc(text):
    return html.escape(text, quote=False)


def load():
    job = yaml.safe_load(JOB_YAML.read_text(encoding="utf-8"))
    cand = yaml.safe_load(CAND_YAML.read_text(encoding="utf-8"))
    keys = sorted(job["industries"])
    unknown = set(keys) ^ set(META)
    if unknown:
        sys.exit(
            "design/generate.py is out of step with the YAML.\n"
            f"  industries without presentation metadata: {sorted(set(keys) - set(META))}\n"
            f"  metadata for industries that no longer exist: {sorted(set(META) - set(keys))}\n"
            "Add or remove the entry in META, then re-run."
        )
    if set(cand["industries"]) != set(keys):
        sys.exit("the two YAML files configure different industries; reconcile them first")
    return job, cand, keys


def alias_stat(aliases_by_canonical, key):
    alias = aliases_by_canonical.get(key)
    if alias:
        return f'        <div class="stat">alias: <code>{alias}</code></div>\n'
    return '        <div class="stat" style="opacity:.45">no legacy alias</div>\n'


def job_cards(job, keys, aliases):
    out = ['    <div class="industry-grid">\n']
    for key in keys:
        data = job["industries"][key]
        salary = data["salaryRange"]
        emoji, _, colour, _, background, border = META[key]
        out.append(
            f'      <div class="ind-card" style="background:{background};border-color:{border}">\n'
            f'        <h3 style="color:{colour}">{emoji} {esc(data["displayName"])}</h3>\n'
            f'        <div class="stat">YAML key: <code>{key}</code></div>\n'
            f'{alias_stat(aliases, key)}'
            f'        <div class="stat">{len(data["titles"])} job titles · {len(data["skills"])} skills</div>\n'
            f'        <div class="stat">{len(data["descriptionTemplates"])} description templates</div>\n'
            f'        <div class="salary" style="color:{colour}">'
            f'${salary["minLow"] // 1000}k – ${salary["maxHigh"] // 1000}k / year</div>\n'
            f'      </div>\n'
        )
    out.append("    </div>\n")
    return "".join(out)


def candidate_cards(cand, keys, aliases):
    out = ['    <div class="industry-grid">\n']
    for key in keys:
        data = cand["industries"][key]
        emoji, _, colour, _, background, border = META[key]
        courses = data.get("courses", {})
        levels = [len(courses.get(level, [])) for level in ("UNDERGRADUATE", "POSTGRADUATE", "DOCTORATE")]
        out.append(
            f'      <div class="ind-card" style="background:{background};border-color:{border}">\n'
            f'        <h3 style="color:{colour}">{emoji} {esc(data["displayName"])}</h3>\n'
            f'        <div class="stat">YAML key: <code>{key}</code></div>\n'
            f'{alias_stat(aliases, key)}'
            f'        <div class="stat">{len(data["roles"])} roles · {len(data["designations"])} designations</div>\n'
            f'        <div class="stat">{len(data["skills"])} skills · {len(data["responsibilities"])} responsibilities</div>\n'
            f'        <div class="stat">{sum(levels)} degrees ({"/".join(map(str, levels))}) · '
            f'{len(data.get("specializations", []))} specializations</div>\n'
            f'        <div class="salary" style="color:{colour}">{len(data["certifications"])} certifications</div>\n'
            f'      </div>\n'
        )
    out.append("    </div>\n")
    return "".join(out)


def _chart(keys, title, value_of, ticks, max_value, label_fmt, indent):
    """Horizontal bar chart. Bars start at the axis unless value_of returns a (lo, hi) pair."""
    x0, x1 = 190.0, 790.0
    scale = (x1 - x0) / max_value
    row, top = 22, 40
    bottom = top + row * len(keys)
    pad = " " * indent
    svg = [f'{pad}<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 820 {bottom + 34}" width="820" style="max-width:100%">\n',
           f'{pad}  <text x="20" y="20" font-size="11" font-weight="600" fill="#475569">{esc(title)}</text>\n',
           f'{pad}  <line x1="{x0:.0f}" y1="32" x2="795" y2="32" stroke="#e2e8f0" stroke-width="1"/>\n',
           f'{pad}  <line x1="{x0:.0f}" y1="32" x2="{x0:.0f}" y2="{bottom}" stroke="#e2e8f0" stroke-width="1"/>\n']
    for tick in ticks:
        x = x0 + tick * scale
        if tick:
            svg.append(f'{pad}  <line x1="{x:.0f}" y1="32" x2="{x:.0f}" y2="{bottom}" stroke="#f1f5f9" stroke-width="1"/>\n')
        svg.append(f'{pad}  <text x="{x:.0f}" y="{bottom + 14}" text-anchor="middle" font-size="8.5" '
                   f'fill="#94a3b8">{label_fmt(tick)}</text>\n')
    for i, key in enumerate(keys):
        value = value_of(key)
        lo, hi = value if isinstance(value, tuple) else (0, value)
        _, label, colour, fill, _, _ = META[key]
        y = top + row * i
        text_y = y + 14
        x = x0 + lo * scale
        width = (hi - lo) * scale
        svg.append(f'{pad}  <text x="{x0 - 6:.0f}" y="{text_y}" text-anchor="end" font-size="8.5" '
                   f'fill="{colour}" font-weight="600">{esc(label)}</text>\n')
        svg.append(f'{pad}  <rect x="{x:.0f}" y="{y + 3}" width="{width:.0f}" height="15" rx="3" '
                   f'fill="{fill}" opacity=".85"/>\n')
        if isinstance(value, tuple):
            svg.append(f'{pad}  <text x="{x + 5:.0f}" y="{text_y}" font-size="7.5" fill="white">${lo:g}k</text>'
                       f'<text x="{x + width - 6:.0f}" y="{text_y}" text-anchor="end" font-size="7.5" '
                       f'fill="white">${hi:g}k</text>\n')
        else:
            svg.append(f'{pad}  <text x="{x + width + 6:.0f}" y="{text_y}" font-size="8.5" fill="#64748b">{hi}</text>\n')
    svg.append(f"{pad}</svg>\n")
    return "".join(svg)


def salary_chart(job, keys):
    def bounds(key):
        salary = job["industries"][key]["salaryRange"]
        return salary["minLow"] / 1000, salary["maxHigh"] / 1000

    return _chart(keys, "Salary range comparison (USD/year, bounds as configured)", bounds,
                  (0, 62.5, 125, 187.5, 250), 250.0,
                  lambda t: "$0" if t == 0 else f"${t:g}k", indent=6)


def cert_chart(cand, keys):
    counts = [len(cand["industries"][k]["certifications"]) for k in keys]
    top = max(counts)
    return _chart(keys, "Certification pool size per industry",
                  lambda k: len(cand["industries"][k]["certifications"]),
                  tuple(range(2, top + 1, 2)), float(top),
                  lambda t: str(int(t)), indent=6)


def splice(text, marker, replacement):
    pattern = re.compile(
        rf"([ \t]*<!-- GENERATED:{re.escape(marker)} START[^>]*-->\n).*?([ \t]*<!-- GENERATED:{re.escape(marker)} END -->\n)",
        re.S,
    )
    if not pattern.search(text):
        sys.exit(f"marker {marker!r} not found — the document lost its GENERATED comments")
    return pattern.sub(lambda m: m.group(1) + replacement + m.group(2), text, count=1)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true",
                        help="report whether the documents are up to date; do not write")
    args = parser.parse_args()

    job, cand, keys = load()
    aliases = {canonical: alias for alias, canonical in job["aliases"].items()}

    updates = {
        JOB_DOC: [("job-industry-cards", job_cards(job, keys, aliases)),
                  ("job-salary-chart", salary_chart(job, keys))],
        CAND_DOC: [("candidate-industry-cards", candidate_cards(cand, keys, aliases)),
                   ("candidate-cert-chart", cert_chart(cand, keys))],
    }

    stale = []
    for path, sections in updates.items():
        original = path.read_text(encoding="utf-8")
        updated = original
        for marker, replacement in sections:
            updated = splice(updated, marker, replacement)
        if updated == original:
            print(f"{path.relative_to(ROOT)}: up to date")
            continue
        stale.append(path)
        if args.check:
            print(f"{path.relative_to(ROOT)}: OUT OF DATE")
        else:
            path.write_text(updated, encoding="utf-8")
            print(f"{path.relative_to(ROOT)}: regenerated ({len(keys)} industries)")

    if args.check and stale:
        print("\nRun: python3 design/generate.py", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
