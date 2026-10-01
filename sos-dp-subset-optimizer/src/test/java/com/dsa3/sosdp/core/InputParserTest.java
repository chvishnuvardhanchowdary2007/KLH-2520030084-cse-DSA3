package com.dsa3.sosdp.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InputParserTest {
    @Test
    void parsesElements() {
        assertArrayEquals(new int[]{5, 3, 8}, InputParser.parseElements("5, 3 8"));
    }

    @Test
    void parsesAllQueryFormats() {
        assertEquals(5, InputParser.parseQuery("5", 4));
        assertEquals(5, InputParser.parseQuery("0b101", 4));
        assertEquals(5, InputParser.parseQuery("{0,2}", 4));
        assertEquals(0, InputParser.parseQuery("{}", 4));
    }

    @Test
    void parsesQueryListWithCommentsAndSemicolons() {
        assertArrayEquals(new int[]{1, 2, 3}, InputParser.parseQueries("1; 2\n# note\n3", 3));
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> InputParser.parseQuery("16", 4));
        assertThrows(IllegalArgumentException.class, () -> InputParser.parseQuery("{4}", 4));
        assertThrows(IllegalArgumentException.class, () -> InputParser.parseQuery("abc", 4));
        assertThrows(IllegalArgumentException.class, () -> InputParser.parseElements(""));
        assertThrows(IllegalArgumentException.class, () -> InputParser.parseElements("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21"));
    }

    @Test
    void parsesSampleFile() {
        InputParser.SampleFile sf = InputParser.parseFile("# c\nelements: 1, 2\nqueries:\n3\n{0}\n");
        assertEquals("1, 2", sf.elements());
        assertArrayEquals(new int[]{3, 1}, InputParser.parseQueries(sf.queries(), 2));
    }
}
