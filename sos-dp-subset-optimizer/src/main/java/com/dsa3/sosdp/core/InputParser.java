package com.dsa3.sosdp.core;

/** Parses the element list and the queries typed in the GUI or read from a sample file. */
public final class InputParser {
    private InputParser() {}

    /** "5, 3 8" -> {5,3,8}. */
    public static int[] parseElements(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Enter at least one element.");
        String[] parts = text.trim().split("[,\\s]+");
        int[] v = new int[parts.length];
        for (int i = 0; i < parts.length; i++) v[i] = toInt(parts[i], 10);
        Bitmasks.checkN(v.length);
        return v;
    }

    /** Queries separated by new lines or ';'. Each is decimal, 0b binary, or {i,j,k} indices. */
    public static int[] parseQueries(String text, int n) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Enter at least one query.");
        String[] tokens = text.split("[\\n;]+");
        int[] out = new int[tokens.length];
        int count = 0;
        for (String raw : tokens) {
            String t = raw.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            out[count++] = parseQuery(t, n);
        }
        if (count == 0) throw new IllegalArgumentException("Enter at least one query.");
        int[] result = new int[count];
        System.arraycopy(out, 0, result, 0, count);
        return result;
    }

    public static int parseQuery(String t, int n) {
        int mask = 0;
        if (t.startsWith("{")) {
            if (!t.endsWith("}")) throw new IllegalArgumentException("Missing '}' in query: " + t);
            String inner = t.substring(1, t.length() - 1).trim();
            if (!inner.isEmpty()) {
                for (String p : inner.split("[,\\s]+")) {
                    int idx = toInt(p, 10);
                    if (idx < 0 || idx >= n)
                        throw new IllegalArgumentException("Index " + idx + " out of range 0.." + (n - 1) + " in " + t);
                    mask |= 1 << idx;
                }
            }
        } else if (t.startsWith("0b") || t.startsWith("0B")) {
            mask = toInt(t.substring(2), 2);
        } else {
            mask = toInt(t, 10);
        }
        if (mask < 0 || mask >= (1 << n))
            throw new IllegalArgumentException("Mask " + mask + " is outside 0.." + ((1 << n) - 1) + " for n=" + n);
        return mask;
    }

    private static int toInt(String s, int radix) {
        try {
            return Integer.parseInt(s.trim(), radix);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a valid number: '" + s.trim() + "'");
        }
    }

    /** Text of a sample file split into the two GUI fields. */
    public record SampleFile(String elements, String queries) {}

    public static SampleFile parseFile(String text) {
        String elements = "";
        StringBuilder q = new StringBuilder();
        boolean inQueries = false;
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String low = line.toLowerCase();
            if (low.startsWith("elements:")) {
                elements = line.substring(9).trim();
                inQueries = false;
            } else if (low.startsWith("queries:")) {
                inQueries = true;
                String rest = line.substring(8).trim();
                if (!rest.isEmpty()) q.append(rest).append('\n');
            } else if (inQueries) {
                q.append(line).append('\n');
            }
        }
        if (elements.isEmpty()) throw new IllegalArgumentException("File has no 'elements:' line.");
        return new SampleFile(elements, q.toString());
    }
}
