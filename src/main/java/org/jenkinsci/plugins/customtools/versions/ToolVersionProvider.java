/*
 * Copyright 2014 Oleg Nenashev <o.v.nenashev@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jenkinsci.plugins.customtools.versions;

import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersion;
import hudson.model.Describable;
import hudson.model.ParameterDefinition;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 * An extension point, which allows to specify parameter definitions for
 * {@link ToolVersion}s.
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 * @since TODO
 */
public abstract class ToolVersionProvider implements Describable<ToolVersionProvider> {
    /**
     * Gets the parameter definition to be displayed on the configuration page.
     * @return Parameter definition
     */
    public abstract @Nonnull ParameterDefinition getParameterDefinition();
      
    @Override
    public ToolVersionProviderDescriptor getDescriptor() {
        return (ToolVersionProviderDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
}
