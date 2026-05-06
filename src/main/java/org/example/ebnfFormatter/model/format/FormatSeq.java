package org.example.ebnfFormatter.model.format;

import java.util.List;

public record FormatSeq(List<FormatAst> items) implements FormatAst {
}