/*
 * Copyright 2013 Oleg Nenashev, Synopsys Inc..
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
package com.synopsys.arc.jenkinsci.plugins.customtools.versions;

import com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition;
import com.synopsys.arc.jenkinsci.plugins.customtools.Messages;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import java.io.Serializable;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Class implements support of versions for custom tools. 
 * @author Oleg Nenashev
 * @since 0.4
 */
public class ToolVersionConfig extends AbstractDescribableImpl<ToolVersionConfig>
        implements Serializable {

    public static final ToolVersionConfig DEFAULT = null;
    private final @Nonnull ExtendedChoiceParameterDefinition versionsListSource;

    @DataBoundConstructor
    public ToolVersionConfig(@Nonnull ExtendedChoiceParameterDefinition versionsListSource) {
        this.versionsListSource = versionsListSource;
    }

    public @Nonnull ExtendedChoiceParameterDefinition getVersionsListSource() {
        return versionsListSource;
    }
     
    @Extension
    public static class DescriptorImpl extends Descriptor<ToolVersionConfig> {
        @Override
        public String getDisplayName() {
            return Messages.Versions_ToolVersionConfig_DisplayName();
        }
    }
}
