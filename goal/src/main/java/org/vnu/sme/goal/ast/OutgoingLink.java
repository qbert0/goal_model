package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

/**
 * Quan hệ gắn với phần tử chủ đích nguồn (ví dụ {@code task X &> Y} → nguồn X, đích tên trong {@code target}).
 */
public record OutgoingLink(Kind kind, Token target) {

    public enum Kind {
        REFINE_AND,
        REFINE_OR,
        CONTRIB_MAKE,
        CONTRIB_HELP,
        CONTRIB_HURT,
        CONTRIB_BREAK,
        QUALIFY,
        NEEDED_BY
    }
}
