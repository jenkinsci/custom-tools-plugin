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

import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Constructs 
 * @author Oleg Nenashev <nenashev@synopsys.com>
 */
public class EnvVariablesConstructor extends Hashtable<String, EnvVariablesConstructor.Entity>
{    
    private EnvVariablesConstructor(int initialCapacity)
    {
        super(initialCapacity);
    }
     
    public static EnvVariablesConstructor Create(String props) throws IOException {
        Properties prop = new Properties();
        StringReader rdr = new StringReader(props);       
        prop.load(rdr);
        
        EnvVariablesConstructor vars = new EnvVariablesConstructor(prop.size());
        for (Entry<Object,Object> entry: prop.entrySet())
        {
            
        }        
        return vars;
    }
    
    
    public static class Entity {
        public String envName;
        public String envValue;
        public String listDelimiter;
        public boolean isList;
        public boolean isOverrides;      
        
        public Entity(String envName, String envValue, String listDelimiter, 
                boolean isList, boolean isOverrides)
        {
            this.envName = envName;
            this.envValue = envValue;
            this.listDelimiter = listDelimiter;
            this.isList = isList;
            this.isOverrides = isOverrides;           
        }
        
        
    }
    
    
}
