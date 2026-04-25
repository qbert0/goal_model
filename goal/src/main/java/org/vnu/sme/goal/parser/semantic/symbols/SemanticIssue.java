package org.vnu.sme.goal.parser.semantic.symbols;

public record SemanticIssue(String code, String message, int line, int column) {
}

