/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.customtools;

import com.google.common.collect.ImmutableSet;
import com.keypoint.PngEncoder;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A workflow step for installing a single custom tool
 *
 * @author Alex Johnson
 */
public class CustomToolStep extends CoreWrapperStep {

    private String name;
    private String version;

    @DataBoundConstructor
    public CustomToolStep (String name){
        super(new CustomToolInstallWrapper(
                new CustomToolInstallWrapper.SelectedTool[]{new CustomToolInstallWrapper.SelectedTool(name)
            }));
        this.name = name;
    }

    @DataBoundSetter
    public void setVersion (String version) {
        this.version = version;
    }

    public String getVersion () {
        return this.version;
    }

    public String getName () {
        return this.name;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, Launcher.class, TaskListener.class);
        }

        public String[] getInstallations() {
            CustomTool[] tools =
                    Jenkins.getActiveInstance().getDescriptorByType(CustomTool.DescriptorImpl.class).getInstallations();
            List<String> toolNames = new ArrayList<String>();
            for (CustomTool c : tools) {
                toolNames.add(c.getName());
            }
            return toolNames.toArray(new String[]{});
        }

        @Override
        public String getFunctionName() {
            return "customTool";
        }

        @Override
        public String getDisplayName() {
            return "Installs a custom tool";
        }
    }
}
