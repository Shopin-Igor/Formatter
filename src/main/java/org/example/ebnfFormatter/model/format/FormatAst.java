package org.example.ebnfFormatter.model.format;

public sealed interface FormatAst permits FormatDirective, FormatGroup,
        FormatIfPresent, FormatJoin, FormatPlaceholder, FormatSeq, FormatText {
}