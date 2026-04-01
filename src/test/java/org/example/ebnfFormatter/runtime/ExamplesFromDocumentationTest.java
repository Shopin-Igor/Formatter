package org.example.ebnfFormatter.runtime;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.example.ebnfFormatter.dsl.RuleAstBuilder;
import org.example.ebnfFormatter.match.PatternMatcher;
import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.render.TemplateRenderer;
import org.example.ebnfLexer;
import org.example.ebnfParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ExamplesFromDocumentationTest {
    private static final String ifStmtRules = """
              <IfStmt> ::= IfStmt(condition=<Expression>, thenStmt=<Statement>, elseStmt?=<Statement>)
            => "if" sp "(" <Expression> ")" sp <Statement>
               ifpresent(Statement?, sp "else" sp <Statement?>);
            """;

    @Test
    void ifStmtExample1() {
        String code = """
                public class AST {
                    public int sum(int a, int b) {
                        if(a == b){return 2 * a;}
                        return a + b;
                    }
                }
                """;

        String expected = """
                if (a == b) {
                    return 2 * a;
                }""";

String formatted = formatFirstNode(ifStmtRules, code, IfStmt.class, "IfStmt");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void ifStmtExample2() {
        String rules = """
            <IfStmt> ::= IfStmt(condition=<CondExpr>, thenStmt=<ThenStmt>, elseStmt=<ElseStmt>)
                => "if" sp "(" <CondExpr> ")" nl indent <ThenStmt> dedent
                    nl "else" sp nl indent <ElseStmt> dedent;

            <ThenStmt> ::= ReturnStmt(expression=<ThenExpr>)
                => nl indent "return" sp <ThenExpr> ";" dedent;
    
            <ElseStmt> ::= ReturnStmt(expression=<ElseExpr>)
                => nl indent "return" sp <ElseExpr> ";" dedent;
    
            <ElseStmt> ::= IfStmt
                => <IfStmt>;
            """;

        String code = """
                public class AST {
                    public int sum(int a, int b) {
                        if(a == b)return 2 * a;
                        else return a + b;
                    }
                }
                """;

        String expected = """
                if (a == b)
                    return 2 * a;
                else\s
                    return a + b;""";

        String formatted = formatFirstNode(rules, code, IfStmt.class, "IfStmt");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void ifStmtExample3() {
        String rules = """
            <IfStmt> ::= IfStmt(condition=<CondExpr>, thenStmt=<ThenStmt>, elseStmt=<ElseStmt>)
                => "if" sp "(" <CondExpr> ")" nl indent <ThenStmt> dedent
                    nl "else" sp nl indent <ElseStmt> dedent;

            <ThenStmt> ::= ReturnStmt(expression=<ThenExpr>)
                => nl indent "return" sp <ThenExpr> ";" dedent;
    
            <ElseStmt> ::= ReturnStmt(expression=<ElseExpr>)
                => nl indent "return" sp <ElseExpr> ";" dedent;
    
            <ElseStmt> ::= IfStmt
                => <IfStmt>;
            """;

        String code = """
                public class AST {
                    public int sum(int a, int b) {
                        if(a == b)return 2 * a;
                        else if((a&2)==2) return a + b;
                        else return b;
                    }
                }
                """;

        String expected = """
                if (a == b)
                    return 2 * a;
                else\s
                    if ((a & 2) == 2)
                        return a + b;
                    else
                        return b;""";

        String formatted = formatFirstNode(rules, code, IfStmt.class, "IfStmt");
        assertThat(formatted).isEqualTo(expected);
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    private static final String methodDeclarationRules = """
//              <MethodDeclaration> ::= MethodDeclaration(
//            modifiers=[<Modifier*>],
//            type=<Type>,
//            name=<SimpleName>,
//            parameters=[<Parameter*>],
//            body?=<Statement>)
//                => join(<Modifier*>, sp) sp <Type> sp <SimpleName>
//                   "(" ifpresent(Parameter*, join(<Parameter*>, ", ")) ")"
//                   ifpresent(Statement?, sp <Statement?>);
//            """;

    @Test
    void methodDeclarationExample1() {
        String rules = """
                <MethodDeclaration> ::= MethodDeclaration(body=BlockStmt)
                    => "public" sp "int" sp "sum" "(" "int a, int b" ")" sp "{"
                       nl indent "if(a==b){return 2*a;}return a+b;" nl dedent "}";
                """;

        String code = """
                public class AST {
                    public int sum(int a,int b){if(a==b){return 2*a;}return a+b;}
                }
                """;

        String expected = """
                public int sum(int a, int b) {
                    if(a==b){return 2*a;}return a+b;
                }""";

        String formatted = formatFirstNode(rules, code, MethodDeclaration.class, "MethodDeclaration");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void methodDeclarationExample2() {
        String rules = """
                <MethodDeclaration> ::= MethodDeclaration(body=BlockStmt)
                    => "public" sp "int" sp "sum" "("
                       "Parameter a, Parameter b, Parameter c, Parameter d"
                       ")" sp "{" nl indent nl dedent "}";
                """;

        String code = """
                public class AST {
                    public int sum(Parameter a,Parameter b,Parameter c,Parameter d) {}
                }
                """;

        String expected = """
                public int sum(Parameter a, Parameter b, Parameter c, Parameter d) {

                }""";

        String formatted = formatFirstNode(rules, code, MethodDeclaration.class, "MethodDeclaration");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void methodDeclarationExample3() {
        String rules = """
            <MethodDeclaration> ::= MethodDeclaration
                => "public" sp "abstract" sp "int" sp "sum" "(" "Input input" ")" ";";
            """;


        String code = """
                abstract class AST {
                    public abstract int sum(Input
                                                            input);
                }
                """;

        String expected = "public abstract int sum(Input input);";

        String formatted = formatFirstNode(rules, code, MethodDeclaration.class, "MethodDeclaration");
        assertThat(formatted).isEqualTo(expected);
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//    private static final String forStmtRules = """
//            <ForStmt> ::= ForStmt(
//                initialization=[<Expression*>],
//                compare?=<Expression>,
//                update=[<Expression*>],
//                body=<Statement>
//            )
//                => "for" sp "("
//                   ifpresent(Expression*, join(<Expression*>, ", "))
//                   ";" ifpresent(Expression?, sp <Expression?>)
//                   ";" ifpresent(Expression*, sp join(<Expression*>, ", "))
//                   ")" sp <ForBody>;
//
//            <ForBody> ::= BlockStmt(statements=[<Statement*>])
//                => "{" nl indent join(<Statement*>, nl) nl dedent "}";
//
//            <ForBody> ::= ExpressionStmt(expression=<Expression>)
//                => nl indent <Expression> ";" dedent;
//
//            <ForBody> ::= ReturnStmt(expression=<Expression>)
//                => nl indent "return" sp <Expression> ";" dedent;
//            """;
    @Test
    void forStmtExample1() {
        String rules = """
                  <ForStmt> ::= ForStmt(body=BlockStmt)
                        => "for" sp "(" "int i=0" ";" sp "i<5" ";" sp "++i" ")" sp "{"
                            nl indent "sm += i;" nl dedent "}";
                  """;

        String code = """
                public class AST {
                    public int sum(int a, int b) {
                        int sm = 0;
                        for (int i=0;i<5;++i){sm += i;}
                    }
                }
                """;

        String expected = """
                for (int i=0; i<5; ++i) {
                    sm += i;
                }""";

        String formatted = formatFirstNode(rules, code, ForStmt.class, "ForStmt");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void forStmtExample2() {
        String rules = """
                 <ForStmt> ::= ForStmt(body=ExpressionStmt)
                    => "for" sp "(" "int i=0" ";" sp "i<5" ";" sp "++i" ")" nl indent "sm += i;" dedent;
                 """;

        String code = """
                public class AST {
                    public int sum(int a, int b) {
                        int sm = 0;
                        for (int i=0;i<5;++i)sm += i;
                    }
                }
                """;

        String expected = """
                for (int i=0; i<5; ++i)
                    sm += i;""";

        String formatted = formatFirstNode(rules, code, ForStmt.class, "ForStmt");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void forStmtExample3() {
        String rules = """
                <ForStmt> ::= ForStmt(body=<Statement>)
                    => "for" sp "(" ";" ";" ")" nl indent <Statement> dedent;
        
                <Statement> ::= ExpressionStmt(expression=<Expression>)
                    => <Expression> ";";
        
                <Expression> ::= MethodCallExpr(name=<SimpleName>)
                    => <SimpleName> "(" ")";
        
                <SimpleName> ::= SimpleName(identifier="make")
                    => "make";
                """;

        String code = """
                public class AST {
                    public int sum(int a, int b) {
                        for (;;)make();
                    }
                }
                """;

        String expected = """
                for (;;)
                    make();""";

        String formatted = formatFirstNode(rules, code, ForStmt.class, "ForStmt");
        assertThat(formatted).isEqualTo(expected);
    }

    private <T extends Node> String formatFirstNode(
            String rules,
            String code,
            Class<T> nodeClass,
            String ruleName
    ) {
        List<RuleDef> parsed = parseRules(rules);

        RuleRegistry ruleRegistry = new RuleRegistry();
        ruleRegistry.registerAll(parsed);

        TypeRegistryUniversal typeRegistry = new TypeRegistryUniversal();
        PatternMatcher patternMatcher = new PatternMatcher(typeRegistry, ruleRegistry);
        TemplateRenderer templateRenderer = new TemplateRenderer();
        FormatterEngine engine = new FormatterEngine(ruleRegistry, patternMatcher, templateRenderer);

        T node = StaticJavaParser.parse(code)
                .findFirst(nodeClass)
                .orElseThrow(() -> new AssertionError(nodeClass.getSimpleName() + " not found"));

        return engine.format(node, ruleName);
    }

    private List<RuleDef> parseRules(String text) {
        ebnfLexer lexer = new ebnfLexer(CharStreams.fromString(text));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ebnfParser parser = new ebnfParser(tokens);

        ebnfParser.RulelistContext ctx = parser.rulelist();

        RuleAstBuilder builder = new RuleAstBuilder();
        return builder.buildRules(ctx);
    }
}