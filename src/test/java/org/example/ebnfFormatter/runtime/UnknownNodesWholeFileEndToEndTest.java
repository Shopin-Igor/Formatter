package org.example.ebnfFormatter.runtime;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
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

public class UnknownNodesWholeFileEndToEndTest {

    private static final String ALL_RULES = """
            <CompilationUnit> ::= CompilationUnit(packageDeclaration?=<PackageDeclaration>, imports=[<ImportDeclaration>*], types=[<ClassOrInterfaceDeclaration>*])
              => ifpresent(PackageDeclaration, <PackageDeclaration> nl nl)
                 ifpresent(ImportDeclaration, join(<ImportDeclaration>, nl) nl nl)
                 join(<ClassOrInterfaceDeclaration>, nl nl);

            <PackageDeclaration> ::= PackageDeclaration(name=<Name>)
              => "package" sp <Name> ";";

            <ImportDeclaration> ::= ImportDeclaration(name=<Name>)
              => "import" sp <Name> ";";

            <ClassOrInterfaceDeclaration> ::= ClassOrInterfaceDeclaration(modifiers=[<Modifier>*], name=<SimpleName>, members=[<MethodDeclaration>*])
              => ifpresent(Modifier, join(<Modifier>, "")) "class" sp <SimpleName> sp "{"
                 ifpresent(MethodDeclaration, nl indent join(<MethodDeclaration>, nl nl) nl dedent)
                 "}";

            <MethodDeclaration> ::= MethodDeclaration(modifiers=[<Modifier>*], type=<Type>, name=<SimpleName>, parameters=[<Parameter>*], body=<Statement>)
              => ifpresent(Modifier, join(<Modifier>, "")) <Type> sp <SimpleName> "("
                 ifpresent(Parameter, join(<Parameter>, ", "))
                 ")" sp <Statement>;

            <MethodDeclaration> ::= MethodDeclaration(modifiers=[<Modifier>*], type=<Type>, name=<SimpleName>, parameters=[<Parameter>*])
              => ifpresent(Modifier, join(<Modifier>, "")) <Type> sp <SimpleName> "("
                 ifpresent(Parameter, join(<Parameter>, ", "))
                 ")" ";";

            <Statement> ::= BlockStmt(statements=[<Statement>*])
              => "{" nl indent join(<Statement>, nl) nl dedent "}";

            <Statement> ::= IfStmt(condition=<CondExpr>, thenStmt=<ThenStmt>, elseStmt?=<ElseStmt>)
              => "if" sp "(" <CondExpr> ")" <ThenStmt>
                 ifpresent(ElseStmt, nl "else" <ElseStmt>);

            <ThenStmt> ::= BlockStmt(statements=[<Statement>*])
              => sp "{" nl indent join(<Statement>, nl) nl dedent "}";

            <ThenStmt> ::= ReturnStmt(expression=<ThenExpr>)
              => nl indent "return" sp <ThenExpr> ";" dedent;

            <ThenStmt> ::= ExpressionStmt(expression=<ThenExpr>)
              => nl indent <ThenExpr> ";" dedent;

            <ThenStmt> ::= ForStmt(initialization=[<InitExpr>*], compare?=<CompareExpr>, update=[<UpdateExpr>*], body=<ForBody>)
              => nl indent "for" sp "("
                 ifpresent(InitExpr, join(<InitExpr>, ", "))
                 ";" ifpresent(CompareExpr, sp <CompareExpr>)
                 ";" ifpresent(UpdateExpr, sp join(<UpdateExpr>, ", "))
                 ")" <ForBody> dedent;

            <ElseStmt> ::= <NestedIf>
              => sp <NestedIf>;

            <NestedIf> ::= IfStmt(condition=<CondExpr>, thenStmt=<ThenStmt>, elseStmt?=<ElseStmt>)
              => "if" sp "(" <CondExpr> ")" <ThenStmt>
                 ifpresent(ElseStmt, nl "else" <ElseStmt>);

            <ElseStmt> ::= BlockStmt(statements=[<Statement>*])
              => sp "{" nl indent join(<Statement>, nl) nl dedent "}";

            <ElseStmt> ::= ReturnStmt(expression=<ElseExpr>)
              => nl indent "return" sp <ElseExpr> ";" dedent;

            <ElseStmt> ::= ExpressionStmt(expression=<ElseExpr>)
              => nl indent <ElseExpr> ";" dedent;

            <ElseStmt> ::= ForStmt(initialization=[<InitExpr>*], compare?=<CompareExpr>, update=[<UpdateExpr>*], body=<ForBody>)
              => nl indent "for" sp "("
                 ifpresent(InitExpr, join(<InitExpr>, ", "))
                 ";" ifpresent(CompareExpr, sp <CompareExpr>)
                 ";" ifpresent(UpdateExpr, sp join(<UpdateExpr>, ", "))
                 ")" <ForBody> dedent;

            <Statement> ::= ForStmt(initialization=[<InitExpr>*], compare?=<CompareExpr>, update=[<UpdateExpr>*], body=<ForBody>)
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

            <Statement> ::= ReturnStmt(expression=<Expression>)
              => "return" sp <Expression> ";";

            <Statement> ::= ExpressionStmt(expression=<Expression>)
              => <Expression> ";";
            """;

    private static final String BINARY_EXPR_RULES = """
            <ReturnStmt> ::= ReturnStmt(expression=<Expression>)
              => "return" sp <Expression> ";";

            <BinaryExpr> ::= BinaryExpr(left=<LeftExpr>, right=<RightExpr>)
              => <LeftExpr> sp "[binary]" sp <RightExpr>;
            """;

    private static final List<RuleDef> ruleDefs = parseRules(ALL_RULES);
    private static final RuleRegistry registry = registryWithRules(ruleDefs);
    private static final TypeRegistryUniversal typeRegistry = new TypeRegistryUniversal();
    private static final PatternMatcher matcher = new PatternMatcher(typeRegistry, registry);
    private static final FormatterEngine engine = new FormatterEngine(registry, matcher, new TemplateRenderer());

    private static final List<RuleDef> binaryExprRuleDefs = parseRules(BINARY_EXPR_RULES);
    private static final RuleRegistry binaryExprRegistry = registryWithRules(binaryExprRuleDefs);
    private static final TypeRegistryUniversal binaryExprTypeRegistry = new TypeRegistryUniversal();
    private static final PatternMatcher binaryExprMatcher = new PatternMatcher(binaryExprTypeRegistry, binaryExprRegistry);
    private static final FormatterEngine binaryExprEngine = new FormatterEngine(
            binaryExprRegistry,
            binaryExprMatcher,
            new TemplateRenderer()
    );

    private static final JavaParser javaParser = new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)
    );

    @Test
    void formats_block_with_while_and_return_without_expression() {
        assertFormatsWholeFile(
                """
                class Flow{void run(){while(i<limit){i++;}return;}}
                """,
                """
                class Flow {
                    void run() {
                        while(i<limit){i++;}
                        return ;
                    }
                }"""
        );
    }

    @Test
    void formats_block_with_do_while_and_known_return() {
        assertFormatsWholeFile(
                """
                class Flow{int run(){do{tick();i++;}while(i<limit);return i;}}
                """,
                """
                class Flow {
                    int run() {
                        do{tick();i++;}while(i<limit);
                        return i;
                    }
                }"""
        );
    }

    @Test
    void formats_block_with_switch_arrow_statement() {
        assertFormatsWholeFile(
                """
                class Switcher{void run(){switch(mode){case A,B->run();case C->stop();default->reset();}done();}}
                """,
                """
                class Switcher {
                    void run() {
                        switch(mode){case A,B->run();case C->stop();default->reset();}
                        done();
                    }
                }"""
        );
    }

    @Test
    void formats_block_with_try_catch_finally() {
        assertFormatsWholeFile(
                """
                class Worker{void run(){try{work();}catch(IllegalArgumentException|IllegalStateException e){handle(e);}finally{cleanup();}done();}}
                """,
                """
                class Worker {
                    void run() {
                        try{work();}catch(IllegalArgumentException|IllegalStateException e){handle(e);}finally{cleanup();}
                        done();
                    }
                }"""
        );
    }

    @Test
    void formats_block_with_try_with_resources() {
        assertFormatsWholeFile(
                """
                class Reader{void run(){try(Input input=open()){read(input);}closeCount++;}}
                """,
                """
                class Reader {
                    void run() {
                        try(Input input=open()){read(input);}
                        closeCount++;
                    }
                }"""
        );
    }

    @Test
    void formats_block_with_synchronized_and_assert() {
        assertFormatsWholeFile(
                """
                class Guarded{void run(){synchronized(lock){work();}assert ready:"not ready";done();}}
                """,
                """
                class Guarded {
                    void run() {
                        synchronized(lock){work();}
                        assert ready:"not ready";
                        done();
                    }
                }"""
        );
    }

    @Test
    void formats_block_with_for_each_statement() {
        assertFormatsWholeFile(
                """
                class Iteration{void run(){for(String item:items){use(item);}done();}}
                """,
                """
                class Iteration {
                    void run() {
                        for(String item:items){use(item);}
                        done();
                    }
                }"""
        );
    }

    @Test
    void formats_block_with_lambda_and_method_reference_declarations() {
        assertFormatsWholeFile(
                """
                class LocalStuff{void run(){Runnable task=()->work();java.util.function.Function<String,String> trim=String::trim;task.run();}}
                """,
                """
                class LocalStuff {
                    void run() {
                        Runnable task=()->work();
                        java.util.function.Function<String,String> trim=String::trim;
                        task.run();
                    }
                }"""
        );
    }

    @Test
    void formats_block_with_instanceof_pattern_and_throw() {
        assertFormatsWholeFile(
                """
                class PatternUse{void run(Object value){if(value instanceof String text)use(text);throw new RuntimeException("boom");}}
                """,
                """
                class PatternUse {
                    void run(Object value) {
                        if (value instanceof String text)
                            use(text);
                        throw new RuntimeException("boom");
                    }
                }"""
        );
    }

    @Test
    void formats_unknown_if_as_raw_when_then_and_else_are_unknown() {
        assertFormatsWholeFile(
                """
                class Branch{void run(){if(ready)while(running){tick();}else do{sleep();}while(waiting);done();}}
                """,
                """
                class Branch {
                    void run() {
                        if(ready)while(running){tick();}else do{sleep();}while(waiting);
                        done();
                    }
                }"""
        );
    }

    @Test
    void formats_known_for_with_unknown_switch_body() {
        assertFormatsWholeFile(
                """
                class Mixed{void run(){for(i=0;i<limit;i++)switch(i){case 0->start();default->tick();}done();}}
                """,
                """
                class Mixed {
                    void run() {
                        for(i=0;i<limit;i++)switch(i){case 0->start();default->tick();}
                        done();
                    }
                }"""
        );
    }

    @Test
    void formats_package_imports_and_unknown_constructs_in_multiple_methods() {
        assertFormatsWholeFile(
                """
                package demo;
                import java.util.List;
                class Mixed{void loop(){while((line=reader.readLine())!=null){process(line);}}void choose(){switch(mode){case A,B->run();default->reset();}}}
                """,
                """
                package demo;

                import java.util.List;

                class Mixed {
                    void loop() {
                        while((line=reader.readLine())!=null){process(line);}
                    }

                    void choose() {
                        switch(mode){case A,B->run();default->reset();}
                    }
                }"""
        );
    }

    @Test
    void raw_expression_can_be_rendered_by_concrete_binary_expr_rule() {
        Node node = parseStatement("return a + b;");

        assertThat(binaryExprEngine.format(node, "ReturnStmt"))
                .isEqualTo("return a [binary] b;");
    }

    private static void assertFormatsWholeFile(String code, String expected) {
        assertThat(formatASTFromRootToLeafs(code)).isEqualTo(expected);
    }

    private static String formatASTFromRootToLeafs(String code) {
        return engine.format(parseCompilationUnit(code), "CompilationUnit");
    }

    private static RuleRegistry registryWithRules(List<RuleDef> rules) {
        RuleRegistry ruleRegistry = new RuleRegistry();
        ruleRegistry.registerAll(rules);
        return ruleRegistry;
    }

    private static CompilationUnit parseCompilationUnit(String code) {
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        return result.getResult()
                .orElseThrow(() -> new IllegalArgumentException(result.getProblems().toString()));
    }

    private static Node parseStatement(String statement) {
        ParseResult<com.github.javaparser.ast.stmt.Statement> result = javaParser.parseStatement(statement);
        return result.getResult()
                .orElseThrow(() -> new IllegalArgumentException(result.getProblems().toString()));
    }

    private static List<RuleDef> parseRules(String rules) {
        ebnfLexer lexer = new ebnfLexer(CharStreams.fromString(rules));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ebnfParser parser = new ebnfParser(tokens);
        ebnfParser.RulelistContext ctx = parser.rulelist();
        RuleAstBuilder builder = new RuleAstBuilder();
        return builder.buildRules(ctx);
    }
}
