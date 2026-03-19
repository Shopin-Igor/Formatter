package org.example.ebnfFormatter.model.pattern;

import java.util.List;

public record Seq(List<PatternAst> items) implements PatternAst {
    @Override public String dump() {
        if (items.size() == 1) return items.get(0).dump();
        StringBuilder sb = new StringBuilder("(seq");
        for (PatternAst it : items) sb.append("\n  ").append(PatternAst.indent(it.dump()));
        sb.append("\n)");
        return sb.toString();
    }
}
