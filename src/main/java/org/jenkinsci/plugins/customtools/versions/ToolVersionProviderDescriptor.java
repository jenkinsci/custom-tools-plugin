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

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import java.util.List;
import jenkins.model.Jenkins;

/**
 *
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 */
public abstract class ToolVersionProviderDescriptor extends Descriptor<ToolVersionProvider> {

    /**
     * Get list of all registered {@link ToolVersionProvider}s.
     * @return List of {@link ToolVersionProvider}s.
     */    
    public static DescriptorExtensionList<ToolVersionProvider,ToolVersionProviderDescriptor> all() {
        return Jenkins.getInstance().<ToolVersionProvider,ToolVersionProviderDescriptor>getDescriptorList
            (ToolVersionProvider.class);
    }
    
    /**
     * Returns list of {@link ToolVersionProviderDescriptor}s.
     * @return List of available descriptors.
     * @since 0.2
     */
    public static List<ToolVersionProviderDescriptor> allDescriptors() {
        return all().reverseView();
    }
}
