/*
 * Copyright 2014 Oleg Nenashev <o.v.nenashev@gmail.com>.
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

package org.jenkinsci.plugins.customtools.versions;

import hudson.model.ParameterDefinition;
import hudson.model.SimpleParameterDefinition;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Version provider for simple versions.
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 */
public class SimpleParameterDefinitionProvider extends ToolVersionProvider {

    SimpleParameterDefinition parameterDefinition;

    @DataBoundConstructor
    public SimpleParameterDefinitionProvider(SimpleParameterDefinition parameterDefinition) {
        this.parameterDefinition = parameterDefinition;
    }
       
    @Override
    public ParameterDefinition getParameterDefinition() {
        return parameterDefinition;
    }
    
    public static class DescriptorImpl extends ToolVersionProviderDescriptor {

        @Override
        public String getDisplayName() {
            throw new UnsupportedOperationException("TODO"); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
