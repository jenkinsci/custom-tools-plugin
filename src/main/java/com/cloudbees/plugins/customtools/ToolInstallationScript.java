package com.cloudbees.plugins.customtools;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.tasks.Shell;

import org.kohsuke.stapler.DataBoundConstructor;


public class ToolInstallationScript {

    private String script;

    @DataBoundConstructor
    public ToolInstallationScript(String script) {
        this.script = script;
    }
    
    public void run(Launcher launcher, BuildListener listener) {
        
        
    }
    
    
}
