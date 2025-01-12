/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.apm.agent.ecs_logging;

import co.elastic.apm.agent.bci.TracerAwareInstrumentation;
import co.elastic.apm.agent.bci.bytebuddy.CustomElementMatchers;
import co.elastic.apm.agent.loginstr.correlation.CorrelationIdMapAdapter;
import co.elastic.logging.jul.EcsFormatter;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Instruments {@link EcsFormatter#getMdcEntries()} to provide correlation IDs at runtime.
 * Application(s) copies of ecs-logging and the one in the agent will be instrumented, hence providing log correlation
 * for all of them.
 */
@SuppressWarnings("JavadocReference")
public class JulEcsFormatterInstrumentation extends TracerAwareInstrumentation {

    @Override
    public ElementMatcher.Junction<ClassLoader> getClassLoaderMatcher() {
        // ECS formatter that is loaded within the agent should not be instrumented
        return not(CustomElementMatchers.isInternalPluginClassLoader());
    }

    @Override
    public Collection<String> getInstrumentationGroupNames() {
        return Arrays.asList("logging", "jul-ecs");
    }

    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return named("co.elastic.logging.jul.EcsFormatter");
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("getMdcEntries");
    }

    public static class AdviceClass {

        @Advice.AssignReturned.ToReturned
        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class, inline = false)
        public static Map<String, String> onExit() {
            return CorrelationIdMapAdapter.get();
        }

    }


}
