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
package in.sureshcoder.datafaker.jobposting;

import in.sureshcoder.datafaker.jobposting.faker.JobFaker;
import in.sureshcoder.datafaker.jobposting.model.BaseSalary;
import in.sureshcoder.datafaker.jobposting.model.JobPosting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JobPostingProviderTest {

    private JobFaker faker;

    @BeforeEach
    void setUp() {
        faker = new JobFaker();
    }

    @Test
    @DisplayName("build() returns a fully populated JobPosting")
    void buildReturnsFullyPopulatedPosting() {
        JobPosting job = faker.jobPosting().build();

        assertThat(job.industry()).isNotBlank();
        assertThat(job.title()).isNotBlank();
        assertThat(job.description()).isNotBlank();
        assertThat(job.skills()).isNotEmpty().hasSizeBetween(3, 5);
        assertThat(job.datePosted()).isNotNull().isBeforeOrEqualTo(LocalDate.now());
        assertThat(job.validThrough()).isAfter(job.datePosted());
        assertThat(job.employmentType()).isIn(
                "FULL_TIME", "PART_TIME", "CONTRACTOR", "TEMPORARY",
                "INTERN", "VOLUNTEER", "PER_DIEM");
        assertThat(job.hiringOrganization()).isNotBlank();
        assertThat(job.jobLocation()).isNotBlank();
    }

    @Test
    @DisplayName("Skills in a single posting are unique")
    void skillsAreUnique() {
        for (int i = 0; i < 20; i++) {
            List<String> skills = faker.jobPosting().build().skills();
            assertThat(new HashSet<>(skills)).hasSameSizeAs(skills);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"technology", "healthcare", "finance", "retail"})
    @DisplayName("buildForIndustry returns the correct industry display name")
    void buildForIndustryReturnsCorrectIndustry(String industryKey) {
        JobPosting job = faker.jobPosting().buildForIndustry(industryKey);
        assertThat(job.industry()).isNotBlank();
        assertThat(job.title()).isNotBlank();
        assertThat(job.skills()).isNotEmpty();
    }

    @Test
    @DisplayName("description contains the job title")
    void descriptionContainsTitle() {
        for (int i = 0; i < 10; i++) {
            JobPosting job = faker.jobPosting().build();
            assertThat(job.description()).contains(job.title());
        }
    }

    @Test
    @DisplayName("description contains at least one skill")
    void descriptionContainsAtLeastOneSkill() {
        for (int i = 0; i < 10; i++) {
            JobPosting job = faker.jobPosting().build();
            boolean containsASkill = job.skills().stream()
                    .anyMatch(s -> job.description().contains(s));
            assertThat(containsASkill)
                    .as("description '%s' should mention at least one skill from %s",
                            job.description(), job.skills())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("baseSalary max >= min, currency and unit are from the configured sets")
    void salaryConstraints() {
        for (int i = 0; i < 20; i++) {
            BaseSalary salary = faker.jobPosting().build().baseSalary();
            assertThat(salary.maxValue()).isGreaterThanOrEqualTo(salary.minValue());
            assertThat(salary.minValue()).isPositive();
            assertThat(salary.currency()).isIn("USD", "EUR", "GBP", "INR", "CAD", "AUD", "SGD");
            assertThat(salary.unitText()).isIn("HOUR", "DAY", "WEEK", "MONTH", "YEAR");
        }
    }

    @Test
    @DisplayName("datePosted is within the past 30 days")
    void datePostedWithinLast30Days() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        for (int i = 0; i < 20; i++) {
            LocalDate datePosted = faker.jobPosting().build().datePosted();
            assertThat(datePosted).isBetween(thirtyDaysAgo, today);
        }
    }

    @Test
    @DisplayName("validThrough is 30-90 days after datePosted")
    void validThroughIsWithinRange() {
        for (int i = 0; i < 20; i++) {
            JobPosting job = faker.jobPosting().build();
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    job.datePosted(), job.validThrough());
            assertThat(daysBetween).isBetween(30L, 90L);
        }
    }

    @Test
    @DisplayName("Seeded faker produces reproducible results")
    void seededFakerIsReproducible() {
        JobFaker f1 = new JobFaker(new Random(42L));
        JobFaker f2 = new JobFaker(new Random(42L));

        JobPosting job1 = f1.jobPosting().build();
        JobPosting job2 = f2.jobPosting().build();

        assertThat(job1.title()).isEqualTo(job2.title());
        assertThat(job1.skills()).isEqualTo(job2.skills());
        assertThat(job1.employmentType()).isEqualTo(job2.employmentType());
        assertThat(job1.baseSalary()).isEqualTo(job2.baseSalary());
    }

    @Test
    @DisplayName("All ten industries are represented across 500 random builds")
    void allIndustriesAppearInRandomBuilds() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            seen.add(faker.jobPosting().build().industry());
        }
        assertThat(seen).containsExactlyInAnyOrder(
                "Accommodation Services", "Administrative and Support Services",
                "Construction", "Consumer Services", "Education",
                "Entertainment Providers", "Farming, Ranching, Forestry",
                "Financial Services", "Government Administration",
                "Holding Companies", "Hospitals and Health Care", "Manufacturing",
                "Oil, Gas, and Mining", "Professional Services",
                "Real Estate and Equipment Rental Services", "Retail",
                "Technology, Information and Media",
                "Transportation, Logistics, Supply Chain and Storage",
                "Utilities", "Wholesale"
        );
    }

    @Test
    @DisplayName("availableIndustries returns all 20 LinkedIn industry keys")
    void availableIndustriesListsAllKeys() {
        assertThat(faker.jobPosting().availableIndustries())
                .containsExactlyInAnyOrder(
                        "accommodation_services", "administrative_and_support_services",
                        "construction", "consumer_services", "education",
                        "entertainment_providers", "farming_ranching_forestry",
                        "financial_services", "government_administration",
                        "holding_companies", "hospitals_and_health_care", "manufacturing",
                        "oil_gas_and_mining", "professional_services",
                        "real_estate_and_equipment_rental_services", "retail",
                        "technology_information_and_media",
                        "transportation_logistics_supply_chain_and_storage",
                        "utilities", "wholesale"
                );
    }

    @Test
    @DisplayName("availableIndustries does not list the legacy aliases")
    void availableIndustriesExcludesAliases() {
        assertThat(faker.jobPosting().availableIndustries())
                .doesNotContain("technology", "healthcare", "finance",
                                "logistics", "staffing", "consulting", "media");
    }

    @ParameterizedTest
    @CsvSource({
            "technology, technology_information_and_media",
            "healthcare, hospitals_and_health_care",
            "finance,    financial_services",
            "logistics,  transportation_logistics_supply_chain_and_storage",
            "staffing,   administrative_and_support_services",
            "consulting, professional_services",
            "media,      entertainment_providers"
    })
    @DisplayName("Legacy industry keys resolve to their canonical industry")
    void legacyAliasesResolveToCanonicalIndustry(String alias, String canonical) {
        String viaAlias     = faker.jobPosting().buildForIndustry(alias).industry();
        String viaCanonical = faker.jobPosting().buildForIndustry(canonical).industry();

        assertThat(viaAlias).isEqualTo(viaCanonical).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("allIndustryKeys")
    @DisplayName("Every industry produces a complete posting")
    void everyIndustryProducesCompletePosting(String industryKey) {
        JobPosting job = faker.jobPosting().buildForIndustry(industryKey);

        assertThat(job.industry()).isNotBlank();
        assertThat(job.title()).isNotBlank();
        assertThat(job.skills()).isNotEmpty();
        assertThat(job.description()).contains(job.title());
        assertThat(job.baseSalary().minValue()).isPositive();
        assertThat(job.baseSalary().maxValue())
                .isGreaterThan(job.baseSalary().minValue());
    }

    static Stream<String> allIndustryKeys() {
        return new JobFaker().jobPosting().availableIndustries().stream();
    }

    @Test
    @DisplayName("Unknown industry key falls back to the first configured industry")
    void unknownIndustryFallsBackToFirst() {
        String firstKey = faker.jobPosting().availableIndustries().get(0);
        String expected = faker.jobPosting().buildForIndustry(firstKey).industry();

        assertThat(faker.jobPosting().buildForIndustry("no-such-industry").industry())
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TECHNOLOGY", "Technology", "TeChNoLoGy"})
    @DisplayName("Industry keys resolve case-insensitively")
    void industryKeysAreCaseInsensitive(String industryKey) {
        assertThat(faker.jobPosting().buildForIndustry(industryKey).industry())
                .isEqualTo(faker.jobPosting().buildForIndustry("technology").industry());
    }

    // ── Reference-data list tests ────────────────────────────────────────

    @Test
    @DisplayName("availableEmploymentTypes returns all 7 configured values")
    void availableEmploymentTypesReturnsAllValues() {
        assertThat(faker.jobPosting().availableEmploymentTypes())
                .containsExactly(
                        "FULL_TIME", "PART_TIME", "CONTRACTOR", "TEMPORARY",
                        "INTERN", "VOLUNTEER", "PER_DIEM");
    }

    @Test
    @DisplayName("availableCurrencies returns all 7 configured currencies")
    void availableCurrenciesReturnsAllValues() {
        assertThat(faker.jobPosting().availableCurrencies())
                .containsExactly("USD", "EUR", "GBP", "INR", "CAD", "AUD", "SGD");
    }

    @Test
    @DisplayName("availableSalaryUnits returns all 5 configured units")
    void availableSalaryUnitsReturnsAllValues() {
        assertThat(faker.jobPosting().availableSalaryUnits())
                .containsExactly("HOUR", "DAY", "WEEK", "MONTH", "YEAR");
    }

    @Test
    @DisplayName("All 7 employment types appear across 1 000 random builds")
    void allEmploymentTypesAppearInRandomBuilds() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(faker.jobPosting().build().employmentType());
        }
        assertThat(seen).containsExactlyInAnyOrder(
                "FULL_TIME", "PART_TIME", "CONTRACTOR", "TEMPORARY",
                "INTERN", "VOLUNTEER", "PER_DIEM");
    }

    @Test
    @DisplayName("All 7 currencies appear across 1 000 random builds")
    void allCurrenciesAppearInRandomBuilds() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(faker.jobPosting().build().baseSalary().currency());
        }
        assertThat(seen).containsExactlyInAnyOrder("USD", "EUR", "GBP", "INR", "CAD", "AUD", "SGD");
    }

    @Test
    @DisplayName("All 5 salary units appear across 1 000 random builds")
    void allSalaryUnitsAppearInRandomBuilds() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(faker.jobPosting().build().baseSalary().unitText());
        }
        assertThat(seen).containsExactlyInAnyOrder("HOUR", "DAY", "WEEK", "MONTH", "YEAR");
    }
}
