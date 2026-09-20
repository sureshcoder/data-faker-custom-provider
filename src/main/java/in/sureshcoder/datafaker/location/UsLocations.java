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

import net.datafaker.service.RandomService;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared, immutable reference table of real US city / state / ZIP combinations, loaded once from
 * {@code us-locations.yml} at class-init. Both the Candidate and JobPosting providers draw from it
 * so their geography is genuine rather than the independent, incoherent picks DataFaker's own
 * {@code address()} provider yields for the {@code en} locale.
 */
public final class UsLocations {

    /** The 50 states plus DC, as USPS two-letter abbreviations. */
    private static final Set<String> VALID_STATES = Set.of(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL",
            "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME",
            "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH",
            "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI",
            "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY");

    private static final List<UsLocation> ALL;

    static {
        ALL = parse(loadYaml());
    }

    private UsLocations() {
    }

    /** Returns every configured location, in YAML document order. Unmodifiable. */
    public static List<UsLocation> all() {
        return ALL;
    }

    /** Returns the states covered by the dataset, as USPS abbreviations. */
    public static Set<String> validStates() {
        return VALID_STATES;
    }

    /**
     * Draws a location using the faker's own {@link RandomService}, so seeded fakers stay
     * reproducible.
     */
    public static UsLocation pick(RandomService random) {
        return ALL.get(random.nextInt(ALL.size()));
    }

    /** Draws one of the location's own ZIP codes, again via the faker's {@link RandomService}. */
    public static String pickZip(UsLocation location, RandomService random) {
        List<String> zips = location.zips();
        return zips.get(random.nextInt(zips.size()));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadYaml() {
        Yaml yaml = new Yaml();
        try (InputStream is = UsLocations.class.getClassLoader()
                .getResourceAsStream("us-locations.yml")) {
            if (is == null) {
                throw new IllegalStateException("us-locations.yml not found on classpath");
            }
            Map<String, Object> root = yaml.load(is);
            Object raw = root.get("locations");
            if (raw == null) {
                throw new IllegalStateException("us-locations.yml has no 'locations' block");
            }
            return (List<Map<String, Object>>) raw;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load us-locations.yml", e);
        }
    }

    /**
     * Validates and materialises the YAML entries. Bad reference data is a configuration error, so
     * it fails at class-init rather than surfacing as a wrong-looking address much later.
     */
    @SuppressWarnings("unchecked")
    private static List<UsLocation> parse(List<Map<String, Object>> raw) {
        List<UsLocation> result = new ArrayList<>(raw.size());
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> entry : raw) {
            String city  = (String) entry.get("city");
            String state = (String) entry.get("state");
            List<String> zips = (List<String>) entry.get("zips");

            if (city == null || city.isBlank()) {
                throw new IllegalStateException("us-locations.yml has an entry without a city");
            }
            if (!VALID_STATES.contains(state)) {
                throw new IllegalStateException(
                        "City '" + city + "' has an unknown state abbreviation: " + state);
            }
            if (zips == null || zips.isEmpty()) {
                throw new IllegalStateException("City '" + city + ", " + state + "' has no ZIP codes");
            }
            for (String zip : zips) {
                if (zip == null || !zip.matches("\\d{5}")) {
                    throw new IllegalStateException(
                            "City '" + city + ", " + state + "' has a malformed ZIP code: " + zip);
                }
            }
            if (!seen.add(city + ", " + state)) {
                throw new IllegalStateException("Duplicate location: " + city + ", " + state);
            }
            result.add(new UsLocation(city, state, List.copyOf(zips)));
        }
        return Collections.unmodifiableList(result);
    }
}
