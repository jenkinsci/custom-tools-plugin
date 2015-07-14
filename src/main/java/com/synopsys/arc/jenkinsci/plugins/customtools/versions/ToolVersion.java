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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A stub for a tool versions.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.4
 */
public class ToolVersion implements Serializable {
    private String variableName;
    private @CheckForNull String defaultVersion;
    private @CheckForNull String actualVersion;
    private @CheckForNull String versionSource;
    public static final String DEFAULTS_SOURCE = "defaults";

    /**
     * Constructs a default version.
     * @param variableName
     * @param defaultVersion 
     */
    private ToolVersion(String variableName, String defaultVersion) {
        this(variableName, defaultVersion, null, null);
    }
    
    private ToolVersion (@Nonnull ToolVersion defaultVersion, @CheckForNull String actualVersion, 
            @CheckForNull String versionSource) {
        this(defaultVersion.getVariableName(), defaultVersion.getDefaultVersion(), actualVersion, versionSource);
    }

    public ToolVersion(String variableName, @CheckForNull String defaultVersion, 
            @CheckForNull String actualVersion, @CheckForNull String versionSource) {
        this.variableName = variableName;
        this.defaultVersion = defaultVersion;
        this.actualVersion = actualVersion;
        this.versionSource = versionSource;
    }
    
    public boolean hasDefaultVersion() {
        return defaultVersion != null;
    }

    public @CheckForNull String getDefaultVersion() {
        return defaultVersion;
    }
        
    public void setDefaultVersion(@CheckForNull String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getVariableName() {
        return variableName;
    }

    public @CheckForNull String getActualVersion() {
        return actualVersion;
    }

    public @CheckForNull String getVersionSource() {
        return versionSource;
    }
    
    /**
     * Retrieves the default {@link CustomTool} version.
     * @param tool Tool
     * @return The default version or null if the versioning is not configured. 
     */
    public static @CheckForNull ToolVersion getDefaultToolVersion(@Nonnull CustomTool tool) {
        final ToolVersionConfig versionConfig = tool.getToolVersion();
        if (versionConfig == null) {
            return null;
        }
        
        ExtendedChoiceParameterDefinition def = versionConfig.getVersionsListSource();
        String defaultVersion = hudson.Util.fixEmptyAndTrim(def.getEffectiveDefaultValue());
        return new ToolVersion(def.getName(), defaultVersion);
    }
    
    /**
     * Method gets effective tool version for the build.
     * @param tool Custom tool
     * @param buildEnv Current build environment
     * @param node Node, where the build runs 
     * @return Effective tool version. Null if the version is unavailable
     */
    public static @CheckForNull ToolVersion getEffectiveToolVersion(CustomTool tool, EnvVars buildEnv, Node node) {
        final ToolVersion defaultVersion = getDefaultToolVersion(tool);
        if (defaultVersion == null) {
            return null;
        }
        
        // Check if the node has version specified
        String subst = "${"+defaultVersion.getVariableName()+"}"; 
                
        // Try to find a variable in environment
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
        return defaultVersion != null ? defaultVersion : "null";
    }    
}
