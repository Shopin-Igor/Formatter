package org.example.rules.ast;

import java.util.List;

public record ListPat(List<RuleAst> items) implements RuleAst {
    @Override public String dump() {
        StringBuilder sb = new StringBuilder("(list");
        for (RuleAst it : items) sb.append("\n  ").append(RuleAst.indent(it.dump()));
        sb.append("\n)");
        return sb.toString();
    }
}
