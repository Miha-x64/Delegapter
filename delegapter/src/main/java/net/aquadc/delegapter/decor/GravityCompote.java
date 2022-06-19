/*
 * Copyright (C) 2006 The Android Open Source Project
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
package net.aquadc.delegapter.decor;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER;
import static android.view.Gravity.CENTER_HORIZONTAL;
import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.Gravity.CLIP_HORIZONTAL;
import static android.view.Gravity.CLIP_VERTICAL;
import static android.view.Gravity.END;
import static android.view.Gravity.FILL;
import static android.view.Gravity.FILL_HORIZONTAL;
import static android.view.Gravity.FILL_VERTICAL;
import static android.view.Gravity.LEFT;
import static android.view.Gravity.RIGHT;
import static android.view.Gravity.START;
import static android.view.Gravity.TOP;

// Mike-BORROWED from android.view.Gravity

/**
 * Standard constants and tools for placing an object within a potentially
 * larger container.
 */
/*Mike-CHANGED public to package-private final*/
final class GravityCompote /*Mike-ADDED some compote 🍷*/ {

    // Mike-BORROWED android.view.Gravity.GravityFlags
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {
        FILL,
        FILL_HORIZONTAL,
        FILL_VERTICAL,
        START,
        END,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        CENTER,
        CENTER_HORIZONTAL,
        CENTER_VERTICAL,
// Mike-REMOVED DISPLAY_CLIP_HORIZONTAL, DISPLAY_CLIP_VERTICAL, (not applicable)
        CLIP_HORIZONTAL,
        CLIP_VERTICAL,
// Mike-REMOVED NO_GRAVITY (useless)
    })
    /*Mike-CHANGED to package-private instead of @hide*/ @interface GravityFlags {
    }

    // Mike-BORROWED android.view.Gravity#toString
    /*Mike-CHANGED to package-private instead of @hide*/ static String toString(int gravity) {
        final StringBuilder result = new StringBuilder();
        if ((gravity & FILL) == FILL) {
            result.append("FILL").append(' ');
        } else {
            if ((gravity & FILL_VERTICAL) == FILL_VERTICAL) {
                result.append("FILL").append('_').append("VERTICAL").append(' '); // Mike-CHANGED split string
            } else {
                if ((gravity & TOP) == TOP) {
                    result.append("TOP").append(' ');
                }
                if ((gravity & BOTTOM) == BOTTOM) {
                    result.append("BOTTOM").append(' ');
                }
            }
            if ((gravity & FILL_HORIZONTAL) == FILL_HORIZONTAL) {
                result.append("FILL").append('_').append("HORIZONTAL").append(' '); // Mike-CHANGED split string
            } else {
                if ((gravity & START) == START) {
                    result.append("START").append(' ');
                } else if ((gravity & LEFT) == LEFT) {
                    result.append("LEFT").append(' ');
                }
                if ((gravity & END) == END) {
                    result.append("END").append(' ');
                } else if ((gravity & RIGHT) == RIGHT) {
                    result.append("RIGHT").append(' ');
                }
            }
        }
        if ((gravity & CENTER) == CENTER) {
            result.append("CENTER").append(' ');
        } else {
            if ((gravity & CENTER_VERTICAL) == CENTER_VERTICAL) {
                result.append("CENTER").append('_').append("VERTICAL").append(' '); // Mike-CHANGED split string
            }
            if ((gravity & CENTER_HORIZONTAL) == CENTER_HORIZONTAL) {
                result.append("CENTER").append('_').append("HORIZONTAL").append(' '); // Mike-CHANGED split string
            }
        }
        // Mike-REMOVED "NO GRAVITY" option
        // Mike-REMOVED DISPLAY_CLIP_ handling
        // Mike-ADDED CLIP_ handling
        if ((gravity & (CLIP_VERTICAL | CLIP_HORIZONTAL)) == (CLIP_VERTICAL | CLIP_HORIZONTAL)) {
            result.append("CLIP").append(' ');
        } else {
            if ((gravity & CLIP_VERTICAL) == CLIP_VERTICAL) {
                result.append("CLIP").append('_').append("VERTICAL").append(' '); // Mike-CHANGED split string
            }
            if ((gravity & CLIP_HORIZONTAL) == CLIP_HORIZONTAL) {
                result.append("CLIP").append('_').append("HORIZONTAL").append(' '); // Mike-CHANGED split string
            }
        }
        // END Mike-ADDED
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

}
