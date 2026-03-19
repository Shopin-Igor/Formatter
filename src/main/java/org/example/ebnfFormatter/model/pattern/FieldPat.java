package org.example.ebnfFormatter.model.pattern;

public record FieldPat(String name, boolean optional, PatternAst value) implements PatternAst {
    @Override
    public String dump() {
        return "(field " + name + (optional ? "?" : "") + "\n  "
                + PatternAst.indent(value.dump()) + "\n)";
    }
}
