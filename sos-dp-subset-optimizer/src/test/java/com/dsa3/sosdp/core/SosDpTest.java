package com.dsa3.sosdp.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SosDpTest {
    private static final int[] SAMPLE = {5, 3, 8, 2, 7, 1};

    @Test
    void subsetSumsAreCorrect() {
        long[] f = SubsetSums.compute(new int[]{5, 3, 8});
        assertEquals(0, f[0]);
        assertEquals(5, f[0b001]);
        assertEquals(8, f[0b011]);
        assertEquals(16, f[0b111]);
    }

    @Test
    void sosMatchesBruteForceForEveryMask() {
        long[] f = SubsetSums.compute(SAMPLE);
        SosDp sos = new SosDp(f, SAMPLE.length);
        BruteForce bf = new BruteForce(f);
        for (int m = 0; m < (1 << SAMPLE.length); m++) assertEquals(bf.query(m), sos.query(m), "mask " + m);
    }

    @Test
    void matchesClosedForm() {
        // Each element of Q appears in exactly 2^(k-1) submasks, so F(Q) = 2^(k-1) * sum(Q).
        long[] f = SubsetSums.compute(SAMPLE);
        SosDp sos = new SosDp(f, SAMPLE.length);
        for (int m = 1; m < (1 << SAMPLE.length); m++) {
            assertEquals((1L << (Bitmasks.popcount(m) - 1)) * f[m], sos.query(m));
        }
    }

    @Test
    void emptyQueryIsZero() {
        long[] f = SubsetSums.compute(SAMPLE);
        assertEquals(0, new SosDp(f, SAMPLE.length).query(0));
        assertEquals(0, new BruteForce(f).query(0));
    }

    @Test
    void handWorkedExample() {
        // {1,2}: subsets {} {1} {2} {1,2} -> sums 0+1+2+3 = 6
        long[] f = SubsetSums.compute(new int[]{1, 2});
        assertEquals(6, new SosDp(f, 2).query(0b11));
        assertEquals(6, new BruteForce(f).query(0b11));
    }

    @Test
    void traceEndsWithFinalDp() {
        long[] f = SubsetSums.compute(SAMPLE);
        long[][] tr = SosDp.trace(f, SAMPLE.length);
        SosDp sos = new SosDp(f, SAMPLE.length);
        assertEquals(SAMPLE.length + 1, tr.length);
        for (int m = 0; m < f.length; m++) assertEquals(sos.query(m), tr[SAMPLE.length][m]);
    }

    @Test
    void largeValuesDoNotOverflow() {
        int[] v = new int[20];
        java.util.Arrays.fill(v, 2_000_000_000);
        long[] f = SubsetSums.compute(v);
        assertEquals(20L * 2_000_000_000L, f[(1 << 20) - 1]);
        assertEquals((1L << 19) * f[(1 << 20) - 1], new SosDp(f, 20).query((1 << 20) - 1));
    }

    @Test
    void benchmarkVerifiesAndCountsSteps() {
        Benchmark.Report r = Benchmark.run(SAMPLE, new int[]{45, 7, 63, 0});
        assertTrue(r.allMatch());
        assertEquals(16 + 8 + 64 + 1, r.bruteSteps());
        assertEquals(6L * 64, r.sosPreSteps());
    }

    @Test
    void scalingResultsMatch() {
        for (Benchmark.ScalePoint p : Benchmark.scaling(4, 10)) assertTrue(p.match(), "n=" + p.n());
    }

    @Test
    void rejectsTooManyElements() {
        assertThrows(IllegalArgumentException.class, () -> SubsetSums.compute(new int[21]));
    }
}
