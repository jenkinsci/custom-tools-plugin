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
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import java.io.Serializable;
import java.util.ArrayList;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.plugins.customtools.util.envvars.VariablesSubstitutionHelper;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Contains label-specific options.
 * @author Oleg Nenashev
 * @since 0.3
 */
public class LabelSpecifics extends AbstractDescribableImpl<LabelSpecifics> implements Serializable {

    private final @CheckForNull String label;
    private final @CheckForNull String additionalVars;
    private final @CheckForNull String exportedPaths;

    @DataBoundConstructor
    public LabelSpecifics(@CheckForNull String label, @CheckForNull String additionalVars, @CheckForNull String exportedPaths) {
        this.label = Util.fixEmptyAndTrim(label);
        this.additionalVars = additionalVars;
        this.exportedPaths = exportedPaths;
    }

    public @CheckForNull String getAdditionalVars() {
        return additionalVars;
    }

    public boolean hasAdditionalVars() {
        return additionalVars != null;
    }

    public @CheckForNull String getLabel() {
        return label;
    }

    public @CheckForNull String getExportedPaths() {
        return exportedPaths;
    }

    /**
     * Check if specifics is applicable to node
     * @param node Node to be checked
     * @return True if specifics is applicable to node
     */
    public boolean appliesTo(@Nonnull Node node) {
        String correctedLabel = Util.fixEmptyAndTrim(label);
        if (correctedLabel == null) {
            return true;
        }

        Label l = Jenkins.getActiveInstance().getLabel(label);
        return l == null || l.contains(node);
    }

    public @Nonnull LabelSpecifics substitute(@Nonnull EnvVars vars) {
        return new LabelSpecifics(label,
                VariablesSubstitutionHelper.PROP_FILE.resolveVariable(additionalVars, vars),
                VariablesSubstitutionHelper.PATH.resolveVariable(exportedPaths, vars));
    }

    public @Nonnull LabelSpecifics substitute(@Nonnull Node node) {
        return new LabelSpecifics(label,
                VariablesSubstitutionHelper.PROP_FILE.resolveVariable(additionalVars, node),
                VariablesSubstitutionHelper.PATH.resolveVariable(exportedPaths, node));
    }

    public static @Nonnull
    ArrayList<LabelSpecifics> substitute (ArrayList<LabelSpecifics> specifics, @Nonnull EnvVars vars) {
        ArrayList<LabelSpecifics> out = new ArrayList<>();
        for (int i=0; i<specifics.size(); i++) {
            out.add(specifics.get(i).substitute(vars));
        }
        return out;
    }

    public static @Nonnull ArrayList<LabelSpecifics> substitute (ArrayList<LabelSpecifics> specifics, Node node) {
        ArrayList<LabelSpecifics> out = new ArrayList<>();
        for (int i=0; i<specifics.size(); i++) {
            out.add(specifics.get(i).substitute(node));
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
