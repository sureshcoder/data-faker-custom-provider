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
package in.sureshcoder.datafaker.jobposting.faker;

import in.sureshcoder.datafaker.jobposting.provider.JobPostingProvider;
import net.datafaker.Faker;

import java.util.Locale;
import java.util.Random;

/**
 * Faker subclass that exposes the JobPosting custom provider.
 *
 * Usage:
 *   JobFaker faker = new JobFaker();
 *   JobPosting job = faker.jobPosting().build();
 *   JobPosting techJob = faker.jobPosting().buildForIndustry("technology");
 *
 * Seeded (reproducible):
 *   JobFaker faker = new JobFaker(new Random(42L));
 */
public class JobFaker extends Faker {

    public JobFaker() {
        super();
    }

    public JobFaker(Locale locale) {
        super(locale);
    }

    public JobFaker(Random random) {
        super(random);
    }

    public JobFaker(Locale locale, Random random) {
        super(locale, random);
    }

    public JobPostingProvider jobPosting() {
        // DataFaker 2.x: getProvider(Class<AP>, Function<PR, AP>) — constructor ref provides the function
        return getProvider(JobPostingProvider.class, JobPostingProvider::new);
    }
}
