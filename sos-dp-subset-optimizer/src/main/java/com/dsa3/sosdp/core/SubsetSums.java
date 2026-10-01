package com.dsa3.sosdp.core;

/** Base array f[mask] = sum of the elements chosen by mask.  Built in O(2^n). */
public final class SubsetSums {
    private SubsetSums() {}

    public static long[] compute(int[] values) {
        int n = values.length;
        Bitmasks.checkN(n);
        long[] f = new long[1 << n];              // f[0] = 0 (empty set)
        for (int mask = 1; mask < f.length; mask++) {
            int low = Integer.numberOfTrailingZeros(mask);   // index of lowest set bit
            f[mask] = f[mask & (mask - 1)] + values[low];    // mask without that bit + that element
        }
        return f;
    }
}
