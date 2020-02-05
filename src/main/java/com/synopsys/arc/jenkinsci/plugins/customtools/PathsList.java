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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Structure with list
 * @author Oleg Nenashev
 */
public class PathsList implements Serializable {
    public @Nonnull List<String> paths;
    /*Restored home dir*/
    public @CheckForNull String homeDir;
    public @CheckForNull String pathSeparator;
    public @CheckForNull String separator;

    public static final PathsList EMPTY = new PathsList(new LinkedList<String>(), null);

    /**
     * Constructor. Sets system's default separator and pathSeparator
     * @param paths List of paths to be returned
     * @param homeDir Home directory of the tool
     */
    public PathsList(@Nonnull Collection<String> paths, @CheckForNull String homeDir) {
        this(paths, File.pathSeparator, File.separator, homeDir);
    }

    /**
     * Empty constructor. doesn't set pathSeparator and separator
     */
    public PathsList() {
        this(new LinkedList<String>(), null, null, null);
    }

    public PathsList(@Nonnull Collection<String> paths,
            @CheckForNull String pathSeparator, @CheckForNull String separator,
            @CheckForNull String homeDir) {
        this.paths = new ArrayList<String>(paths);
        this.pathSeparator = pathSeparator;
        this.separator = separator;
        this.homeDir = homeDir;
    }

    public @CheckForNull String getHomeDir() {
        return homeDir;
    }

    public boolean add(@Nonnull String path) {
        return paths.add(path);
    }

    /**
     * Adds PathsList and overrides null variables.
     * @param pathsList PathsList to be added
     * @return True if the paths list has been modified after the tool installation
     */
    //TODO: Is it a bug?
    public boolean add(@Nonnull PathsList pathsList) {
        if (pathSeparator == null) {
            pathSeparator = pathsList.pathSeparator;
        }
        if (separator == null) {
            separator = pathsList.separator;
        }
        // Add homeDir as well (legacy behavior)
        if (pathsList.homeDir != null) {
            this.paths.add(pathsList.homeDir);
        }

        return this.paths.addAll(pathsList.paths);
    }

    /**
     * Gets the list of installed tools.
     * @return A list with valid delimiters or null if paths is empty
     */
    public @CheckForNull String toListString() {
        if (paths.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for ( String path : paths) {
            builder.append(path);
            builder.append(pathSeparator);
        }
        return builder.toString();
    }
}
