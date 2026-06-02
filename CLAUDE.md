# data-faker-custom-provider

Custom [DataFaker](https://www.datafaker.net/) provider that generates realistic job posting data modelled after [schema.org/JobPosting](https://schema.org/JobPosting).

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

All 14 tests live in `JobPostingProviderTest`. They run in ~200 ms.

## Project structure

```
src/main/
  java/in/sureshcoder/datafaker/jobposting/
    faker/
      JobFaker.java              extends Faker; exposes jobPosting()
    model/
      JobPosting.java            10-field Java 21 record
      BaseSalary.java            currency / minValue / maxValue / unitText
    provider/
      JobPostingProvider.java    core generation logic
      IndustryData.java          package-private record (YAML data holder)
      SalaryRange.java           package-private record (salary bounds)
  resources/
    job-posting-mappings.yml     industry → titles / skills / templates / salary

src/test/
  java/in/sureshcoder/datafaker/jobposting/
    JobPostingProviderTest.java  14 JUnit 5 tests

design/
  index.html                     architecture & design document

LICENSE                          Apache 2.0
```

## Usage

```java
// Fully random across all industries
JobFaker faker = new JobFaker();
JobPosting job = faker.jobPosting().build();

// Targeted industry
JobPosting tech = faker.jobPosting().buildForIndustry("technology");

// Seeded — reproducible output
JobFaker seeded = new JobFaker(new Random(42L));
JobPosting same = seeded.jobPosting().build(); // deterministic
```

## Supported industries

| YAML key | Display name | Salary range (USD/yr) |
|----------|-------------|----------------------|
| `technology` | Technology  | $80k – $220k |
| `healthcare` | Healthcare  | $50k – $180k |
| `finance`    | Finance     | $70k – $250k |
| `retail`     | Retail      | $35k – $120k |

## Adding a new industry

Edit `src/main/resources/job-posting-mappings.yml` — no Java changes needed:

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
      currency: "USD"
      unitText: "YEAR"
```

## Key implementation notes

- **YAML loading** — `JobPostingProvider` loads `job-posting-mappings.yml` at class-init via SnakeYAML (transitive DataFaker dep). The map is `static final`; one parse per JVM lifetime.
- **Seeded randomness** — all random picks use `faker.random()` (DataFaker's `RandomService`), not `new Random()`. This means `new JobFaker(new Random(seed))` produces fully deterministic output.
- **DataFaker 2.x API** — `AbstractProvider<T>` exposes the faker as a `protected final T faker` field. `getProvider` takes `Function<PR, AP>`, so `JobPostingProvider::new` is the correct constructor reference.
- **Skill deduplication** — Fisher-Yates shuffle over `faker.random()` guarantees unique skills per posting while preserving seed reproducibility.

## License

Apache 2.0 — see [LICENSE](LICENSE).
