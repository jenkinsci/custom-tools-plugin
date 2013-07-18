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
package com.synopsys.arc.jenkinsci.plugins.customtools;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Structure with list
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 */
public class PathsList implements Serializable {
    public List<String> paths;
    /*Restored home dir*/
    public String homeDir;
    public String pathSeparator;
    public String separator;
    
    public static final PathsList EMPTY = new PathsList(new String[0], null);

    /**
     * Constructor. Sets system's default separator and pathSeparator
     * @param paths List of paths to be returned
     */
    public PathsList(String[] paths, String homeDir) {
        this(paths, File.pathSeparator, File.separator, homeDir);
    }

    /**
     * Empty constructor. doesn't set pathSeparator and separator
     */
    public PathsList() {
        this(new String[0], null, null, null);
    }
    
    public PathsList(String[] paths, String pathSeparator, String separator, String homeDir) {
        this.paths = new ArrayList<String>(Arrays.asList(paths));
        this.pathSeparator = pathSeparator;
        this.separator = separator;
        this.homeDir = homeDir;
    }

    public String getHomeDir() {
        return homeDir;
    }
     
    public boolean add(String path) {
        return paths.add(path);
    }
    
    /**
     * Adds PathsList and overrides null variables.
     * @param pathsList PathsList to be added
     * @return True if 
     */
    public boolean add(PathsList pathsList) {
        if (pathSeparator == null) {
            pathSeparator = pathsList.pathSeparator;
        }
        if (separator == null) {
            separator = pathsList.separator;
        }
        if (homeDir == null) {
            homeDir = pathsList.homeDir;
        }
        return this.paths.addAll(pathsList.paths);
    }
    
    public String toListString() {
        StringBuilder builder = new StringBuilder(paths.size()*2);
        for ( String path : paths) {
            builder.append(pathSeparator);
            builder.append(path);
        } 
        return builder.toString();
    }
}
