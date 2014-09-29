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

import com.synopsys.arc.jenkinsci.plugins.customtools.CustomToolsLogger;
import com.synopsys.arc.jenkinsci.plugins.customtools.CustomToolException;
import com.synopsys.arc.jenkinsci.plugins.customtools.EnvVariablesInjector;
import com.synopsys.arc.jenkinsci.plugins.customtools.LabelSpecifics;
import com.synopsys.arc.jenkinsci.plugins.customtools.PathsList;
import com.synopsys.arc.jenkinsci.plugins.customtools.multiconfig.MulticonfigWrapperOptions;
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersion;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Installs tools selected by the user. Exports configured paths and a home variable for each tool.
 * 
 * @author rcampbell
 * @author Oleg Nenashev
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
        private final String name;
        
        @DataBoundConstructor
        public SelectedTool(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
        
        public @CheckForNull CustomTool toCustomTool() {
            return ((CustomTool.DescriptorImpl)Hudson.getInstance().getDescriptor(CustomTool.class)).byName(name);
        }
        
        public @Nonnull CustomTool toCustomToolValidated() throws CustomToolException {
            CustomTool tool = toCustomTool();
            if (tool == null) {
                throw new CustomToolException(
                    Messages.CustomTool_GetToolByName_ErrorMessage(name));
            }
            return tool;
        }
    }
    
    private @Nonnull SelectedTool[] selectedTools = new SelectedTool[0];
    private final @CheckForNull MulticonfigWrapperOptions multiconfigOptions;    
    private final boolean convertHomesToUppercase;
    
    @DataBoundConstructor
    public CustomToolInstallWrapper(SelectedTool[] selectedTools, MulticonfigWrapperOptions multiconfigOptions, boolean convertHomesToUppercase) {
        this.selectedTools = (selectedTools != null) ? selectedTools : new SelectedTool[0];
        this.multiconfigOptions = (multiconfigOptions != null) ? multiconfigOptions : MulticonfigWrapperOptions.DEFAULT;
        this.convertHomesToUppercase = convertHomesToUppercase;
    }
    
    public boolean isConvertHomesToUppercase() {
        return convertHomesToUppercase;
    }   
    
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException {
        
        final EnvVars buildEnv = build.getEnvironment(listener);
        final Node node = build.getBuiltOn();
         
        return new Environment() {            
            @Override
            public void buildEnvVars(Map<String, String> env) {    
                
                // TODO: Inject Home dirs as well
                for (SelectedTool selectedTool : selectedTools) {
                    CustomTool tool = selectedTool.toCustomTool();
                    if (tool != null && tool.hasVersions()) {
                        ToolVersion version = ToolVersion.getEffectiveToolVersion(tool, buildEnv, node);   
                        if (version != null && !env.containsKey(version.getVariableName())) {
                            env.put(version.getVariableName(), version.getDefaultVersion());
                        }
                    }
                } 
            }
        };
    }
    
    public @Nonnull SelectedTool[] getSelectedTools() {
        return selectedTools.clone();
    }
    
    /**
     * The heart of the beast. Installs selected tools and exports their paths to the 
     * PATH and their HOMEs as environment variables.
     * @return A decorated launcher
     */
    @Override
    public Launcher decorateLauncher(AbstractBuild build, final Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException,
            RunnerAbortedException {
        
        EnvVars buildEnv = build.getEnvironment(listener); 
        final EnvVars homes = new EnvVars();
        final EnvVars versions = new EnvVars();
        
        final PathsList paths = new PathsList();
        final List<EnvVariablesInjector> additionalVarInjectors = new LinkedList<EnvVariablesInjector>();
        
        // Handle multi-configuration build
        if (build instanceof MatrixBuild) {  
            CustomToolsLogger.logMessage(listener, "Skipping installation of tools at the master job");
            if (getMulticonfigOptions().isSkipMasterInstallation()) {
                return launcher;
            }
        }
        
        // Each tool can export zero or many directories to the PATH
        final Node node =  Computer.currentComputer().getNode();
        if (node == null) {
            throw new CustomToolException("Cannot install tools on the deleted node");
        }
        
        for (SelectedTool selectedToolName : selectedTools) {
            CustomTool tool = selectedToolName.toCustomToolValidated();            
            CustomToolsLogger.logMessage(listener, tool.getName(), "Starting installation");
            
            // Check versioning
            checkVersions(tool, listener, buildEnv, node, versions);
            
            // This installs the tool if necessary
            CustomTool installed = tool
                    .forNode(node, listener)
                    .forEnvironment(buildEnv)
                    .forBuildProperties(build.getProject().getProperties());
            
            try {
                installed.check();
            } catch (CustomToolException ex) {
                throw new AbortException(ex.getMessage());
            }
            
            // Handle global options of the tool      
            //TODO: convert to label specifics?
            final PathsList installedPaths = installed.getPaths(node);          
            installed.correctHome(installedPaths);
            paths.add(installedPaths);
            if (installed.hasAdditionalVariables()) {
                additionalVarInjectors.add(
                   EnvVariablesInjector.Create(installed.getAdditionalVariables()));
            }

            // Handle label-specific options of the tool
            for (LabelSpecifics spec : installed.getLabelSpecifics()) {              
                if (!spec.appliesTo(node)) {
                    continue;
                }
                CustomToolsLogger.logMessage(listener, installed.getName(), "Label specifics from '"+spec.getLabel()+"' will be applied");
                               
                if (spec.hasAdditionalVars()) {
                    additionalVarInjectors.add(EnvVariablesInjector.Create(spec.getAdditionalVars()));
                }
            }
            
            CustomToolsLogger.logMessage(listener, installed.getName(), "Tool is installed at "+ installed.getHome());
            String homeDirVarName = (convertHomesToUppercase ? installed.getName().toUpperCase() : installed.getName()) +"_HOME";
            CustomToolsLogger.logMessage(listener, installed.getName(), "Setting "+ homeDirVarName+"="+installed.getHome());
            homes.put(homeDirVarName, installed.getHome());
        }

        return new DecoratedLauncher(launcher) {                    
            @Override
            public Proc launch(ProcStarter starter) throws IOException {           
                EnvVars vars;
                try { // Dirty hack, which allows to avoid NPEs in Launcher::envs()
                    vars = toEnvVars(starter.envs());
                } catch (NullPointerException npe) {
                    vars = new EnvVars();
                } catch (InterruptedException x) {
                    throw new IOException(x);
                }
                 
                // Inject paths
                final String injectedPaths = paths.toListString();              
                if (injectedPaths != null) {              
                    vars.override("PATH+", injectedPaths);
                }
                               
                // Inject additional variables
                vars.putAll(homes);
                vars.putAll(versions);
                for (EnvVariablesInjector injector : additionalVarInjectors) {
                    injector.Inject(vars);
                }
                           
                // Override paths to prevent JENKINS-20560              
                if (vars.containsKey("PATH")) {
                    final String overallPaths=vars.get("PATH");
                    vars.remove("PATH");
                    vars.put("PATH+", overallPaths);
                }
                
                return getInner().launch(starter.envs(vars));
            }
                        
            private EnvVars toEnvVars(String[] envs) throws IOException, InterruptedException {
                EnvVars vars = node.toComputer().getEnvironment();
                for (String line : envs) {
                    vars.addLine(line);
                }
                return vars;
            }
        };
    }
    
    /**
     * @deprecated The method is deprecated. It will be removed in future versions.
     * @throws CustomToolException
     */
    public void CheckVersions (CustomTool tool, BuildListener listener, EnvVars buildEnv, Node node, EnvVars target) 
            throws CustomToolException  {
        checkVersions(tool, listener, buildEnv, node, target);
    }
    
    /**
     * Checks versions and modify build environment if required.
     * @param tool Custom Tool
     * @param listener Build Listener
     * @param buildEnv Build Environment (can be modified)
     * @param node Target Node
     * @param target Build Internal Environment (can be modified)
     * @throws CustomToolException 
     * @since 0.4
     */
    public void checkVersions (@Nonnull CustomTool tool, @Nonnull BuildListener listener, 
            @Nonnull EnvVars buildEnv, @Nonnull Node node, @Nonnull EnvVars target) throws CustomToolException {
        // Check version
        if (tool.hasVersions()) {
            ToolVersion version = ToolVersion.getEffectiveToolVersion(tool, buildEnv, node);   
            if (version == null) {
                CustomToolsLogger.logMessage(listener, tool.getName(), "Error: No version has been specified, no default version. Failing the build...");
                throw new CustomToolException("Version has not been specified for the "+tool.getName());
            }
            
            CustomToolsLogger.logMessage(listener, tool.getName(), "Version "+version.getActualVersion()+" has been specified by "+version.getVersionSource());
            
            // Override default versions
            if (version.getVersionSource().equals(ToolVersion.DEFAULTS_SOURCE)) {            
                String envStr = version.getVariableName()+"="+version.getDefaultVersion();
                target.addLine(envStr);
                buildEnv.addLine(envStr);
            }
        }  
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
     * @return Configured options. Returns default options if not specified.
     * @since 0.3
     */
    public @Nonnull MulticonfigWrapperOptions getMulticonfigOptions() {
        return multiconfigOptions != null ? multiconfigOptions : MulticonfigWrapperOptions.DEFAULT;
    }  

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        
        public DescriptorImpl() {
            super(CustomToolInstallWrapper.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.Descriptor_DisplayName();
        }

        @Override
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

