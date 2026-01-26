package org.example.rules.ast;

public record Lit(Object value) implements RuleAst {
    @Override public String dump() {
        if (value == null) return "(lit null)";
        if (value instanceof String s) return "(lit \"" + s + "\")";
        return "(lit " + value + ")";
    }
}
