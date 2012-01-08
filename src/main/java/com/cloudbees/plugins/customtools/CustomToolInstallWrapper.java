package com.cloudbees.plugins.customtools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Run.RunnerAbortedException;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Installs tools selected by the user. Exports configured paths and a home variable for each tool.
 * 
 * @author rcampbell
 *
 */
public class CustomToolInstallWrapper extends BuildWrapper {

    /**
     * Ceremony needed to satisfy NoStaplerConstructionException:
     * "There's no @DataBoundConstructor on any constructor of class java.lang.String"
     * @author rcampbell
     *
     */
    public static class SelectedTool {
        private String name;
        
        @DataBoundConstructor
        public SelectedTool(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        
        public CustomTool toCustomTool() {
            return ((CustomTool.DescriptorImpl)Hudson.getInstance().getDescriptor(CustomTool.class)).byName(name);
        }
    }
    
    private SelectedTool[] selectedTools = new SelectedTool[0];

    @DataBoundConstructor
    public CustomToolInstallWrapper(SelectedTool[] selectedTools) {
        this.selectedTools = selectedTools;
    }
    
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException {
        return new Environment(){
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                return true;
            }
        };
    }
    
    public SelectedTool[] getSelectedTools() {
        return selectedTools.clone();
    }
    
    private List<CustomTool> customTools() {
        List<CustomTool> tools = new ArrayList<CustomTool>();
        for (SelectedTool selected : selectedTools) {
            tools.add(selected.toCustomTool());
        }
        return tools;
    }
    
    /**
     * The heart of the beast. Installs selected tools and exports their paths to the 
     * PATH and their HOMEs as environment variables.
     */
    @Override
    public Launcher decorateLauncher(AbstractBuild build, final Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException,
            RunnerAbortedException {

        EnvVars buildEnv = build.getEnvironment(listener); 
        final EnvVars homes = new EnvVars();
        final List<String> paths = new ArrayList<String>();
        
        //each tool can export zero or many directories to the PATH
        for (CustomTool tool : customTools()) {
            //this installs the tool if necessary
            CustomTool installed = tool.forEnvironment(buildEnv).forNode(Computer.currentComputer().getNode(), listener);
            listener.getLogger().println(tool.getName()+" is installed at "+ installed.getHome());

            homes.put(tool.getName()+"_HOME", installed.getHome());
            paths.addAll(installed.getPaths(Computer.currentComputer().getNode()));
        }


        return new DecoratedLauncher(launcher) {            
            @Override
            public Proc launch(ProcStarter starter) throws IOException {
                EnvVars vars = toEnvVars(starter.envs());
                
                for (String path : paths) {
                    vars.override("PATH+", path);
                }
                vars.putAll(homes);
                return super.launch(starter.envs(Util.mapToEnv(vars)));
            }

            private EnvVars toEnvVars(String[] envs) {
                EnvVars vars = new EnvVars();
                for (String line : envs) {
                    vars.addLine(line);
                }
                return vars;
            }
        };
    }
    
    @Override
    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        
        public DescriptorImpl() {
            super(CustomToolInstallWrapper.class);
        }

        public String getDisplayName() {
            return Messages.Descriptor_DisplayName();
        }

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }
        
        public CustomTool[] getInstallations() {
            return Hudson.getInstance().getDescriptorByType(CustomTool.DescriptorImpl.class).getInstallations();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json)
                throws hudson.model.Descriptor.FormException {
            // TODO Auto-generated method stub
            return super.configure(req, json);
        }

    }
}

