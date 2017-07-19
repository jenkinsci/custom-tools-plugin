/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.customtools;

import com.synopsys.arc.jenkinsci.plugins.customtools.*;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Proc;
import hudson.matrix.MatrixBuild;
import hudson.model.*;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Decorates the Launcher
 *
 * @author rcampbell
 * @author Oleg Nenashev
 */
public class CustomToolsLauncherDecorator {

    public static Launcher decorate(CustomToolInstallWrapper wrapper, Run run, final Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {
        EnvVars buildEnv = run.getEnvironment(listener);
        final EnvVars homes = new EnvVars();
        final EnvVars versions = new EnvVars();

        final PathsList paths = new PathsList();
        final List<EnvVariablesInjector> additionalVarInjectors = new LinkedList<EnvVariablesInjector>();

        // Handle multi-configuration build
        if (run instanceof MatrixBuild) {
            CustomToolsLogger.logMessage(listener, "Skipping installation of tools at the master job");
            if (wrapper.getMulticonfigOptions().isSkipInstallationOnMaster()) {
                return launcher;
            }
        }

        // Each tool can export zero or many directories to the PATH
        final Node node = launcher.getComputer().getNode();
        if (node == null) {
            throw new CustomToolException("Cannot install tools on the deleted node");
        }

        for (CustomToolInstallWrapper.SelectedTool selectedToolName : wrapper.getSelectedTools()) {
            CustomTool tool = selectedToolName.toCustomToolValidated();
            CustomToolsLogger.logMessage(listener, tool.getName(), "Starting installation");

            // Check versioning
            wrapper.checkVersions(tool, listener, buildEnv, node, versions);

            // This installs the tool if necessary
            CustomTool installed = tool
                    .forNode(node, listener)
                    .forEnvironment(buildEnv)
                    .forBuildProperties(run.getParent().getProperties());

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
            final String additionalVars = installed.getAdditionalVariables();
            if (additionalVars != null) {
                additionalVarInjectors.add(EnvVariablesInjector.create(additionalVars));
            }

            // Handle label-specific options of the tool
            for (LabelSpecifics spec : installed.getLabelSpecifics()) {
                if (!spec.appliesTo(node)) {
                    continue;
                }
                CustomToolsLogger.logMessage(listener, installed.getName(), "Label specifics from '" + spec.getLabel() + "' will be applied");

                final String additionalLabelSpecificVars = spec.getAdditionalVars();
                if (additionalLabelSpecificVars != null) {
                    additionalVarInjectors.add(EnvVariablesInjector.create(additionalLabelSpecificVars));
                }
            }

            CustomToolsLogger.logMessage(listener, installed.getName(), "Tool is installed at " + installed.getHome());
            String homeDirVarName = (wrapper.isConvertHomesToUppercase() ? installed.getName().toUpperCase(Locale.ENGLISH) : installed.getName()) + "_HOME";
            CustomToolsLogger.logMessage(listener, installed.getName(), "Setting " + homeDirVarName + "=" + installed.getHome());
            homes.put(homeDirVarName, installed.getHome());
        }

        return new Launcher.DecoratedLauncher(launcher) {
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
                    final String overallPaths = vars.get("PATH");
                    vars.remove("PATH");
                    vars.put("PATH+", overallPaths);
                }

                return getInner().launch(starter.envs(vars));
            }

            private EnvVars toEnvVars(String[] envs) throws IOException, InterruptedException {
                Computer computer = node.toComputer();
                EnvVars vars = computer != null ? computer.getEnvironment() : new EnvVars();
                for (String line : envs) {
                    vars.addLine(line);
                }
                return vars;
            }
        };
    }
}
