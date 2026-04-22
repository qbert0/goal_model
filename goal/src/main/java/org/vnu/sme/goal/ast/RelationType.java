package org.vnu.sme.goal.ast;

public enum RelationType {
    AND_REFINE,      // &>
    OR_REFINE,       // |>
    CONTRIB_MAKE,    // ++>
    CONTRIB_HELP,    // +>
    CONTRIB_HURT,    // ->
    CONTRIB_BREAK,   // -->
    QUALIFY,         // =>
    NEEDED_BY        // <>
}