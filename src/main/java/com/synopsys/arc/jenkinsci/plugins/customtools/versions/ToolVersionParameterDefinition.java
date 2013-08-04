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
import com.synopsys.arc.jenkinsci.plugins.customtools.Messages;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterDefinition.ParameterDescriptor;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.tools.ToolInstallation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 */
public class ToolVersionParameterDefinition extends ParameterDefinition {
    private String toolName;
    
    @DataBoundConstructor
    public ToolVersionParameterDefinition(String toolName) {
        super( ToolVersionHelper.getVersionDescr(toolName).getName(), 
               ToolVersionHelper.getVersionDescr(toolName).getDescription());
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
    
    public final CustomTool getTool() {
        CustomTool.DescriptorImpl tool = ToolInstallation.all().get(CustomTool.DescriptorImpl.class);
        return tool.byName(toolName);
    }
    
    public final ToolVersionConfig getVersionConfig() {
        return getTool().getToolVersion();
    }
    
    
 
    @Override
    public StringParameterValue createValue(StaplerRequest req, JSONObject jo) {
        ParameterValue paramVal = getVersionConfig().getVersionsListSource().createValue(req, jo);
        return new StringParameterValue(paramVal.getName(), paramVal.getDescription());
    }

    @Override
    public StringParameterValue createValue(StaplerRequest req) {       
        ParameterValue paramVal = getVersionConfig().getVersionsListSource().createValue(req);
        return new StringParameterValue(paramVal.getName(), paramVal.getDescription());
    }
    
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.Versions_ToolVersionParameterDefinition_DisplayName(); 
        }   
    }     
}
