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
import hudson.model.Label;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Contains label-specific options.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.3
 */
public class LabelSpecifics {
    private String label;
    private String additionalVars;

    @DataBoundConstructor
    public LabelSpecifics(String label, String additionalVars) {
        this.label = label;
        this.additionalVars = additionalVars;
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
    
    /**
     * Check if specifics is applicable to node 
     * @param node Node to be checked
     * @return True if specifics is applicable to node
     */
    public boolean appliesTo(Node node) {
        Label l = Jenkins.getInstance().getLabel(label);
        return l == null || l.contains(node);
    }
    
    public LabelSpecifics substitute(EnvVars vars) {
        return new LabelSpecifics(label, vars.expand(additionalVars));
    }
    
    public LabelSpecifics substitute(Node node) {
        return new LabelSpecifics(label, EnvStringParseHelper.resolveExportedPath(additionalVars, node));
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
            
}
