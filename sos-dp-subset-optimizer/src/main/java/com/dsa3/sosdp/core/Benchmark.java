package com.dsa3.sosdp.core;

/** Runs brute force and SOS DP on the same queries, times both, and verifies they agree. */
public final class Benchmark {
    private static boolean warmedUp = false;

    private Benchmark() {}

    public record QueryResult(int mask, long directSum, long bruteValue, long sosValue, long bruteSteps) {
        public boolean match() {
            return bruteValue == sosValue;
        }
    }

    public record Report(int n, QueryResult[] results, long baseNanos, long sosBuildNanos,
                         long bruteQueryNanos, long sosQueryNanos, long bruteSteps, long sosPreSteps) {
        public boolean allMatch() {
            for (QueryResult r : results) if (!r.match()) return false;
            return true;
        }
        public long bruteTotalNanos() { return baseNanos + bruteQueryNanos; }
        public long sosTotalNanos()   { return baseNanos + sosBuildNanos + sosQueryNanos; }
    }

    public record ScalePoint(int n, long bruteNanos, long sosNanos, boolean match) {}

    /** Answer every query in {@code masks} with both methods. */
    public static Report run(int[] values, int[] masks) {
        warmUp();
        int n = values.length;

        long t0 = System.nanoTime();
        long[] f = SubsetSums.compute(values);          // shared base array
        long t1 = System.nanoTime();
        SosDp sos = new SosDp(f, n);                    // SOS preprocessing O(n*2^n)
        long t2 = System.nanoTime();

        BruteForce bf = new BruteForce(f);
        long[] brute = new long[masks.length];
        for (int i = 0; i < masks.length; i++) brute[i] = bf.query(masks[i]);
        long t3 = System.nanoTime();

        long[] fast = new long[masks.length];
        for (int i = 0; i < masks.length; i++) fast[i] = sos.query(masks[i]);   // O(1) each
        long t4 = System.nanoTime();

        QueryResult[] results = new QueryResult[masks.length];
        long bruteSteps = 0;
        for (int i = 0; i < masks.length; i++) {
            long st = BruteForce.steps(masks[i]);
            bruteSteps += st;
            results[i] = new QueryResult(masks[i], f[masks[i]], brute[i], fast[i], st);
        }
        long sosPre = (long) n << n;                    // n * 2^n loop iterations
        return new Report(n, results, t1 - t0, t2 - t1, t3 - t2, t4 - t3, bruteSteps, sosPre);
    }

    /**
     * Scaling experiment: for each n, query ALL 2^n masks (brute force = 3^n steps in total)
     * against SOS DP (build n*2^n + 2^n lookups). Values come from a tiny built-in generator.
     */
    public static ScalePoint[] scaling(int minN, int maxN) {
        warmUp();
        ScalePoint[] out = new ScalePoint[maxN - minN + 1];
        long seed = 88172645463325252L;
        for (int n = minN; n <= maxN; n++) {
            int[] values = new int[n];
            for (int i = 0; i < n; i++) {
                seed = nextRandom(seed);
                values[i] = 1 + (int) ((seed >>> 33) % 100);
            }
            out[n - minN] = timeAllMasks(values);
        }
        return out;
    }

    private static ScalePoint timeAllMasks(int[] values) {
        int n = values.length;
        int size = 1 << n;
        long[] f = SubsetSums.compute(values);

        BruteForce bf = new BruteForce(f);
        long[] a = new long[size];
        long t0 = System.nanoTime();
        for (int m = 0; m < size; m++) a[m] = bf.query(m);
        long t1 = System.nanoTime();

        long[] b = new long[size];
        SosDp sos = new SosDp(f, n);
        for (int m = 0; m < size; m++) b[m] = sos.query(m);
        long t2 = System.nanoTime();

        boolean same = true;
        for (int m = 0; m < size; m++) if (a[m] != b[m]) { same = false; break; }
        return new ScalePoint(n, t1 - t0, t2 - t1, same);
    }

    /** xorshift64: enough randomness for demo data, and no java.util needed. */
    private static long nextRandom(long x) {
        x ^= x << 13;
        x ^= x >>> 7;
        x ^= x << 17;
        return x;
    }

    /** Tiny untimed run so the JIT compiler has seen the hot loops before we measure. */
    private static void warmUp() {
        if (warmedUp) return;
        warmedUp = true;
        int[] v = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int r = 0; r < 20; r++) timeAllMasks(v);
    }
}
