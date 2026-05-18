package org.example.ebnfFormatter.runtime;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.example.ebnfFormatter.dsl.RuleAstBuilder;
import org.example.ebnfFormatter.match.PatternMatcher;
import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.render.TemplateRenderer;
import org.example.ebnfLexer;
import org.example.ebnfParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class UnknownNodesFallbackEndToEndTest {

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
    private static final TypeRegistryUniversal typeRegistry = new TypeRegistryUniversal();
    private static final PatternMatcher matcher = new PatternMatcher(typeRegistry, registry);
    private static final FormatterEngine engine = new FormatterEngine(registry, matcher, new TemplateRenderer());
    private static final JavaParser javaParser = new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)
    );

    @TestFactory
    Stream<DynamicTest> formats_unknown_nodes_without_gluing_words() {
        List<FallbackCase> cases = fallbackCases();

        return cases.stream()
                .map(testCase -> dynamicTest(
                        testCase.name(),
                        () -> assertFormatsSingleStatement(testCase.statement())
                ));
    }

    private static List<FallbackCase> fallbackCases() {
        return List.of(
                new FallbackCase("while block", "while (i < limit) { i++; }"),
                new FallbackCase("while expression body", "while (reader.ready()) line = reader.readLine();"),
                new FallbackCase("do while block", "do { i++; } while (i < limit);"),
                new FallbackCase("do while expression body", "do work(); while (running);"),
                new FallbackCase("switch colon labels", "switch (kind) { case 1: work(); break; default: reset(); }"),
                new FallbackCase("switch arrow labels", "switch (kind) { case 1 -> work(); default -> reset(); }"),
                new FallbackCase("try catch", "try { work(); } catch (RuntimeException e) { handle(e); }"),
                new FallbackCase("try finally", "try { work(); } finally { cleanup(); }"),
                new FallbackCase("try multi catch finally", "try { work(); } catch (IllegalArgumentException | IllegalStateException e) { handle(e); } finally { cleanup(); }"),
                new FallbackCase("try with resources", "try (Input input = open()) { read(input); }"),
                new FallbackCase("synchronized block", "synchronized (lock) { work(); }"),
                new FallbackCase("assert expression", "assert ready;"),
                new FallbackCase("assert expression with message", "assert ready : \"not ready\";"),
                new FallbackCase("throw statement", "throw new RuntimeException(\"boom\");"),
                new FallbackCase("break statement", "break;"),
                new FallbackCase("continue statement", "continue;"),
                new FallbackCase("labeled while", "outer: while (running) { break outer; }"),
                new FallbackCase("local int declaration", "int count = 0;"),
                new FallbackCase("final local declaration", "final String name = user.name();"),
                new FallbackCase("var local declaration", "var result = compute();"),
                new FallbackCase("array creation declaration", "int[] values = new int[] {1, 2, 3};"),
                new FallbackCase("nested array initializer", "int[][] matrix = {{1, 2}, {3, 4}};"),
                new FallbackCase("generic collection declaration", "java.util.List<String> names = new java.util.ArrayList<>();"),
                new FallbackCase("lambda declaration", "Runnable task = () -> work();"),
                new FallbackCase("method reference declaration", "java.util.function.Function<String, String> trim = String::trim;"),
                new FallbackCase("anonymous class declaration", "Object object = new Object() { public String toString() { return \"x\"; } };"),
                new FallbackCase("instanceof pattern declaration", "boolean matches = value instanceof String text;"),
                new FallbackCase("ternary declaration", "int selected = flag ? left : right;"),
                new FallbackCase("cast declaration", "String casted = (String) value;"),
                new FallbackCase("shift declaration", "long mask = 1L << shift;"),
                new FallbackCase("assignment expression", "a = b + c * d;"),
                new FallbackCase("array assignment expression", "arr[index++] += delta;"),
                new FallbackCase("prefix increment expression", "++counter;"),
                new FallbackCase("postfix decrement expression", "counter--;"),
                new FallbackCase("field assignment expression", "this.field = value;"),
                new FallbackCase("super method call expression", "super.toString();"),
                new FallbackCase("chained call expression", "builder.append(\"a\").append(\"b\");"),
                new FallbackCase("stream method references", "names.stream().map(String::trim).forEach(this::use);"),
                new FallbackCase("lambda in constructor call", "new Thread(() -> run()).start();"),
                new FallbackCase("optional method reference", "java.util.Optional.ofNullable(name).ifPresent(System.out::println);"),
                new FallbackCase("local class declaration", "class Local { void go() {} }"),
                new FallbackCase("if with unknown direct bodies", "if (ready) while (running) { tick(); } else do { sleep(); } while (waiting);"),
                new FallbackCase("for with unknown switch body", "for (i = 0; i < limit; i++) switch (i) { case 0 -> start(); default -> tick(); }"),
                new FallbackCase("for each statement", "for (String item : items) { use(item); }"),
                new FallbackCase("try catch rethrow", "try { risky(); } catch (Exception e) { throw new IllegalStateException(e); }"),
                new FallbackCase("switch multiple arrow labels", "switch (mode) { case A, B -> run(); case C -> stop(); default -> reset(); }"),
                new FallbackCase("while assignment condition", "while ((line = reader.readLine()) != null) { process(line); }"),
                new FallbackCase("do while assignment body", "do { current = current.next(); } while (current != null);"),
                new FallbackCase("synchronized computed lock", "synchronized (cache.compute(key, (k, v) -> v)) { touch(); }"),
                new FallbackCase("assert lambda expression", "assert items.stream().allMatch(x -> x != null) : items;")
        );
    }

    private static void assertFormatsSingleStatement(String statement) {
        String code = """
                class Sample { void run() { %s } }
                """.formatted(statement);

        String expected = """
                class Sample {
                    void run() {
                %s
                    }
                }""".formatted(indent(parseStatement(statement).toString(), 8));

        assertThat(formatASTFromRootToLeafs(code)).isEqualTo(expected);
    }

    private static String indent(String text, int spaces) {
        String indentation = " ".repeat(spaces);
        return text.lines()
                .map(line -> indentation + line)
                .collect(Collectors.joining("\n"));
    }

    private static String formatASTFromRootToLeafs(String code) {
        CompilationUnit compilationUnit = parseCompilationUnit(code);
        return engine.format(compilationUnit, "CompilationUnit");
    }

    private static CompilationUnit parseCompilationUnit(String code) {
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        return result.getResult()
                .orElseThrow(() -> new IllegalArgumentException(result.getProblems().toString()));
    }

    private static Statement parseStatement(String statement) {
        ParseResult<Statement> result = javaParser.parseStatement(statement);
        return result.getResult()
                .orElseThrow(() -> new IllegalArgumentException(result.getProblems().toString()));
    }

    private static RuleRegistry registryWithRules() {
        RuleRegistry ruleRegistry = new RuleRegistry();
        ruleRegistry.registerAll(ruleDefs);
        return ruleRegistry;
    }

    private static List<RuleDef> parseRules() {
        ebnfLexer lexer = new ebnfLexer(CharStreams.fromString(ALL_RULES));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ebnfParser parser = new ebnfParser(tokens);
        ebnfParser.RulelistContext ctx = parser.rulelist();
        RuleAstBuilder builder = new RuleAstBuilder();
        return builder.buildRules(ctx);
    }

    private record FallbackCase(String name, String statement) {
    }
}
