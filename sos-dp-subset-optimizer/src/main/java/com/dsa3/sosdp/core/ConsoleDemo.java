package com.dsa3.sosdp.core;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Text-only demo (no JavaFX needed). Run:  java -cp target/classes com.dsa3.sosdp.core.ConsoleDemo */
public final class ConsoleDemo {
    public static void main(String[] args) throws Exception {
        String text;
        try (InputStream in = ConsoleDemo.class.getResourceAsStream("/sample-input.txt")) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        InputParser.SampleFile file = InputParser.parseFile(text);
        int[] values = InputParser.parseElements(file.elements());
        int[] masks = InputParser.parseQueries(file.queries(), values.length);

        Benchmark.Report r = Benchmark.run(values, masks);
        System.out.printf("n = %d%n%n", r.n());
        System.out.printf("%-8s %-10s %-14s %10s %12s %12s  %s%n",
                "mask", "binary", "elements", "sum(Q)", "brute F(Q)", "SOS F(Q)", "match");
        for (Benchmark.QueryResult q : r.results()) {
            System.out.printf("%-8d %-10s %-14s %10d %12d %12d  %s%n", q.mask(),
                    Bitmasks.binary(q.mask(), r.n()), Bitmasks.elementsOf(q.mask(), values),
                    q.directSum(), q.bruteValue(), q.sosValue(), q.match() ? "OK" : "MISMATCH");
        }
        System.out.printf("%nAll results equal: %b%n", r.allMatch());
        System.out.printf("Brute force: %d submask visits, %d us%n", r.bruteSteps(), r.bruteQueryNanos() / 1000);
        System.out.printf("SOS DP     : %d build steps, %d us build + %d us queries%n",
                r.sosPreSteps(), r.sosBuildNanos() / 1000, r.sosQueryNanos() / 1000);

        System.out.println("\nScaling (all 2^n queries):");
        System.out.printf("%-4s %14s %12s %s%n", "n", "brute (ms)", "SOS (ms)", "match");
        for (Benchmark.ScalePoint p : Benchmark.scaling(4, 16)) {
            System.out.printf("%-4d %14.3f %12.3f %s%n", p.n(), p.bruteNanos() / 1e6, p.sosNanos() / 1e6, p.match());
        }
    }
}
