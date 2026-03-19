package org.example.ebnfFormatter.model;

import org.example.ebnfFormatter.model.format.FormatAst;
import org.example.ebnfFormatter.model.pattern.PatternAst;

public record RuleDef(String name, PatternAst pattern, FormatAst format) {}