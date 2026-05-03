package org.vnu.sme.goal.parser.semantic.enums;

/**
 * Predicate kind of a {@code goalContract} attached to an {@code ElementSymbol}.
 * <p>{@code ACHIEVE_UNIQUE} is the form-2 variant defined in spec v2 §3.3
 * (i.e. {@code achieve for unique (...) in ...:&lt;body&gt;}); the other three
 * cover the simple forms {@code achieve|maintain|avoid: &lt;body&gt;}.</p>
 */
public enum GoalContractType {
    ACHIEVE,
    ACHIEVE_UNIQUE,
    MAINTAIN,
    AVOID
}
