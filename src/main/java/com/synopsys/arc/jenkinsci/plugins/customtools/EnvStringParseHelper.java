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
package com.synopsys.arc.jenkinsci.plugins.customtools;

import hudson.EnvVars;
import hudson.model.Node;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.plugins.customtools.util.envvars.VariablesSubstitutionHelper;
import jenkins.plugins.customtools.util.envvars.VariablesSubstitutionHelper.SimpleVariablesSubstitutionHelper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Provides parsing of environment variables in input string.
 * @author Oleg Nenashev
 * @since 0.3
 */
@Restricted(NoExternalUse.class)
public class EnvStringParseHelper {
    
    private EnvStringParseHelper() {};
    
    private static final SimpleVariablesSubstitutionHelper HELPER = new VariablesSubstitutionHelper.SimpleVariablesSubstitutionHelper();
         
    /**
     * Resolves tools installation directory using global variables.
     * @param inputString Input path with macro calls
     * @param macroName Input string
     * @throws CustomToolException String validation failed
     * @since 0.3
     */
    public static void checkStringForMacro(@CheckForNull String macroName, @CheckForNull String inputString) 
            throws CustomToolException { 
        if (HELPER.hasMacros(inputString)) {
           throw new CustomToolException("Can't resolve all variables in "+macroName+" string. Final state: "+inputString);
        } 
    }
    
    @Deprecated
    public static String resolveExportedPath(@CheckForNull String exportedPaths, @Nonnull EnvVars environment)  {
        return HELPER.resolveVariable(exportedPaths, environment);
    }
    
    @Deprecated        
    public static String resolveExportedPath(@CheckForNull String exportedPaths, @Nonnull Node node) {
        return HELPER.resolveVariable(exportedPaths, node);
    }
}
