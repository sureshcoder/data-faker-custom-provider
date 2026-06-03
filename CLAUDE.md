# data-faker-custom-provider

Custom [DataFaker](https://www.datafaker.net/) provider with two independent providers:
- **JobPosting** — generates realistic job postings modelled after [schema.org/JobPosting](https://schema.org/JobPosting)
- **Candidate** — generates fully-synthetic job candidates with education history, job history, skills, and industry-specific certifications

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 (Azul Zulu 21 recommended) |
| Maven | 3.x |

> **Note:** The default JDK on this machine is Java 8. Always prefix `mvn` commands with `JAVA_HOME=…`:
> ```
> JAVA_HOME=/Users/sbshanmugam/Library/Java/JavaVirtualMachines/azul-21.0.11/Contents/Home
> ```

## Build

```bash
JAVA_HOME=… mvn compile
```

## Test

```bash
JAVA_HOME=… mvn test
```

**61 tests total**, running in ~400 ms:
- `JobPostingProviderTest` — 26 tests (existing)
- `CandidateProviderTest` — 35 tests (new)

## Project structure

```
src/main/
  java/in/sureshcoder/datafaker/
    jobposting/
      faker/
        JobFaker.java              extends Faker; exposes jobPosting()
      model/
        JobPosting.java            10-field Java 21 record
        BaseSalary.java            currency / minValue / maxValue / unitText
      provider/
        JobPostingProvider.java    core generation logic
        IndustryData.java          package-private record (YAML data holder)
        SalaryRange.java           package-private record (salary bounds)
    candidate/
      faker/
        CandidateFaker.java        extends Faker; exposes candidate()
      model/
        Candidate.java             9-field Java 21 record
        Address.java               6-field record (US address)
        EducationHistory.java      8-field record (degree, dates, score)
        JobHistory.java            5-field record (company, role, responsibilities, skills)
        Certification.java         3-field record (name, issuer, issuedDate)
      provider/
        CandidateProvider.java     core generation logic
        CandidateIndustryData.java package-private record (YAML data holder)
        CertificationData.java     package-private record (YAML cert holder)
  resources/
    job-posting-mappings.yml       employment_type / currency / salary_unit + 10 industries
    candidate-mappings.yml         colleges / courses / specializations + 10 industries

src/test/
  java/in/sureshcoder/datafaker/
    jobposting/
      JobPostingProviderTest.java  26 JUnit 5 tests
    candidate/
      CandidateProviderTest.java   35 JUnit 5 tests

design/
  index.html                       JobPosting provider — architecture & design document
  candidate.html                   Candidate provider — architecture & design document

LICENSE                            Apache 2.0
```

## Usage — JobPosting provider

```java
// Fully random across all industries
JobFaker faker = new JobFaker();
JobPosting job = faker.jobPosting().build();

// Targeted industry
JobPosting tech = faker.jobPosting().buildForIndustry("technology");

// Seeded — reproducible output
JobFaker seeded = new JobFaker(new Random(42L));
JobPosting same = seeded.jobPosting().build(); // deterministic

// Query configured reference data
List<String> industries    = faker.jobPosting().availableIndustries();
List<String> empTypes      = faker.jobPosting().availableEmploymentTypes();
List<String> currencies    = faker.jobPosting().availableCurrencies();
List<String> salaryUnits   = faker.jobPosting().availableSalaryUnits();
```

## Usage — Candidate provider

```java
// Fully random candidate
CandidateFaker faker = new CandidateFaker();
Candidate c = faker.candidate().build();

// Industry-targeted — job history, skills and certifications match industry
Candidate techCandidate = faker.candidate().buildForIndustry("technology");

// Seeded — fully reproducible
CandidateFaker seeded = new CandidateFaker(new Random(42L));
Candidate same = seeded.candidate().build(); // deterministic

// Query reference data
List<String> industries = faker.candidate().availableIndustries();
List<String> certNames  = faker.candidate().availableCertificationNames("finance");

// Access generated fields
c.firstName();                    // "Sarah"
c.email();                        // "sarahjohnson47@yopmail.com"
c.mobileNumber();                 // "+1-415-782-3091"
c.address().country();            // "US"
c.educationHistory();             // 1–3 EducationHistory records, chronological
c.jobHistory();                   // 1–4 JobHistory records
c.skills();                       // 4–8 unique skills
c.certifications();               // 0–3 industry-specific Certification records
```

## Supported industries (shared by both providers)

| YAML key      | Display name          | Salary range (USD/yr) |
|---------------|-----------------------|----------------------|
| `technology`  | Technology            | $80k – $220k |
| `healthcare`  | Healthcare            | $50k – $180k |
| `finance`     | Finance               | $70k – $250k |
| `retail`      | Retail                | $35k – $120k |
| `manufacturing` | Manufacturing       | $45k – $130k |
| `logistics`   | Logistics             | $40k – $120k |
| `education`   | Education             | $38k – $110k |
| `staffing`    | Staffing & Recruiting | $45k – $140k |
| `consulting`  | Consulting            | $70k – $220k |
| `media`       | Media & Entertainment | $40k – $150k |

## Adding a new industry

Edit `src/main/resources/job-posting-mappings.yml` — no Java changes needed. The top-level `employment_type`, `currency`, and `salary_unit` lists apply to all industries; only `salaryRange` bounds are per-industry:

```yaml
industries:
  education:
    displayName: "Education"
    titles:
      - "Curriculum Developer"
      - "Instructional Designer"
    skills:
      - "Instructional Design"
      - "LMS Platforms"
    descriptionTemplates:
      - "Join {company} as a {title} skilled in {skills}."
    salaryRange:
      minLow: 40000
      minHigh: 60000
      maxLow: 65000
      maxHigh: 100000
```

## Adding a new candidate industry

Edit `src/main/resources/candidate-mappings.yml` under `industries:` — no Java changes needed:

```yaml
industries:
  legaltech:
    displayName: "Legal Technology"
    roles:
      - "Legal Engineering"
      - "Compliance"
    designations:
      - "Legal Tech Specialist"
      - "Compliance Analyst"
    skills:
      - "Contract Analysis"
      - "Relativity"
    responsibilities:
      - "Managed e-discovery workflows for cases valued over $50M"
    certifications:
      - name: "Certified E-Discovery Specialist (CEDS)"
        issuer: "ACEDS"
```

## Key implementation notes — JobPosting provider

- **YAML loading** — `JobPostingProvider` loads `job-posting-mappings.yml` at class-init via SnakeYAML (transitive DataFaker dep). The map is `static final`; one parse per JVM lifetime.
- **Seeded randomness** — all random picks use `faker.random()` (DataFaker's `RandomService`), not `new Random()`. This means `new JobFaker(new Random(seed))` produces fully deterministic output.
- **DataFaker 2.x API** — `AbstractProvider<T>` exposes the faker as a `protected final T faker` field. `getProvider` takes `Function<PR, AP>`, so `JobPostingProvider::new` is the correct constructor reference.
- **Skill deduplication** — Fisher-Yates shuffle over `faker.random()` guarantees unique skills per posting while preserving seed reproducibility.
- **Currency / employment type / salary unit** — picked at random from the top-level YAML lists (not per-industry). These lists are exposed via `availableCurrencies()`, `availableEmploymentTypes()`, and `availableSalaryUnits()`.
- **Location** — 20 % chance of "Remote"; otherwise `faker.address().city() + ", " + stateAbbr()`.

## Key implementation notes — Candidate provider

- **Education backward algorithm** — education history is generated right-to-left: the most recent degree end date is fixed 1–5 years ago, then each prior degree is placed chronologically earlier. This guarantees all entries are in the past and non-overlapping.
- **Score types** — CGPA is `6.0 + nextInt(41) * 0.1` (range 6.0–10.0); PERCENTAGE is `55.0 + nextInt(91) * 0.5` (range 55.0–100.0). Integer-only arithmetic preserves full seed reproducibility without `nextDouble()`.
- **Email domains** — restricted to `@yopmail.com` and `@mailinator.com`. Local part is `(firstName + lastName).toLowerCase().replaceAll("[^a-z0-9]","") + nextInt(999)`.
- **Phone format** — US format `+1-XXX-XXX-XXXX` using three separate `faker.random().nextInt()` calls, never `faker.numerify()`, ensuring seed safety.
- **Address line 2** — 40 % probability (when `faker.random().nextInt(5) < 2`); blank string otherwise.
- **Certifications** — drawn from the industry's YAML list using Fisher-Yates shuffle; 0–3 per candidate. `availableCertificationNames(key)` exposes the full list per industry for test assertions.
- **Generic `pickUniqueN<T>`** — single generic Fisher-Yates helper handles String skills, String responsibilities, and `CertificationData` objects uniformly.
- **Separate fakers** — `CandidateFaker` and `JobFaker` both extend `Faker` independently; they share no state. Use each standalone.

## Design documents

| Provider | Document |
|---|---|
| JobPosting | `design/index.html` |
| Candidate | `design/candidate.html` |

Both are self-contained HTML files with inline SVG diagrams — open directly in a browser, no server needed.

## License

Apache 2.0 — see [LICENSE](LICENSE).
