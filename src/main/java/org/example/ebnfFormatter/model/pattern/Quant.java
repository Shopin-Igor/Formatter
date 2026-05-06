package org.example.ebnfFormatter.model.pattern;

public record Quant(PatternAst pattern, QuantifierKind quantifier) implements PatternAst {
    @Override
    public String dump() {
        return "(quant " + quantifier + "\n  " + PatternAst.indent(pattern.dump()) + "\n)";
    }
}
