package org.example.ebnfFormatter.model.pattern;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PatternModelSmokeTest {

    @Test
    void creates_node_pattern_with_fields() {
        FieldPat condition = new FieldPat("condition", false, new RuleRef("Expr"));
        FieldPat thenStmt = new FieldPat("thenStmt", false, new RuleRef("Stmt"));
        FieldPat elseStmt = new FieldPat("elseStmt", true, new RuleRef("Stmt?"));

        NodePat nodePat = new NodePat("IfStmt", List.of(condition, thenStmt, elseStmt));

        assertThat(nodePat.typeName()).isEqualTo("IfStmt");
        assertThat(nodePat.fields()).hasSize(3);
    }

    @Test
    void creates_quantified_pattern() {
        Quant quant = new Quant(new RuleRef("Stmt"), QuantifierKind.ZERO_OR_MORE);

        assertThat(quant.quantifier()).isEqualTo(QuantifierKind.ZERO_OR_MORE);
        assertThat(quant.pattern()).isNotNull();
    }

    @Test
    void creates_alt_and_seq() {
        Seq seq = new Seq(List.of(new RuleRef("Expr"), new RuleRef("Stmt")));
        Alt alt = new Alt(List.of(seq, new RuleRef("Block")));

        assertThat(seq.items()).hasSize(2);
        assertThat(alt.options()).hasSize(2);
    }

    @Test
    void creates_list_pattern() {
        ListPat listPat = new ListPat(List.of(new RuleRef("Stmt"), new RuleRef("Stmt")));

        assertThat(listPat.items()).hasSize(2);
    }

    @Test
    void creates_literal() {
        Lit lit = new Lit("if");
        assertThat(lit.value()).isEqualTo("if");
    }
}