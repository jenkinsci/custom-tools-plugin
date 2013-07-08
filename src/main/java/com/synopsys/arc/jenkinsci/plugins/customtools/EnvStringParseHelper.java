/*
 * Copyright 2013 Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
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
package com.synopsys.arc.jenkinsci.plugins.customtools;

import hudson.EnvVars;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import java.util.Map;

/**
 * Provides parsing of environment variables in input string;
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.3
 */
public class EnvStringParseHelper {
    private EnvStringParseHelper() {};
    
    /**
     * Resolves tools installation directory using global variables.
     * @param environment Collection of environment variables
     * @param exportedPaths Input path with macro calls
     * @return Raw string
     * @since 0.3
     */
    public static String resolveExportedPath(String exportedPaths, EnvVars environment)  {
        if (exportedPaths == null) return null;
        if (!exportedPaths.contains("${")) {
            return exportedPaths;
        }    
        
        // Substitute parameters      
        // TODO: optimize via parsing of string
        String substitutedString = exportedPaths;
        for (Map.Entry<String,String> entry : environment.entrySet()) {
            substitutedString = substitutedString.replace("${" + entry.getKey() + "}", entry.getValue());
        }
             
        return substitutedString;     
    }
    
    public static String resolveExportedPath(String exportedPaths, Node node) {
        if (exportedPaths == null) return null;
        if (!exportedPaths.contains("${")) {
            return exportedPaths;
        } 
        
        String substitutedString = exportedPaths;
        for (NodeProperty<?> entry : node.getNodeProperties()) {
            // Get environment variables
            if (EnvironmentVariablesNodeProperty.class.equals(entry.getClass())) {
               EnvironmentVariablesNodeProperty prop = (EnvironmentVariablesNodeProperty)entry;
               substitutedString = resolveExportedPath(substitutedString, prop.getEnvVars());
            }
            //TODO: add support of other configuration entries or propagate environments
        }        
        return substitutedString;
    }
    
    /**
     * Resolves tools installation directory using global variables.
     * @param exportedPaths Input path with macro calls
     * @return Raw string
     * @throws CustomToolException String validation failed
     * @since 0.3
     */
    public static void checkStringForMacro(String stringName, String exportedPaths) 
            throws CustomToolException {     
        // Check consistensy and throw errors
        if (exportedPaths.contains("${")) {
           throw new CustomToolException("Can't resolve all variables in "+stringName+" string. Final state: "+exportedPaths);
        } 
    }
}
