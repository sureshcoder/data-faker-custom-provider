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

import java.util.List;

/**
 * A real US city, its state abbreviation, and ZIP codes actually assigned to that city.
 * Drawing a city and then one of its own ZIPs keeps the generated triple geographically coherent.
 */
public record UsLocation(String city, String state, List<String> zips) {

    /** Renders the location as "Austin, TX" — the form used for job posting locations. */
    public String cityState() {
        return city + ", " + state;
    }
}
