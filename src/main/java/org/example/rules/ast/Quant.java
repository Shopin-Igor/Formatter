package org.example.rules.ast;

public record Quant(Q q, RuleAst child) implements RuleAst {
    @Override public String dump() {
        return "(quant " + q + "\n  " + RuleAst.indent(child.dump()) + "\n)";
    }
}
