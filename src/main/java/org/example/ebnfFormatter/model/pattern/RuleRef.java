package org.example.ebnfFormatter.model.pattern;

public record RuleRef(String name) implements PatternAst {
    @Override
    public String dump() {
        return "(ref <" + name + ">)";
    }
}