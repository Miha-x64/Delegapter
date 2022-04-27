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
package net.aquadc.delegapter;

import android.util.TypedValue;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.util.TypedValue.*;

final class ComplexDimension {

    @IntDef({COMPLEX_UNIT_PX, COMPLEX_UNIT_DIP, COMPLEX_UNIT_SP, COMPLEX_UNIT_PT, COMPLEX_UNIT_IN, COMPLEX_UNIT_MM})
    @Retention(RetentionPolicy.SOURCE)
    @interface Unit {
    }

    static int createComplexDimension(@FloatRange(from = -0x800000, to = 0x7FFFFF) float value, @Unit int unit) {
        if (unit < COMPLEX_UNIT_PX || unit > TypedValue.COMPLEX_UNIT_MM) {
            throw new IllegalArgumentException("Must be a valid COMPLEX_UNIT_*: " + unit);
        }
        return floatToComplex(value) | unit;
    }

    private static int floatToComplex(@FloatRange(from = -0x800000, to = 0x7FFFFF) float value) {
        // validate that the magnitude fits in this representation
        if (value < (float) -0x800000 - .5f || value >= (float) 0x800000 - .5f) {
            throw new IllegalArgumentException("Magnitude of the value is too large: " + value);
        }
        try {
            // If there's no fraction, use integer representation, as that's clearer
            if (value == (float) (int) value) {
                return createComplex((int) value, TypedValue.COMPLEX_RADIX_23p0);
            }
            float absValue = Math.abs(value);
            // If the magnitude is 0, we don't need any magnitude digits
            if (absValue < 1f) {
                return createComplex(Math.round(value * (1 << 23)), TypedValue.COMPLEX_RADIX_0p23);
            }
            // If the magnitude is less than 2^8, use 8 magnitude digits
            if (absValue < (float) (1 << 8)) {
                return createComplex(Math.round(value * (1 << 15)), TypedValue.COMPLEX_RADIX_8p15);
            }
            // If the magnitude is less than 2^16, use 16 magnitude digits
            if (absValue < (float) (1 << 16)) {
                return createComplex(Math.round(value * (1 << 7)), TypedValue.COMPLEX_RADIX_16p7);
            }
            // The magnitude requires all 23 digits
            return createComplex(Math.round(value), TypedValue.COMPLEX_RADIX_23p0);
        } catch (IllegalArgumentException ex) {
            // Wrap exception to include the value argument in the message.
            throw new IllegalArgumentException("Unable to convert value to complex: " + value, ex);
        }
    }

    private static int createComplex(@IntRange(from = -0x800000, to = 0x7FFFFF) int mantissa, int radix) {
        if (mantissa < -0x800000 || mantissa >= 0x800000) {
            throw new IllegalArgumentException("Magnitude of mantissa is too large: " + mantissa);
        }
        if (radix < TypedValue.COMPLEX_RADIX_23p0 || radix > TypedValue.COMPLEX_RADIX_0p23) {
            throw new IllegalArgumentException("Invalid radix: " + radix);
        }
        return ((mantissa & TypedValue.COMPLEX_MANTISSA_MASK) << TypedValue.COMPLEX_MANTISSA_SHIFT)
            | (radix << TypedValue.COMPLEX_RADIX_SHIFT);
    }

}
