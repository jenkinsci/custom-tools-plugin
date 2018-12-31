package com.cloudbees.jenkins.plugins.customtools;

import com.cloudbees.jenkins.plugins.customtools.CustomToolInstallWrapper.DescriptorImpl;
import com.synopsys.arc.jenkinsci.plugins.customtools.CustomToolsLogger;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


import java.io.IOException;
import java.util.Locale;

@Extension
@Restricted(NoExternalUse.class)
public class CustomToolEnvironmentContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(Run r, EnvVars envs, TaskListener listener)
        throws IOException, InterruptedException {
        CustomTool.DescriptorImpl tools = ToolInstallation.all().get(CustomTool.DescriptorImpl.class);
        for (CustomTool tool : tools.getInstallations()) {
            if (tool.getHome() != null) {
                envs.put(tool.getName().toUpperCase(Locale.ENGLISH) +"_HOME", String.valueOf(tool.getHome()));
            }
        }
        super.buildEnvironmentFor(r, envs, listener);
    }
}
