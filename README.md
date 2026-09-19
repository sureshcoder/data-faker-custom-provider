# data-faker-custom-provider

[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://www.azul.com/downloads/?version=java-21-lts)
[![DataFaker 2.5.4](https://img.shields.io/badge/DataFaker-2.5.4-blue)](https://www.datafaker.net/)
[![Tests](https://img.shields.io/badge/tests-76%20passing-brightgreen)](#building--testing)
[![Release](https://img.shields.io/github/v/release/sureshcoder/data-faker-custom-provider?label=release&color=success)](https://github.com/sureshcoder/data-faker-custom-provider/releases/latest)
[![JitPack](https://img.shields.io/jitpack/version/com.github.sureshcoder/data-faker-custom-provider.svg?color=blue)](https://jitpack.io/#sureshcoder/data-faker-custom-provider)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-yellow.svg)](LICENSE)

Two independent, YAML-driven custom providers for [DataFaker](https://www.datafaker.net/) that generate realistic, fully synthetic **job postings** and **job candidates** for ten industries. Everything is seed-reproducible, returned as immutable Java 21 records, and extensible by editing YAML rather than Java.

| Provider | Entry point | Produces |
|---|---|---|
| **JobPosting** | `new JobFaker().jobPosting()` | A [schema.org/JobPosting](https://schema.org/JobPosting)-shaped record with title, description, skills, dates, employment type, employer, location and salary range |
| **Candidate** | `new CandidateFaker().candidate()` | A résumé-like record with contact details, US address, education history, dated job history, skills, certifications and a professional summary |

## Table of contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
  - [Option A: build and install locally](#option-a-build-and-install-locally)
  - [Option B: JitPack](#option-b-jitpack)
- [Quick start](#quick-start)
- [JobPosting provider](#jobposting-provider)
  - [API](#jobposting-api)
  - [Fields and generation rules](#jobposting-fields-and-generation-rules)
  - [Example](#jobposting-example)
  - [Sample output](#jobposting-sample-output)
- [Candidate provider](#candidate-provider)
  - [API](#candidate-api)
  - [Fields and generation rules](#candidate-fields-and-generation-rules)
  - [Professional summary](#professional-summary)
  - [Example](#candidate-example)
  - [Sample output](#candidate-sample-output)
- [Seeding and reproducibility](#seeding-and-reproducibility)
- [Extended examples](#extended-examples)
  - [Bulk generation to JSON with Jackson](#bulk-generation-to-json-with-jackson)
  - [JUnit 5 test fixtures](#junit-5-test-fixtures)
  - [CSV export with streams](#csv-export-with-streams)
  - [Locale and DataFaker interop](#locale-and-datafaker-interop)
- [Industries reference](#industries-reference)
- [Extending via YAML](#extending-via-yaml)
  - [Adding an industry to JobPosting](#adding-an-industry-to-jobposting)
  - [Adding an industry to Candidate](#adding-an-industry-to-candidate)
  - [Editing professional summary templates](#editing-professional-summary-templates)
- [Building & testing](#building--testing)
- [Design documents](#design-documents)
- [FAQ / troubleshooting](#faq--troubleshooting)
- [Contributing](#contributing)
- [Roadmap](#roadmap)
- [License](#license)

## Features

- **Two providers, one dependency.** `JobFaker` and `CandidateFaker` both extend DataFaker's `Faker`, so every built-in provider (`name()`, `company()`, `address()`, …) is available on the same instance.
- **schema.org-modelled job postings** with per-industry title, skill and description pools and per-industry salary bounds.
- **Fully synthetic candidates** with chronologically consistent education history, dated job history (the last job is always current), 4–8 unique skills, 0–3 industry-specific certifications and a 2–3 sentence professional summary that is coherent with the rest of the record.
- **Ten industries shared by both providers**: technology, healthcare, finance, retail, manufacturing, logistics, education, staffing, consulting, media.
- **YAML-driven reference data.** Add an industry, a certification or a summary template by editing a YAML file; no Java changes are needed.
- **Seed-reproducible.** Every random pick goes through DataFaker's `RandomService`, so `new CandidateFaker(new Random(42L))` yields identical output on every run.
- **Immutable Java 21 records** with unmodifiable lists.
- **Safe contact data.** Emails use the throwaway domains `yopmail.com` and `mailinator.com`; phone numbers use the US `+1-XXX-XXX-XXXX` shape with non-routable random digits.

## Requirements

| Tool | Version | Notes |
|---|---|---|
| Java | 21+ | The library is compiled for Java 21 and uses records and switch expressions |
| Maven | 3.x | Only needed to build from source |
| DataFaker | 2.5.x | Declared as a compile dependency, so it is pulled in transitively |
| SnakeYAML | 2.5 | Declared explicitly and pinned to the version DataFaker 2.5.4 uses |

## Installation

The artifact is not yet on Maven Central. Choose one of the two options below.

### Option A: build and install locally

```bash
git clone https://github.com/sureshcoder/data-faker-custom-provider.git
cd data-faker-custom-provider
JAVA_HOME=/path/to/jdk-21 mvn install
```

Then depend on it from your project.

Maven:

```xml
<dependency>
    <groupId>in.sureshcoder</groupId>
    <artifactId>data-faker-custom-provider</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("in.sureshcoder:data-faker-custom-provider:1.0.0")
}
```

Gradle (Groovy DSL):

```groovy
dependencies {
    implementation 'in.sureshcoder:data-faker-custom-provider:1.0.0'
}
```

### Option B: JitPack

[JitPack](https://jitpack.io/#sureshcoder/data-faker-custom-provider) builds the library straight from GitHub. Pin a tag such as `v1.0.0` for a stable, reproducible build. The `main-SNAPSHOT` version tracks the tip of `main` instead and is **not** a stable pin; JitPack caches snapshots, so pass `-U` to Maven (or `--refresh-dependencies` to Gradle) to pick up new commits.

Maven:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.sureshcoder</groupId>
        <artifactId>data-faker-custom-provider</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

Gradle (Kotlin DSL):

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.sureshcoder:data-faker-custom-provider:v1.0.0")
}
```

Gradle (Groovy DSL):

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.sureshcoder:data-faker-custom-provider:v1.0.0'
}
```

> JitPack builds with Java 8 unless the repository tells it otherwise. See [JitPack build fails](#jitpack-build-fails-or-the-artifact-never-appears) in the FAQ.

## Quick start

```java
import in.sureshcoder.datafaker.candidate.faker.CandidateFaker;
import in.sureshcoder.datafaker.candidate.model.Candidate;
import in.sureshcoder.datafaker.jobposting.faker.JobFaker;
import in.sureshcoder.datafaker.jobposting.model.JobPosting;

public class QuickStart {
    public static void main(String[] args) {
        JobPosting job = new JobFaker().jobPosting().build();
        System.out.println(job.title() + " @ " + job.hiringOrganization());

        Candidate candidate = new CandidateFaker().candidate().buildForIndustry("technology");
        System.out.println(candidate.firstName() + " — " + candidate.professionalSummary());
    }
}
```

## JobPosting provider

### JobPosting API

Obtain the provider with `new JobFaker().jobPosting()`. `JobFaker` has the same four constructors as DataFaker's `Faker`: `()`, `(Locale)`, `(Random)` and `(Locale, Random)`.

| Method | Returns | Notes |
|---|---|---|
| `build()` | `JobPosting` | Industry chosen at random from the configured list |
| `buildForIndustry(String key)` | `JobPosting` | Key is case-insensitive; unknown keys fall back to the first configured industry (`technology`) |
| `availableIndustries()` | `List<String>` | The ten YAML keys, in file order |
| `availableEmploymentTypes()` | `List<String>` | `FULL_TIME`, `PART_TIME`, `CONTRACTOR`, `TEMPORARY`, `INTERN`, `VOLUNTEER`, `PER_DIEM` |
| `availableCurrencies()` | `List<String>` | `USD`, `EUR`, `GBP`, `INR`, `CAD`, `AUD`, `SGD` |
| `availableSalaryUnits()` | `List<String>` | `HOUR`, `DAY`, `WEEK`, `MONTH`, `YEAR` |

### JobPosting fields and generation rules

`JobPosting` is a 10-field record; `BaseSalary` is a 4-field record.

| Field | Type | How it is generated |
|---|---|---|
| `industry` | `String` | Industry display name, e.g. `"Technology"` |
| `title` | `String` | Random pick from the industry's `titles` list |
| `description` | `String` | Random industry template with `{company}`, `{title}` and `{skills}` (first three skills) filled in |
| `skills` | `List<String>` | 3–5 unique skills from the industry pool (Fisher–Yates shuffle) |
| `datePosted` | `LocalDate` | 0–30 days before today |
| `validThrough` | `LocalDate` | `datePosted` plus 30–90 days |
| `employmentType` | `String` | Random pick from the top-level `employment_type` list |
| `hiringOrganization` | `String` | `faker.company().name()` |
| `jobLocation` | `String` | 20 % `"Remote"`, otherwise `"City, ST"` |
| `baseSalary.currency` | `String` | Random pick from the top-level `currency` list |
| `baseSalary.minValue` | `int` | Random within the industry's `minLow`–`minHigh` bounds |
| `baseSalary.maxValue` | `int` | Random within `maxLow`–`maxHigh`; forced to at least `minValue + 20 000` |
| `baseSalary.unitText` | `String` | Random pick from the top-level `salary_unit` list |

Currency, employment type and salary unit are picked independently of the industry, so a `"VOLUNTEER"` posting priced in `INR` per `WEEK` is possible. Constrain them in YAML if your use case needs tighter data.

### JobPosting example

```java
import in.sureshcoder.datafaker.jobposting.faker.JobFaker;
import in.sureshcoder.datafaker.jobposting.model.BaseSalary;
import in.sureshcoder.datafaker.jobposting.model.JobPosting;

import java.util.List;

public class JobPostingExample {
    public static void main(String[] args) {
        JobFaker faker = new JobFaker();

        // Any industry
        JobPosting any = faker.jobPosting().build();

        // A specific industry
        JobPosting finance = faker.jobPosting().buildForIndustry("finance");
        System.out.printf("%s | %s | %s%n",
                finance.industry(), finance.title(), finance.hiringOrganization());
        System.out.println(finance.description());
        System.out.println("Skills: " + String.join(", ", finance.skills()));
        System.out.printf("Open %s → %s, %s, %s%n",
                finance.datePosted(), finance.validThrough(),
                finance.employmentType(), finance.jobLocation());

        BaseSalary salary = finance.baseSalary();
        System.out.printf("%s %,d – %,d per %s%n",
                salary.currency(), salary.minValue(), salary.maxValue(), salary.unitText());

        // Reference data, handy for validation or drop-downs
        List<String> industries = faker.jobPosting().availableIndustries();
        List<String> types      = faker.jobPosting().availableEmploymentTypes();
        System.out.println(industries + " " + types);
    }
}
```

### JobPosting sample output

Produced by `new JobFaker(new Random(42L)).jobPosting().build()` on 2026-09-12. The two dates are relative to the run date; everything else is fixed by the seed.

```json
{
  "industry": "Technology",
  "title": "Principal Software Engineer",
  "description": "As a Principal Software Engineer at Hegmann, Watsica and Schaden, you will design and implement high-performance solutions. Strong proficiency in Python, Redis, Azure is required. We offer competitive compensation and a remote-friendly culture.",
  "skills": ["Python", "Redis", "Azure"],
  "datePosted": "2026-08-25",
  "validThrough": "2026-10-14",
  "employmentType": "VOLUNTEER",
  "hiringOrganization": "Hegmann, Watsica and Schaden",
  "jobLocation": "Remote",
  "baseSalary": { "currency": "INR", "minValue": 92594, "maxValue": 147425, "unitText": "WEEK" }
}
```

## Candidate provider

### Candidate API

Obtain the provider with `new CandidateFaker().candidate()`. `CandidateFaker` has the same four constructors as `Faker`.

| Method | Returns | Notes |
|---|---|---|
| `build()` | `Candidate` | Industry chosen at random |
| `buildForIndustry(String key)` | `Candidate` | Job history, skills, certifications and summary all match the industry; unknown keys fall back to `technology` |
| `availableIndustries()` | `List<String>` | The ten YAML keys |
| `availableCertificationNames(String key)` | `List<String>` | Every certification name configured for that industry, useful for assertions |
| `availableSummaryTemplates()` | `List<String>` | All professional summary templates (both pools), useful for assertions |

### Candidate fields and generation rules

`Candidate` (10 fields):

| Field | Type | How it is generated |
|---|---|---|
| `firstName`, `lastName` | `String` | `faker.name().firstName()` / `lastName()` |
| `email` | `String` | `firstname + lastname` lower-cased, non-alphanumerics stripped, a number 0–998 appended, at `@yopmail.com` or `@mailinator.com` |
| `mobileNumber` | `String` | `+1-AAA-EEE-NNNN`, area and exchange 200–999, subscriber 1000–9999 |
| `professionalSummary` | `String` | 2–3 sentence paragraph; see [Professional summary](#professional-summary) |
| `address` | `Address` | US address, see below |
| `educationHistory` | `List<EducationHistory>` | 1–3 entries, oldest first, see below |
| `jobHistory` | `List<JobHistory>` | 1–4 entries, oldest first, last entry is the current job, see below |
| `skills` | `List<String>` | 4–8 unique skills from the industry pool |
| `certifications` | `List<Certification>` | 0–3 unique certifications from the industry pool |

`Address` (6 fields):

| Field | How it is generated |
|---|---|
| `addressLine1` | `faker.address().streetAddress()` |
| `addressLine2` | 40 % chance of `faker.address().secondaryAddress()`, otherwise `""` |
| `city`, `state`, `zipCode` | DataFaker city, two-letter state abbreviation and ZIP |
| `country` | Always `"US"` |

`EducationHistory` (8 fields). Entries are generated backwards: the most recent degree ends 1–5 years before today, and each earlier degree ends 3–12 months before the next one starts, so the list is chronological and non-overlapping.

| Field | How it is generated |
|---|---|
| `institutionName` | One of 20 US universities |
| `degreeLevel` | `UNDERGRADUATE`, `POSTGRADUATE`, `DOCTORATE` or `POST_DOCTORAL`; entry *i* of *n* gets level *i* |
| `courseName` | From the per-level `courses` list, e.g. `"Master of Science in Data Science"` |
| `specialization` | From the shared `specializations` list |
| `startDate`, `endDate` | Duration 3–4 years (UG), 1–2 (PG), 3–5 (doctorate), 1–2 (post-doc) |
| `scoreType`, `scoreValue` | 50/50 `CGPA` (6.0–10.0, step 0.1) or `PERCENTAGE` (55.0–100.0, step 0.5) |

`JobHistory` (7 fields, plus `isCurrent()`). Entries are generated backwards from a current job and never start before the most recent degree ends.

| Field | How it is generated |
|---|---|
| `companyName` | `faker.company().name()` |
| `role` | Functional area from the industry's `roles`, e.g. `"Data Engineering"` |
| `designation` | Formal title from the industry's `designations`, e.g. `"Senior Software Engineer"` |
| `startDate` | Current job: 1–36 months ago. Earlier jobs: 6–36 months long, separated by 0–6 month gaps, clamped to the latest education end date |
| `endDate` | `null` for the current (last) job, otherwise a past date; `isCurrent()` returns `endDate == null` |
| `responsibilities` | 2–4 unique bullet points from the industry pool |
| `skills` | 3–5 unique skills from the industry pool |

`Certification` (3 fields):

| Field | How it is generated |
|---|---|
| `name`, `issuingOrganization` | From the industry's `certifications` list |
| `issuedDate` | 0–59 months before today |

### Professional summary

`professionalSummary` is filled from a YAML template using data that is already on the record, so it never contradicts the other fields:

| Placeholder | Source |
|---|---|
| `{designation}` | Designation of the current (last) job |
| `{industry}` | Industry display name |
| `{skills}` | First three top-level skills, joined as `"A, B and C"` |
| `{degree}` | `courseName` of the highest (last) education entry |
| `{experience}` | Months since the first job started: `"under a year"`, `"1 year"` or `"N years"` |
| `{certification}` | Name of the first certification |

There are two template pools. `withCertification` is used only when the candidate has at least one certification, so `{certification}` is never blank. Every template is a single paragraph of two or three sentences.

### Candidate example

```java
import in.sureshcoder.datafaker.candidate.faker.CandidateFaker;
import in.sureshcoder.datafaker.candidate.model.Candidate;
import in.sureshcoder.datafaker.candidate.model.Certification;
import in.sureshcoder.datafaker.candidate.model.EducationHistory;
import in.sureshcoder.datafaker.candidate.model.JobHistory;

public class CandidateExample {
    public static void main(String[] args) {
        CandidateFaker faker = new CandidateFaker();
        Candidate c = faker.candidate().buildForIndustry("healthcare");

        System.out.printf("%s %s <%s> %s%n",
                c.firstName(), c.lastName(), c.email(), c.mobileNumber());
        System.out.printf("%s, %s, %s %s%n",
                c.address().addressLine1(), c.address().city(),
                c.address().state(), c.address().zipCode());
        System.out.println();
        System.out.println(c.professionalSummary());

        System.out.println("\nEducation");
        for (EducationHistory e : c.educationHistory()) {
            System.out.printf("  %s – %s  %s, %s (%s %.1f)%n",
                    e.startDate(), e.endDate(), e.courseName(),
                    e.institutionName(), e.scoreType(), e.scoreValue());
        }

        System.out.println("\nExperience");
        for (JobHistory j : c.jobHistory()) {
            String end = j.isCurrent() ? "present" : j.endDate().toString();
            System.out.printf("  %s – %s  %s @ %s (%s)%n",
                    j.startDate(), end, j.designation(), j.companyName(), j.role());
            j.responsibilities().forEach(r -> System.out.println("    • " + r));
        }

        System.out.println("\nSkills: " + String.join(", ", c.skills()));

        System.out.println("\nCertifications");
        for (Certification cert : c.certifications()) {
            System.out.printf("  %s (%s, %s)%n",
                    cert.name(), cert.issuingOrganization(), cert.issuedDate());
        }
    }
}
```

### Candidate sample output

Produced by `new CandidateFaker(new Random(42L)).candidate().build()` on 2026-09-12. Dates are relative to the run date; everything else is fixed by the seed.

```json
{
  "firstName": "Garnet",
  "lastName": "VonRueden",
  "email": "garnetvonrueden637@yopmail.com",
  "mobileNumber": "+1-905-318-2519",
  "professionalSummary": "Detail-oriented Data Scientist with 2 years of experience delivering measurable impact in Technology. Expertise spans React, Kafka and Docker. Certifications include HashiCorp Certified: Terraform Associate; completed a Doctor of Philosophy in Data Science.",
  "address": {
    "addressLine1": "205 Elvina Cliff", "addressLine2": "",
    "city": "Lake Graig", "state": "AL", "country": "US", "zipCode": "36872"
  },
  "educationHistory": [
    {
      "institutionName": "Georgia Institute of Technology",
      "degreeLevel": "UNDERGRADUATE",
      "courseName": "Bachelor of Science in Information Technology",
      "specialization": "Human-Computer Interaction",
      "startDate": "2011-12-12", "endDate": "2015-12-12",
      "scoreType": "CGPA", "scoreValue": 9.8
    },
    {
      "institutionName": "University of Illinois at Urbana-Champaign",
      "degreeLevel": "POSTGRADUATE",
      "courseName": "Master of Engineering in Software Engineering",
      "specialization": "Healthcare Management",
      "startDate": "2016-06-12", "endDate": "2017-06-12",
      "scoreType": "CGPA", "scoreValue": 9.5
    },
    {
      "institutionName": "University of Wisconsin-Madison",
      "degreeLevel": "DOCTORATE",
      "courseName": "Doctor of Philosophy in Data Science",
      "specialization": "Embedded Systems",
      "startDate": "2017-09-12", "endDate": "2022-09-12",
      "scoreType": "PERCENTAGE", "scoreValue": 93.5
    }
  ],
  "jobHistory": [
    {
      "companyName": "Nienow-Cummings",
      "role": "Platform Engineering",
      "designation": "Data Scientist",
      "startDate": "2024-01-12", "endDate": null,
      "responsibilities": [
        "Developed RESTful APIs consumed by web and mobile clients",
        "Collaborated with product teams to define technical requirements and deliver features on schedule",
        "Implemented monitoring and alerting using Prometheus and Grafana",
        "Led architecture decisions for cloud-native applications on AWS"
      ],
      "skills": ["Java", "Kafka", "Docker", "CI/CD"]
    }
  ],
  "skills": ["React", "Kafka", "Docker", "Go", "Java", "PostgreSQL", "Redis"],
  "certifications": [
    { "name": "HashiCorp Certified: Terraform Associate", "issuingOrganization": "HashiCorp", "issuedDate": "2024-02-12" }
  ]
}
```

## Seeding and reproducibility

Pass a `java.util.Random` to the faker constructor. Two fakers built from the same seed produce identical records, field for field, including list order.

```java
import in.sureshcoder.datafaker.candidate.faker.CandidateFaker;
import in.sureshcoder.datafaker.candidate.model.Candidate;

import java.util.Random;

public class SeededExample {
    public static void main(String[] args) {
        Candidate a = new CandidateFaker(new Random(42L)).candidate().build();
        Candidate b = new CandidateFaker(new Random(42L)).candidate().build();

        System.out.println(a.equals(b));                       // true
        System.out.println(a.professionalSummary().equals(b.professionalSummary())); // true
    }
}
```

What you can rely on:

- Every random pick goes through `faker.random()` (DataFaker's `RandomService`). The library never calls `new Random()`, `Math.random()` or `faker.numerify()`.
- Each faker instance owns its RNG. Two fakers with different seeds never interfere; `JobFaker` and `CandidateFaker` share no state.
- Calling `build()` repeatedly on one seeded faker gives a deterministic *sequence* of different records.

What changes the output for a given seed:

- Dates are anchored to `LocalDate.now()`, so `datePosted`, education and job dates shift with the calendar while every other field stays fixed.
- Editing either YAML file changes list sizes and therefore the values picked.
- Upgrading the library or DataFaker can change the order of RNG calls. Do not persist seeded output as a golden file across versions.

## Extended examples

### Bulk generation to JSON with Jackson

Jackson is **not** a dependency of this library. Add it to your own project:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.2</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>2.17.2</version>
</dependency>
```

Jackson 2.12+ serialises records without annotations. Register `JavaTimeModule` so `LocalDate` fields are written as ISO strings.

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.sureshcoder.datafaker.candidate.faker.CandidateFaker;
import in.sureshcoder.datafaker.candidate.model.Candidate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

public class BulkJsonExample {
    public static void main(String[] args) throws Exception {
        CandidateFaker faker = new CandidateFaker(new Random(7L));

        List<Candidate> candidates = Stream.generate(faker.candidate()::build)
                .limit(1_000)
                .toList();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);

        Files.writeString(Path.of("candidates.json"), mapper.writeValueAsString(candidates));
    }
}
```

`JobHistory.isCurrent()` is a plain method, not a record component. Depending on the Jackson version it may be picked up as a `"current"` property through the `isXxx()` getter convention; if you want only the record components, add a mix-in with `@JsonIgnoreProperties("current")` for `JobHistory`.

### JUnit 5 test fixtures

A seeded faker gives every test run the same data while keeping fixtures readable. The parameterised test walks every industry the library knows about, so it keeps passing when industries are added to the YAML.

```java
import in.sureshcoder.datafaker.candidate.faker.CandidateFaker;
import in.sureshcoder.datafaker.candidate.model.Candidate;
import in.sureshcoder.datafaker.jobposting.faker.JobFaker;
import in.sureshcoder.datafaker.jobposting.model.JobPosting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchingServiceTest {

    private CandidateFaker candidates;
    private JobFaker jobs;

    @BeforeEach
    void setUp() {
        candidates = new CandidateFaker(new Random(2024L));
        jobs = new JobFaker(new Random(2024L));
    }

    @Test
    void candidateSkillsOverlapWithPostingInSameIndustry() {
        Candidate c = candidates.candidate().buildForIndustry("technology");
        JobPosting j = jobs.jobPosting().buildForIndustry("technology");

        assertEquals("Technology", j.industry());
        assertFalse(c.skills().isEmpty());
        assertTrue(c.jobHistory().get(c.jobHistory().size() - 1).isCurrent());
    }

    static List<String> industries() {
        return new CandidateFaker().candidate().availableIndustries();
    }

    @ParameterizedTest
    @MethodSource("industries")
    void everyIndustryProducesConsistentCertifications(String industry) {
        Candidate c = candidates.candidate().buildForIndustry(industry);
        List<String> allowed = candidates.candidate().availableCertificationNames(industry);

        c.certifications().forEach(cert -> assertTrue(allowed.contains(cert.name())));
    }
}
```

### CSV export with streams

Plain JDK, no extra dependencies. Values are quoted and embedded quotes doubled, which is enough for company names such as `"Hegmann, Watsica and Schaden"`.

```java
import in.sureshcoder.datafaker.jobposting.faker.JobFaker;
import in.sureshcoder.datafaker.jobposting.model.JobPosting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvExportExample {
    public static void main(String[] args) throws Exception {
        JobFaker faker = new JobFaker(new Random(1L));

        List<String> lines = new ArrayList<>();
        lines.add("industry,title,company,location,employment_type,currency,min,max,unit,date_posted");

        Stream.generate(faker.jobPosting()::build)
                .limit(500)
                .map(CsvExportExample::toCsv)
                .forEach(lines::add);

        Files.write(Path.of("job-postings.csv"), lines);
    }

    static String toCsv(JobPosting j) {
        return Stream.of(
                j.industry(), j.title(), j.hiringOrganization(), j.jobLocation(),
                j.employmentType(), j.baseSalary().currency(),
                String.valueOf(j.baseSalary().minValue()),
                String.valueOf(j.baseSalary().maxValue()),
                j.baseSalary().unitText(), j.datePosted().toString()
        ).map(CsvExportExample::quote).collect(Collectors.joining(","));
    }

    static String quote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
```

### Locale and DataFaker interop

Both fakers extend `net.datafaker.Faker`, so built-in providers are available on the same seeded instance. The locale affects DataFaker's own providers (names, companies, street addresses); the custom YAML data, the `+1` phone format and the `"US"` country are locale-independent.

```java
import in.sureshcoder.datafaker.candidate.faker.CandidateFaker;
import in.sureshcoder.datafaker.candidate.model.Candidate;

import java.util.Locale;
import java.util.Random;

public class InteropExample {
    public static void main(String[] args) {
        CandidateFaker faker = new CandidateFaker(Locale.US, new Random(7L));

        Candidate c = faker.candidate().build();

        // Built-in providers share the same RNG, so this stays reproducible
        String recruiter = faker.name().fullName();
        String agency    = faker.company().name();
        String note      = faker.lorem().sentence();

        System.out.printf("%s referred by %s (%s): %s%n",
                c.firstName(), recruiter, agency, note);
    }
}
```

## Industries reference

Both YAML files use the same ten keys. Salary ranges apply to the JobPosting provider and are per year in the sense of the `minLow`/`maxHigh` bounds; the `unitText` on a posting is picked separately.

| YAML key | Display name | JobPosting salary bounds (`minLow` – `maxHigh`) |
|---|---|---|
| `technology` | Technology | 80 000 – 220 000 |
| `healthcare` | Healthcare | 50 000 – 180 000 |
| `finance` | Finance | 70 000 – 250 000 |
| `retail` | Retail | 35 000 – 120 000 |
| `manufacturing` | Manufacturing | 45 000 – 130 000 |
| `logistics` | Logistics | 40 000 – 120 000 |
| `education` | Education | 38 000 – 110 000 |
| `staffing` | Staffing & Recruiting | 45 000 – 140 000 |
| `consulting` | Consulting | 70 000 – 220 000 |
| `media` | Media & Entertainment | 40 000 – 150 000 |

Per industry, `job-posting-mappings.yml` holds titles, skills, description templates and salary bounds; `candidate-mappings.yml` holds roles, designations, skills, responsibilities and certifications.

## Extending via YAML

Both providers parse their YAML once, at class initialisation, from the classpath. To change the data, edit the file under `src/main/resources` and rebuild. No Java changes are needed for any of the edits below.

### Adding an industry to JobPosting

Edit `src/main/resources/job-posting-mappings.yml`. The top-level `employment_type`, `currency` and `salary_unit` lists apply to every industry.

```yaml
industries:
  legaltech:
    displayName: "Legal Technology"
    titles:
      - "Legal Engineer"
      - "E-Discovery Analyst"
    skills:
      - "Contract Analysis"
      - "Relativity"
      - "Regulatory Research"
    descriptionTemplates:
      - "Join {company} as a {title} skilled in {skills}."
    salaryRange:
      minLow: 60000
      minHigh: 90000
      maxLow: 110000
      maxHigh: 180000
```

### Adding an industry to Candidate

Edit `src/main/resources/candidate-mappings.yml` under `industries:`. Use the same key as in the JobPosting file if you want both providers to line up. Summary templates are shared, so a new industry needs none of its own.

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

### Editing professional summary templates

Templates live at the top level of `candidate-mappings.yml`:

```yaml
professionalSummaryTemplates:
  withoutCertification:
    - "{designation} with {experience} of experience in the {industry} industry. Skilled in {skills}. Holds a {degree}."
  withCertification:
    - "{designation} with {experience} of experience in {industry}. Skilled in {skills} and certified as {certification}. Holds a {degree}."
```

Rules:

- `{certification}` may appear only in the `withCertification` pool.
- Each template is one paragraph of two or three sentences and ends with a period. The test suite counts `.`, `!` and `?`, so avoid abbreviations with internal periods.
- Both pools must be non-empty; the provider fails fast at class load if either is missing.

## Building & testing

```bash
JAVA_HOME=/path/to/jdk-21 mvn compile   # build
JAVA_HOME=/path/to/jdk-21 mvn test      # run the suite
JAVA_HOME=/path/to/jdk-21 mvn install   # install into ~/.m2 for local consumers
```

The suite has 76 tests and runs in well under a second:

| Class | Tests |
|---|---|
| `JobPostingProviderTest` | 26 |
| `CandidateProviderTest` | 50 |

Tests use JUnit 5 and AssertJ. Several assert statistical properties over 100–500 builds (for example that both email domains appear and that `addressLine2` shows up roughly 40 % of the time), so an unseeded faker is used deliberately there.

## Design documents

Each document is a single self-contained HTML file with inline SVG diagrams. Open it locally or through the preview links.

| Provider | In repo | Rendered |
|---|---|---|
| JobPosting | [design/index.html](design/index.html) | [htmlpreview](https://htmlpreview.github.io/?https://github.com/sureshcoder/data-faker-custom-provider/blob/main/design/index.html) |
| Candidate | [design/candidate.html](design/candidate.html) | [htmlpreview](https://htmlpreview.github.io/?https://github.com/sureshcoder/data-faker-custom-provider/blob/main/design/candidate.html) |

## FAQ / troubleshooting

### `mvn` fails with "invalid target release: 21" or "class file has wrong version"

Your default JDK is older than 21. Point `JAVA_HOME` at a Java 21 installation for every Maven command, or set it in your shell profile.

### JitPack build fails or the artifact never appears

JitPack compiles with Java 8 by default, which cannot build this project. The repository needs a `jitpack.yml` at its root:

```yaml
jdk:
  - openjdk21
```

If the file is absent on the commit you request, the build log at `https://jitpack.io/com/github/sureshcoder/data-faker-custom-provider/v1.0.0/build.log` shows the compiler error. The first request for any version also triggers a build, so expect a short delay.

### I asked for an industry and got Technology instead

Unknown keys fall back to the first configured industry rather than throwing. Check `availableIndustries()` for the exact keys; matching is case-insensitive but otherwise exact.

### `UnsupportedOperationException` when modifying a list

All list fields are unmodifiable. Copy them first: `new ArrayList<>(candidate.skills())`.

### Why `yopmail.com` and `mailinator.com` emails?

They are public disposable-mail domains, so synthetic data never points at a real mailbox. The local part is derived from the generated name plus a random suffix.

### My seeded output changed after upgrading

The RNG call order is part of the generation algorithm. Any change to it, or to the YAML lists, shifts the sequence. Treat a seed as reproducible within one library version, not across versions.

### Candidates never have more than ~5 years of experience

Career start is anchored after the most recent degree, which ends 1–5 years before today. This keeps the timeline internally consistent at the cost of long careers. When the window is short, fewer than the drawn 1–4 jobs are emitted, but the current job always exists.

### What does `endDate == null` mean on a job?

The position is current. The last entry of `jobHistory` is always current; earlier entries always have an end date. Use `isCurrent()` rather than a null check.

### Can I use it outside DataFaker?

Yes. `JobFaker` and `CandidateFaker` are ordinary classes; nothing needs registering. They just happen to extend `Faker` so you get its providers for free.

## Contributing

1. Fork the repository and create a branch from `main`.
2. Make your change. Data changes go in the YAML files; keep both files' industry keys aligned if the change is industry-related.
3. Run the suite with Java 21: `JAVA_HOME=… mvn test`. Add tests under `src/test/java` for new behaviour.
4. If you add or remove tests, update the counts in this README and in `CLAUDE.md`.
5. Open a pull request against `main` describing what changed and why.

## Roadmap

- More industries beyond the current ten.
- Additional providers and locales.

## License

Apache License 2.0. See [LICENSE](LICENSE).
