package org.example.ebnfFormatter.runtime;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LikeReadmeNodeFormattingE2ETest {

    @Test
    void if_stmt_example_2_node_only() {
        String before = """
                public class AST {
                     public int sum(int a, int b) {
                         if(a == b)return 2 * a;
                         else return a + b;
                     }
                 }
        """.trim();
        String expected = """
                if (a == b)
                    return 2 * a;
                else
                    return a + b;
                """.trim();

        IfStmt node = StaticJavaParser.parse(before)
                .findFirst(IfStmt.class)
                .orElseThrow();

        String actual = formatIf(node);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void for_stmt_example_3_node_only() {
        String before = """
                public class AST {
                     public int sum(int a, int b) {
                         for (;;)make();
                     }
                }
                """.trim();
        String expected = """
                for (; ; )
                    make();
                """.trim();

        ForStmt node = StaticJavaParser.parse(before)
                .findFirst(ForStmt.class)
                .orElseThrow();

        String actual = formatFor(node);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void method_declaration_example_3_node_only() {
        String source = """
                abstract class AST {
                     public abstract int sum(Input\s
                                                         input);
                }
                """;

        String expected = "public abstract int sum(Input input)";

        MethodDeclaration node = StaticJavaParser.parse(source)
                .findFirst(MethodDeclaration.class)
                .orElseThrow();

        String actual = formatMethod(node);

        assertThat(actual).isEqualTo(expected);
    }

    private String formatIf(IfStmt node) {
        FormatterEngine engine = TestFormatterFactory.createEngine();
        return engine.format(node, "IfStmt");
    }

    private String formatFor(ForStmt node) {
        FormatterEngine engine = TestFormatterFactory.createEngine();
        return engine.format(node, "ForStmt");
    }

    private String formatMethod(MethodDeclaration node) {
        FormatterEngine engine = TestFormatterFactory.createEngine();
        return engine.format(node, "MethodDeclaration");
    }
}