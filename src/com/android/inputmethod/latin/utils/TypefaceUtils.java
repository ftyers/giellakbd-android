/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.inputmethod.latin.utils;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import com.android.inputmethod.latin.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public final class TypefaceUtils {
    private static final String TAG = TypefaceUtils.class.getSimpleName();

    private TypefaceUtils() {
        // This utility class is not publicly instantiable.
    }

    private static final Map<Integer, BitSet> sMissingCodepoints = new HashMap<Integer, BitSet>();

    private static byte[] getByteBufferForPath(Context context, int resid) {
        InputStream is = context.getResources().openRawResource(resid);

        byte[] data = new byte[0];

        try {
            data = new byte[is.available()];
            is.read(data);
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "failed to read resid " + resid, e);
        }

        return data;
    }

    public static void init(Context context) {
        sMissingCodepoints.put(16, BitSet.valueOf(getByteBufferForPath(context, R.raw.unicode_api16)));
        sMissingCodepoints.put(19, BitSet.valueOf(getByteBufferForPath(context, R.raw.unicode_api19)));
        sMissingCodepoints.put(21, BitSet.valueOf(getByteBufferForPath(context, R.raw.unicode_api21)));
    }

    // This sparse array caches key label text height in pixel indexed by key label text size.
    private static final SparseArray<Float> sTextHeightCache = CollectionUtils.newSparseArray();
    // Working variable for the following method.
    private static final Rect sTextHeightBounds = new Rect();

    public static float getCharHeight(final char[] referenceChar, final Paint paint) {
        final int key = getCharGeometryCacheKey(referenceChar[0], paint);
        synchronized (sTextHeightCache) {
            final Float cachedValue = sTextHeightCache.get(key);
            if (cachedValue != null) {
                return cachedValue;
            }

            paint.getTextBounds(referenceChar, 0, 1, sTextHeightBounds);
            final float height = sTextHeightBounds.height();
            sTextHeightCache.put(key, height);
            return height;
        }
    }

    // This sparse array caches key label text width in pixel indexed by key label text size.
    private static final SparseArray<Float> sTextWidthCache = CollectionUtils.newSparseArray();
    // Working variable for the following method.
    private static final Rect sTextWidthBounds = new Rect();

    public static float getCharWidth(final char[] referenceChar, final Paint paint) {
        final int key = getCharGeometryCacheKey(referenceChar[0], paint);
        synchronized (sTextWidthCache) {
            final Float cachedValue = sTextWidthCache.get(key);
            if (cachedValue != null) {
                return cachedValue;
            }

            paint.getTextBounds(referenceChar, 0, 1, sTextWidthBounds);
            final float width = sTextWidthBounds.width();
            sTextWidthCache.put(key, width);
            return width;
        }
    }

    public static boolean isGlyphDrawable(String glyph) {
        int sdk = Build.VERSION.SDK_INT;

        if (!sMissingCodepoints.containsKey(sdk)) {
            int fallback = sdk;

            if (sdk > 21 || sdk == 20) {
                fallback = 21;
            } else {
                fallback = 16;
            }

            Log.w(TAG, String.format("No missing codepoints for API %s; falling back to %s.", sdk, fallback));
            sdk = fallback;
        }

        int[] cps = StringUtils.toCodePointArray(glyph);
        BitSet bs = sMissingCodepoints.get(sdk);

        for (int i = 0; i < cps.length; ++i) {
            if (!bs.get(cps[i])) {
                return false;
            }
        }

        return true;
    }

    public static float getStringWidth(final String string, final Paint paint) {
        paint.getTextBounds(string, 0, string.length(), sTextWidthBounds);
        return sTextWidthBounds.width();
    }

    private static int getCharGeometryCacheKey(final char referenceChar, final Paint paint) {
        final int labelSize = (int)paint.getTextSize();
        final Typeface face = paint.getTypeface();
        final int codePointOffset = referenceChar << 15;
        if (face == Typeface.DEFAULT) {
            return codePointOffset + labelSize;
        } else if (face == Typeface.DEFAULT_BOLD) {
            return codePointOffset + labelSize + 0x1000;
        } else if (face == Typeface.MONOSPACE) {
            return codePointOffset + labelSize + 0x2000;
        } else {
            return codePointOffset + labelSize;
        }
    }

    public static float getLabelWidth(final String label, final Paint paint) {
        final Rect textBounds = new Rect();
        paint.getTextBounds(label, 0, label.length(), textBounds);
        return textBounds.width();
    }
}
