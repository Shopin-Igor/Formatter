package org.example.ebnfFormatter.runtime;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
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

public class ALotOfEndToEndTest {

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
    private static final List<RuleDef> ruleDefs = parseRules();
    private static final RuleRegistry registry = registryWithRules();

    private static RuleRegistry registryWithRules() {
        RuleRegistry ruleRegistry = new RuleRegistry();
        ruleRegistry.registerAll(ruleDefs);
        return ruleRegistry;
    }

    private static final TypeRegistryUniversal typeRegistry = new TypeRegistryUniversal();
    private static final PatternMatcher matcher = new PatternMatcher(typeRegistry, registry);
    private static final FormatterEngine engine = new FormatterEngine(registry, matcher, new TemplateRenderer());

    @Test
    void formats_whole_file_with_single_class_and_single_method() {
        assertFormatsWholeFile(
                """
                public class Sample{public int one(){return 1;}}
                """,
                """
                public class Sample {
                    public int one() {
                        return 1;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_public_final_empty_class() {
        assertFormatsWholeFile(
                """
                public final class Empty{}
                """,
                """
                public final class Empty {}"""
        );
    }

    @Test
    void formats_whole_file_with_empty_method_body() {
        assertFormatsWholeFile(
                """
                class EmptyMethod{void run(){}}
                """,
                """
                class EmptyMethod {
                    void run() {

                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_abstract_method() {
        assertFormatsWholeFile(
                """
                abstract class AbstractMath{public abstract int sum(int a,int b);}
                """,
                """
                abstract class AbstractMath {
                    public abstract int sum(int a, int b);
                }"""
        );
    }

    @Test
    void formats_whole_file_with_static_method_and_parameters() {
        assertFormatsWholeFile(
                """
                public class MathBox{public static int sum(int a,int b){return a+b;}}
                """,
                """
                public class MathBox {
                    public static int sum(int a, int b) {
                        return a + b;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_two_methods() {
        assertFormatsWholeFile(
                """
                class PairOps{int left(){return 1;}int right(){return 2;}}
                """,
                """
                class PairOps {
                    int left() {
                        return 1;
                    }

                    int right() {
                        return 2;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_if_else_returns() {
        assertFormatsWholeFile(
                """
                class Branches{int max(int a,int b){if(a>b)return a;else return b;}}
                """,
                """
                class Branches {
                    int max(int a, int b) {
                        if (a > b)
                            return a;
                        else
                            return b;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_else_if_chain() {
        assertFormatsWholeFile(
                """
                class Branches{int choose(int a,int b){if(a>b)return a;else if(a==b)return 0;else return b;}}
                """,
                """
                class Branches {
                    int choose(int a, int b) {
                        if (a > b)
                            return a;
                        else if (a == b)
                            return 0;
                        else
                            return b;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_if_block_and_else_block() {
        assertFormatsWholeFile(
                """
                class Branches{int max(int a,int b){if(a>b){a++;return a;}else{b++;return b;}}}
                """,
                """
                class Branches {
                    int max(int a, int b) {
                        if (a > b) {
                            a++;
                            return a;
                        }
                        else {
                            b++;
                            return b;
                        }
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_if_without_else_and_expression_body() {
        assertFormatsWholeFile(
                """
                class Steps{void run(boolean ready){if(ready)step();}}
                """,
                """
                class Steps {
                    void run(boolean ready) {
                        if (ready)
                            step();
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_for_compare_and_expression_body() {
        assertFormatsWholeFile(
                """
                class Loop{void run(){for(i=0,j=1;i<10;i++,j++)step();}}
                """,
                """
                class Loop {
                    void run() {
                        for (i = 0, j = 1; i < 10; i++, j++)
                            step();
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_for_without_compare() {
        assertFormatsWholeFile(
                """
                class Loop{void run(){for(i=0,j=1;;i++,j++)step();}}
                """,
                """
                class Loop {
                    void run() {
                        for (i = 0, j = 1;; i++, j++)
                            step();
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_for_block_body() {
        assertFormatsWholeFile(
                """
                class Loop{void run(){for(i=0;i<3;i++){step();step();}}}
                """,
                """
                class Loop {
                    void run() {
                        for (i = 0; i < 3; i++) {
                            step();
                            step();
                        }
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_nested_block_statement() {
        assertFormatsWholeFile(
                """
                class NestedBlock{int run(){{step();return 1;}}}
                """,
                """
                class NestedBlock {
                    int run() {
                        {
                            step();
                            return 1;
                        }
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_variable_increment_and_return() {
        assertFormatsWholeFile(
                """
                class Counter{int run(){int x=1;x++;return x;}}
                """,
                """
                class Counter {
                    int run() {
                        int x = 1;
                        x++;
                        return x;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_package_declaration() {
        assertFormatsWholeFile(
                """
                package demo;
                class Single{int one(){return 1;}}
                """,
                """
                package demo;

                class Single {
                    int one() {
                        return 1;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_imports() {
        assertFormatsWholeFile(
                """
                import java.util.List;
                import java.util.Map;
                class Uses{void run(){step();}}
                """,
                """
                import java.util.List;
                import java.util.Map;

                class Uses {
                    void run() {
                        step();
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_package_imports_and_two_classes() {
        assertFormatsWholeFile(
                """
                package demo;
                    import java.util.List;
                class First{}
                class Second{int two(){return 2;}}
                """,
                """
                package demo;

                import java.util.List;

                class First {}

                class Second {
                    int two() {
                        return 2;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_abstract_and_concrete_methods() {
        assertFormatsWholeFile(
                """
                abstract class Mixed{abstract int one();int two(){return 2;}}
                """,
                """
                abstract class Mixed {
                    abstract int one();

                    int two() {
                        return 2;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_multiple_top_level_classes() {
        assertFormatsWholeFile(
                """
                class First{void a(){alpha();}}class Second{void b(){beta();}}
                """,
                """
                class First {
                    void a() {
                        alpha();
                    }
                }

                class Second {
                    void b() {
                        beta();
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_if_then_for_and_else_return() {
        assertFormatsWholeFile(
                """
                class Complex{int run(int x){if(x>0)for(i=0;i<3;i++)tick();else return x;}}
                """,
                """
                class Complex {
                    int run(int x) {
                        if (x > 0)
                            for (i = 0; i < 3; i++)
                                tick();
                        else
                            return x;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_if_then_return_and_else_for_block() {
        assertFormatsWholeFile(
                """
                class Complex{int run(int x){if(x>0)return x;else for(i=0;i<2;i++){tick();tick();}}}
                """,
                """
                class Complex {
                    int run(int x) {
                        if (x > 0)
                            return x;
                        else
                            for (i = 0; i < 2; i++) {
                                tick();
                                tick();
                            }
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_for_block_containing_if_and_following_expression() {
        assertFormatsWholeFile(
                """
                class Scanner{int scan(int limit){for(i=0;i<limit;i++){if(i==2)return i;step();}return limit;}}
                """,
                """
                class Scanner {
                    int scan(int limit) {
                        for (i = 0; i < limit; i++) {
                            if (i == 2)
                                return i;
                            step();
                        }
                        return limit;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_for_block_containing_nested_for_and_expression() {
        assertFormatsWholeFile(
                """
                class NestedLoop{void run(){for(i=0;i<2;i++){for(j=0;j<2;j++)tick();step();}}}
                """,
                """
                class NestedLoop {
                    void run() {
                        for (i = 0; i < 2; i++) {
                            for (j = 0; j < 2; j++)
                                tick();
                            step();
                        }
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_if_block_containing_for_and_return() {
        assertFormatsWholeFile(
                """
                class BlockFlow{int run(int a){if(a>0){for(i=0;i<a;i++)work();return a;}else{a--;return a;}}}
                """,
                """
                class BlockFlow {
                    int run(int a) {
                        if (a > 0) {
                            for (i = 0; i < a; i++)
                                work();
                            return a;
                        }
                        else {
                            a--;
                            return a;
                        }
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_package_imports_and_complex_two_classes() {
        assertFormatsWholeFile(
                """
                package demo.deep; import java.util.List;import java.util.Map;abstract class Base{abstract int value();int fallback(){return 0;}}
                class Derived{int run(int a){if(a>1)return a;else return 1;}}
                """,
                """
                package demo.deep;

                import java.util.List;
                import java.util.Map;

                abstract class Base {
                    abstract int value();

                    int fallback() {
                        return 0;
                    }
                }

                class Derived {
                    int run(int a) {
                        if (a > 1)
                            return a;
                        else
                            return 1;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_three_methods_and_mixed_control_flow() {
        assertFormatsWholeFile(
                """
                class Program{void boot(){start();}int choose(int a,int b){if(a>b)return a;else return b;}void spin(){for(i=0;i<3;i++)tick();}}
                """,
                """
                class Program {
                    void boot() {
                        start();
                    }

                    int choose(int a, int b) {
                        if (a > b)
                            return a;
                        else
                            return b;
                    }

                    void spin() {
                        for (i = 0; i < 3; i++)
                            tick();
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_multiple_classes_and_else_if_in_second_class() {
        assertFormatsWholeFile(
                """
                class First{int id(){return 1;}}
                class Second{int pick(int a,int b,int c){if(a>b)return a;else if(b>c)return b;else return c;}}
                """,
                """
                class First {
                    int id() {
                        return 1;
                    }
                }

                class Second {
                    int pick(int a, int b, int c) {
                        if (a > b)
                            return a;
                        else if (b > c)
                            return b;
                        else
                            return c;
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_package_imports_empty_class_and_worker_class() {
        assertFormatsWholeFile(
                """
                package app.core;
                import java.util.List;
                import java.util.Set;
                class Empty{}
                class Worker{void go(){for(i=0;i<1;i++){step();}}}
                """,
                """
                package app.core;

                import java.util.List;
                import java.util.Set;

                class Empty {}

                class Worker {
                    void go() {
                        for (i = 0; i < 1; i++) {
                            step();
                        }
                    }
                }"""
        );
    }

    @Test
    void formats_whole_file_with_deeply_nested_if_for_and_blocks() {
        assertFormatsWholeFile(
                """
                class Deep{int run(int a,int b){if(a>b){for(i=0;i<a;i++){if(i==b)return i;tick();}return a;}else if(a==b){for(i=0;;i++)step();}else{b--;return b;}}}
                """,
                """
                class Deep {
                    int run(int a, int b) {
                        if (a > b) {
                            for (i = 0; i < a; i++) {
                                if (i == b)
                                    return i;
                                tick();
                            }
                            return a;
                        }
                        else if (a == b) {
                            for (i = 0;; i++)
                                step();
                        }
                        else {
                            b--;
                            return b;
                        }
                    }
                }"""
        );
    }

    private static void assertFormatsWholeFile(String code, String expected) {
        String formatted = formatASTFromRootToLeafs(code);
        assertThat(formatted).isEqualTo(expected);
    }

    private static String formatASTFromRootToLeafs(String code) {
        CompilationUnit compilationUnit = StaticJavaParser.parse(code);
        return engine.format(compilationUnit, "CompilationUnit");
    }

    private static List<RuleDef> parseRules() {
        ebnfLexer lexer = new ebnfLexer(CharStreams.fromString(ALL_RULES));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ebnfParser parser = new ebnfParser(tokens);
        ebnfParser.RulelistContext ctx = parser.rulelist();
        RuleAstBuilder builder = new RuleAstBuilder();
        return builder.buildRules(ctx);
    }
}
