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

import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition;
import com.synopsys.arc.jenkinsci.plugins.customtools.Messages;
import hudson.EnvVars;
import hudson.Extension;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.plugins.customtools.util.versions.CustomToolVersionInfo;
import jenkins.plugins.customtools.util.versions.CustomToolVersionProvider;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Class implements support of versions for custom versions.
 * @author Oleg Nenashev
 * @since 0.4
 * @deprecated The implementation has been converted to entension point.
 *             Integration with Extended Choice Plugin is deprecated and will be removed soon.
 * @see CustomToolVersionProvider
 */
@Deprecated
@Restricted(NoExternalUse.class)
public class ToolVersionConfig extends CustomToolVersionProvider
        implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(ToolVersionConfig.class.getName());

    @Deprecated
    public static final ToolVersionConfig DEFAULT = null;
    private final @Nonnull ExtendedChoiceParameterDefinition versionsListSource;

    @DataBoundConstructor
    public ToolVersionConfig(@Nonnull ExtendedChoiceParameterDefinition versionsListSource) {
        this.versionsListSource = versionsListSource;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        // This override is required to keep binary compatibility with versions before 0.6
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public CustomToolVersionInfo resolveToolVersion(@Nonnull CustomTool tool, @Nonnull Node node,
                                                    @CheckForNull Run<?, ?> run, @CheckForNull EnvVars extraEnvVars,
                                                    @Nonnull TaskListener listener) {
        return ToolVersion.getEffectiveToolVersion(tool, extraEnvVars, node);
    }

    public @Nonnull ExtendedChoiceParameterDefinition getVersionsListSource() {
        return versionsListSource;
    }
     
    @Extension(optional = true)
    public static class DescriptorImpl extends ToolVersionProviderDescriptor {

        static {
            // Prevents extension from loading if the Extended Choice Plugin is not installed
            Class<?> clazz = ExtendedChoiceParameterDefinition.class;
            LOGGER.log(Level.WARNING, "Loading Custom Tool Version Provider based on Extended Choice Plugin's {0}. " +
                "Note that this version provider is deprecated and not guaranteed to be stable.", clazz.getName());
        }

        @Override
        public String getDisplayName() {
            return Messages.Versions_ToolVersionConfig_DisplayName();
        }
    }


    /**
     * Compatibility method, which retrieves old version structure from Custom Tools Plugin 1.x
     * @param tool Custom Tool
     * @return Tool Version. {@code null} if it is not defined or if it is a new {@link CustomToolVersionProvider} type
     */
    @CheckForNull
    /**package*/ static ToolVersionConfig forTool(@Nonnull CustomTool tool) {
        final CustomToolVersionProvider provider = tool.getToolVersion();
        if (provider instanceof ToolVersionConfig) {
            return (ToolVersionConfig)provider;
        }
        return null;
    }
}
