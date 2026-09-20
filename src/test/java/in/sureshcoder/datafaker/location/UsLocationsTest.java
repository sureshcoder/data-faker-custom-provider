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
package in.sureshcoder.datafaker.location;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Integrity of the shared us-locations.yml reference data. */
class UsLocationsTest {

    @Test
    @DisplayName("dataset is non-empty and unmodifiable")
    void datasetIsNonEmptyAndUnmodifiable() {
        assertThat(UsLocations.all()).isNotEmpty();
        assertThatThrownBy(() -> UsLocations.all().add(new UsLocation("X", "TX", List.of("00000"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("every state is a valid USPS abbreviation")
    void everyStateIsValid() {
        for (UsLocation loc : UsLocations.all()) {
            assertThat(UsLocations.validStates())
                    .as("state of %s", loc.cityState())
                    .contains(loc.state());
        }
    }

    @Test
    @DisplayName("all 50 states plus DC are represented")
    void allStatesAreCovered() {
        Set<String> covered = UsLocations.all().stream()
                .map(UsLocation::state)
                .collect(Collectors.toSet());
        assertThat(covered).hasSize(51).containsAll(UsLocations.validStates());
    }

    @Test
    @DisplayName("every state has at least two cities — DC has only one postal city name")
    void everyStateHasAtLeastTwoCities() {
        for (String state : UsLocations.validStates()) {
            long count = UsLocations.all().stream().filter(l -> l.state().equals(state)).count();
            long expected = "DC".equals(state) ? 1 : 2;
            assertThat(count).as("cities in %s", state).isGreaterThanOrEqualTo(expected);
        }
    }

    @Test
    @DisplayName("every location has at least one five-digit ZIP code")
    void zipCodesAreWellFormed() {
        for (UsLocation loc : UsLocations.all()) {
            assertThat(loc.zips()).as("ZIPs of %s", loc.cityState()).isNotEmpty();
            for (String zip : loc.zips()) {
                assertThat(zip).as("ZIP of %s", loc.cityState()).matches("\\d{5}");
            }
        }
    }

    @Test
    @DisplayName("a location's ZIP codes are unique and share its state's leading digit")
    void zipCodesAreConsistentWithinACity() {
        for (UsLocation loc : UsLocations.all()) {
            assertThat(loc.zips()).as("ZIPs of %s", loc.cityState()).doesNotHaveDuplicates();
            Set<Character> leading = loc.zips().stream()
                    .map(z -> z.charAt(0))
                    .collect(Collectors.toCollection(HashSet::new));
            assertThat(leading).as("ZIP regions of %s", loc.cityState()).hasSize(1);
        }
    }

    @Test
    @DisplayName("no duplicate city and state pairs")
    void noDuplicateCityStatePairs() {
        List<String> pairs = UsLocations.all().stream().map(UsLocation::cityState).toList();
        assertThat(pairs).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("cityState() renders as 'City, ST'")
    void cityStateRendersCorrectly() {
        assertThat(new UsLocation("Austin", "TX", List.of("78701")).cityState())
                .isEqualTo("Austin, TX");
        for (UsLocation loc : UsLocations.all()) {
            assertThat(loc.cityState()).isEqualTo(loc.city() + ", " + loc.state());
        }
    }
}
