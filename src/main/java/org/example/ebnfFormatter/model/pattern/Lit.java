package org.example.ebnfFormatter.model.pattern;

public record Lit(Object value) implements PatternAst {
    @Override public String dump() {
        if (value == null) return "(lit null)";
        if (value instanceof String s) return "(lit \"" + s + "\")";
        return "(lit " + value + ")";
    }
}
