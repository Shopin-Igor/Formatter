package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JavaFormatterTest {

    @Test
    void formatsSimpleClass() {
        JavaFormatter formatter = new JavaFormatter();

        String unformatted = "class A{void m(){int x=1+2;}}";

        String expectedFormatted = """
                class A {
                    void m() {
                        int x = 1 + 2;
                    }
                }
                """;

        String actual = formatter.format(unformatted);

        //   must falll

        assertNotEquals(expectedFormatted, actual);
    }
}
