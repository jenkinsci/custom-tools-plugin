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

package com.synopsys.arc.jenkins.plugins.customtools.versions;

import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition;
import com.synopsys.arc.jenkins.plugins.customtools.util.CLICommandInvoker;
import static com.synopsys.arc.jenkins.plugins.customtools.util.CLICommandInvoker.Matcher.succeeded;
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionConfig;
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionParameterDefinition;
import hudson.cli.BuildCommand;
import hudson.model.Executor;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.Slave;
import hudson.model.StringParameterDefinition;
import hudson.slaves.DumbSlave;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;

/**
 *
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 */
public class ToolVersionParameterDefinitionTest {
    
    @Rule public JenkinsRule r = new JenkinsRule();
    private CLICommandInvoker command;
    
    private static final String TEST_TOOL_NAME="test";
    private static final ToolVersionConfig versionConfig = new ToolVersionConfig(
            new ExtendedChoiceParameterDefinition("TOOL_VERSION", 
                    ExtendedChoiceParameterDefinition.PARAMETER_TYPE_TEXT_BOX
                    , "5", null, null, "5", null, null, false, 5, "description")
    );
    
    private void setupVersionedTool() throws Exception {
        CustomTool.DescriptorImpl tools = r.hudson.getDescriptorByType(CustomTool.DescriptorImpl.class);
        List<ToolInstaller> installers = new ArrayList<ToolInstaller>();
        installers.add(new CommandInstaller(null, "ln -s `which true` mytrue", "./"));
        List<ToolProperty<ToolInstallation>> properties = new ArrayList<ToolProperty<ToolInstallation>>();
        properties.add(new InstallSourceProperty(installers));
        CustomTool installation = new CustomTool(TEST_TOOL_NAME, null, properties, "./", null, versionConfig, null);
        tools.setInstallations(installation);
    }
    
    private FreeStyleProject setupJobWithVersionParam(Slave targetSlave) throws Exception {
        FreeStyleProject project = r.createFreeStyleProject("foo");
        ParametersDefinitionProperty pdp = new ParametersDefinitionProperty(
                new StringParameterDefinition("string", "defaultValue", "description"),
                new ToolVersionParameterDefinition(TEST_TOOL_NAME));
        
        project.addProperty(pdp);
        project.getBuildersList().add(new CaptureEnvironmentBuilder());
        project.setAssignedNode(targetSlave);
        
        return project;
    }
    
    @Test
    @Bug(22925)
    public void testDefaultValueOnCLICall() throws Exception {           
        // Setup the environment
        setupVersionedTool();
        DumbSlave slave = r.createSlave();
        FreeStyleProject project = setupJobWithVersionParam(slave);
               
        // Create CLI & run command
        command = new CLICommandInvoker(r, new BuildCommand());
        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.ADMINISTER)
                .invokeWithArgs("foo","-p","string=foo");
        assertThat(result, succeeded());
                
        // Check the job
        Queue.Item q = r.jenkins.getQueue().getItem(project);
        Thread.sleep(5000);
        
        // Check executors health after a timeout
        for (Executor exec : slave.toComputer().getExecutors()) {
            Assert.assertTrue("Executor is dead: "+exec, exec.isAlive());
        }
    }
}
