/*
 * Copyright 2012, CloudBees Inc., Synopsys Inc. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.jenkins.plugins.customtools;

import com.synopsys.arc.jenkinsci.plugins.customtools.CustomToolException;
import com.synopsys.arc.jenkinsci.plugins.customtools.EnvStringParseHelper;
import com.synopsys.arc.jenkinsci.plugins.customtools.LabelSpecifics;
import com.synopsys.arc.jenkinsci.plugins.customtools.PathsList;
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersion;
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.EnvironmentSpecific;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallation;
import hudson.tools.ZipExtractionInstaller;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.MasterToSlaveFileCallable;
import jenkins.plugins.customtools.util.envvars.VariablesSubstitutionHelper;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An arbitrary tool, which can add directories to the build's PATH.
 * @author rcampbell
 * @author Oleg Nenashev
 * 
 */
@SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID",
        justification = "Actually we do not send the class over the channel. Serial version ID is not required for XStream")
public class CustomTool extends ToolInstallation implements
        NodeSpecific<CustomTool>, EnvironmentSpecific<CustomTool> {

    /**
     * File set includes string like **\/bin These will be added to the PATH
     */
    private final @CheckForNull String exportedPaths;
    /**
     * Label-specific options.
     */
    private final @CheckForNull LabelSpecifics[] labelSpecifics;
    /**
     * A cached value of the home directory.
     */
    private transient @CheckForNull String correctedHome = null;
    /**
     * Optional field, which referenced the {@link ToolVersion} configuration.
     */
    private final @CheckForNull ToolVersionConfig toolVersion;
    /**
     * Additional variables string.
     * Stores variables expression in *.properties format.
     */
    private final @CheckForNull String additionalVariables;
    
    private static final LabelSpecifics[] EMPTY_LABELS = new LabelSpecifics[0];           
    
    @DataBoundConstructor
    public CustomTool(@Nonnull String name, @Nonnull String home, 
            @CheckForNull List<? extends hudson.tools.ToolProperty<?>> properties, @CheckForNull String exportedPaths,
            @CheckForNull LabelSpecifics[] labelSpecifics, @CheckForNull ToolVersionConfig toolVersion, 
            @CheckForNull String additionalVariables) {
        super(name, home, properties);
        this.exportedPaths = exportedPaths;
        this.labelSpecifics = labelSpecifics != null ? Arrays.copyOf(labelSpecifics, labelSpecifics.length) : null;
        this.toolVersion = toolVersion;
        this.additionalVariables = additionalVariables;
    }
    
    public @CheckForNull String getExportedPaths() {
        return exportedPaths;
    }

    /**
     * Gets the tool version configuration.
     * @return Tool version configuration or null if it is not configured.
     */
    public @CheckForNull ToolVersionConfig getToolVersion() {
        return toolVersion;
    }
    
    public boolean hasVersions() {
        return toolVersion != null;
    }
    
    @Override
    @CheckForNull
    public String getHome() {
        return (correctedHome != null) ? correctedHome : super.getHome(); 
    }
        
    public void correctHome(@Nonnull PathsList pathList) {
        correctedHome = pathList.getHomeDir(); 
    }

    public @Nonnull LabelSpecifics[] getLabelSpecifics() {
        return (labelSpecifics!=null) ? labelSpecifics : EMPTY_LABELS;
    }

    /**
     * Check if the tool has additional environment variables set.
     * @return true when the tool injects additional environment variables. 
     */
    public boolean hasAdditionalVariables() {
        return additionalVariables != null;
    }
   
    public @CheckForNull String getAdditionalVariables() {
        return additionalVariables;
    }
         
    @Override
    public CustomTool forEnvironment(EnvVars environment) {
        String substitutedHomeDir = VariablesSubstitutionHelper.PATH.resolveVariable(getHome(), environment);     
        String substitutedPath = VariablesSubstitutionHelper.PATH.resolveVariable(exportedPaths, environment);          
        String substitutedAdditionalVariables = VariablesSubstitutionHelper.PROP_FILE.resolveVariable(additionalVariables, environment);
        
        return new CustomTool(getName(), substitutedHomeDir,
                getProperties().toList(), substitutedPath,
                LabelSpecifics.substitute(getLabelSpecifics(), environment), 
                toolVersion, substitutedAdditionalVariables);
    }

    @Override
    public @Nonnull CustomTool forNode(Node node, TaskListener log) 
            throws IOException, InterruptedException {   
        String substitutedHomeDir = VariablesSubstitutionHelper.PATH.resolveVariable(translateFor(node, log), node);  
        String substitutedPath = VariablesSubstitutionHelper.PATH.resolveVariable(exportedPaths, node);            
        String substitutedAdditionalVariables = VariablesSubstitutionHelper.PROP_FILE.resolveVariable(additionalVariables, node);
        
        return new CustomTool(getName(), substitutedHomeDir, getProperties().toList(), 
                substitutedPath, LabelSpecifics.substitute(getLabelSpecifics(), node), 
                toolVersion, substitutedAdditionalVariables);
    }
    
    //FIXME: just a stub
    @Deprecated
    @Restricted(NoExternalUse.class)
    public CustomTool forBuildProperties(Map<JobPropertyDescriptor,JobProperty> properties) {
        final String toolHome = getHome();
        if (toolHome == null) {
            throw new IllegalStateException("Tool home must not be null at this stage, likely it's an API misusage");
        }
        return new CustomTool(getName(), toolHome, getProperties().toList(),
                getExportedPaths(), getLabelSpecifics(), 
                toolVersion, getAdditionalVariables());
    }
    
    /**
     * Checks the tool consistency.
     * @throws CustomToolException Validation error
     */
    public void check() throws CustomToolException {
        EnvStringParseHelper.checkStringForMacro("EXPORTED_PATHS", getExportedPaths());
        EnvStringParseHelper.checkStringForMacro("HOME_DIR", getHome());
    }
    
    /**
     * Get list of label specifics, which apply to the specified node.
     * @param node Node to be checked
     * @return List of the specifics to be applied
     * @since 0.3
     */
    public @Nonnull List<LabelSpecifics> getAppliedSpecifics(@Nonnull Node node) {
        List<LabelSpecifics> out = new LinkedList<LabelSpecifics>();
        if (labelSpecifics != null) {
            for (LabelSpecifics spec : labelSpecifics) {
                if (spec.appliesTo(node)) {
                    out.add(spec);
                }
            }
        }
        return out;
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<CustomTool> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.CustomTool_DescriptorImpl_DisplayName();
        }

        @Override
        public void setInstallations(CustomTool... installations) {
            super.setInstallations(installations);
            save();
        }

        /**
         * Gets a {@link CustomTool} by its name.
         * @param name A name of the tool to be retrieved.
         * @return A {@link CustomTool} or null if it has no found
         */
        public @CheckForNull CustomTool byName(String name) {
            for (CustomTool tool : getInstallations()) {
                if (tool.getName().equals(name)) {
                    return tool;
                }
            }
            return null;
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new ZipExtractionInstaller(null,
                    null, null));
        }
    }

    /**
     * Finds the directories to add to the path, for the given node.
     * Uses Ant filesets to expand the patterns in the exportedPaths field.
     * 
     * @param node where the tool has been installed
     * @return a list of directories to add to the $PATH
     * 
     * @throws IOException
     * @throws InterruptedException Operation has been interrupted 
     */
    protected @Nonnull PathsList getPaths(@Nonnull Node node) throws IOException, InterruptedException {

        FilePath homePath = new FilePath(node.getChannel(), getHome());
        //FIXME: Why?
        if (exportedPaths == null) {
            return PathsList.EMPTY;
        }       
        final List<LabelSpecifics> specs = getAppliedSpecifics(node);
        
        PathsList pathsFound = homePath.act(new MasterToSlaveFileCallable<PathsList>() {
            private void parseLists(String pathList, List<String> target) {
                String[] items = pathList.split("\\s*,\\s*");              
                for (String item : items) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    target.add(item);
                }
            }
            
            @Override
            public PathsList invoke(File f, VirtualChannel channel)
                    throws IOException, InterruptedException {           
                
                // Construct output paths
                List<String> items = new LinkedList<String>();
                if (exportedPaths != null) {
                    parseLists(exportedPaths, items);
                }
                for (LabelSpecifics spec : specs) {
                    final String exportedPathsFromSpec = spec.getExportedPaths();
                    if (exportedPathsFromSpec != null) {
                        parseLists(exportedPathsFromSpec, items);
                    }
                }
                             
                // Resolve exported paths
                List<String> outList = new LinkedList<String>();
                for (String item : items) {    
                    File file = new File(item);
                    if (!file.isAbsolute()) {
                        file = new File (getHome(), item);
                    }
                    
                    // Check if directory exists
                    if (!file.isDirectory() || !file.exists()) {
                        throw new AbortException("Wrong EXPORTED_PATHS configuration. Can't find "+file.getPath());
                    } 
                    outList.add(file.getAbsolutePath());
                }
                
                // Resolve home dir
                final String toolHome = getHome();
                if (toolHome == null) {
                    throw new IOException("Cannot retrieve Tool home directory. Should never happen ant this stage, please file a bug");
                }
                final File homeDir = new File(toolHome);
                return new PathsList(outList, homeDir.getAbsolutePath());               
            };
        });
              
        return pathsFound;
    }

}