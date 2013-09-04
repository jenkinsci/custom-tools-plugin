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
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionConfig;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
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

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An arbitrary tool, which can add directories to the build's PATH
 * 
 * @author rcampbell
 * 
 */
public class CustomTool extends ToolInstallation implements
        NodeSpecific<CustomTool>, EnvironmentSpecific<CustomTool> {

    /**
     * File set includes string like **\/bin These will be added to the PATH
     */
    private final String exportedPaths;
    private final LabelSpecifics[] labelSpecifics;
    private static final LabelSpecifics[] EMPTY_LABELS = new LabelSpecifics[0];           
    private transient String correctedHome;
    private final ToolVersionConfig toolVersion;
    
    @DataBoundConstructor
    public CustomTool(String name, String home, List properties,
            String exportedPaths, LabelSpecifics[] labelSpecifics, ToolVersionConfig toolVersion) {
        super(name, home, properties);
        this.exportedPaths = exportedPaths;
        this.labelSpecifics = labelSpecifics;
        this.toolVersion = toolVersion;
    }
    
    public String getExportedPaths() {
        return exportedPaths;
    }

    public ToolVersionConfig getToolVersion() {
        return toolVersion;
    }
    
    public boolean hasVersions() {
        return toolVersion != null;
    }
    
    @Override
    public String getHome() {
        return (correctedHome != null) ? correctedHome : super.getHome(); 
    }
        
    public void correctHome(PathsList pathList) {
        correctedHome = pathList.getHomeDir(); 
    }

    public LabelSpecifics[] getLabelSpecifics() {
        return (labelSpecifics!=null) ? labelSpecifics : EMPTY_LABELS;
    }
         
    @Override
    public CustomTool forEnvironment(EnvVars environment) {
        return new CustomTool(getName(), environment.expand(getHome()),
                getProperties().toList(), environment.expand(exportedPaths),
                LabelSpecifics.substitute(getLabelSpecifics(), environment), toolVersion);
    }

    @Override
    public CustomTool forNode(Node node, TaskListener log) throws IOException,
            InterruptedException {       
        String substitutedPath = EnvStringParseHelper.resolveExportedPath(exportedPaths, node);
        String substitutedHomeDir = EnvStringParseHelper.resolveExportedPath(translateFor(node, log), node);
        
        return new CustomTool(getName(), substitutedHomeDir, getProperties().toList(), 
                substitutedPath, LabelSpecifics.substitute(getLabelSpecifics(), node), toolVersion);
    }
    
    //FIXME: just a stub
    public CustomTool forBuildProperties(Map<JobPropertyDescriptor,JobProperty> properties) {
        return new CustomTool(getName(), getHome(), getProperties().toList(), getExportedPaths(), getLabelSpecifics(), toolVersion);
    }
    
    /**
     * Checks consistency of the tool.
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
    public List<LabelSpecifics> getAppliedSpecifics(Node node) {
        List<LabelSpecifics> out = new LinkedList<LabelSpecifics>();
        for (LabelSpecifics spec : labelSpecifics) {
            if (spec.appliesTo(node)) {
                out.add(spec);
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

        public CustomTool byName(String name) {
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
     * @throws InterruptedException
     */
    protected PathsList getPaths(Node node) throws IOException, InterruptedException {

        FilePath homePath = new FilePath(node.getChannel(), getHome());
        //FIXME: Why?
        if (exportedPaths == null) {
            return PathsList.EMPTY;
        }       
        final List<LabelSpecifics> specs = getAppliedSpecifics(node);
        
        PathsList pathsFound = homePath.act(new FileCallable<PathsList>() {          
            
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
                parseLists(exportedPaths, items);
                for (LabelSpecifics spec : specs) {
                   parseLists(spec.getExportedPaths(), items);
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
                File homeDir = new File(getHome());   
                return new PathsList(outList, homeDir.getAbsolutePath());               
            };
        });
              
        return pathsFound;
    }

}