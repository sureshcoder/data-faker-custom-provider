/*
 * Copyright 2026 Suresh Shanmugam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package in.sureshcoder.datafaker.candidate.provider;

import in.sureshcoder.datafaker.candidate.model.Address;
import in.sureshcoder.datafaker.candidate.model.Candidate;
import in.sureshcoder.datafaker.candidate.model.Certification;
import in.sureshcoder.datafaker.candidate.model.EducationHistory;
import in.sureshcoder.datafaker.candidate.model.JobHistory;
import net.datafaker.Faker;
import net.datafaker.providers.base.AbstractProvider;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CandidateProvider extends AbstractProvider<Faker> {

    private static final Map<String, CandidateIndustryData> INDUSTRY_MAP;
    private static final List<String> INDUSTRY_KEYS;
    private static final Map<String, List<String>> COURSES_BY_DEGREE;
    private static final List<String> COLLEGES;
    private static final List<String> SPECIALIZATIONS;

    private static final String[] DEGREE_LEVELS =
            {"UNDERGRADUATE", "POSTGRADUATE", "DOCTORATE", "POST_DOCTORAL"};
    private static final String[] EMAIL_DOMAINS = {"yopmail.com", "mailinator.com"};

    static {
        Map<String, Object> root = loadYaml();
        COLLEGES        = parseStringList(root, "colleges");
        SPECIALIZATIONS = parseStringList(root, "specializations");
        COURSES_BY_DEGREE = parseCourseMap(root);
        INDUSTRY_MAP    = parseIndustries(root);
        INDUSTRY_KEYS   = List.copyOf(INDUSTRY_MAP.keySet());
    }

    public CandidateProvider(Faker f) {
        super(f);
    }

    /** Builds a fully random Candidate across all industries. */
    public Candidate build() {
        return buildForIndustry(randomIndustryKey());
    }

    /** Builds a Candidate whose job history and certifications match the given industry key. */
    public Candidate buildForIndustry(String industryKey) {
        CandidateIndustryData data = INDUSTRY_MAP.getOrDefault(
                industryKey.toLowerCase(),
                INDUSTRY_MAP.get(INDUSTRY_KEYS.get(0))
        );

        String firstName = faker.name().firstName();
        String lastName  = faker.name().lastName();

        return new Candidate(
                firstName,
                lastName,
                buildEmail(firstName, lastName),
                buildMobileNumber(),
                buildAddress(),
                buildEducationHistory(),
                buildJobHistory(data),
                pickUniqueN(data.skills(), 4 + faker.random().nextInt(5)),
                buildCertifications(data)
        );
    }

    /** Returns all available industry keys. */
    public List<String> availableIndustries() {
        return INDUSTRY_KEYS;
    }

    /** Returns the certification names available for a given industry key — useful in tests. */
    public List<String> availableCertificationNames(String industryKey) {
        CandidateIndustryData data = INDUSTRY_MAP.getOrDefault(
                industryKey.toLowerCase(),
                INDUSTRY_MAP.get(INDUSTRY_KEYS.get(0))
        );
        return data.certifications().stream().map(CertificationData::name).toList();
    }

    // ── private builders ────────────────────────────────────────────────────

    private String buildEmail(String firstName, String lastName) {
        String domain = EMAIL_DOMAINS[faker.random().nextInt(2)];
        String local = (firstName + lastName)
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                + faker.random().nextInt(999);
        return local + "@" + domain;
    }

    private String buildMobileNumber() {
        int area = 200 + faker.random().nextInt(800);
        int exch = 200 + faker.random().nextInt(800);
        int sub  = 1000 + faker.random().nextInt(9000);
        return String.format("+1-%03d-%03d-%04d", area, exch, sub);
    }

    private Address buildAddress() {
        // 40% chance of a secondary address line
        String line2 = faker.random().nextInt(5) < 2
                ? faker.address().secondaryAddress()
                : "";
        return new Address(
                faker.address().streetAddress(),
                line2,
                faker.address().city(),
                faker.address().stateAbbr(),
                "US",
                faker.address().zipCode()
        );
    }

    /**
     * Generates 1–3 education entries in chronological order.
     * Works backwards from a most-recent end date so all entries are in the past.
     */
    private List<EducationHistory> buildEducationHistory() {
        int count = 1 + faker.random().nextInt(3); // 1–3

        // Most recent degree ended 1–5 years ago
        LocalDate latestEndDate = LocalDate.now().minusYears(1 + faker.random().nextInt(5));

        List<EducationHistory> result = new ArrayList<>();
        LocalDate currentEndDate = latestEndDate;

        // Loop from highest degree (index count-1) down to UNDERGRADUATE (index 0)
        for (int i = count - 1; i >= 0; i--) {
            String degreeLevel = DEGREE_LEVELS[Math.min(i, DEGREE_LEVELS.length - 1)];
            int durationYears  = degreeDurationYears(degreeLevel);
            LocalDate startDate = currentEndDate.minusYears(durationYears);

            result.add(0, buildEducation(degreeLevel, startDate, currentEndDate));

            // Gap between successive degrees: 3–12 months
            currentEndDate = startDate.minusMonths(3 + faker.random().nextInt(10));
        }

        return Collections.unmodifiableList(result);
    }

    private int degreeDurationYears(String degreeLevel) {
        return switch (degreeLevel) {
            case "UNDERGRADUATE" -> 3 + faker.random().nextInt(2);  // 3–4 yr
            case "POSTGRADUATE"  -> 1 + faker.random().nextInt(2);  // 1–2 yr
            case "DOCTORATE"     -> 3 + faker.random().nextInt(3);  // 3–5 yr
            default              -> 1 + faker.random().nextInt(2);  // POST_DOCTORAL 1–2 yr
        };
    }

    private EducationHistory buildEducation(String degreeLevel, LocalDate start, LocalDate end) {
        List<String> courses = COURSES_BY_DEGREE.getOrDefault(degreeLevel, List.of("Bachelor of Science"));
        boolean isCgpa = faker.random().nextInt(2) == 0;
        double scoreValue = isCgpa
                ? 6.0 + faker.random().nextInt(41) * 0.1  // 6.0 – 10.0
                : 55.0 + faker.random().nextInt(91) * 0.5; // 55.0 – 100.0

        return new EducationHistory(
                pickRandom(COLLEGES),
                degreeLevel,
                pickRandom(courses),
                pickRandom(SPECIALIZATIONS),
                start,
                end,
                isCgpa ? "CGPA" : "PERCENTAGE",
                Math.round(scoreValue * 10.0) / 10.0
        );
    }

    /** Generates 1–4 job history entries for the given industry. */
    private List<JobHistory> buildJobHistory(CandidateIndustryData data) {
        int count = 1 + faker.random().nextInt(4); // 1–4
        List<JobHistory> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(buildJob(data));
        }
        return Collections.unmodifiableList(result);
    }

    private JobHistory buildJob(CandidateIndustryData data) {
        int respCount  = 2 + faker.random().nextInt(3); // 2–4 bullet points
        int skillCount = 3 + faker.random().nextInt(3); // 3–5 skills
        return new JobHistory(
                faker.company().name(),
                pickRandom(data.roles()),
                pickRandom(data.designations()),
                pickUniqueN(data.responsibilities(), respCount),
                pickUniqueN(data.skills(), skillCount)
        );
    }

    /** Generates 0–3 industry-specific certifications. */
    private List<Certification> buildCertifications(CandidateIndustryData data) {
        int count = faker.random().nextInt(4); // 0–3
        return pickUniqueN(data.certifications(), count).stream()
                .map(cd -> new Certification(
                        cd.name(),
                        cd.issuer(),
                        LocalDate.now().minusMonths(faker.random().nextInt(60))
                ))
                .toList();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String randomIndustryKey() {
        return INDUSTRY_KEYS.get(faker.random().nextInt(INDUSTRY_KEYS.size()));
    }

    private <T> T pickRandom(List<T> list) {
        return list.get(faker.random().nextInt(list.size()));
    }

    /** Fisher-Yates shuffle using faker's seeded Random; returns n unique elements. */
    private <T> List<T> pickUniqueN(List<T> source, int n) {
        if (n <= 0 || source.isEmpty()) {
            return List.of();
        }
        List<T> copy = new ArrayList<>(source);
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = faker.random().nextInt(i + 1);
            T tmp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, tmp);
        }
        return Collections.unmodifiableList(copy.subList(0, Math.min(n, copy.size())));
    }

    // ── YAML loading ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml() {
        Yaml yaml = new Yaml();
        try (InputStream is = CandidateProvider.class.getClassLoader()
                .getResourceAsStream("candidate-mappings.yml")) {
            if (is == null) {
                throw new IllegalStateException("candidate-mappings.yml not found on classpath");
            }
            return yaml.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load candidate-mappings.yml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseStringList(Map<String, Object> root, String key) {
        return Collections.unmodifiableList((List<String>) root.get(key));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> parseCourseMap(Map<String, Object> root) {
        Map<String, Object> raw = (Map<String, Object>) root.get("courses");
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            result.put(e.getKey(), Collections.unmodifiableList((List<String>) e.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, CandidateIndustryData> parseIndustries(Map<String, Object> root) {
        Map<String, Object> industries = (Map<String, Object>) root.get("industries");
        Map<String, CandidateIndustryData> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : industries.entrySet()) {
            Map<String, Object> ind = (Map<String, Object>) entry.getValue();
            List<Map<String, String>> rawCerts =
                    (List<Map<String, String>>) ind.get("certifications");
            List<CertificationData> certs = rawCerts.stream()
                    .map(m -> new CertificationData(m.get("name"), m.get("issuer")))
                    .toList();
            result.put(entry.getKey(), new CandidateIndustryData(
                    (String) ind.get("displayName"),
                    (List<String>) ind.get("roles"),
                    (List<String>) ind.get("designations"),
                    (List<String>) ind.get("skills"),
                    (List<String>) ind.get("responsibilities"),
                    certs
            ));
        }
        return Collections.unmodifiableMap(result);
    }
}
