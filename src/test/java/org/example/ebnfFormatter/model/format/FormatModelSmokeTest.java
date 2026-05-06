package org.example.ebnfFormatter.model.format;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class FormatModelSmokeTest {

    @Test
    void creates_format_sequence() {
        FormatSeq seq = new FormatSeq(List.of(
                new FormatText("if"),
                new FormatDirective(DirectiveKind.SP),
                new FormatPlaceholder("Expr")
        ));

        assertThat(seq.items()).hasSize(3);
    }

    @Test
    void creates_if_present_node() {
        FormatIfPresent node = new FormatIfPresent(
                "Stmt?",
                new FormatSeq(List.of(
                        new FormatDirective(DirectiveKind.SP),
                        new FormatText("else"),
                        new FormatDirective(DirectiveKind.SP),
                        new FormatPlaceholder("Stmt?")
                ))
        );

        assertThat(node.name()).isEqualTo("Stmt?");
        assertThat(node.body()).isNotNull();
    }

    @Test
    void creates_join_node() {
        FormatJoin join = new FormatJoin(
                "ParamList*",
                new FormatText(", ")
        );

        assertThat(join.placeholderName()).isEqualTo("ParamList*");
        assertThat(join.separator()).isNotNull();
    }

    @Test
    void creates_group() {
        FormatGroup group = new FormatGroup(
                new FormatSeq(List.of(
                        new FormatText("("),
                        new FormatPlaceholder("Expr"),
                        new FormatText(")")
                ))
        );

        assertThat(group.body()).isNotNull();
    }
}