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

package org.jenkinsci.plugins.customtools;

import com.cloudbees.jenkins.plugins.customtools.CustomToolInstallWrapper;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Provides the tool installation build step.
 * This {@link Builder} allows to install custom tools on-demand.
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 * @since TODO: define a version
 */
public class CustomToolInstallStep extends Builder {

    private final @Nonnull CustomToolInstallWrapper.SelectedTool[] selectedTools;

    @DataBoundConstructor
    public CustomToolInstallStep(CustomToolInstallWrapper.SelectedTool[] selectedTools) {
        this.selectedTools = (selectedTools != null) 
                ? selectedTools 
                : new CustomToolInstallWrapper.SelectedTool[0];
    }

    public @Nonnull CustomToolInstallWrapper.SelectedTool[] getSelectedTools() {
        return selectedTools;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
       return true;
    }
    
}
