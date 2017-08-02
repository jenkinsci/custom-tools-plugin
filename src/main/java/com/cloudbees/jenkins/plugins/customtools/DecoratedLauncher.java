/*
 * Copyright 2012, CloudBees Inc., Synopsys Inc. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.jenkins.plugins.customtools;

import hudson.Launcher;
import hudson.RestrictedSince;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

/**
 * A launcher which delegates to a provided inner launcher.
 * Allows subclasses to only implement methods they want to
 * override.
 *
 * @author rcampbell
 * @author Oleg Nenashev
 */
@Deprecated
@Restricted(DoNotUse.class)
@RestrictedSince("0.6")
public class DecoratedLauncher extends Launcher.DecoratedLauncher {

    public DecoratedLauncher(Launcher inner) {
        super(inner);
    }
}
