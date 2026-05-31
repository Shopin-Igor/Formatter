package org.example.ebnfFormatter.runtime;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
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
            <IfStmt> ::= IfStmt(condition=<CondExpr>, thenStmt=<ThenStmt>, elseStmt?=<ElseStmt>)
              => "if" sp "(" <CondExpr> ")" <ThenStmt>
                 ifpresent(ElseStmt, nl "else" <ElseStmt>);
            
            <ThenStmt> ::= BlockStmt(statements=[<Statement>*])
              => sp "{" nl indent join(<Statement>, nl) nl dedent "}";
            
            <ThenStmt> ::= ReturnStmt(expression=<ThenExpr>)
              => nl indent "return" sp <ThenExpr> ";" dedent;
            
            <ThenStmt> ::= ExpressionStmt(expression=<ThenExpr>)
              => nl indent <ThenExpr> ";" dedent;
            
            <ElseStmt> ::= IfStmt
              => sp <IfStmt>;
            
            <ElseStmt> ::= BlockStmt(statements=[<Statement>*])
              => sp "{" nl indent join(<Statement>, nl) nl dedent "}";
            
            <ElseStmt> ::= ReturnStmt(expression=<ElseExpr>)
              => nl indent "return" sp <ElseExpr> ";" dedent;
            
            <ElseStmt> ::= ExpressionStmt(expression=<ElseExpr>)
              => nl indent <ElseExpr> ";" dedent;
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
                else
                    return a + b;""";

        String formatted = formatFirstNode(ifStmtRules, code, IfStmt.class, "IfStmt");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void ifStmtExample3() {
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
                else if ((a&2)==2)
                    return a + b;
                else
                    return b;""";

        String formatted = formatFirstNode(ifStmtRules, code, IfStmt.class, "IfStmt");
        assertThat(formatted).isEqualTo(expected);
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String methodDeclarationRules = """
              <MethodDeclaration> ::= MethodDeclaration(
                                        modifiers=[<Modifier>*],
                                        type=<Type>,
                                        name=<SimpleName>,
                                        parameters=[<Parameter>*],
                                        body?=<Statement>)
                => join(<Modifier*>, "") <Type> sp <SimpleName>
                   "(" ifpresent(Parameter*, join(<Parameter>, ", ")) ")"
                   ifpresent(Statement?, sp <Statement>);
              """;

    @Test
    void methodDeclarationExample1() {
        String code = """
                public class AST {
                    public int sum(int a,int b){if(a==b){return 2*a;}return a+b;}
                }
                """;

        String expected = "public int sum(int a, int b) {if(a==b){return 2*a;}return a+b;}";

        String formatted = formatFirstNode(methodDeclarationRules, code, MethodDeclaration.class, "MethodDeclaration");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void methodDeclarationExample2() {
        String code = """
                public class AST {
                    public int sum(Parameter a,Parameter b,Parameter c,Parameter d) {}
                }
                """;

        String expected = "public int sum(Parameter a, Parameter b, Parameter c, Parameter d) {}";

        String formatted = formatFirstNode(methodDeclarationRules, code, MethodDeclaration.class, "MethodDeclaration");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void methodDeclarationExample3() {
        String code = """
                abstract class AST {
                    public abstract int sum(Input
                                                            input);
                }
                """;

        String expected = "public abstract int sum(Input\n                                        input)";

        String formatted = formatFirstNode(methodDeclarationRules, code, MethodDeclaration.class, "MethodDeclaration");
        assertThat(formatted).isEqualTo(expected);
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String forStmtRules = """
            <ForStmt> ::= ForStmt(
                            initialization=[<InitExpr>*],
                            compare?=<CompareExpr>,
                            update=[<UpdateExpr>*],
                            body=<ForBody>)
              => "for" sp "("
                 ifpresent(InitExpr, join(<InitExpr>, ", "))
                 ";" ifpresent(CompareExpr, sp <CompareExpr>)
                 ";" ifpresent(UpdateExpr, sp join(<UpdateExpr>, ", "))
                 ")" <ForBody>;

            <ForBody> ::= BlockStmt(statements=[<Statement>*])
              => sp "{" nl indent join(<Statement>, nl) nl dedent "}";

            <ForBody> ::= ExpressionStmt(expression=<ForExpr>)
              => nl indent <ForExpr> ";" dedent;

            <ForBody> ::= ReturnStmt(expression=<ForExpr>)
              => nl indent "return" sp <ForExpr> ";" dedent;
            """;
    @Test
    void forStmtExample1() {
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

        String formatted = formatFirstNode(forStmtRules, code, ForStmt.class, "ForStmt");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void forStmtExample2() {
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

        String formatted = formatFirstNode(forStmtRules, code, ForStmt.class, "ForStmt");
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void forStmtExample3() {
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

        String formatted = formatFirstNode(forStmtRules, code, ForStmt.class, "ForStmt");
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