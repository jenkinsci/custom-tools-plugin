/*
 * Copyright 2013 Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.synopsys.arc.jenkins.plugins.customtools.util;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * A stub wrapper, which injects a test variable into the build environment.
 *
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.4.1
 */
public class StubWrapper extends BuildWrapper {
    public static final String SCRIPT_TESTVAR_NAME = "SCRIPT_FOO_NAME";
    public static final String SCRIPT_TESTVAR_VALUE = "SCRIPT_FOO_VALUE";
    public static final String ENV_TESTVAR_NAME = "ENV_FOO_NAME";
    public static final String ENV_TESTVAR_VALUE = "ENV_FOO_VALUE";

    @Override
    public Launcher decorateLauncher(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException, Run.RunnerAbortedException {
        final Map<String, String> envs = new TreeMap<String, String>();
      
        return new Launcher.LocalLauncher(listener) {
            @Override
            public Proc launch(Launcher.ProcStarter ps) throws IOException {
                EnvVars envs = toEnvVars(ps.envs());
                envs.put(SCRIPT_TESTVAR_NAME, SCRIPT_TESTVAR_VALUE);
                return super.launch(ps.envs(Util.mapToEnv(envs)));
            }
            
            private EnvVars toEnvVars(String[] envs) {
                EnvVars vars = new EnvVars();
                for (String line : envs) {
                    vars.addLine(line);
                }
                return vars;
            }
        };
    }
    
    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public String getDisplayName() {
            return "Test stub";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> ap) {
            return true;
        }     
    }
    
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException {       
        return new Environment(){            
            @Override
            public void buildEnvVars(Map<String, String> env) {          
                env.put(ENV_TESTVAR_NAME, ENV_TESTVAR_VALUE);
            }
        };
    }
}
