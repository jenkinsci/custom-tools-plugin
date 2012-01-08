package com.cloudbees.plugins.customtools;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.CommandInstaller;
import hudson.tools.ToolInstallation;

/**
 * A command installer which is easy on users since they don't have to write idempotent scripts.
 * 
 * The installer keeps a of the script in a hidden file, and only reruns the script
 * if the hash of the script changes.
 * 
 * @author rcampbell
 *
 */
public class EasyCommandInstaller extends CommandInstaller {

    @DataBoundConstructor
    public EasyCommandInstaller(String label, String command, String toolHome) {
        super(label, command, toolHome);
    }
    
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath dir = preferredLocation(tool, node);
        
        if (hashMarker(dir).exists()) {
            if (hashMarker(dir).readToString().equals(commandDigest())) {
                return dir.child(getToolHome());    
            } else {
                log.getLogger().println("Reinstalling "+tool.getName()+" because the command changed");
                dir.deleteContents();
                //and continue with installation
            }
        }
        
        // XXX support Windows batch scripts, Unix scripts with interpreter line, etc. (see CommandInterpreter subclasses)
        FilePath script = dir.createTextTempFile("hudson", ".sh", getCommand());
        try {
            String[] cmd = {"sh", "-ex", script.getRemote()};
            int r = node.createLauncher(log).launch().cmds(cmd).stdout(log).pwd(dir).join();
            if (r != 0) {
                throw new IOException("Command returned status " + r);
            }
        } finally {
            script.delete();
        }
        
        hashMarker(dir).write(commandDigest(), null);
        
        return dir.child(getToolHome());
    }

    private String commandDigest() {
        return Util.getDigestOf(getCommand());
    }

    private FilePath hashMarker(FilePath expectedLocation) {
        return expectedLocation.child(".script.md5sum");
    }
    
    @Extension
    public static class DescriptorImpl extends CommandInstaller.DescriptorImpl {
        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType.equals(CustomTool.class);
        }
        
        public String getDisplayName() {
            return Messages.EasyCommandInstaller_DescriptorImpl_DisplayName();
        }

    }
    
}
