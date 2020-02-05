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

import java.util.Arrays;
import javax.annotation.CheckForNull;

/**
 * @author Oleg Nenashev
 */
public class ArrayHelper {

    /**
     * Merges two arrays
     * @param <TItemType> Type of the items
     * @param first First array
     * @param second Second array
     * @return Merged array
     */
    @CheckForNull
    public static <TItemType> TItemType[] merge(@CheckForNull TItemType[] first, @CheckForNull TItemType[] second) {
        // Handle nulls
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        // Handle two arrays
        TItemType[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
