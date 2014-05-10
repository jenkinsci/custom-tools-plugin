/*
 * Copyright 2013 Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc..
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
import com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterValue;
import com.synopsys.arc.jenkinsci.plugins.customtools.Messages;
import hudson.Extension;
import hudson.cli.CLICommand;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterDefinition.ParameterDescriptor;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.tools.ToolInstallation;
import java.io.IOException;
import javax.annotation.CheckForNull;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Implements "Tool version" parameter.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.4
 */
public class ToolVersionParameterDefinition extends ParameterDefinition {
    
    private final String toolName;
    
    @DataBoundConstructor
    public ToolVersionParameterDefinition(String toolName) {
        super( ToolVersionHelper.getVersionDescr(toolName).getName(), 
               ToolVersionHelper.getVersionDescr(toolName).getDescription());
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
    
    /**
     * Gets a {@link CustomTool} linked with this Parameter definition.
     * @return A custom tool or null if it has not been found
     */
    @CheckForNull
    public final CustomTool getTool() {
        CustomTool.DescriptorImpl tool = ToolInstallation.all().get(CustomTool.DescriptorImpl.class);
        return tool.byName(toolName);
    }
    
    /**
     * Gets a Tool Version configuration for the parameter definition.
     * @return A tool version configuration or null if the tool cannot be found.
     */
    @CheckForNull
    public final ToolVersionConfig getVersionConfig() {
        CustomTool tool = getTool();
        return tool != null ? tool.getToolVersion() : null;
    }
    
    private ExtendedChoiceParameterDefinition getVersionsListSource() {
       ToolVersionConfig versionConfig = getVersionConfig();
        if (versionConfig == null) {
            throw new IllegalStateException(
                    Messages.Versions_ToolVersionParameterDefinition_GetVersionConfigError(toolName));
        }

        return versionConfig.getVersionsListSource();
    }
        
    @Override
    public StringParameterValue createValue(StaplerRequest req, JSONObject jo)
            throws IllegalStateException {
        ExtendedChoiceParameterValue paramVal = (ExtendedChoiceParameterValue) 
                getVersionsListSource().createValue(req, jo);
        return new StringParameterValue(paramVal.getName(), paramVal.value);
    }

    @Override
    public StringParameterValue createValue(StaplerRequest req) {
        ExtendedChoiceParameterValue paramVal = (ExtendedChoiceParameterValue) 
                getVersionsListSource().createValue(req);
        return new StringParameterValue(paramVal.getName(), paramVal.value);
    }

    @Override
    public ParameterValue createValue(CLICommand command, String value) throws IOException, InterruptedException {
        ParameterValue val = getDefaultParameterValue();
        return new StringParameterValue(val.getName(), value);
    }
    
    @Override
    public ParameterValue getDefaultParameterValue() {
        ExtendedChoiceParameterValue paramVal = (ExtendedChoiceParameterValue) 
                getVersionsListSource().getDefaultParameterValue();
        return new StringParameterValue(paramVal.getName(), paramVal.value);
    }
       
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.Versions_ToolVersionParameterDefinition_DisplayName(); 
        }   
    }     
}
