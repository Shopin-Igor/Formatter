package org.example.HardCodeIfVisitor;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.example.OldEbnfBaseVisitor;
import org.example.OldEbnfParser;
import org.example.ebnfParser;

public class IfVisitor extends OldEbnfBaseVisitor<Void> {
    private final TokenStreamRewriter rewriter;

    public IfVisitor(TokenStreamRewriter rewriter) {
        this.rewriter = rewriter;
    }

    @Override
    public Void visitIf_stmt(OldEbnfParser.If_stmtContext ctx) {
        rewriter.insertAfter(ctx.IF().getSymbol(), " ");

        TerminalNode lbraceNode = ctx.LBRACE(0);
        if (lbraceNode != null) {
            rewriter.insertBefore(lbraceNode.getSymbol(), " \n");
        }

        return visitChildren(ctx);
    }

}