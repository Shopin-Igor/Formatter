package org.example.sqlLikeRequest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestWithOrAtBlockWhereTest {
    // dslWithOR (OR)
    @Test
    void selectIfStmtWithOrAtWhere() {
        String dsl = "SELECT IfStmt WHERE IfStmt.thenStmt != BlockStmt OR IfStmt.thenStmt == BlockStmt";

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

        assertThat(matches.size()).isEqualTo(3);

        assertThat(matches)
                .extracting(m -> m.code())
                .anySatisfy(code -> assertThat(code).contains("if (a == b)"))
                .anySatisfy(code -> assertThat(code).contains("if (a == 2)"))
                .anySatisfy(code -> assertThat(code).contains("if (b == 5)"));
    }
}
