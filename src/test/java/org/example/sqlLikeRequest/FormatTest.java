package org.example.sqlLikeRequest;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import org.example.SqlLikeRequestParserParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FormatTest {
    @Test
    void formatBasicTest() {
        String dsl = """
            SELECT IfStmt as $if WHERE $if.thenStmt == BlockStmt
            FORMAT if ($if.condition) $if.thenStatement else $if.elseStatement
        """;

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
        List<Match> matches = runner.run(dsl, javaCode);

        for (Match match : matches) {
            IO.println(match.code());
        }
        assertThat(matches.size()).isEqualTo(1);
        assertThat(matches.getFirst().code().equals("if(a == 2){return 2;}")).isTrue();
    }
}
