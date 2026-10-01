package com.dsa3.sosdp.core;

/**
 * Small bitmask helpers. Element i of the set  <->  bit i of the mask.
 * (Engine code deliberately uses only arrays: no java.util collections.)
 */
public final class Bitmasks {
    public static final int MAX_N = 20;

    private Bitmasks() {}

    /** Number of 1-bits (Kernighan: each step clears the lowest set bit). */
    public static int popcount(int mask) {
        int c = 0;
        while (mask != 0) {
            mask &= mask - 1;
            c++;
        }
        return c;
    }

    /** Positions of the set bits, ascending. */
    public static int[] indices(int mask) {
        int[] out = new int[popcount(mask)];
        int k = 0;
        for (int i = 0; i < 31; i++) {
            if (((mask >> i) & 1) == 1) out[k++] = i;
        }
        return out;
    }

    /** Binary string of width n, most significant bit first (e.g. 5 with n=4 -> "0101"). */
    public static String binary(int mask, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = n - 1; i >= 0; i--) sb.append(((mask >> i) & 1) == 1 ? '1' : '0');
        return sb.toString();
    }

    /** The element VALUES selected by the mask, e.g. "{5,8}", or "∅". */
    public static String elementsOf(int mask, int[] values) {
        if (mask == 0) return "∅";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i < values.length; i++) {
            if (((mask >> i) & 1) == 1) {
                if (!first) sb.append(',');
                sb.append(values[i]);
                first = false;
            }
        }
        return sb.append('}').toString();
    }

    public static void checkN(int n) {
        if (n < 1 || n > MAX_N)
            throw new IllegalArgumentException("n must be between 1 and " + MAX_N + " (got " + n + ")");
    }
}
