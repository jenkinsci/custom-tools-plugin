/*
 * Copyright 2013 Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc..
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

import hudson.Extension;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.io.IOException;
import java.util.Map;

/**
 * Implements a wrapper, which contains internal launcher call.
 * The wrapper aims <a href="https://issues.jenkins-ci.org/browse/JENKINS-19506">JENKINS-19506</a> issue.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.4.1
 */
public class CommandCallerInstaller extends StubWrapper {

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        ProcStarter starter = launcher.launch().cmds("echo","Hello");
        starter.start();
        starter.join();
        
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                super.buildEnvVars(env); //To change body of generated methods, choose Tools | Templates.
            } 
        };  
    }
    
    @Extension
    public static class DescriptorImpl extends StubWrapper.DescriptorImpl {
        
    }
}
