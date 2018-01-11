/*
 * Copyright 2013 Oleg Nenashev, Synopsys Inc.
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
package com.synopsys.arc.jenkinsci.plugins.customtools.multiconfig;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.matrix.MatrixProject;
import java.io.Serializable;

import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Provides specific options for {@link MatrixProject}s.
 * @author Oleg Nenashev
 */
public class MulticonfigWrapperOptions implements Serializable, Describable<MulticonfigWrapperOptions>, ExtensionPoint {
    private final boolean skipMasterInstallation;
    public static final MulticonfigWrapperOptions DEFAULT = new MulticonfigWrapperOptions(false);

    @DataBoundConstructor
    public MulticonfigWrapperOptions(boolean skipInstallationOnMaster) {
        this.skipMasterInstallation = skipInstallationOnMaster;
    }

    @Deprecated
    public boolean isSkipMasterInstallation() {
        return skipMasterInstallation;
    }

    public boolean isSkipInstallationOnMaster() {
        return skipMasterInstallation;
    }

    @Override
    public MulticonfigWrapperOptionsDescriptor getDescriptor() {
        return (MulticonfigWrapperOptionsDescriptor) Jenkins.getActiveInstance().getDescriptor(getClass());
    }

    @Symbol("multiconfigToolOptions")
    @Extension
    public static class MulticonfigWrapperOptionsDescriptor extends Descriptor<MulticonfigWrapperOptions> {

        @Override
        public String getDisplayName() {
            return "MultiConfig wrapper options";
        }
    }
}
