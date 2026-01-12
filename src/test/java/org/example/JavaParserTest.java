package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaParserTest {

    @Test
    void isAnyWordsAtNodesAST() {
        String s = """
            public class AST {
                public int sum(int a, int b) {
                    System.out.println(a);
                    System.out.println(b);
                    if (a == b) {
                        return 2 * a;
                    }
                    return a + b;
                }
            }
            """;
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> resultOfParse = parser.parse(s);
        assertTrue(resultOfParse.isSuccessful(), ()->"Problems of parsing are: " + resultOfParse.getProblems());
        CompilationUnit cu = resultOfParse.getResult().orElseThrow();


        Optional<ClassOrInterfaceDeclaration> clazz =
                cu.findFirst(ClassOrInterfaceDeclaration.class, c -> c.getNameAsString().equals("AST"));
        assertTrue(clazz.isPresent(), "Class AST not found");


        Optional<MethodDeclaration> greet =
                cu.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("sum"));
        assertTrue(greet.isPresent(), "Method sum not found");


        assertTrue(cu.findFirst(IfStmt.class).isPresent(), "IfStmt not found");

        boolean hasPrintln = cu.findAll(MethodCallExpr.class).stream()
                .anyMatch(call ->
                        call.getNameAsString().equals("println")
                                && call.getScope().map(Object::toString).orElse("").contains("System.out")
                );
        assertTrue(hasPrintln, "System.out.println(...) call not found");
    }
}