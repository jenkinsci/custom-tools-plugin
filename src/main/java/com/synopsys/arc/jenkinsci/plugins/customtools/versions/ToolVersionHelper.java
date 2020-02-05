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
package com.synopsys.arc.jenkinsci.plugins.customtools.versions;

import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import hudson.tools.ToolInstallation;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;

/**
 * Provides helper for tool version handling.
 * @author Oleg Nenashev
 * @since 0.4
 */
public class ToolVersionHelper {
    public static ExtendedChoiceParameterDefinition getDefaultVersionDescr(String toolName) {
        return new ExtendedChoiceParameterDefinition(
                getDefaultVersionVariableName(toolName),
                ExtendedChoiceParameterDefinition.PARAMETER_TYPE_TEXT_BOX,
                "default", null, null,
                "default", null, null,
                false, 1, "Default version stub"
                );
    }

    public static String getDefaultVersionVariableName(String toolName) {
        return toolName.toUpperCase(Locale.ENGLISH).replaceAll("\\s+", "_");
    }

    public static ExtendedChoiceParameterDefinition getVersionDescr(String toolName) {
        CustomTool.DescriptorImpl tools = ToolInstallation.all().get(CustomTool.DescriptorImpl.class);
        CustomTool tool = tools != null ? tools.byName(toolName) : null;
        ToolVersionConfig toolVersionConfig = tool != null ? tool.getToolVersion() : null;

        return toolVersionConfig != null ? toolVersionConfig.getVersionsListSource() : getDefaultVersionDescr(toolName);
    }

    /**
     * Gets list of versioned tools.
     * @return List of tools, which have versions specified
     */
    public static @Nonnull List<CustomTool> getAllVersionedTools() {
        CustomTool.DescriptorImpl tools = ToolInstallation.all().get(CustomTool.DescriptorImpl.class);
        List<CustomTool> res = new LinkedList<CustomTool>();
        if (tools != null ) {
            for (CustomTool tool : tools.getInstallations()) {
                if (tool.hasVersions()) {
                    res.add(tool);
                }
            }
        }
        return res;
    }
}