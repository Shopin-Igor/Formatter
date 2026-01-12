package org.example;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;


class IfVisitor extends ebnfBaseVisitor<Void> {
    private final TokenStreamRewriter rewriter;

    public IfVisitor(TokenStreamRewriter rewriter) {
        this.rewriter = rewriter;
    }

    @Override
    public Void visitIf_stmt(ebnfParser.If_stmtContext ctx) {
        rewriter.insertAfter(ctx.IF().getSymbol(), " ");

        TerminalNode lbraceNode = ctx.LBRACE(0);
        if (lbraceNode != null) {
            rewriter.insertBefore(lbraceNode.getSymbol(), " \n");
        }

        return visitChildren(ctx);
    }

}
