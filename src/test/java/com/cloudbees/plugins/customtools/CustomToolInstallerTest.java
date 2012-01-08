package com.cloudbees.plugins.customtools;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstallation;
import hudson.util.StreamTaskListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.jvnet.hudson.test.HudsonTestCase;

import com.cloudbees.plugins.customtools.CustomTool.DescriptorImpl;


public class CustomToolInstallerTest extends HudsonTestCase {

    private DescriptorImpl tools;

    public void testSmoke() throws IOException, InterruptedException {
        VirtualChannel channel = Hudson.getInstance().getChannel();
        EnvVars envVars = new EnvVars();
        
        
        CustomTool installation = createTool("MyTrue");        
        
        TaskListener listener = new StreamTaskListener(System.out);
        installation = installation.forEnvironment(envVars).forNode(Computer.currentComputer().getNode(), listener);
        
        if (!   new FilePath(channel, installation.getHome()).exists()) {
            throw new IOException(installation.getHome() + "doesn't exist!");
        }
    }

    public void testBasicCase() throws Exception {
        hudson.setNumExecutors(0);
        createSlave();
        tools = hudson.getDescriptorByType(CustomTool.DescriptorImpl.class);
        tools.setInstallations(createTool("MyTrue"));
        FreeStyleProject project = createFreeStyleProject();
        CustomToolInstallWrapper.SelectedTool selectedTool = new CustomToolInstallWrapper.SelectedTool("MyTrue");
        
        CustomToolInstallWrapper wrapper = new CustomToolInstallWrapper(
                new CustomToolInstallWrapper.SelectedTool[] { selectedTool });
        project.getBuildWrappersList().add(wrapper);
        Builder b = new Shell("echo $PATH; mytrue");
        project.getBuildersList().add(b);
        Future<FreeStyleBuild> build = project.scheduleBuild2(0);
        assertBuildStatusSuccess(build);
        
    }

        
        
    private CustomTool createTool(String name) throws IOException {
        List<ToolInstaller> installers = new ArrayList<ToolInstaller>();
        installers.add(new CommandInstaller(null, "ln -s `which true` mytrue",
                "./"));

        List<ToolProperty<ToolInstallation>> properties = new ArrayList<ToolProperty<ToolInstallation>>();
        properties.add(new InstallSourceProperty(installers));

        CustomTool installation = new CustomTool("MyTrue", null, properties,
                "./");
        return installation;
    }
    
}
