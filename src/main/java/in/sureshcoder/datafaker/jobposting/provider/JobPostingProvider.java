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
package in.sureshcoder.datafaker.jobposting.provider;

import in.sureshcoder.datafaker.jobposting.model.BaseSalary;
import in.sureshcoder.datafaker.jobposting.model.JobPosting;
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

public class JobPostingProvider extends AbstractProvider<Faker> {

    private static final Map<String, IndustryData> INDUSTRY_MAP;
    private static final List<String> INDUSTRY_KEYS;
    private static final List<String> EMPLOYMENT_TYPES;
    private static final List<String> CURRENCIES;
    private static final List<String> SALARY_UNITS;

    static {
        Map<String, Object> root = loadYaml();
        INDUSTRY_MAP = parseIndustries(root);
        INDUSTRY_KEYS = List.copyOf(INDUSTRY_MAP.keySet());
        EMPLOYMENT_TYPES = parseStringList(root, "employment_type");
        CURRENCIES       = parseStringList(root, "currency");
        SALARY_UNITS     = parseStringList(root, "salary_unit");
    }

    public JobPostingProvider(Faker f) {
        super(f);
    }

    /** Builds a fully random JobPosting across all industries. */
    public JobPosting build() {
        return buildForIndustry(randomIndustryKey());
    }

    /** Builds a JobPosting for a specific industry key, e.g. "technology" or "healthcare". */
    public JobPosting buildForIndustry(String industryKey) {
        IndustryData data = INDUSTRY_MAP.getOrDefault(
                industryKey.toLowerCase(),
                INDUSTRY_MAP.get(INDUSTRY_KEYS.get(0))
        );

        String title   = pickRandom(data.titles());
        int skillCount = 3 + faker.random().nextInt(3); // 3–5 skills
        List<String> skills = pickUniqueN(data.skills(), skillCount);
        String company = faker.company().name();
        String description = interpolate(pickRandom(data.descriptionTemplates()), title, skills, company);

        LocalDate datePosted  = LocalDate.now().minusDays(faker.random().nextInt(0, 30));
        LocalDate validThrough = datePosted.plusDays(30 + faker.random().nextInt(0, 60));

        return new JobPosting(
                data.displayName(),
                title,
                description,
                skills,
                datePosted,
                validThrough,
                pickRandom(EMPLOYMENT_TYPES),
                company,
                buildLocation(),
                buildSalary(data.salaryRange())
        );
    }

    /** Returns all available industry keys. */
    public List<String> availableIndustries() {
        return INDUSTRY_KEYS;
    }

    /** Returns all employment types as configured in the YAML. */
    public List<String> availableEmploymentTypes() {
        return EMPLOYMENT_TYPES;
    }

    /** Returns all currencies as configured in the YAML. */
    public List<String> availableCurrencies() {
        return CURRENCIES;
    }

    /** Returns all salary units as configured in the YAML. */
    public List<String> availableSalaryUnits() {
        return SALARY_UNITS;
    }

    private String randomIndustryKey() {
        return INDUSTRY_KEYS.get(faker.random().nextInt(INDUSTRY_KEYS.size()));
    }

    private String buildLocation() {
        // 20% chance of remote
        if (faker.random().nextInt(5) == 0) {
            return "Remote";
        }
        return faker.address().city() + ", " + faker.address().stateAbbr();
    }

    private BaseSalary buildSalary(SalaryRange r) {
        int min = r.minLow() + faker.random().nextInt(0, r.minHigh() - r.minLow());
        int max = r.maxLow() + faker.random().nextInt(0, r.maxHigh() - r.maxLow());
        if (max <= min) {
            max = min + 20_000;
        }
        return new BaseSalary(pickRandom(CURRENCIES), min, max, pickRandom(SALARY_UNITS));
    }

    private String interpolate(String template, String title, List<String> skills, String company) {
        String skillStr = String.join(", ", skills.subList(0, Math.min(3, skills.size())));
        return template
                .replace("{title}", title)
                .replace("{skills}", skillStr)
                .replace("{company}", company);
    }

    private <T> T pickRandom(List<T> list) {
        return list.get(faker.random().nextInt(list.size()));
    }

    /** Fisher-Yates shuffle via faker's seeded Random to support reproducible builds. */
    private List<String> pickUniqueN(List<String> source, int n) {
        List<String> copy = new ArrayList<>(source);
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = faker.random().nextInt(i + 1);
            String tmp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, tmp);
        }
        return Collections.unmodifiableList(copy.subList(0, Math.min(n, copy.size())));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml() {
        Yaml yaml = new Yaml();
        try (InputStream is = JobPostingProvider.class.getClassLoader()
                .getResourceAsStream("job-posting-mappings.yml")) {
            if (is == null) {
                throw new IllegalStateException("job-posting-mappings.yml not found on classpath");
            }
            return yaml.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load job-posting-mappings.yml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, IndustryData> parseIndustries(Map<String, Object> root) {
        Map<String, Object> industries = (Map<String, Object>) root.get("industries");
        Map<String, IndustryData> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : industries.entrySet()) {
            Map<String, Object> ind = (Map<String, Object>) entry.getValue();
            Map<String, Object> sr = (Map<String, Object>) ind.get("salaryRange");
            result.put(entry.getKey(), new IndustryData(
                    (String) ind.get("displayName"),
                    (List<String>) ind.get("titles"),
                    (List<String>) ind.get("skills"),
                    (List<String>) ind.get("descriptionTemplates"),
                    new SalaryRange(
                            (Integer) sr.get("minLow"), (Integer) sr.get("minHigh"),
                            (Integer) sr.get("maxLow"), (Integer) sr.get("maxHigh")
                    )
            ));
        }
        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseStringList(Map<String, Object> root, String key) {
        return Collections.unmodifiableList((List<String>) root.get(key));
    }
}
