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
package in.sureshcoder.datafaker.candidate.model;

import java.time.LocalDate;

/**
 * One completed academic qualification.
 * degreeLevel values: UNDERGRADUATE, POSTGRADUATE, DOCTORATE.
 * scoreType values: CGPA (0.0–10.0), PERCENTAGE (0.0–100.0).
 */
public record EducationHistory(
        String institutionName,
        String degreeLevel,
        String courseName,
        String specialization,
        LocalDate startDate,
        LocalDate endDate,
        String scoreType,
        double scoreValue
) {}
