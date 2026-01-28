package org.example.rules.ast;

import java.util.List;

public record Alt(List<RuleAst> options) implements RuleAst {
    @Override public String dump() {
        StringBuilder sb = new StringBuilder("(alt");
        for (RuleAst o : options) sb.append("\n  ").append(RuleAst.indent(o.dump()));
        sb.append("\n)");
        return sb.toString();
    }
}
