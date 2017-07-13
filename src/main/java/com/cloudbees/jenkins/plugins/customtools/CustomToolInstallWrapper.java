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
import com.synopsys.arc.jenkinsci.plugins.customtools.multiconfig.MulticonfigWrapperOptions;
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.*;
import hudson.model.*;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import jenkins.model.Jenkins;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
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
            return ((CustomTool.DescriptorImpl) Jenkins.getActiveInstance().getDescriptor(CustomTool.class)).byName(name);
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
    private @CheckForNull MulticonfigWrapperOptions multiconfigOptions = MulticonfigWrapperOptions.DEFAULT;
    private boolean convertHomesToUppercase = false;
    
    @DataBoundConstructor
    public CustomToolInstallWrapper(SelectedTool[] selectedTools) {
        this.selectedTools = (selectedTools != null) ? selectedTools : new SelectedTool[0];
    }

    @DataBoundSetter
    public void setMulticonfigOptions (MulticonfigWrapperOptions multiconfigOptions){
        this.multiconfigOptions = (multiconfigOptions != null) ? multiconfigOptions : MulticonfigWrapperOptions.DEFAULT;
    }

    @DataBoundSetter
    public void setConvertHomesToUppercase (boolean convertHomesToUppercase) {
        this.convertHomesToUppercase = convertHomesToUppercase;
    }
    
    public boolean isConvertHomesToUppercase() {
        return convertHomesToUppercase;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return setUp((Run)build, launcher, listener);
    }

    public Environment setUp(Run run, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        assert run.getParent() instanceof BuildableItem;
        final EnvVars buildEnv = run.getEnvironment(listener);
        final Node node = ((BuildableItem) run.getParent()).getLastBuiltOn();

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
        return CustomToolsLauncherDecorator.decorate(this, build, launcher, listener);
    }
    
    /**
     * @deprecated The method is deprecated. It will be removed in future versions.
     * @throws CustomToolException
     */
    @SuppressFBWarnings(value = "NM_METHOD_NAMING_CONVENTION", justification = "Deprecated, will be removed later")
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
            final String versionSource = version.getVersionSource();
            if (versionSource != null && versionSource.equals(ToolVersion.DEFAULTS_SOURCE)) {            
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
            return Jenkins.getActiveInstance().getDescriptorByType(CustomTool.DescriptorImpl.class).getInstallations();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json)
                throws hudson.model.Descriptor.FormException {
            //TODO: Auto-generated method stub
            return super.configure(req, json);
        }     
    }    
}

