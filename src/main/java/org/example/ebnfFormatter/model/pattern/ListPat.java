package org.example.ebnfFormatter.model.pattern;

import java.util.List;

public record ListPat(List<PatternAst> items) implements PatternAst {
    @Override public String dump() {
        StringBuilder sb = new StringBuilder("(list");
        for (PatternAst it : items) sb.append("\n  ").append(PatternAst.indent(it.dump()));
        sb.append("\n)");
        return sb.toString();
    }
}
