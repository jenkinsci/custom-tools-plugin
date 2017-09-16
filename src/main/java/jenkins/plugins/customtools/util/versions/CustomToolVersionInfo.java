package jenkins.plugins.customtools.util.versions;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Describes Tool Version information.
 * The object is being used for version resolution in Jenkins.
 * This class may be propagated to Jenkins agents in order to evaluate the effective version value.
 *
 * @author Oleg Nenashev
 * @since 1.0
 */
public class CustomToolVersionInfo implements Serializable {
    @Nonnull
    private final String variableName;
    @CheckForNull
    private final String defaultVersion;
    @CheckForNull
    private final String actualVersion;
    @CheckForNull
    private final String versionSource;

    public CustomToolVersionInfo(@Nonnull String variableName, @CheckForNull String defaultVersion,
                       @CheckForNull String actualVersion, @CheckForNull String versionSource) {
        this.variableName = variableName;
        this.defaultVersion = defaultVersion;
        this.actualVersion = actualVersion;
        this.versionSource = versionSource;
    }

    /**
     * Gets name of the tool version environment variable.
     * @return Environment variable name
     */
    @Nonnull
    public String getVariableName() {
        return variableName;
    }

    @CheckForNull
    public String getDefaultVersion() {
        return defaultVersion;
    }

    public boolean hasDefaultVersion() {
        return defaultVersion != null;
    }

    @CheckForNull
    public String getActualVersion() {
        return actualVersion;
    }

    @CheckForNull
    public String getVersionSource() {
        return versionSource;
    }
}
