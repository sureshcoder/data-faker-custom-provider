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
import in.sureshcoder.datafaker.location.UsLocation;
import in.sureshcoder.datafaker.location.UsLocations;
import net.datafaker.Faker;
import net.datafaker.providers.base.AbstractProvider;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CandidateProvider extends AbstractProvider<Faker> {

    private static final Map<String, CandidateIndustryData> INDUSTRY_MAP;
    private static final List<String> INDUSTRY_KEYS;
    private static final Map<String, String> ALIAS_MAP;
    private static final Map<String, List<String>> COURSES_BY_DEGREE;
    private static final List<String> COLLEGES;
    private static final List<String> SPECIALIZATIONS;
    private static final List<String> SUMMARY_TEMPLATES_PLAIN;
    private static final List<String> SUMMARY_TEMPLATES_WITH_CERT;

    private static final String[] DEGREE_LEVELS =
            {"UNDERGRADUATE", "POSTGRADUATE", "DOCTORATE"};
    private static final String[] EMAIL_DOMAINS = {"yopmail.com", "mailinator.com"};

    static {
        Map<String, Object> root = loadYaml();
        COLLEGES        = parseStringList(root, "colleges");
        SPECIALIZATIONS = parseStringList(root, "specializations");
        COURSES_BY_DEGREE = parseCourseMap(root);
        INDUSTRY_MAP    = parseIndustries(root);
        INDUSTRY_KEYS   = List.copyOf(INDUSTRY_MAP.keySet());
        ALIAS_MAP       = parseAliases(root, INDUSTRY_MAP.keySet());
        SUMMARY_TEMPLATES_PLAIN     = parseSummaryTemplates(root, "withoutCertification");
        SUMMARY_TEMPLATES_WITH_CERT = parseSummaryTemplates(root, "withCertification");
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
        CandidateIndustryData data = resolveIndustry(industryKey);

        String firstName    = faker.name().firstName();
        String lastName     = faker.name().lastName();
        String email        = buildEmail(firstName, lastName);
        String mobileNumber = buildMobileNumber();
        Address address     = buildAddress();

        List<EducationHistory> educationHistory = buildEducationHistory(data);
        LocalDate careerFloor = educationHistory.get(educationHistory.size() - 1).endDate();
        List<JobHistory> jobHistory = buildJobHistory(data, careerFloor);
        List<String> skills = pickUniqueN(data.skills(), 4 + faker.random().nextInt(5));
        List<Certification> certifications = buildCertifications(data);
        String professionalSummary = buildProfessionalSummary(
                data, jobHistory, skills, certifications, educationHistory);

        return new Candidate(
                firstName,
                lastName,
                email,
                mobileNumber,
                professionalSummary,
                address,
                educationHistory,
                jobHistory,
                skills,
                certifications
        );
    }

    /** Returns all available industry keys. */
    public List<String> availableIndustries() {
        return INDUSTRY_KEYS;
    }

    /** Returns every real US location the provider draws city / state / ZIP from. */
    public List<UsLocation> availableLocations() {
        return UsLocations.all();
    }

    /** Returns the certification names available for a given industry key — useful in tests. */
    public List<String> availableCertificationNames(String industryKey) {
        CandidateIndustryData data = resolveIndustry(industryKey);
        return data.certifications().stream().map(CertificationData::name).toList();
    }

    /**
     * Returns the degree pools for an industry, keyed by degree level. Levels the industry does
     * not configure fall back to the top-level pool, so the result is what generation will draw
     * from. The key may be an alias.
     */
    public Map<String, List<String>> availableCourses(String industryKey) {
        CandidateIndustryData data = resolveIndustry(industryKey);
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String level : DEGREE_LEVELS) {
            result.put(level, coursesFor(data, level));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns the specializations an industry draws from, falling back to the top-level pool when
     * the industry configures none. The key may be an alias.
     */
    public List<String> availableSpecializations(String industryKey) {
        List<String> own = resolveIndustry(industryKey).specializations();
        return own.isEmpty() ? SPECIALIZATIONS : own;
    }

    /** Returns every professional summary template (with and without certification) — useful in tests. */
    public List<String> availableSummaryTemplates() {
        List<String> all = new ArrayList<>(SUMMARY_TEMPLATES_PLAIN);
        all.addAll(SUMMARY_TEMPLATES_WITH_CERT);
        return Collections.unmodifiableList(all);
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
        // City, state and ZIP come from one real location, so the triple is coherent. The street
        // stays synthetic on purpose — a real street in a real city could hit a real mailbox.
        UsLocation location = UsLocations.pick(faker.random());
        return new Address(
                faker.address().streetAddress(),
                line2,
                location.city(),
                location.state(),
                "US",
                UsLocations.pickZip(location, faker.random())
        );
    }

    /**
     * Generates 1–3 education entries in chronological order.
     * Works backwards from a most-recent end date so all entries are in the past.
     */
    private List<EducationHistory> buildEducationHistory(CandidateIndustryData data) {
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

            result.add(0, buildEducation(data, degreeLevel, startDate, currentEndDate));

            // Gap between successive degrees: 3–12 months
            currentEndDate = startDate.minusMonths(3 + faker.random().nextInt(10));
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Courses for a degree level, preferring the industry's own list and falling back to the
     * top-level pool when the industry does not configure that level.
     */
    private static List<String> coursesFor(CandidateIndustryData data, String degreeLevel) {
        List<String> industryCourses = data.courses().get(degreeLevel);
        if (industryCourses != null && !industryCourses.isEmpty()) {
            return industryCourses;
        }
        return COURSES_BY_DEGREE.getOrDefault(degreeLevel, List.of("Bachelor of Science"));
    }

    private int degreeDurationYears(String degreeLevel) {
        return switch (degreeLevel) {
            case "UNDERGRADUATE" -> 3 + faker.random().nextInt(2);  // 3–4 yr
            case "POSTGRADUATE"  -> 1 + faker.random().nextInt(2);  // 1–2 yr
            case "DOCTORATE"     -> 3 + faker.random().nextInt(3);  // 3–5 yr
            default              -> 1 + faker.random().nextInt(2);  // unreachable via DEGREE_LEVELS
        };
    }

    private EducationHistory buildEducation(CandidateIndustryData data, String degreeLevel,
                                            LocalDate start, LocalDate end) {
        List<String> courses = coursesFor(data, degreeLevel);
        List<String> specializations =
                data.specializations().isEmpty() ? SPECIALIZATIONS : data.specializations();
        boolean isCgpa = faker.random().nextInt(2) == 0;
        double scoreValue = isCgpa
                ? 6.0 + faker.random().nextInt(41) * 0.1  // 6.0 – 10.0
                : 55.0 + faker.random().nextInt(91) * 0.5; // 55.0 – 100.0

        return new EducationHistory(
                pickRandom(COLLEGES),
                degreeLevel,
                pickRandom(courses),
                pickRandom(specializations),
                start,
                end,
                isCgpa ? "CGPA" : "PERCENTAGE",
                Math.round(scoreValue * 10.0) / 10.0
        );
    }

    /**
     * Generates 1–4 job history entries in chronological order (oldest first).
     * Works backwards from a current job (endDate == null) so that all positions are
     * non-overlapping, separated by 0–6 month gaps, and never start before careerFloor
     * (the end date of the most recent degree). When the window between careerFloor and
     * today is short, fewer than the drawn count may fit; the current job always exists.
     */
    private List<JobHistory> buildJobHistory(CandidateIndustryData data, LocalDate careerFloor) {
        int count = 1 + faker.random().nextInt(4); // 1–4
        LocalDate today = LocalDate.now();
        long windowMonths = Math.max(1, ChronoUnit.MONTHS.between(careerFloor, today));

        // Current job began 1..min(36, window) months ago — never before careerFloor
        int maxStartAgo = (int) Math.min(36, windowMonths);
        LocalDate currentStart = today.minusMonths(1 + faker.random().nextInt(maxStartAgo));

        List<JobHistory> result = new ArrayList<>();
        result.add(buildJob(data, currentStart, null));

        LocalDate cursorEnd = currentStart.minusMonths(faker.random().nextInt(7)); // gap 0–6 months
        while (result.size() < count && !cursorEnd.minusMonths(1).isBefore(careerFloor)) {
            int durationMonths = 6 + faker.random().nextInt(31); // 6–36 months
            LocalDate start = cursorEnd.minusMonths(durationMonths);
            if (start.isBefore(careerFloor)) {
                start = careerFloor;
            }
            result.add(0, buildJob(data, start, cursorEnd));
            cursorEnd = start.minusMonths(faker.random().nextInt(7)); // gap 0–6 months
        }
        return Collections.unmodifiableList(result);
    }

    private JobHistory buildJob(CandidateIndustryData data, LocalDate start, LocalDate end) {
        int respCount  = 2 + faker.random().nextInt(3); // 2–4 bullet points
        int skillCount = 3 + faker.random().nextInt(3); // 3–5 skills
        return new JobHistory(
                faker.company().name(),
                pickRandom(data.roles()),
                pickRandom(data.designations()),
                start,
                end,
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

    /**
     * Builds a 2–3 sentence professional summary from a YAML template, filled with the
     * candidate's own generated data so the text is coherent with the rest of the record.
     */
    private String buildProfessionalSummary(CandidateIndustryData data,
                                            List<JobHistory> jobHistory,
                                            List<String> skills,
                                            List<Certification> certifications,
                                            List<EducationHistory> educationHistory) {
        List<String> templates = certifications.isEmpty()
                ? SUMMARY_TEMPLATES_PLAIN
                : SUMMARY_TEMPLATES_WITH_CERT;
        String template = pickRandom(templates);

        Map<String, String> tokens = Map.of(
                "{designation}",   jobHistory.get(jobHistory.size() - 1).designation(),
                "{industry}",      data.displayName(),
                "{skills}",        joinNatural(skills.subList(0, Math.min(3, skills.size()))),
                "{degree}",        educationHistory.get(educationHistory.size() - 1).courseName(),
                "{experience}",    experiencePhrase(jobHistory.get(0).startDate()),
                "{certification}", certifications.isEmpty() ? "" : certifications.get(0).name()
        );
        return interpolate(template, tokens);
    }

    /** "under a year", "1 year", or "N years" measured from the first job's start date. */
    private static String experiencePhrase(LocalDate firstJobStart) {
        long months = ChronoUnit.MONTHS.between(firstJobStart, LocalDate.now());
        if (months < 12) {
            return "under a year";
        }
        if (months < 24) {
            return "1 year";
        }
        return (months / 12) + " years";
    }

    /** Joins as "A", "A and B", or "A, B and C". */
    private static String joinNatural(List<String> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1))
                + " and " + items.get(items.size() - 1);
    }

    private static String interpolate(String template, Map<String, String> tokens) {
        String result = template;
        for (Map.Entry<String, String> e : tokens.entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
        }
        return result;
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

    /**
     * Resolves an industry key to its data, following any alias configured in the
     * YAML {@code aliases} block. Unknown keys fall back to the first configured
     * industry.
     */
    private static CandidateIndustryData resolveIndustry(String industryKey) {
        String key = industryKey.toLowerCase();
        key = ALIAS_MAP.getOrDefault(key, key);
        return INDUSTRY_MAP.getOrDefault(key, INDUSTRY_MAP.get(INDUSTRY_KEYS.get(0)));
    }

    /**
     * Parses the optional top-level {@code aliases} block, mapping legacy industry
     * keys onto canonical ones. An absent block yields an empty map. An alias whose
     * target is not a configured industry is a configuration error and fails fast.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> parseAliases(Map<String, Object> root, Set<String> industryKeys) {
        Object raw = root.get("aliases");
        if (raw == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) raw).entrySet()) {
            String alias  = entry.getKey().toLowerCase();
            String target = String.valueOf(entry.getValue()).toLowerCase();
            if (!industryKeys.contains(target)) {
                throw new IllegalStateException(
                        "Industry alias '" + alias + "' points at unknown industry '" + target + "'");
            }
            result.put(alias, target);
        }
        return Collections.unmodifiableMap(result);
    }

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
    private static List<String> parseSummaryTemplates(Map<String, Object> root, String key) {
        Map<String, Object> block = (Map<String, Object>) root.get("professionalSummaryTemplates");
        List<String> list = block == null ? null : (List<String>) block.get(key);
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException(
                    "candidate-mappings.yml: professionalSummaryTemplates." + key + " is missing or empty");
        }
        return Collections.unmodifiableList(list);
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

    /** Parses an optional per-industry courses block; absent yields an empty map. */
    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> parseCourses(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : ((Map<String, Object>) raw).entrySet()) {
            result.put(e.getKey(), Collections.unmodifiableList((List<String>) e.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    /** Parses an optional per-industry string list; absent yields an empty list. */
    @SuppressWarnings("unchecked")
    private static List<String> parseOptionalList(Object raw) {
        return raw == null ? List.of() : Collections.unmodifiableList((List<String>) raw);
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
                    certs,
                    parseCourses(ind.get("courses")),
                    parseOptionalList(ind.get("specializations"))
            ));
        }
        return Collections.unmodifiableMap(result);
    }
}
