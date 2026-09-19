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

import java.util.List;
import java.util.Map;

/**
 * All static reference data for one industry, loaded from candidate-mappings.yml.
 *
 * <p>{@code courses} and {@code specializations} are optional per-industry overrides: an empty
 * map or list means the industry falls back to the top-level pools.
 */
record CandidateIndustryData(
        String displayName,
        List<String> roles,
        List<String> designations,
        List<String> skills,
        List<String> responsibilities,
        List<CertificationData> certifications,
        Map<String, List<String>> courses,
        List<String> specializations
) {}
