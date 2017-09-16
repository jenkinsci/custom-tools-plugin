package jenkins.plugins.customtools.util.versions;

import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author Oleg Nenashev
 * @since TODO
 */
public abstract class CustomToolVersionProvider extends AbstractDescribableImpl<CustomToolVersionProvider>
        implements ExtensionPoint {

    private static final String DEFAULTS_SOURCE = "defaults";

    public abstract CustomToolVersionInfo resolveToolVersion(@Nonnull CustomTool tool, @Nonnull Node node,
                                                             @CheckForNull Run<?,?> run, @CheckForNull EnvVars extraEnvVars,
                                                             @Nonnull TaskListener listener);

    public static abstract class ToolVersionProviderDescriptor extends Descriptor<CustomToolVersionProvider> {

    }

    public static String getDefaultsSource() {
        return DEFAULTS_SOURCE;
    }
}
