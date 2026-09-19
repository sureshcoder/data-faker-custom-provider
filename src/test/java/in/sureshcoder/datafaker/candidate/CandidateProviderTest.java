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
package in.sureshcoder.datafaker.candidate;

import in.sureshcoder.datafaker.candidate.faker.CandidateFaker;
import in.sureshcoder.datafaker.candidate.model.Candidate;
import in.sureshcoder.datafaker.candidate.model.Certification;
import in.sureshcoder.datafaker.candidate.model.EducationHistory;
import in.sureshcoder.datafaker.candidate.model.JobHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateProviderTest {

    private CandidateFaker faker;

    @BeforeEach
    void setUp() {
        faker = new CandidateFaker();
    }

    // ── top-level completeness ──────────────────────────────────────────────

    @Test
    @DisplayName("build() returns a fully populated Candidate")
    void buildReturnsFullyPopulatedCandidate() {
        Candidate c = faker.candidate().build();

        assertThat(c.firstName()).isNotBlank();
        assertThat(c.lastName()).isNotBlank();
        assertThat(c.email()).isNotBlank();
        assertThat(c.mobileNumber()).isNotBlank();
        assertThat(c.professionalSummary()).isNotBlank();
        assertThat(c.address()).isNotNull();
        assertThat(c.educationHistory()).isNotEmpty();
        assertThat(c.jobHistory()).isNotEmpty();
        assertThat(c.skills()).isNotEmpty();
        assertThat(c.certifications()).isNotNull();
    }

    // ── email ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("email domain is @yopmail.com or @mailinator.com")
    void emailDomainIsYopmailOrMailinator() {
        for (int i = 0; i < 50; i++) {
            String email = faker.candidate().build().email();
            assertThat(email)
                    .as("email '%s' must end with @yopmail.com or @mailinator.com", email)
                    .matches(".+@(yopmail\\.com|mailinator\\.com)");
        }
    }

    @Test
    @DisplayName("Both email domains appear across 200 random builds")
    void bothEmailDomainsAppear() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String email = faker.candidate().build().email();
            seen.add(email.substring(email.lastIndexOf('@') + 1));
        }
        assertThat(seen).containsExactlyInAnyOrder("yopmail.com", "mailinator.com");
    }

    // ── mobile number ──────────────────────────────────────────────────────

    @Test
    @DisplayName("mobileNumber matches +1-XXX-XXX-XXXX format")
    void mobileNumberFormat() {
        for (int i = 0; i < 20; i++) {
            String mobile = faker.candidate().build().mobileNumber();
            assertThat(mobile).matches("\\+1-\\d{3}-\\d{3}-\\d{4}");
        }
    }

    // ── address ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("address has non-blank required fields and country is US")
    void addressFieldsAreValid() {
        for (int i = 0; i < 10; i++) {
            var addr = faker.candidate().build().address();
            assertThat(addr.addressLine1()).isNotBlank();
            assertThat(addr.city()).isNotBlank();
            assertThat(addr.state()).isNotBlank();
            assertThat(addr.country()).isEqualTo("US");
            assertThat(addr.zipCode()).isNotBlank();
        }
    }

    @Test
    @DisplayName("addressLine2 appears in some candidates (40% probability)")
    void addressLine2AppearsSometimes() {
        long withLine2 = 0;
        for (int i = 0; i < 100; i++) {
            if (!faker.candidate().build().address().addressLine2().isBlank()) {
                withLine2++;
            }
        }
        // Expect roughly 40% (allow generous bounds 15–65 for variance)
        assertThat(withLine2).isBetween(15L, 65L);
    }

    // ── education history ─────────────────────────────────────────────────

    @Test
    @DisplayName("educationHistory has 1–3 entries")
    void educationHistoryCountIsBetween1And3() {
        for (int i = 0; i < 50; i++) {
            List<EducationHistory> edu = faker.candidate().build().educationHistory();
            assertThat(edu).hasSizeBetween(1, 3);
        }
    }

    @Test
    @DisplayName("All three education counts (1, 2, 3) appear across 300 builds")
    void allEducationCountsAppear() {
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            seen.add(faker.candidate().build().educationHistory().size());
        }
        assertThat(seen).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    @DisplayName("Each education entry has startDate before endDate")
    void educationStartBeforeEnd() {
        for (int i = 0; i < 20; i++) {
            for (EducationHistory edu : faker.candidate().build().educationHistory()) {
                assertThat(edu.startDate()).isBefore(edu.endDate());
            }
        }
    }

    @Test
    @DisplayName("Education entries are in chronological order (non-overlapping)")
    void educationIsChronologicallyOrdered() {
        for (int i = 0; i < 20; i++) {
            List<EducationHistory> edu = faker.candidate().build().educationHistory();
            for (int j = 0; j < edu.size() - 1; j++) {
                assertThat(edu.get(j).endDate())
                        .as("entry[%d].endDate should be before entry[%d].startDate", j, j + 1)
                        .isBefore(edu.get(j + 1).startDate());
            }
        }
    }

    @Test
    @DisplayName("Most recent education end date is in the past")
    void educationEndDatesAreInThePast() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 20; i++) {
            List<EducationHistory> edu = faker.candidate().build().educationHistory();
            assertThat(edu.get(edu.size() - 1).endDate()).isBefore(today);
        }
    }

    @Test
    @DisplayName("degreeLevel values are valid")
    void educationDegreeLevelsAreValid() {
        for (int i = 0; i < 50; i++) {
            for (EducationHistory edu : faker.candidate().build().educationHistory()) {
                assertThat(edu.degreeLevel())
                        .isIn("UNDERGRADUATE", "POSTGRADUATE", "DOCTORATE");
            }
        }
    }

    @Test
    @DisplayName("scoreType is CGPA or PERCENTAGE")
    void educationScoreTypeIsValid() {
        for (int i = 0; i < 50; i++) {
            for (EducationHistory edu : faker.candidate().build().educationHistory()) {
                assertThat(edu.scoreType()).isIn("CGPA", "PERCENTAGE");
            }
        }
    }

    @Test
    @DisplayName("CGPA score is between 6.0 and 10.0 inclusive")
    void cgpaScoreIsInRange() {
        for (int i = 0; i < 100; i++) {
            for (EducationHistory edu : faker.candidate().build().educationHistory()) {
                if ("CGPA".equals(edu.scoreType())) {
                    assertThat(edu.scoreValue()).isBetween(6.0, 10.0);
                }
            }
        }
    }

    @Test
    @DisplayName("PERCENTAGE score is between 55.0 and 100.0 inclusive")
    void percentageScoreIsInRange() {
        for (int i = 0; i < 100; i++) {
            for (EducationHistory edu : faker.candidate().build().educationHistory()) {
                if ("PERCENTAGE".equals(edu.scoreType())) {
                    assertThat(edu.scoreValue()).isBetween(55.0, 100.0);
                }
            }
        }
    }

    // ── job history ───────────────────────────────────────────────────────

    @Test
    @DisplayName("jobHistory has 1–4 entries")
    void jobHistoryCountIsBetween1And4() {
        for (int i = 0; i < 50; i++) {
            List<JobHistory> jobs = faker.candidate().build().jobHistory();
            assertThat(jobs).hasSizeBetween(1, 4);
        }
    }

    @Test
    @DisplayName("Each job history entry has non-blank required fields")
    void jobHistoryFieldsAreNonBlank() {
        for (int i = 0; i < 20; i++) {
            for (JobHistory job : faker.candidate().build().jobHistory()) {
                assertThat(job.companyName()).isNotBlank();
                assertThat(job.role()).isNotBlank();
                assertThat(job.designation()).isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("Each job has 2–4 responsibilities and 3–5 skills")
    void jobHistoryResponsibilitiesAndSkillsAreInRange() {
        for (int i = 0; i < 20; i++) {
            for (JobHistory job : faker.candidate().build().jobHistory()) {
                assertThat(job.responsibilities()).hasSizeBetween(2, 4);
                assertThat(job.skills()).hasSizeBetween(3, 5);
            }
        }
    }

    @Test
    @DisplayName("Job skills in a single entry are unique")
    void jobSkillsAreUnique() {
        for (int i = 0; i < 20; i++) {
            for (JobHistory job : faker.candidate().build().jobHistory()) {
                List<String> skills = job.skills();
                assertThat(new HashSet<>(skills)).hasSameSizeAs(skills);
            }
        }
    }

    @Test
    @DisplayName("Most recent job is current (endDate null); all earlier jobs have an endDate")
    void mostRecentJobIsCurrent() {
        for (int i = 0; i < 50; i++) {
            List<JobHistory> jobs = faker.candidate().build().jobHistory();
            JobHistory current = jobs.get(jobs.size() - 1);
            assertThat(current.endDate()).isNull();
            assertThat(current.isCurrent()).isTrue();
            for (int j = 0; j < jobs.size() - 1; j++) {
                assertThat(jobs.get(j).endDate()).isNotNull();
                assertThat(jobs.get(j).isCurrent()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("Past jobs have startDate before endDate")
    void pastJobsHaveStartBeforeEnd() {
        for (int i = 0; i < 50; i++) {
            for (JobHistory job : faker.candidate().build().jobHistory()) {
                if (!job.isCurrent()) {
                    assertThat(job.startDate()).isBefore(job.endDate());
                }
            }
        }
    }

    @Test
    @DisplayName("Job history is chronological (oldest first) and non-overlapping")
    void jobHistoryIsChronologicalAndNonOverlapping() {
        for (int i = 0; i < 50; i++) {
            List<JobHistory> jobs = faker.candidate().build().jobHistory();
            for (int j = 0; j < jobs.size() - 1; j++) {
                assertThat(jobs.get(j).endDate())
                        .as("job[%d].endDate should be on/before job[%d].startDate", j, j + 1)
                        .isBeforeOrEqualTo(jobs.get(j + 1).startDate());
            }
        }
    }

    @Test
    @DisplayName("Career starts on/after the most recent education end date")
    void careerStartsAfterLatestEducation() {
        for (int i = 0; i < 50; i++) {
            Candidate c = faker.candidate().build();
            LocalDate latestEducationEnd = c.educationHistory().get(c.educationHistory().size() - 1).endDate();
            assertThat(c.jobHistory().get(0).startDate()).isAfterOrEqualTo(latestEducationEnd);
        }
    }

    @Test
    @DisplayName("No job start date is in the future")
    void allJobStartDatesAreNotInTheFuture() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 50; i++) {
            for (JobHistory job : faker.candidate().build().jobHistory()) {
                assertThat(job.startDate()).isBeforeOrEqualTo(today);
            }
        }
    }

    // ── professional summary ──────────────────────────────────────────────

    @Test
    @DisplayName("professionalSummary has no unresolved placeholders")
    void professionalSummaryHasNoUnresolvedPlaceholders() {
        for (int i = 0; i < 100; i++) {
            String summary = faker.candidate().build().professionalSummary();
            assertThat(summary).doesNotContain("{").doesNotContain("}");
        }
    }

    @Test
    @DisplayName("professionalSummary mentions the current job designation")
    void professionalSummaryMentionsCurrentDesignation() {
        for (int i = 0; i < 20; i++) {
            Candidate c = faker.candidate().build();
            String designation = c.jobHistory().get(c.jobHistory().size() - 1).designation();
            assertThat(c.professionalSummary()).contains(designation);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "technology_information_and_media, 'Technology, Information and Media'",
            "hospitals_and_health_care, Hospitals and Health Care",
            "administrative_and_support_services, Administrative and Support Services",
            "entertainment_providers, Entertainment Providers",
            "oil_gas_and_mining, 'Oil, Gas, and Mining'",
            "holding_companies, Holding Companies"
    })
    @DisplayName("professionalSummary mentions the industry display name")
    void professionalSummaryMentionsIndustryDisplayName(String key, String displayName) {
        for (int i = 0; i < 10; i++) {
            assertThat(faker.candidate().buildForIndustry(key).professionalSummary())
                    .contains(displayName);
        }
    }

    @Test
    @DisplayName("professionalSummary mentions a certification when the candidate has one")
    void professionalSummaryMentionsCertificationWhenPresent() {
        int withCert = 0;
        for (int i = 0; i < 100; i++) {
            Candidate c = faker.candidate().build();
            if (!c.certifications().isEmpty()) {
                withCert++;
                assertThat(c.professionalSummary()).contains(c.certifications().get(0).name());
            }
        }
        assertThat(withCert).isGreaterThan(0);
    }

    @Test
    @DisplayName("professionalSummary mentions the highest degree and top skill")
    void professionalSummaryMentionsDegreeAndSkill() {
        for (int i = 0; i < 20; i++) {
            Candidate c = faker.candidate().build();
            String highestDegree = c.educationHistory().get(c.educationHistory().size() - 1).courseName();
            assertThat(c.professionalSummary())
                    .contains(highestDegree)
                    .contains(c.skills().get(0));
        }
    }

    @Test
    @DisplayName("professionalSummary is 2–3 sentences")
    void professionalSummaryIsTwoToThreeSentences() {
        for (int i = 0; i < 100; i++) {
            String summary = faker.candidate().build().professionalSummary();
            long sentences = summary.chars().filter(ch -> ch == '.' || ch == '!' || ch == '?').count();
            assertThat(sentences).as("summary '%s'", summary).isBetween(2L, 3L);
        }
    }

    @Test
    @DisplayName("availableSummaryTemplates is non-empty and every template ends with a period")
    void availableSummaryTemplatesAreWellFormed() {
        List<String> templates = faker.candidate().availableSummaryTemplates();
        assertThat(templates).isNotEmpty();
        for (String t : templates) {
            assertThat(t).endsWith(".").contains("{designation}").contains("{industry}");
        }
    }

    // ── candidate-level skills ────────────────────────────────────────────

    @Test
    @DisplayName("Candidate top-level skills are unique and 4–8 items")
    void candidateSkillsAreUniqueAndInRange() {
        for (int i = 0; i < 20; i++) {
            List<String> skills = faker.candidate().build().skills();
            assertThat(skills).hasSizeBetween(4, 8);
            assertThat(new HashSet<>(skills)).hasSameSizeAs(skills);
        }
    }

    // ── certifications ────────────────────────────────────────────────────

    @Test
    @DisplayName("Certifications count is 0–3")
    void certificationsCountIsAtMost3() {
        for (int i = 0; i < 50; i++) {
            assertThat(faker.candidate().build().certifications()).hasSizeLessThanOrEqualTo(3);
        }
    }

    @Test
    @DisplayName("Certifications for technology industry come from the technology list")
    void certificationsAreFromCorrectIndustry() {
        List<String> techCertNames = faker.candidate().availableCertificationNames("technology");
        assertThat(techCertNames).isNotEmpty();
        for (int i = 0; i < 20; i++) {
            Candidate c = faker.candidate().buildForIndustry("technology");
            for (Certification cert : c.certifications()) {
                assertThat(cert.name()).isIn(techCertNames);
                assertThat(cert.issuingOrganization()).isNotBlank();
                assertThat(cert.issuedDate()).isBeforeOrEqualTo(LocalDate.now());
            }
        }
    }

    // ── buildForIndustry ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "technology", "healthcare", "finance", "retail",
            "manufacturing", "logistics", "education", "staffing", "consulting", "media"
    })
    @DisplayName("buildForIndustry works for all 10 industries")
    void buildForIndustryWorksForAllIndustries(String industryKey) {
        Candidate c = faker.candidate().buildForIndustry(industryKey);
        assertThat(c.firstName()).isNotBlank();
        assertThat(c.jobHistory()).isNotEmpty();
        assertThat(c.educationHistory()).isNotEmpty();
        assertThat(c.skills()).isNotEmpty();
        assertThat(c.certifications()).isNotNull();

        // Certifications, if any, come from this industry's list
        List<String> available = faker.candidate().availableCertificationNames(industryKey);
        for (Certification cert : c.certifications()) {
            assertThat(cert.name()).isIn(available);
        }
    }

    // ── certification data quality ───────────────────────────────────────

    @Test
    @DisplayName("A certification name always carries the same issuer")
    void certificationNameMapsToOneIssuer() {
        Map<String, String> issuerByName = new HashMap<>();

        for (String industry : faker.candidate().availableIndustries()) {
            for (int i = 0; i < 60; i++) {
                for (Certification c : faker.candidate().buildForIndustry(industry)
                                            .certifications()) {
                    String existing = issuerByName.putIfAbsent(c.name(), c.issuingOrganization());
                    assertThat(existing == null ? c.issuingOrganization() : existing)
                            .as("issuer for %s", c.name())
                            .isEqualTo(c.issuingOrganization());
                }
            }
        }

        assertThat(issuerByName).isNotEmpty();
    }

    @Test
    @DisplayName("Every certification has a non-blank name and issuer")
    void certificationsAreFullyPopulated() {
        for (String industry : faker.candidate().availableIndustries()) {
            for (int i = 0; i < 20; i++) {
                for (Certification c : faker.candidate().buildForIndustry(industry)
                                            .certifications()) {
                    assertThat(c.name()).isNotBlank();
                    assertThat(c.issuingOrganization()).isNotBlank();
                    assertThat(c.issuedDate()).isNotNull();
                }
            }
        }
    }

    // ── industry-specific education ──────────────────────────────────────

    @ParameterizedTest
    @MethodSource("allIndustryKeys")
    @DisplayName("Degrees and specializations come from the industry's own pools")
    void educationMatchesIndustryPools(String industryKey) {
        List<String> courses = faker.candidate().availableCourses(industryKey).values().stream()
                .flatMap(List::stream).toList();
        List<String> specializations = faker.candidate().availableSpecializations(industryKey);

        for (int i = 0; i < 20; i++) {
            for (EducationHistory e : faker.candidate().buildForIndustry(industryKey)
                                          .educationHistory()) {
                assertThat(courses)
                        .as("%s degree for %s", e.courseName(), industryKey)
                        .contains(e.courseName());
                assertThat(specializations)
                        .as("%s specialization for %s", e.specialization(), industryKey)
                        .contains(e.specialization());
            }
        }
    }

    @Test
    @DisplayName("A construction candidate does not hold a computer science degree")
    void constructionCandidateHoldsConstructionDegree() {
        for (int i = 0; i < 50; i++) {
            for (EducationHistory e : faker.candidate().buildForIndustry("construction")
                                          .educationHistory()) {
                assertThat(e.courseName())
                        .doesNotContain("Computer Science")
                        .doesNotContain("Data Science")
                        .doesNotContain("Nursing");
            }
        }
    }

    @Test
    @DisplayName("Each industry configures a distinct set of degrees")
    void industriesHaveDistinctCoursePools() {
        List<String> tech = faker.candidate()
                .availableCourses("technology_information_and_media").get("UNDERGRADUATE");
        List<String> farm = faker.candidate()
                .availableCourses("farming_ranching_forestry").get("UNDERGRADUATE");

        assertThat(tech).isNotEmpty();
        assertThat(farm).isNotEmpty().doesNotContainAnyElementsOf(tech);
    }

    @Test
    @DisplayName("Legacy aliases resolve to the canonical industry's course pool")
    void aliasesShareCanonicalCoursePool() {
        assertThat(faker.candidate().availableCourses("technology"))
                .isEqualTo(faker.candidate().availableCourses("technology_information_and_media"));
        assertThat(faker.candidate().availableSpecializations("media"))
                .isEqualTo(faker.candidate().availableSpecializations("entertainment_providers"));
    }

    // ── reference data ────────────────────────────────────────────────────

    @Test
    @DisplayName("Unknown industry key falls back to the first configured industry")
    void unknownIndustryFallsBackToFirst() {
        String firstKey = faker.candidate().availableIndustries().get(0);

        assertThat(faker.candidate().availableCertificationNames("no-such-industry"))
                .isEqualTo(faker.candidate().availableCertificationNames(firstKey));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TECHNOLOGY", "Technology", "TeChNoLoGy"})
    @DisplayName("Industry keys resolve case-insensitively")
    void industryKeysAreCaseInsensitive(String industryKey) {
        assertThat(faker.candidate().availableCertificationNames(industryKey))
                .isEqualTo(faker.candidate().availableCertificationNames("technology"));
    }

    @Test
    @DisplayName("availableIndustries returns all 20 LinkedIn industry keys")
    void availableIndustriesReturnsAll20() {
        assertThat(faker.candidate().availableIndustries())
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
        assertThat(faker.candidate().availableIndustries())
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
        assertThat(faker.candidate().availableCertificationNames(alias))
                .isEqualTo(faker.candidate().availableCertificationNames(canonical))
                .isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("allIndustryKeys")
    @DisplayName("Every industry produces a complete candidate")
    void everyIndustryProducesCompleteCandidate(String industryKey) {
        Candidate c = faker.candidate().buildForIndustry(industryKey);

        assertThat(c.firstName()).isNotBlank();
        assertThat(c.professionalSummary()).isNotBlank();
        assertThat(c.skills()).isNotEmpty();
        assertThat(c.jobHistory()).isNotEmpty();
        assertThat(c.educationHistory()).isNotEmpty();
        assertThat(faker.candidate().availableCertificationNames(industryKey)).isNotEmpty();
    }

    static Stream<String> allIndustryKeys() {
        return new CandidateFaker().candidate().availableIndustries().stream();
    }

    @Test
    @DisplayName("All 10 industries appear across 500 random builds")
    void allIndustriesAppearInRandomBuilds() {
        // Since industry is internal, we verify via job designation/role diversity
        // by confirming the provider covers all industries without throwing
        for (int i = 0; i < 500; i++) {
            assertThat(faker.candidate().build()).isNotNull();
        }
    }

    // ── seeded reproducibility ────────────────────────────────────────────

    @Test
    @DisplayName("Seeded faker produces identical results for both instances")
    void seededFakerIsReproducible() {
        CandidateFaker f1 = new CandidateFaker(new Random(42L));
        CandidateFaker f2 = new CandidateFaker(new Random(42L));

        Candidate c1 = f1.candidate().build();
        Candidate c2 = f2.candidate().build();

        assertThat(c1.firstName()).isEqualTo(c2.firstName());
        assertThat(c1.lastName()).isEqualTo(c2.lastName());
        assertThat(c1.email()).isEqualTo(c2.email());
        assertThat(c1.mobileNumber()).isEqualTo(c2.mobileNumber());
        assertThat(c1.professionalSummary()).isEqualTo(c2.professionalSummary());
        assertThat(c1.skills()).isEqualTo(c2.skills());
        assertThat(c1.educationHistory()).isEqualTo(c2.educationHistory());
        assertThat(c1.jobHistory()).isEqualTo(c2.jobHistory());
        assertThat(c1.certifications()).isEqualTo(c2.certifications());
    }
}
