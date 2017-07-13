/*
 * Copyright 2012, CloudBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.jenkins.plugins.customtools;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstallation;
import hudson.util.StreamTaskListener;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.cloudbees.jenkins.plugins.customtools.CustomTool.DescriptorImpl;
import com.synopsys.arc.jenkinsci.plugins.customtools.LabelSpecifics;
import com.synopsys.arc.jenkinsci.plugins.customtools.multiconfig.MulticonfigWrapperOptions;
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionConfig;
import jenkins.model.Jenkins;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;


public class CustomToolInstallerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    private DescriptorImpl tools;

    @Test
    public void testSmoke() throws IOException, InterruptedException {
        VirtualChannel channel = Jenkins.getActiveInstance().getChannel();
        EnvVars envVars = new EnvVars();
        
        
        CustomTool installation = createTool("MyTrue");        
        
        TaskListener listener = new StreamTaskListener(System.out, Charset.defaultCharset());;
        installation = installation.forEnvironment(envVars).forNode(j.jenkins, listener);
        
        if (!new FilePath(channel, installation.getHome()).exists()) {
            throw new IOException(installation.getHome() + "doesn't exist!");
        }
    }

    @Test
    public void testBasicCase() throws Exception {
        j.jenkins.setNumExecutors(0);
        j.createSlave();
        tools = j.jenkins.getDescriptorByType(CustomTool.DescriptorImpl.class);
        tools.setInstallations(createTool("MyTrue"));
        FreeStyleProject project = j.createFreeStyleProject();
        CustomToolInstallWrapper.SelectedTool selectedTool = new CustomToolInstallWrapper.SelectedTool("MyTrue");
        
        CustomToolInstallWrapper wrapper = new CustomToolInstallWrapper(
                new CustomToolInstallWrapper.SelectedTool[] { selectedTool });
        project.getBuildWrappersList().add(wrapper);
        Builder b = new Shell("echo $PATH; mytrue");
        project.getBuildersList().add(b);
        Future<FreeStyleBuild> build = project.scheduleBuild2(0);
        j.assertBuildStatusSuccess(build);
            
    }
    
    //TODO: Just a stub for testing. Make the test automatic
    @Issue("JENKINS-19889")
    @Ignore @Test
    public void testAdditionalVars() throws Exception {
        j.jenkins.setNumExecutors(0);
        j.createSlave();
        tools = j.jenkins.getDescriptorByType(CustomTool.DescriptorImpl.class);
        tools.setInstallations(createEnvPrinterTool("MyTrue", null, "TEST_ADD_VAR=test"));
        FreeStyleProject project = j.createFreeStyleProject();
        CustomToolInstallWrapper.SelectedTool selectedTool = new CustomToolInstallWrapper.SelectedTool("MyTrue");
        
        CustomToolInstallWrapper wrapper = new CustomToolInstallWrapper(
                new CustomToolInstallWrapper.SelectedTool[] { selectedTool });
        project.getBuildWrappersList().add(wrapper);
        Builder b = new Shell("env; mytrue");
        project.getBuildersList().add(b);
        Future<FreeStyleBuild> build = project.scheduleBuild2(0);
        j.assertBuildStatusSuccess(build);          
    }
       
    public static CustomTool createTool(String name) throws IOException {
        List<ToolInstaller> installers = new ArrayList<ToolInstaller>();
        installers.add(new CommandInstaller(null, "ln -s `which true` mytrue",
                "./"));

        List<ToolProperty<ToolInstallation>> properties = new ArrayList<ToolProperty<ToolInstallation>>();
        properties.add(new InstallSourceProperty(installers));

        CustomTool installation = new CustomTool("MyTrue", null, properties, "./", null, ToolVersionConfig.DEFAULT, null);
        return installation;
    }
    
    //TODO: refactor and generalize
    private CustomTool createEnvPrinterTool(String name, LabelSpecifics[] specifics, String additionalVars) throws IOException {
        List<ToolInstaller> installers = new ArrayList<ToolInstaller>();
        installers.add(new CommandInstaller(null, "ln -s `which true` mytrue", "./"));

        List<ToolProperty<ToolInstallation>> properties = new ArrayList<ToolProperty<ToolInstallation>>();
        properties.add(new InstallSourceProperty(installers));

        CustomTool installation = new CustomTool(name, null, properties, "./", specifics, ToolVersionConfig.DEFAULT, additionalVars);
        return installation;
    }
    
}
