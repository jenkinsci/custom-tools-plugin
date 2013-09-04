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
import com.synopsys.arc.jenkinsci.plugins.customtools.EnvStringParseHelper;
import hudson.EnvVars;
import hudson.model.Node;
import java.io.Serializable;

/**
 * A stub for a tool versions.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.4
 */
public class ToolVersion implements Serializable {
    private String variableName;
    private String defaultVersion;
    private String actualVersion;
    private String versionSource;
    public static final String DEFAULTS_SOURCE = "defaults";

    /**
     * Constructs a default version.
     * @param variableName
     * @param defaultVersion 
     */
    private ToolVersion(String variableName, String defaultVersion) {
        this(variableName, defaultVersion, null, null);
    }
    
    private ToolVersion (ToolVersion defaultVersion, String actualVersion, String versionSource) {
        this(defaultVersion.getVariableName(), defaultVersion.getDefaultVersion(), actualVersion, versionSource);
    }

    public ToolVersion(String variableName, String defaultVersion, String actualVersion, String versionSource) {
        this.variableName = variableName;
        this.defaultVersion = defaultVersion;
        this.actualVersion = actualVersion;
        this.versionSource = versionSource;
    }
    
    public boolean hasDefaultVersion() {
        return defaultVersion != null;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }
        
    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getActualVersion() {
        return actualVersion;
    }

    public String getVersionSource() {
        return versionSource;
    }
    
    public static ToolVersion getDefaultToolVersion(CustomTool tool) {
        ExtendedChoiceParameterDefinition def = tool.getToolVersion().getVersionsListSource();
        String defaultVersion = hudson.Util.fixEmptyAndTrim(def.getEffectiveDefaultValue());
        return new ToolVersion(def.getName(), defaultVersion);
    }
    
    /**
     * Method gets effective tool version for the build
     * @param tool Custom tool
     * @param buildEnv Current build environment
     * @param node Node, where the build runs 
     * @return Effective tool version. null in case of unavailable version
     */
    public static ToolVersion getEffectiveToolVersion(CustomTool tool, EnvVars buildEnv, Node node) {
        ToolVersion defaultVersion = getDefaultToolVersion(tool);
        
        // Check if node has version specified
        String subst = "${"+defaultVersion.getVariableName()+"}"; 
                
        // Try to find variable in environment
        String res = EnvStringParseHelper.resolveExportedPath(subst, node);
        if (!subst.equals(res)) {
            return new ToolVersion(defaultVersion, res, "node or global variables");
        } else if (buildEnv.containsKey(defaultVersion.getVariableName())) {
            String envVersion = buildEnv.get(defaultVersion.getVariableName());
            return new ToolVersion(defaultVersion, envVersion, "build environment");  
        } else if (defaultVersion.hasDefaultVersion()){
            return new ToolVersion(defaultVersion, defaultVersion.getDefaultVersion(), DEFAULTS_SOURCE);     
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return defaultVersion;
    }
    
    
}
