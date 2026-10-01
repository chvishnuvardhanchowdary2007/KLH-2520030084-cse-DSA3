package com.dsa3.sosdp.core;

/**
 * Sum Over Subsets DP:   F[mask] = sum of f[S] for every submask S of mask.
 *
 * State   : dp_i[mask] = sum of f[S] where S is a submask of mask that differs
 *           from mask only in the lowest i bits.
 * Recur.  : if bit i of mask is set:  dp_i+1[mask] = dp_i[mask] + dp_i[mask ^ (1<<i)]
 *           otherwise                 dp_i+1[mask] = dp_i[mask]
 * The i dimension is dropped by updating one array in place.
 * Time O(n * 2^n), space O(2^n), each query afterwards O(1).
 */
public final class SosDp {
    private final long[] dp;
    private final int n;

    public SosDp(long[] f, int n) {
        Bitmasks.checkN(n);
        this.n = n;
        this.dp = f.clone();
        for (int i = 0; i < n; i++) {
            for (int mask = 0; mask < dp.length; mask++) {
                if (((mask >> i) & 1) == 1) dp[mask] += dp[mask ^ (1 << i)];
            }
        }
    }

    public long query(int mask) {
        return dp[mask];
    }

    public int n() {
        return n;
    }

    /** Snapshots for teaching: [0] = f, [i+1] = array after processing bit i. */
    public static long[][] trace(long[] f, int n) {
        long[][] snaps = new long[n + 1][];
        long[] cur = f.clone();
        snaps[0] = cur.clone();
        for (int i = 0; i < n; i++) {
            for (int mask = 0; mask < cur.length; mask++) {
                if (((mask >> i) & 1) == 1) cur[mask] += cur[mask ^ (1 << i)];
            }
            snaps[i + 1] = cur.clone();
        }
        return snaps;
    }
}
