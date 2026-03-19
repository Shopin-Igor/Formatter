package org.example.ebnfFormatter.dsl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.example.ebnfLexer;
import org.example.ebnfParser;
import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.model.format.FormatAst;
import org.example.ebnfFormatter.model.pattern.NodePat;
import org.example.ebnfFormatter.model.pattern.PatternAst;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleAstBuilderTest {

    @Test
    void parses_if_stmt_rule() {

        String rules = """
              <IfStmt> ::= IfStmt(condition=<Expr>, thenStmt=<Stmt>, elseStmt?=<Stmt>)
                    => "if" sp "(" <Expr> ")" sp <Stmt> ifpresent(Stmt?, sp "else" sp <Stmt?>);
              <IfStmt> ::= IfStmt(condition=<Expr>, thenStmt=<Stmt>, elseStmt?=<Stmt>)
                    => "if" "(" <Expr> ")" <Stmt> ifpresent(Stmt?, "else" <Stmt?>);
              <IfStmt> ::= IfStmt(condition=<Expr>, thenStmt=<Stmt>, elseStmt=<Stmt>)
                    => "if" "(" <Expr> ")" sp <Stmt> ifpresent(Stmt?, sp "else" sp <Stmt?>);
              """;
        List<RuleDef> parsed = parseRules(rules);

        assertThat(parsed).hasSize(3);

        RuleDef rule = parsed.getFirst();
        assertThat(rule.name()).isEqualTo("IfStmt");

        PatternAst pattern = rule.pattern();
        assertThat(pattern).isInstanceOf(NodePat.class);

        FormatAst format = rule.format();
        assertThat(format).isNotNull();
    }

    @Test
    void parses_method_declaration_rule() {
        String rules = """
                <MethodDeclaration> ::= MethodDeclaration(type=<Type>, name=<Name>, parameters=[<ParamList*>], body?=<Stmt>)
                    => <Type> sp <Name> "(" join(<ParamList*>, ", ") ")" ifpresent(Stmt?, sp <Stmt?>);
                """;

        List<RuleDef> parsed = parseRules(rules);

        assertThat(parsed).hasSize(1);

        RuleDef rule = parsed.getFirst();
        assertThat(rule.name()).isEqualTo("MethodDeclaration");
        assertThat(rule.pattern()).isNotNull();
        assertThat(rule.format()).isNotNull();
    }

    @Test
    void parses_for_stmt_rule() {
        String rules = """
                <ForStmt> ::= ForStmt(initialization=[<ExprList*>], compare?=<Expr>, update=[<ExprList*>], body=<Stmt>)
                    => "for" sp "(" join(<ExprList*>, ", ") ";" ifpresent(Expr?, sp <Expr?>) ";" ifpresent(ExprList*, sp join(<ExprList*>, ", ")) ")" sp <Stmt>;
                <ForStmt> ::= ForStmt(initialization=[<ExprList+>], compare?=<Expr>, update=[<ExprList*>], body=<Stmt>)
                    => "for" sp "(" join(<ExprList*>, ", ") ";" ifpresent(Expr?, sp <Expr?>) ";" ifpresent(ExprList*, sp join(<ExprList*>, ", ")) ")" sp <Stmt>;
                """;

        List<RuleDef> parsed = parseRules(rules);

        assertThat(parsed).hasSize(2);

        RuleDef rule = parsed.getFirst();
        assertThat(rule.name()).isEqualTo("ForStmt");
        assertThat(rule.pattern()).isNotNull();
        assertThat(rule.format()).isNotNull();
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