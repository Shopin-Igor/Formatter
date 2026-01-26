package org.example.rules.ast;

import java.util.List;

public record Seq(List<RuleAst> items) implements RuleAst {
    @Override public String dump() {
        if (items.size() == 1) return items.get(0).dump();
        StringBuilder sb = new StringBuilder("(seq");
        for (RuleAst it : items) sb.append("\n  ").append(RuleAst.indent(it.dump()));
        sb.append("\n)");
        return sb.toString();
    }
}
