package org.example.rules.ast;

public record FieldPat(String name, boolean optional, RuleAst value) {
    public String dump() {
        return "(field " + name + (optional ? "?" : "") + "\n  "
                + RuleAst.indent(value.dump()) + "\n)";
    }
}
