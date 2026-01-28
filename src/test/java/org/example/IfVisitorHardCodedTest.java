package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IfVisitorHardCodedTest {

    @Test
    void formatInput_onlyIfs_noElse() {
        String input = """
                public class AST {
                    public int sum(int a, int b) {
                        System.out.println(a);
                        System.out.println(b);
                        if(a == b){return 2 * a;}
                        if(a == 2){return 2;}
                        if(b == 5){return 5 * b;}
                        return a + b;
                    }
                }
                """;

        String expected = """
                public class AST {
                    public int sum(int a, int b) {
                        System.out.println(a);
                        System.out.println(b);
                        if (a == b) { 
                            return 2 * a;
                        }
                        if (a == 2) { 
                            return 2;
                        }
                        if (b == 5) { 
                            return 5 * b;
                        }
                        return a + b;
                    }
                }
                """;

        ParseResult<CompilationUnit> pr = new JavaParser().parse(input);
        assertTrue(pr.isSuccessful(), () -> "Parse problems: " + pr.getProblems());
        CompilationUnit cu = pr.getResult().orElseThrow();


        ClassOrInterfaceDeclaration clazz = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
        MethodDeclaration method = clazz.findFirst(MethodDeclaration.class).orElseThrow();
        List<Statement> statements = method.getBody().orElseThrow().getStatements();

        StringBuilder out = new StringBuilder();

        out.append("public class ").append(clazz.getNameAsString()).append(" {\n");
        out.append("    public ").append(method.getType()).append(" ").append(method.getNameAsString()).append("(");

        for (int i = 0; i < method.getParameters().size(); i++) {
            var p = method.getParameter(i);
            out.append(p.getType()).append(" ").append(p.getNameAsString());
            if (i != method.getParameters().size() - 1) out.append(", ");
        }
        out.append(") {\n");



        for (Statement st : statements) {
            if (st.isIfStmt()) {
                IfStmt ifStmt = st.asIfStmt();

                BlockStmt thenBlock = ifStmt.getThenStmt().asBlockStmt();

                ReturnStmt r = thenBlock.getStatement(0).asReturnStmt();

                out.append("        if (").append(ifStmt.getCondition()).append(") {\n");
                out.append("            return ").append(r.getExpression().orElseThrow()).append(";\n");
                out.append("        }\n");
            } else {
                out.append("        ").append(st.toString().trim()).append("\n");
            }
        }

        out.append("    }\n");
        out.append("}\n");

        assertEquals(expected.trim(), out.toString().trim());
    }
}