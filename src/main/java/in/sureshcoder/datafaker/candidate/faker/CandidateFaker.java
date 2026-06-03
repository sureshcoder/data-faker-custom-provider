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
package in.sureshcoder.datafaker.candidate.faker;

import in.sureshcoder.datafaker.candidate.provider.CandidateProvider;
import net.datafaker.Faker;

import java.util.Locale;
import java.util.Random;

/**
 * Faker subclass that exposes the Candidate custom provider.
 *
 * Usage:
 *   CandidateFaker faker = new CandidateFaker();
 *   Candidate c = faker.candidate().build();
 *   Candidate techCandidate = faker.candidate().buildForIndustry("technology");
 *
 * Seeded (reproducible):
 *   CandidateFaker faker = new CandidateFaker(new Random(42L));
 */
public class CandidateFaker extends Faker {

    public CandidateFaker() {
        super();
    }

    public CandidateFaker(Locale locale) {
        super(locale);
    }

    public CandidateFaker(Random random) {
        super(random);
    }

    public CandidateFaker(Locale locale, Random random) {
        super(locale, random);
    }

    public CandidateProvider candidate() {
        return getProvider(CandidateProvider.class, CandidateProvider::new);
    }
}
