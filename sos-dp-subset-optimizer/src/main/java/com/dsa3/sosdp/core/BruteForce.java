package com.dsa3.sosdp.core;

/**
 * Brute force: for ONE query mask Q, visit every submask of Q and add f[submask].
 * One query costs O(2^popcount(Q)); answering all 2^n masks costs sum = 3^n.
 */
public final class BruteForce {
    private final long[] f;

    public BruteForce(long[] f) {
        this.f = f;
    }

    public long query(int mask) {
        long total = 0;
        int sub = mask;
        while (true) {
            total += f[sub];
            if (sub == 0) break;
            sub = (sub - 1) & mask;               // next smaller submask of mask
        }
        return total;
    }

    /** Number of submasks visited for this query. */
    public static long steps(int mask) {
        return 1L << Bitmasks.popcount(mask);
    }
}
