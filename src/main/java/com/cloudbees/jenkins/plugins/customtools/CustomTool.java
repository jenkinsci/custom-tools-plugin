/*
 * Copyright 2012, CloudBees Inc.
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
import com.synopsys.arc.jenkinsci.plugins.customtools.PathsList;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.EnvironmentSpecific;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    @DataBoundConstructor
    public CustomTool(String name, String home, List properties,
            String exportedPaths) {
        super(name, home, properties);
        this.exportedPaths = exportedPaths;
    }
    
    public String getExportedPaths() {
        return exportedPaths;
    }
        
    

    @Override
    public CustomTool forEnvironment(EnvVars environment) {
        return new CustomTool(getName(), environment.expand(getHome()),
                getProperties().toList(), environment.expand(exportedPaths));
    }

    @Override
    public CustomTool forNode(Node node, TaskListener log) throws IOException,
            InterruptedException {       
        String substitutedPath = EnvStringParseHelper.resolveExportedPath(exportedPaths, node);
        String toolHomeDir = EnvStringParseHelper.resolveExportedPath(translateFor(node, log), node);
                
        return new CustomTool(getName(), toolHomeDir,
                getProperties().toList(), substitutedPath);
    }
    
    /**
     * Checks consistency of the tool.
     * @throws CustomToolException Validation error
     */
    public void check() throws CustomToolException {
        EnvStringParseHelper.checkStringForMacro("EXPORTED_PATHS", getExportedPaths());
        EnvStringParseHelper.checkStringForMacro("HOME_DIR", getHome());
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
        if (exportedPaths == null) {
            return PathsList.EMPTY;
        }

        PathsList pathsFound = homePath.act(new FileCallable<PathsList>() {

            public PathsList invoke(File f, VirtualChannel channel)
                    throws IOException, InterruptedException {           
                String[] items = exportedPaths.split("\\s*,\\s*");
                String[] res = new String[items.length];
                int i=0;
                for (String item : items) {
                    File file = new File(item);
                    if (!file.isAbsolute()) {
                        file = new File (getHome(), item);
                    }
                    
                    // Check if directory exists
                    if (!file.isDirectory() || !file.exists()) {
                        throw new AbortException("Wrong EXPORTED_PATHS configuration. Can't find "+file.getPath());
                    } 
                    res[i]=file.getAbsolutePath();
                    i++;
                }
                return new PathsList(res);
                
                /**
                 * Previous implementation:
                 * FileSet fs = Util.createFileSet(new File(getHome()),exportedPaths);     
                 * DirectoryScanner ds = fs.getDirectoryScanner();
                 -- added: ds.scan();
               */                 
            };
        });
              
        // be extra greedy in case they added "./. or . or ./"
        pathsFound.add(getHome());
        
        return pathsFound;
    }

}