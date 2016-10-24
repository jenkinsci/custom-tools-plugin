/*
 * Copyright 2013 Oleg Nenashev, Synopsys Inc.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.BuildListener;
import javax.annotation.Nonnull;

/**
 * Provides logging routines for the plugin.
 * @author Oleg Nenashev
 * @since 0.3
 */
public class CustomToolsLogger {
    public static final String LOG_PREFIX = "[CustomTools] - ";
    
    /**
     * @deprecated Use {@link #logMessage(hudson.model.BuildListener, java.lang.String)}
     * instead.
     */
    @SuppressFBWarnings(value = "NM_METHOD_NAMING_CONVENTION", justification = "Deprecated, will be removed later")
    public static void LogMessage(@Nonnull BuildListener listener, String message) {
        logMessage(listener, message);
    }
    
    /**
     * @deprecated Use {@link #logMessage(hudson.model.BuildListener, java.lang.String, java.lang.String)}
     * instead.
     */
    @SuppressFBWarnings(value = "NM_METHOD_NAMING_CONVENTION", justification = "Deprecated, will be removed later")
    public static void LogMessage(@Nonnull BuildListener listener, String toolName, String message) {
        logMessage(listener, toolName, message);
    }
    
    public static void logMessage(@Nonnull BuildListener listener, String message) {
        listener.getLogger().println(CustomToolsLogger.LOG_PREFIX+message);
    }
    
    public static void logMessage(@Nonnull BuildListener listener, String toolName, String message) {
        listener.getLogger().println(CustomToolsLogger.LOG_PREFIX+toolName+": "+message);
    }
}
