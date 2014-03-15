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
package com.synopsys.arc.jenkinsci.plugins.customtools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import java.io.Serializable;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Contains label-specific options.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.3
 */
public class LabelSpecifics extends AbstractDescribableImpl<LabelSpecifics> implements Serializable {
    
    private final String label;
    private final String additionalVars;
    private final String exportedPaths;
    
    @DataBoundConstructor
    public LabelSpecifics(String label, String additionalVars, String exportedPaths) {
        this.label = Util.fixEmptyAndTrim(label);
        this.additionalVars = additionalVars;
        this.exportedPaths = exportedPaths;
    }
    
    public String getAdditionalVars() {
        return additionalVars;
    }
    
     public boolean hasAdditionalVars() {
        return additionalVars != null;
    }

    public String getLabel() {
        return label;
    }

    public String getExportedPaths() {
        return exportedPaths;
    }
    
    /**
     * Check if specifics is applicable to node 
     * @param node Node to be checked
     * @return True if specifics is applicable to node
     */
    public boolean appliesTo(Node node) {
        String correctedLabel = Util.fixEmptyAndTrim(label);
        if (correctedLabel == null) {
            return true;
        }
        
        Label l = Jenkins.getInstance().getLabel(label);
        return l == null || l.contains(node);
    }
    
    public LabelSpecifics substitute(EnvVars vars) {
        return new LabelSpecifics(label, vars.expand(additionalVars), vars.expand(exportedPaths));
    }
    
    public LabelSpecifics substitute(Node node) {
        return new LabelSpecifics(label, 
                EnvStringParseHelper.resolveExportedPath(additionalVars, node), 
                EnvStringParseHelper.resolveExportedPath(exportedPaths, node));
    }
    
    public static LabelSpecifics[] substitute (LabelSpecifics[] specifics, EnvVars vars) {
        LabelSpecifics[] out = new LabelSpecifics[specifics.length];
        for (int i=0; i<specifics.length; i++) {
            out[i] = specifics[i].substitute(vars);
        }
        return out;
    }
    
    public static LabelSpecifics[] substitute (LabelSpecifics[] specifics, Node node) {
        LabelSpecifics[] out = new LabelSpecifics[specifics.length];
        for (int i=0; i<specifics.length; i++) {
            out[i] = specifics[i].substitute(node);
        }
        return out;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<LabelSpecifics> {
        @Override
        public String getDisplayName() {
            return Messages.LabelSpecifics_DisplayName();
        }
    }  
}
