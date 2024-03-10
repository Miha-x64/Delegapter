/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.util.DisplayMetrics;
import android.util.TypedValue;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.util.TypedValue.*;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public final class ComplexDimension {

    // Mike-BORROWED android.util.TypedValue.ComplexDimensionUnit
    @IntDef({COMPLEX_UNIT_PX, COMPLEX_UNIT_DIP, COMPLEX_UNIT_SP, COMPLEX_UNIT_PT, COMPLEX_UNIT_IN, COMPLEX_UNIT_MM})
    @Retention(RetentionPolicy.SOURCE)
    /*Mike-REMOVED @hide*/ public @interface ComplexDimensionUnit {
    }

    // Mike-BORROWED android.util.TypedValue#createComplexDimension(int, int)
    /**
     * <p>Creates a complex data integer that stores a dimension value and units.
     *
     * <p>The resulting value can be passed to e.g.
     * {@link TypedValue#complexToDimensionPixelOffset(int, DisplayMetrics)} to calculate the pixel
     * value for the dimension.
     *
     * @param value the value of the dimension
     * @param units the units of the dimension, e.g. {@link TypedValue#COMPLEX_UNIT_DIP}
     * @return A complex data integer representing the value and units of the dimension.
     */
    /*Mike-CHANGED to package-private instead of @hide*/ static int createComplexDimension(
        @IntRange(from = -0x800000, to = 0x7FFFFF) int value, @ComplexDimensionUnit int units) {
        if (units < TypedValue.COMPLEX_UNIT_PX || units > TypedValue.COMPLEX_UNIT_MM) {
            throw new IllegalArgumentException("Must be a valid COMPLEX_UNIT_*: " + units);
        }
        if (value < -0x800000 || value >= 0x800000) {
            throw new IllegalArgumentException("Magnitude of the value is too large: " + value);
        }
        // Mike-PICKED from android.util.TypedValue#intToComplex and android.util.TypedValue#createComplex
        return ((value & TypedValue.COMPLEX_MANTISSA_MASK) << TypedValue.COMPLEX_MANTISSA_SHIFT)
            | (TypedValue.COMPLEX_RADIX_23p0 << TypedValue.COMPLEX_RADIX_SHIFT) | units;
    }

    // Mike-BORROWED android.util.TypedValue#DIMENSION_UNIT_STRS
    private static final String[] UNITS = new String[]{
        "px", /* Mike-CHANGED from "dip" to */ "dp", "sp", "pt", "in", "mm"
    };

    static String toString(int value) {
        if (value == WRAP_CONTENT) return "WRAP_CONTENT"; // Mike-ADDED

        // Mike-PICKED from android.util.TypedValue.coerceToString(int, int)
        float v = complexToFloat(value);
        return (((int) v == v) ? Integer.toString((int) v) : Float.toString(v)) +
            UNITS[(value >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK];
    }

}
