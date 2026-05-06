package org.example.ebnfFormatter.model.format;

public record FormatJoin(String placeholderName, FormatAst separator) implements FormatAst {
}