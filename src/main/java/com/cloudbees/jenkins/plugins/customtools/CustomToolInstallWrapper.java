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
import com.synopsys.arc.jenkinsci.plugins.customtools.EnvVariablesInjector;
import com.synopsys.arc.jenkinsci.plugins.customtools.LabelSpecifics;
import com.synopsys.arc.jenkinsci.plugins.customtools.PathsList;
import com.synopsys.arc.jenkinsci.plugins.customtools.multiconfig.MulticonfigWrapperOptions;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Run.RunnerAbortedException;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sf.json.JSONObject;

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
    private MulticonfigWrapperOptions multiconfigOptions;    
    
    @DataBoundConstructor
    public CustomToolInstallWrapper(SelectedTool[] selectedTools, MulticonfigWrapperOptions multiconfigOptions) {
        this.selectedTools = (selectedTools != null) ? selectedTools : new SelectedTool[0];
        this.multiconfigOptions = (multiconfigOptions != null) ? multiconfigOptions : MulticonfigWrapperOptions.DEFAULT;
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
        final PathsList paths = new PathsList();
        final List<EnvVariablesInjector> additionalVarInjectors = new LinkedList<EnvVariablesInjector>();
        
        // Handle multi-configuration build
        if (MatrixBuild.class.isAssignableFrom(build.getClass())) {
            listener.getLogger().println("[CustomTools] - Skipping installation of tools at the master job");
            if (multiconfigOptions.isSkipMasterInstallation()) {
                return launcher;
            }
        }
        
        //each tool can export zero or many directories to the PATH
        for (CustomTool tool : customTools()) {
            //this installs the tool if necessary
            CustomTool installed = tool
                    .forNode(Computer.currentComputer().getNode(), listener)
                    .forEnvironment(buildEnv)
                    .forBuildProperties(build.getProject().getProperties());
            
            try {
                installed.check();
            } catch (CustomToolException ex) {
                throw new AbortException(ex.getMessage());
            }
            
            // Prepare additional variables
            Node node =  Computer.currentComputer().getNode();
            PathsList installedPaths = installed.getPaths(node);          
            installed.correctHome(installedPaths);
            paths.add(installedPaths);

            for (LabelSpecifics spec : installed.getLabelSpecifics()) {
                if (!spec.appliesTo(node)) {
                    continue;
                }
                               
                if (spec.hasAdditionalVars()) {
                    additionalVarInjectors.add(EnvVariablesInjector.Create(spec.getAdditionalVars()));
                }
            }
            
            listener.getLogger().println("[CustomTools] - "+installed.getName()+" is installed at "+ installed.getHome());
            homes.put(installed.getName()+"_HOME", installed.getHome());
        }


        return new DecoratedLauncher(launcher) {            
            @Override
            public Proc launch(ProcStarter starter) throws IOException {
                EnvVars vars = toEnvVars(starter.envs());
                
                // HACK: Avoids issue with invalid separators in EnvVars::override in case of different master/slave
                String overridenPaths = vars.get("PATH");
                overridenPaths += paths.toListString();
                vars.override("PATH", overridenPaths);
                vars.putAll(homes);
                
                // Inject additional variables
                for (EnvVariablesInjector injector : additionalVarInjectors) {
                    injector.Inject(vars);
                }
                
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
    
    /**
     * Check if build has specific multi-configuration options
     * @return True if multi-configuration options are configured
     * @since 0.3
     */
    public boolean hasMulticonfigOptions() {
        return multiconfigOptions != MulticonfigWrapperOptions.DEFAULT;
    }

    /**
     * Gets multi-configuration job parameters.
     * @return 
     * @since 0.3
     */
    public MulticonfigWrapperOptions getMulticonfigOptions() {
        return multiconfigOptions;
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
            //TODO: Auto-generated method stub
            return super.configure(req, json);
        }     
    }    
}

