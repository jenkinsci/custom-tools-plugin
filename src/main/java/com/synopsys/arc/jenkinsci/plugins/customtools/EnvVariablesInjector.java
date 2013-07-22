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
import java.io.IOException;
import java.io.StringReader;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Tool-specific environment variables injector.
 * Implements additionalVariables for CutomTool.
 * @author Oleg Nenashev <nenashev@synopsys.com>
 * @since  0.3
 */
public class EnvVariablesInjector extends TreeMap<String, EnvVariablesInjector.Entity>
{    
    private EnvVariablesInjector(){}
     
    public static EnvVariablesInjector Create(String props) throws IOException {
        Properties prop = new Properties();
        StringReader rdr = new StringReader(props);       
        prop.load(rdr);
        
        EnvVariablesInjector vars = new EnvVariablesInjector();
        for (Entry<Object,Object> entry: prop.entrySet())
        {     
            String varName = (String)entry.getKey();
            Entity ent = new Entity(varName, (String)entry.getValue());
            vars.put(varName, ent);
        }        
        return vars;
    }
    
    /**
     * Inject variables into EnvVars
     * @param target Target variables
     * @throws IOException Exception during modification of EnvVars
     */
    public void Inject(EnvVars target) throws IOException {
        for (Entry<String, EnvVariablesInjector.Entity> entry: entrySet()) {
            entry.getValue().Inject(target);
        }
    } 
    
    /**
     * Internal entry, which describes modification of Environment Variables
     */
    //TODO: Handle delimiters
    //TODO: Handle modification conflicts
    // etc.
    public static class Entity {
        public String envName;
        public String envValue;
        public final static String DEFAULT_LIST_DELIMITER=",";
               
        @Deprecated
        public String listDelimiter;
        @Deprecated
        public boolean isList;
        @Deprecated
        public boolean isOverrides;     
        
        public Entity(String envName, String envValue) {
            this(envName, envValue, DEFAULT_LIST_DELIMITER, false, true);
        }
        
        /**
         * @deprecated Not implemented in 0.3
         * @param envName
         * @param envValue
         * @param listDelimiter
         * @param isList
         * @param isOverrides 
         */
        public Entity(String envName, String envValue, String listDelimiter, 
                boolean isList, boolean isOverrides)
        {
            this.envName = envName;
            this.envValue = envValue;
            this.listDelimiter = listDelimiter;
            this.isList = isList;
            this.isOverrides = isOverrides;         
        }
        
       /**
        * Inject variables into EnvVars
        * @param target Target environment
        * @throws IOException Exception during modification of EnvVars
        */
        public void Inject(EnvVars target) throws IOException {
            //TODO: check overrides
            //TODO: check lists 
            //TODO: substitute, check, etc.
            
            //Substitute current envValue
            String replacedValue = target.containsKey(envName) ? target.get(envName) : "";        
            String newEnvValue=envValue.replace("${"+envName+"}", replacedValue);                       
            target.put(envName, newEnvValue);
        }
    }
    
    
}
