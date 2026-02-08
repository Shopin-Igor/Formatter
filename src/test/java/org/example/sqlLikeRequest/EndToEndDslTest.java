package org.example.sqlLikeRequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndToEndDslTest {

    @Test
    void selectIfStmtWhereThenNotBlockStmt() {
        String dsl = "SELECT IfStmt WHERE IfStmt.thenStmt != BlockStmt";

        String javaCode = """
            public class AST {
                    public int sum(int a, int b) {
                        System.out.println(a);
                        System.out.println(b);
                        if (a == b)
                            return 2 * a;
                        if (a == 2) {
                            return 2;
                        }
                        if (b == 5)
                            return 5 * b;
                        return a + b;
                    }
                }
            """;

        QueryRunner runner = new QueryRunner();
        var matches = runner.run(dsl, javaCode);

        assertEquals(2, matches.size());
        assertTrue(matches.getFirst().code().contains("if (a == b)"));
    }
}
