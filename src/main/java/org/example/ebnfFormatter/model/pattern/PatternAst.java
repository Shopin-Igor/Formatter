package org.example.ebnfFormatter.model.pattern;


public sealed interface PatternAst permits Alt, Seq, NodePat, ListPat, Quant, Lit, FieldPat, RuleRef {

    String dump();

    static String indent(String s) {
        return s.replace("\n", "\n  ");
    }

    static String block(String s) {
        return "\n  " + indent(s) + "\n";
    }
}
