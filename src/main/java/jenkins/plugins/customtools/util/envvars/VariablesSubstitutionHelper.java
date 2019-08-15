/*
 * Copyright 2014 Oleg Nenashev
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

package jenkins.plugins.customtools.util.envvars;

import hudson.EnvVars;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 * Substitutes variables.
 * @since TODO
 * @author Oleg Nenashev
 */
public abstract class VariablesSubstitutionHelper {

    public static final VariablesSubstitutionHelper PROP_FILE = new PropFileVariablesSubstitutionHelper();
    public static final VariablesSubstitutionHelper PATH = new SimpleVariablesSubstitutionHelper();

    /**
     * Escapes variable values for the required format.
     * @param variableName Name of the variable, which is being substituted
     * @param rawValue Input value
     * @return Escaped value
     */
    public String escapeVariableValue(String variableName, String rawValue) {
        return rawValue;
    }

    /**
     * Resolves tools installation directory using global variables.
     * @param environment Collection of environment variables
     * @param inputValue Input path with macro calls
     * @return Raw string
     * @since 0.3
     */
    public String resolveVariable(@CheckForNull String inputValue, @Nonnull EnvVars environment)  {
        if (inputValue == null || !hasMacros(inputValue))
            return inputValue;

        // Substitute parameters
        String substitutedString = inputValue;
        for (Map.Entry<String,String> entry : environment.entrySet()) {
            if (hasMacros(inputValue, entry.getKey())) {
                final String escapedValue = escapeVariableValue(entry.getKey(), entry.getValue());
                substitutedString = substitutedString.replace("${" + entry.getKey() + "}", escapedValue);
            }
        }

        return substitutedString;
    }

    public String resolveVariable(@CheckForNull String inputValue, @Nonnull Node node) {
        if (!hasMacros(inputValue))
            return inputValue;

        // Check node properties
        String substitutedString = inputValue;
        for (NodeProperty<?> entry : node.getNodeProperties()) {
            substitutedString = substituteNodeProperty(substitutedString, entry);
        }

        // Substitute global variables
        for (NodeProperty<?> entry : Jenkins.getActiveInstance().getGlobalNodeProperties()) {
            substitutedString = substituteNodeProperty(substitutedString, entry);
        }

        return substitutedString;
    }

    /**
     * Substitutes string according to node property.
     * @param macroString String to be substituted
     * @param property Node property
     * @return Substituted string
     * @since 0.3
     */
    private String substituteNodeProperty(@CheckForNull String macroString, @CheckForNull NodeProperty<?> property) {
        // Get environment variables
        if (property instanceof EnvironmentVariablesNodeProperty) {
           EnvironmentVariablesNodeProperty prop = (EnvironmentVariablesNodeProperty)property;
           return resolveVariable(macroString, prop.getEnvVars());
        }

        //TODO: add support of other configuration entries or propagate environments
        return macroString;
    }

    public static boolean hasMacros(@CheckForNull String inputString) {
        return inputString != null && inputString.contains("${");
    }

    public static boolean hasMacros(@CheckForNull String inputString, String macroName) {
        return inputString != null && inputString.contains("${" + macroName + "}");
    }

    public static class SimpleVariablesSubstitutionHelper extends VariablesSubstitutionHelper {

    }

    public static class PropFileVariablesSubstitutionHelper extends VariablesSubstitutionHelper {

        @Override
        public String escapeVariableValue(String variableName, String rawValue) {
            final ByteArrayOutputStream str= new ByteArrayOutputStream();
            Properties prop = new Properties();
            prop.setProperty("TMP", rawValue);
            try {
                prop.store(str,"tmp");
            } catch (IOException ex) {
                // Fallback to the default behavior
                //TODO: Really???
                return super.escapeVariableValue(variableName, rawValue);
            }

            try {
                return str.toString("UTF-8").split("\n")[2].replaceFirst(".*TMP=", "").trim();
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalStateException("UTF-8 encoding is not supported", ex);
            }
        }
    }
}
