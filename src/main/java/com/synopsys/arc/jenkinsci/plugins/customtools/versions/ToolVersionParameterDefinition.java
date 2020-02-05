package com.synopsys.arc.jenkinsci.plugins.customtools.versions;

import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import com.synopsys.arc.jenkinsci.plugins.customtools.Messages;
import hudson.Extension;
import hudson.cli.CLICommand;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.tools.ToolInstallation;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import hudson.model.StringParameterValue;

/**
 * Implements "Tool version" parameter.
 * @author Oleg Nenashev
 * @since 0.4
 */
public class ToolVersionParameterDefinition extends ParameterDefinition {

    private final String toolName;

    @DataBoundConstructor
    public ToolVersionParameterDefinition(String toolName) {
        super( ToolVersionHelper.getVersionDescr(toolName).getName(),
               ToolVersionHelper.getVersionDescr(toolName).getDescription());
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }

    /**
     * Gets a {@link CustomTool} linked with this Parameter definition.
     * @return A custom tool or null if it has not been found
     */
    public final @CheckForNull CustomTool getTool() {
        CustomTool.DescriptorImpl tools = ToolInstallation.all().get(CustomTool.DescriptorImpl.class);
        return tools != null ? tools.byName(toolName) : null;
    }

    /**
     * Gets a Tool Version configuration for the parameter definition.
     * @return A tool version configuration or null if the tool cannot be found.
     */
    public final @CheckForNull ToolVersionConfig getVersionConfig() {
        CustomTool tool = getTool();
        return tool != null ? tool.getToolVersion() : null;
    }

    private @Nonnull ExtendedChoiceParameterDefinition getVersionsListSource() {
       ToolVersionConfig versionConfig = getVersionConfig();
        if (versionConfig == null) {
            throw new IllegalStateException(
                    Messages.Versions_ToolVersionParameterDefinition_GetVersionConfigError(toolName));
        }

        return versionConfig.getVersionsListSource();
    }

    @Override
    public StringParameterValue createValue(StaplerRequest req, JSONObject jo)
            throws IllegalStateException {
        StringParameterValue paramVal = (StringParameterValue)
                getVersionsListSource().createValue(req, jo);
        if (paramVal == null) {
            return null;
        }
        return new StringParameterValue(paramVal.getName(), paramVal.value);
    }

    @Override
    public StringParameterValue createValue(StaplerRequest req) {
        StringParameterValue paramVal = (StringParameterValue)
                getVersionsListSource().createValue(req);
        if (paramVal == null) {
            return null;
        }
        return new StringParameterValue(paramVal.getName(), paramVal.value);
    }

    @Override
    public ParameterValue createValue(CLICommand command, String value) throws InterruptedException {
        final String paramName = getVersionsListSource().getName();
        return new StringParameterValue(paramName, value);
    }

    @Override
    public ParameterValue getDefaultParameterValue() {
        StringParameterValue paramVal = (StringParameterValue)
                getVersionsListSource().getDefaultParameterValue();
        if (paramVal == null) {
            return null;
        }
        return new StringParameterValue(paramVal.getName(), paramVal.value);
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.Versions_ToolVersionParameterDefinition_DisplayName();
        }
    }
}