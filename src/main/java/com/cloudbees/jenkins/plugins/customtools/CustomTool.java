package com.cloudbees.jenkins.plugins.customtools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.FilePath.FileCallable;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.JDKInstaller;
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

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.customtools.Messages;

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

    public CustomTool forEnvironment(EnvVars environment) {
        return new CustomTool(getName(), environment.expand(getHome()),
                getProperties().toList(), exportedPaths);
    }

    public CustomTool forNode(Node node, TaskListener log) throws IOException,
            InterruptedException {
        return new CustomTool(getName(), translateFor(node, log),
                getProperties().toList(), exportedPaths);
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
    protected List<String> getPaths(Node node) throws IOException, InterruptedException {

        FilePath homePath = new FilePath(node.getChannel(), getHome());
        if (exportedPaths == null) {
            return Collections.emptyList();
        }

        String[] pathsFound = homePath.act(new FileCallable<String []>() {

            public String[] invoke(File f, VirtualChannel channel)
                    throws IOException, InterruptedException {
                FileSet fs = Util.createFileSet(new File(getHome()),exportedPaths);
                DirectoryScanner ds = fs.getDirectoryScanner();

                return ds.getIncludedDirectories();
            };
        });
        
        List<String> completePaths = new ArrayList<String>();
        for (String dir : pathsFound) {
            completePaths.add(new File(getHome(), dir).getAbsolutePath());
        }
        
        // be extra greedy in case they added "./. or . or ./"
        completePaths.add(getHome());
        
        return completePaths;
    }

}