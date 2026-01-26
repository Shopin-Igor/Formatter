package org.example.rules.ast;

public sealed interface RuleAst permits RuleDef, Alt, Seq, NodePat, RuleRef, ListPat, Quant, Lit {

    String dump();

    static String indent(String s) {
        return s.replace("\n", "\n  ");
    }

    static String block(String s) {
        return "\n  " + indent(s) + "\n";
    }
}
