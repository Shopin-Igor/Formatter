package org.example.ebnfFormatter.model.pattern;

import java.util.List;

public record Alt(List<PatternAst> options) implements PatternAst {
    @Override public String dump() {
        StringBuilder sb = new StringBuilder("(alt");
        for (PatternAst o : options) sb.append("\n  ").append(PatternAst.indent(o.dump()));
        sb.append("\n)");
        return sb.toString();
    }
}
