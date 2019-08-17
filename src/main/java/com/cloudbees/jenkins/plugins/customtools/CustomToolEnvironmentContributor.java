package com.cloudbees.jenkins.plugins.customtools;


import com.synopsys.arc.jenkinsci.plugins.customtools.CustomToolException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.Util;
import hudson.tools.ToolProperty;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import java.io.StringReader;
import java.util.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;


import java.io.IOException;

@Extension
@Restricted(NoExternalUse.class)
public class CustomToolEnvironmentContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(Run r, EnvVars envs, TaskListener listener)
        throws IOException, InterruptedException {
        //Checking if the tool propeties are actually configured so this only injects when there are custom tools configured
        List<ToolProperty<ToolInstallation>> toolProperties = new ArrayList<ToolProperty<ToolInstallation>>();
        if (toolProperties != null) {
            Properties properties = new Properties();

            //Grabbing all the custom tool items
            CustomTool.DescriptorImpl tools = ToolInstallation.all().get(CustomTool.DescriptorImpl.class);
            for (CustomTool tool : tools.getInstallations()) {
                //injecting the home for the tool
                if (tool.getHome() != null) {
                    envs.put(tool.getName().toUpperCase(Locale.ENGLISH) +"_HOME", String.valueOf(tool.getHome()));
                }
                //injecting the string of other defined variables
                if (tool.hasAdditionalVariables()){

                    String additionalVariables = tool.getAdditionalVariables();
                    if (additionalVariables != null) {
                        //Regex to escape any single \ characters which are not before \ or n. This is puilled from ENVinject plugin
                        additionalVariables = additionalVariables.replaceAll("(?<=[^\\\\])\\\\(?=[^n])(?=[^\\\\])(?=[^\n])", "\\\\\\\\");
                        StringReader stringReader = new StringReader(additionalVariables);
                        //TO DO: Eventually update this to be try with resources but this plugin is compiled against old versions
                        //of Jenkins so we can not guarantee Java 7
                        try {
                            properties.load(stringReader);
                        } catch (IOException ioe) {
                            throw new CustomToolException("Problem occurs on loading additional string content", ioe);
                        } finally {
                            stringReader.close();
                        }
                        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                            envs.put(processElement(entry.getKey(), envs), processElement(entry.getValue(), envs));
                        }
                    }
                }
            }
            super.buildEnvironmentFor(r, envs, listener);
        }
    }
    @CheckForNull
    private String processElement(@CheckForNull Object prop, @Nonnull Map<String, String> currentEnvVars) {
        String macroProcessedElement = Util.replaceMacro(String.valueOf(prop), currentEnvVars);
        if (macroProcessedElement == null) {
            return null;
        }
        return macroProcessedElement.trim();
    }
}
