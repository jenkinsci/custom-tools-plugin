/*
 * Copyright 2014-2016 Oleg Nenashev.
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
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionConfig;
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionParameterDefinition;
import hudson.cli.BuildCommand;
import hudson.cli.CLICommandInvoker;
import static hudson.cli.CLICommandInvoker.Matcher.succeeded;
import hudson.model.Executor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.Slave;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.slaves.DumbSlave;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for the {@link ToolVersionParameterDefinition} class.
 * @author Oleg Nenashev
 */
public class ToolVersionParameterDefinitionTest {
   
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    private CLICommandInvoker command;
    
    private static final String TEST_TOOL_NAME="test";
    private static final ToolVersionConfig versionConfig = new ToolVersionConfig(
            new ExtendedChoiceParameterDefinition("TOOL_VERSION", 
                    ExtendedChoiceParameterDefinition.PARAMETER_TYPE_TEXT_BOX
                    , "5", null, null, "5", null, null, false, 5, "description")
    );
    
    private void setupVersionedTool() throws Exception {
        CustomTool.DescriptorImpl tools = j.jenkins.getDescriptorByType(CustomTool.DescriptorImpl.class);
        List<ToolInstaller> installers = new ArrayList<ToolInstaller>();
        installers.add(new CommandInstaller(null, "ln -s `which true` mytrue", "./"));
        List<ToolProperty<ToolInstallation>> properties = new ArrayList<ToolProperty<ToolInstallation>>();
        properties.add(new InstallSourceProperty(installers));
        CustomTool installation = new CustomTool(TEST_TOOL_NAME, null, properties, "./", null, versionConfig, null);
        tools.setInstallations(installation);
    }
    
    private FreeStyleProject setupJobWithVersionParam(Slave targetSlave) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        ParametersDefinitionProperty pdp = new ParametersDefinitionProperty(
                new StringParameterDefinition("string", "defaultValue", "description"),
                new ToolVersionParameterDefinition(TEST_TOOL_NAME));
        
        project.addProperty(pdp);
        project.setAssignedNode(targetSlave);
        
        return project;
    }
    
    @Test
    @Issue("JENKINS-22925")
    public void testDefaultValueOnCLICall() throws Exception {           
        // Setup the environment
        setupVersionedTool();
        DumbSlave slave = j.createSlave();
        FreeStyleProject project = setupJobWithVersionParam(slave);
               
        // Create CLI & run command
        CLICommandInvoker command = new CLICommandInvoker(j, new BuildCommand());
        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.ADMINISTER)
                .invokeWithArgs("foo","-p","string=foo");
        MatcherAssert.assertThat(result, succeeded());
                
        // Check the job
        Queue.Item q = j.jenkins.getQueue().getItem(project);
        Thread.sleep(5000);
        
        // Check executors health after a timeout
        for (Executor exec : slave.toComputer().getExecutors()) {
            Assert.assertTrue("Executor is neither parked nor active: " + exec, exec.isActive() || exec.isParking());
        }
    }
    
    @Test
    @Issue("JENKINS-22923")
    public void testSpecifyVersionInCLICall() throws Exception {           
        // Setup the environment
        setupVersionedTool();
        DumbSlave slave = j.createSlave();
        FreeStyleProject project = setupJobWithVersionParam(slave);  
        
        // Create CLI & run command
        CLICommandInvoker command = new CLICommandInvoker(j, new BuildCommand());
        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.ADMINISTER)
                .invokeWithArgs("foo","-p","string=foo","-p","TOOL_VERSION=test");
        MatcherAssert.assertThat(result, succeeded());
                
        // Check the job
        Queue.Item q = j.jenkins.getQueue().getItem(project);
        if (q != null) {
            Thread.sleep(5000);
            q.getFuture();
        } else { 
            // it has benn already executed, we'll check it later
        }
        
        FreeStyleBuild lastBuild = project.getLastBuild();
        assertNotNull("The build has not been executed yet", lastBuild);
        
        ParametersAction params = lastBuild.getAction(ParametersAction.class);
        assertNotNull(params);
        ParameterValue parameterValue = params.getParameter("TOOL_VERSION");
        assertNotNull("Tool version parameter has not been specified", parameterValue);
        assertTrue("Wrong class of the tool version parameter", parameterValue instanceof StringParameterValue);
        assertEquals("test", ((StringParameterValue)parameterValue).value);
    }
}
