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
import java.util.Collection;
import java.util.LinkedList;
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

    public static final PathsList EMPTY = new PathsList(new LinkedList<String>(), null);

    /**
     * Constructor. Sets system's default separator and pathSeparator
     * @param paths List of paths to be returned
     */
    public PathsList(Collection<String> paths, String homeDir) {
        this(paths, File.pathSeparator, File.separator, homeDir);
    }

    /**
     * Empty constructor. doesn't set pathSeparator and separator
     */
    public PathsList() {
        this(new LinkedList<String>(), null, null, null);
    }
    
    public PathsList(Collection<String> paths, String pathSeparator, String separator, String homeDir) {
        this.paths = new ArrayList<String>(paths);
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
     * @return True if the paths list has been modified after the tool installation
     */
    //TODO: Is it a bug?
    public boolean add(PathsList pathsList) {
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
     * Gets the list of installed tools
     * @return A list with valid delimiters or null if paths is empty
     */
    public String toListString() {
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
