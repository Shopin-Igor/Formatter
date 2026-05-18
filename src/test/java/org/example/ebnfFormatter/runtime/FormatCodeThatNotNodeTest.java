package org.example.ebnfFormatter.runtime;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.PrimitiveType;
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

public class FormatCodeThatNotNodeTest {
    private static final String NOT_NODE_RULES = """
            <ClassOrInterfaceDeclaration> ::= ClassOrInterfaceDeclaration(modifiers=[<Modifier>*], name=<SimpleName>)
              => join(<Modifier>, " ") sp "class" sp <SimpleName>;

            <Modifier> ::= Modifier(keyword=<Keyword>)
              => <Keyword>;

            <PrimitiveType> ::= PrimitiveType(type=<Primitive>)
              => <Primitive>;

            <BinaryExpr> ::= BinaryExpr(left=<LeftExpr>, operator=<Operator>, right=<RightExpr>)
              => <LeftExpr> sp <Operator> sp <RightExpr>;

            <AssignExpr> ::= AssignExpr(target=<TargetExpr>, operator=<Operator>, value=<ValueExpr>)
              => <TargetExpr> sp <Operator> sp <ValueExpr>;

            <UnaryExpr> ::= UnaryExpr(operator=<Operator>, expression=<Expression>)
              => <Operator><Expression>;
            """;

    private static final List<RuleDef> ontNodeRuleDefs = parseRules(NOT_NODE_RULES);
    private static final RuleRegistry ontNodeRegistry = registryWithRules(ontNodeRuleDefs);
    private static final TypeRegistryUniversal ontNodeTypeRegistry = new TypeRegistryUniversal();
    private static final PatternMatcher ontNodeMatcher = new PatternMatcher(
            ontNodeTypeRegistry,
            ontNodeRegistry
    );
    private static final FormatterEngine ontNodeEngine = new FormatterEngine(
            ontNodeRegistry,
            ontNodeMatcher,
            new TemplateRenderer()
    );

    private static final JavaParser javaParser = new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)
    );


    @Test
    void formats_modifier_keyword_final_that_is_not_node() {
        Modifier modifier = parseFirstNode("final class Sample {}", Modifier.class);

        assertThat(ontNodeEngine.format(modifier, "Modifier")).isEqualTo("final");
    }

    @Test
    void formats_public_final_keywords_from_class_modifiers() {
        ClassOrInterfaceDeclaration classDeclaration = parseFirstNode(
                "public final class Sample {}",
                ClassOrInterfaceDeclaration.class
        );

        assertThat(ontNodeEngine.format(classDeclaration, "ClassOrInterfaceDeclaration"))
                .isEqualTo("public final class Sample");
    }

    @Test
    void formats_primitive_type_value_that_is_not_node() {
        PrimitiveType primitiveType = parseFirstNode(
                "class Sample { int run() { return 1; } }",
                PrimitiveType.class
        );

        assertThat(ontNodeEngine.format(primitiveType, "PrimitiveType")).isEqualTo("int");
    }

    @Test
    void formats_binary_operator_value_that_is_not_node() {
        BinaryExpr binaryExpr = parseFirstStatementNode("return a == b;", BinaryExpr.class);

        assertThat(ontNodeEngine.format(binaryExpr, "BinaryExpr")).isEqualTo("a == b");
    }

    @Test
    void formats_assign_operator_value_that_is_not_node() {
        AssignExpr assignExpr = parseFirstStatementNode("a += b;", AssignExpr.class);

        assertThat(ontNodeEngine.format(assignExpr, "AssignExpr")).isEqualTo("a += b");
    }

    @Test
    void formats_unary_operator_value_that_is_not_node() {
        UnaryExpr unaryExpr = parseFirstStatementNode("++counter;", UnaryExpr.class);

        assertThat(ontNodeEngine.format(unaryExpr, "UnaryExpr")).isEqualTo("++counter");
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

    private static <T extends Node> T parseFirstNode(String code, Class<T> nodeType) {
        return parseCompilationUnit(code)
                .findFirst(nodeType)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find " + nodeType.getSimpleName()));
    }

    private static <T extends Node> T parseFirstStatementNode(String statement, Class<T> nodeType) {
        return parseStatement(statement)
                .findFirst(nodeType)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find " + nodeType.getSimpleName()));
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
