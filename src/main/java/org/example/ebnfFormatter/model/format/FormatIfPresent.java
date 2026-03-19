package org.example.ebnfFormatter.model.format;

public record FormatIfPresent(String name, FormatAst body) implements FormatAst {
}