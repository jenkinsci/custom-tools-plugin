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
package com.synopsys.arc.jenkinsci.plugins.customtools;

import hudson.model.BuildListener;

/**
 *
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.3
 */
public class CustomToolsLogger {
    public static final String LOG_PREFIX = "[CustomTools] - ";
    
    public static void LogMessage(BuildListener listener, String message) {
        listener.getLogger().println(CustomToolsLogger.LOG_PREFIX+message);
    }
    
    public static void LogMessage(BuildListener listener, String toolName, String message) {
        listener.getLogger().println(CustomToolsLogger.LOG_PREFIX+toolName+": "+message);
    }
}
