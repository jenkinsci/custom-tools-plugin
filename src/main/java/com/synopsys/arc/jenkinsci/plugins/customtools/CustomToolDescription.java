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

package com.synopsys.arc.jenkinsci.plugins.customtools;

import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Stores description fields for {@link CustomTool}.
 * 
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 * @since TODO: define a version
 */
public abstract class CustomToolDescription implements Describable<CustomToolDescription> {
    
    /**
     * HTML-formatted summary.
     */
    private String summary;
    /**
     * HTML-formatted detailed description of the {@link CustomTool}.
     */
    private String details; 

    @DataBoundConstructor
    public CustomToolDescription(String summary, String details) {
        this.summary = summary;
        this.details = details;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public static class DescriptorImpl extends Descriptor<CustomToolDescription> {

        @Override
        public String getDisplayName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
