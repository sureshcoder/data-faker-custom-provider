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
import java.util.Set;
import java.util.function.Function;

public class JobPostingProvider extends AbstractProvider<Faker> {

    private static final Map<String, IndustryData> INDUSTRY_MAP;
    private static final List<String> INDUSTRY_KEYS;
    private static final Map<String, String> ALIAS_MAP;
    private static final List<String> EMPLOYMENT_TYPES;
    private static final List<String> CURRENCIES;
    private static final List<String> SALARY_UNITS;
    private static final Map<String, Double> CURRENCY_RATES;
    private static final Map<String, Integer> UNIT_PERIODS;

    static {
        Map<String, Object> root = loadYaml();
        INDUSTRY_MAP = parseIndustries(root);
        INDUSTRY_KEYS = List.copyOf(INDUSTRY_MAP.keySet());
        ALIAS_MAP = parseAliases(root, INDUSTRY_MAP.keySet());
        EMPLOYMENT_TYPES = parseStringList(root, "employment_type");
        CURRENCY_RATES   = parseNumberMap(root, "currency", Number::doubleValue);
        UNIT_PERIODS     = parseNumberMap(root, "salary_unit", Number::intValue);
        CURRENCIES       = List.copyOf(CURRENCY_RATES.keySet());
        SALARY_UNITS     = List.copyOf(UNIT_PERIODS.keySet());
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
        IndustryData data = resolveIndustry(industryKey);

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

        String currency = pickRandom(CURRENCIES);
        String unit     = pickRandom(SALARY_UNITS);

        // The YAML bounds are USD per year. Convert into the drawn currency, then
        // divide down to the drawn pay period, so the figures agree with the labels.
        double rate   = CURRENCY_RATES.get(currency);
        int periods   = UNIT_PERIODS.get(unit);
        int converted = round(min * rate / periods);
        int maxValue  = round(max * rate / periods);
        if (maxValue <= converted) {
            maxValue = converted + step(converted);
        }

        return new BaseSalary(currency, converted, maxValue, unit);
    }

    /** Rounds to a granularity proportional to magnitude, so figures read naturally. */
    private static int round(double value) {
        int v = (int) Math.round(value);
        int step = step(v);
        return Math.max(step, (v + step / 2) / step * step);
    }

    /** Rounding granularity: 1000 above 10k, 100 above 1k, 10 above 100, else 1. */
    private static int step(int value) {
        if (value >= 10_000) return 1_000;
        if (value >= 1_000)  return 100;
        if (value >= 100)    return 10;
        return 1;
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

    /**
     * Resolves an industry key to its data, following any alias configured in the
     * YAML {@code aliases} block. Unknown keys fall back to the first configured
     * industry.
     */
    private static IndustryData resolveIndustry(String industryKey) {
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

    /** Parses a YAML mapping of name to number, preserving document order. */
    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> parseNumberMap(
            Map<String, Object> root, String key, Function<Number, T> mapper) {
        Map<String, Object> raw = (Map<String, Object>) root.get(key);
        if (raw == null || raw.isEmpty()) {
            throw new IllegalStateException("Missing or empty '" + key + "' block in YAML");
        }
        Map<String, T> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            if (!(e.getValue() instanceof Number n)) {
                throw new IllegalStateException(
                        "Expected a number for " + key + "." + e.getKey() + ", got: " + e.getValue());
            }
            result.put(e.getKey(), mapper.apply(n));
        }
        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseStringList(Map<String, Object> root, String key) {
        return Collections.unmodifiableList((List<String>) root.get(key));
    }
}
