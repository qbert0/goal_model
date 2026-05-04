package org.vnu.sme.goal.validator.proof;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.vnu.sme.goal.mm.ocl.BinaryExp;
import org.vnu.sme.goal.mm.ocl.BooleanLiteralExp;
import org.vnu.sme.goal.mm.ocl.EnumLiteralExp;
import org.vnu.sme.goal.mm.ocl.Expression;
import org.vnu.sme.goal.mm.ocl.FeatureCallExp;
import org.vnu.sme.goal.mm.ocl.IteratorExp;
import org.vnu.sme.goal.mm.ocl.LiteralExp;
import org.vnu.sme.goal.mm.ocl.NullLiteralExp;
import org.vnu.sme.goal.mm.ocl.OperationCallExp;
import org.vnu.sme.goal.mm.ocl.SelfExp;
import org.vnu.sme.goal.mm.ocl.UnaryExp;
import org.vnu.sme.goal.mm.ocl.VariableDeclaration;
import org.vnu.sme.goal.mm.ocl.VariableExp;

public final class OclEntailmentChecker {
    public ProofCheckResult check(ProofObligation obligation) {
        if (obligation.antecedent() == null) {
            return ProofCheckResult.failed("Missing antecedent expression.");
        }
        if (obligation.consequent() == null) {
            return ProofCheckResult.failed("Missing consequent expression.");
        }
        return entails(obligation.antecedent(), obligation.consequent());
    }

    public ProofCheckResult entails(Expression antecedent, Expression consequent) {
        if (consequent == null) {
            return ProofCheckResult.failed("Missing consequent expression.");
        }
        if (antecedent == null) {
            return ProofCheckResult.failed("Missing antecedent expression.");
        }
        if (equivalent(antecedent, consequent)) {
            return ProofCheckResult.proved("Consequent matches the antecedent exactly.");
        }
        if (isBooleanLiteral(consequent, true)) {
            return ProofCheckResult.proved("Consequent is the boolean literal true.");
        }
        if (consequent instanceof BinaryExp binary && binary.getOperator() == BinaryExp.Operator.AND) {
            ProofCheckResult left = entails(antecedent, binary.getLeft());
            if (!left.proved()) {
                return left;
            }
            ProofCheckResult right = entails(antecedent, binary.getRight());
            if (!right.proved()) {
                return right;
            }
            return ProofCheckResult.proved("Both conjuncts are derivable from the antecedent.");
        }
        if (consequent instanceof BinaryExp binary && binary.getOperator() == BinaryExp.Operator.IMPLIES) {
            if (contradicts(antecedent, binary.getLeft())) {
                return ProofCheckResult.proved("Antecedent contradicts the implication premise, so the implication holds.");
            }
            ProofCheckResult right = entails(antecedent, binary.getRight());
            if (right.proved()) {
                return ProofCheckResult.proved("Antecedent entails the implication conclusion.");
            }
            return ProofCheckResult.failed("Could not derive the implication conclusion from the antecedent.");
        }
        if (consequent instanceof UnaryExp unary && unary.getOperator() == UnaryExp.Operator.NOT) {
            if (antecedent instanceof UnaryExp antecedentNot
                    && antecedentNot.getOperator() == UnaryExp.Operator.NOT
                    && equivalent(antecedentNot.getOperand(), unary.getOperand())) {
                return ProofCheckResult.proved("Antecedent already states the required negation.");
            }
            if (contradicts(antecedent, unary.getOperand())) {
                return ProofCheckResult.proved("Antecedent contradicts the forbidden condition.");
            }
            return ProofCheckResult.failed("Could not prove the required negation from the antecedent.");
        }
        if (antecedent instanceof BinaryExp binary && binary.getOperator() == BinaryExp.Operator.AND) {
            List<Expression> conjuncts = flattenAnd(binary);
            for (Expression conjunct : conjuncts) {
                ProofCheckResult direct = entails(conjunct, consequent);
                if (direct.proved()) {
                    return ProofCheckResult.proved("Consequent is derivable from conjunct '" + conjunct.getText() + "'.");
                }
            }
            return proveFromConjunctSet(conjuncts, consequent);
        }
        if (antecedent instanceof IteratorExp leftIterator && consequent instanceof IteratorExp rightIterator) {
            return proveIterator(leftIterator, rightIterator);
        }
        if (structurallyContains(antecedent, consequent)) {
            return ProofCheckResult.proved("Consequent appears structurally within the antecedent.");
        }
        return ProofCheckResult.failed("No symbolic entailment rule could discharge this obligation.");
    }

    private ProofCheckResult proveFromConjunctSet(List<Expression> conjuncts, Expression consequent) {
        if (consequent instanceof IteratorExp rightIterator) {
            for (Expression conjunct : conjuncts) {
                if (!(conjunct instanceof IteratorExp leftIterator)) {
                    continue;
                }
                ProofCheckResult iteratorProof = proveIterator(leftIterator, rightIterator);
                if (iteratorProof.proved()) {
                    return iteratorProof;
                }
            }
        }
        for (Expression conjunct : conjuncts) {
            if (contradicts(conjunct, consequent)) {
                return ProofCheckResult.failed(
                        "Antecedent contains a conjunct that contradicts the required consequent: '" + conjunct.getText() + "'.");
            }
        }
        return ProofCheckResult.failed("Could not derive the consequent from the antecedent conjunct set.");
    }

    private ProofCheckResult proveIterator(IteratorExp antecedent, IteratorExp consequent) {
        if (antecedent.getKind() != consequent.getKind()) {
            return ProofCheckResult.failed("Iterator kinds differ: " + antecedent.getKind() + " vs " + consequent.getKind() + ".");
        }
        if (!equivalent(antecedent.getSource(), consequent.getSource())) {
            return ProofCheckResult.failed("Iterator sources differ: " + antecedent.getSource().getText()
                    + " vs " + consequent.getSource().getText() + ".");
        }
        if (!sameVariableShape(antecedent.getVariables(), consequent.getVariables())) {
            return ProofCheckResult.failed("Iterator variables differ between antecedent and consequent.");
        }
        return entails(antecedent.getBody(), consequent.getBody());
    }

    private boolean sameVariableShape(List<VariableDeclaration> left, List<VariableDeclaration> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            VariableDeclaration leftVar = left.get(i);
            VariableDeclaration rightVar = right.get(i);
            if (!Objects.equals(leftVar.getName(), rightVar.getName())) {
                return false;
            }
            if (!Objects.equals(leftVar.getTypeName(), rightVar.getTypeName())) {
                return false;
            }
        }
        return true;
    }

    private List<Expression> flattenAnd(Expression expression) {
        List<Expression> result = new ArrayList<>();
        flattenAndInto(expression, result);
        return result;
    }

    private void flattenAndInto(Expression expression, List<Expression> result) {
        if (expression instanceof BinaryExp binary && binary.getOperator() == BinaryExp.Operator.AND) {
            flattenAndInto(binary.getLeft(), result);
            flattenAndInto(binary.getRight(), result);
            return;
        }
        result.add(expression);
    }

    private boolean structurallyContains(Expression root, Expression target) {
        if (root == null || target == null) {
            return false;
        }
        if (equivalent(root, target)) {
            return true;
        }
        if (root instanceof BinaryExp binary) {
            return structurallyContains(binary.getLeft(), target) || structurallyContains(binary.getRight(), target);
        }
        if (root instanceof UnaryExp unary) {
            return structurallyContains(unary.getOperand(), target);
        }
        if (root instanceof IteratorExp iterator) {
            return structurallyContains(iterator.getSource(), target) || structurallyContains(iterator.getBody(), target);
        }
        if (root instanceof OperationCallExp call) {
            if (structurallyContains(call.getSource(), target)) {
                return true;
            }
            for (Expression argument : call.getArguments()) {
                if (structurallyContains(argument, target)) {
                    return true;
                }
            }
        }
        if (root instanceof FeatureCallExp feature) {
            return structurallyContains(feature.getSource(), target);
        }
        return false;
    }

    private boolean contradicts(Expression left, Expression right) {
        if (left == null || right == null) {
            return false;
        }
        if (equivalent(left, right)) {
            return false;
        }
        if (left instanceof BinaryExp leftBinary && leftBinary.getOperator() == BinaryExp.Operator.AND) {
            return contradicts(leftBinary.getLeft(), right) || contradicts(leftBinary.getRight(), right);
        }
        if (right instanceof BinaryExp rightBinary && rightBinary.getOperator() == BinaryExp.Operator.AND) {
            return contradicts(left, rightBinary.getLeft()) || contradicts(left, rightBinary.getRight());
        }
        if (left instanceof UnaryExp leftNot && leftNot.getOperator() == UnaryExp.Operator.NOT) {
            return equivalent(leftNot.getOperand(), right);
        }
        if (right instanceof UnaryExp rightNot && rightNot.getOperator() == UnaryExp.Operator.NOT) {
            return equivalent(left, rightNot.getOperand());
        }
        if (!(left instanceof BinaryExp leftBinary) || !(right instanceof BinaryExp rightBinary)) {
            return false;
        }
        if (!equivalent(leftBinary.getLeft(), rightBinary.getLeft())) {
            return false;
        }
        if (leftBinary.getOperator() == BinaryExp.Operator.EQUALS
                && rightBinary.getOperator() == BinaryExp.Operator.EQUALS) {
            return !equivalent(leftBinary.getRight(), rightBinary.getRight());
        }
        if (leftBinary.getOperator() == BinaryExp.Operator.EQUALS
                && rightBinary.getOperator() == BinaryExp.Operator.NOT_EQUALS) {
            return equivalent(leftBinary.getRight(), rightBinary.getRight());
        }
        if (leftBinary.getOperator() == BinaryExp.Operator.NOT_EQUALS
                && rightBinary.getOperator() == BinaryExp.Operator.EQUALS) {
            return equivalent(leftBinary.getRight(), rightBinary.getRight());
        }
        return false;
    }

    private boolean equivalent(Expression left, Expression right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left.getClass() != right.getClass()) {
            return false;
        }
        if (left instanceof BinaryExp leftBinary && right instanceof BinaryExp rightBinary) {
            return leftBinary.getOperator() == rightBinary.getOperator()
                    && equivalent(leftBinary.getLeft(), rightBinary.getLeft())
                    && equivalent(leftBinary.getRight(), rightBinary.getRight());
        }
        if (left instanceof UnaryExp leftUnary && right instanceof UnaryExp rightUnary) {
            return leftUnary.getOperator() == rightUnary.getOperator()
                    && equivalent(leftUnary.getOperand(), rightUnary.getOperand());
        }
        if (left instanceof IteratorExp leftIterator && right instanceof IteratorExp rightIterator) {
            return leftIterator.getKind() == rightIterator.getKind()
                    && sameVariableShape(leftIterator.getVariables(), rightIterator.getVariables())
                    && equivalent(leftIterator.getSource(), rightIterator.getSource())
                    && equivalent(leftIterator.getBody(), rightIterator.getBody());
        }
        if (left instanceof OperationCallExp leftCall && right instanceof OperationCallExp rightCall) {
            if (!Objects.equals(leftCall.getFeatureName(), rightCall.getFeatureName())
                    || leftCall.isAtPre() != rightCall.isAtPre()
                    || !equivalent(leftCall.getSource(), rightCall.getSource())
                    || leftCall.getArguments().size() != rightCall.getArguments().size()) {
                return false;
            }
            for (int i = 0; i < leftCall.getArguments().size(); i++) {
                if (!equivalent(leftCall.getArguments().get(i), rightCall.getArguments().get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (left instanceof FeatureCallExp leftFeature && right instanceof FeatureCallExp rightFeature) {
            return Objects.equals(leftFeature.getFeatureName(), rightFeature.getFeatureName())
                    && leftFeature.isAtPre() == rightFeature.isAtPre()
                    && equivalent(leftFeature.getSource(), rightFeature.getSource());
        }
        if (left instanceof VariableExp leftVar && right instanceof VariableExp rightVar) {
            return Objects.equals(leftVar.getName(), rightVar.getName());
        }
        if (left instanceof SelfExp && right instanceof SelfExp) {
            return true;
        }
        if (left instanceof EnumLiteralExp leftEnum && right instanceof EnumLiteralExp rightEnum) {
            return Objects.equals(leftEnum.getEnumName(), rightEnum.getEnumName())
                    && Objects.equals(leftEnum.getLiteralName(), rightEnum.getLiteralName());
        }
        if (left instanceof BooleanLiteralExp leftBool && right instanceof BooleanLiteralExp rightBool) {
            return leftBool.getValue() == rightBool.getValue();
        }
        if (left instanceof NullLiteralExp && right instanceof NullLiteralExp) {
            return true;
        }
        if (left instanceof LiteralExp && right instanceof LiteralExp) {
            return Objects.equals(left.getText(), right.getText());
        }
        return Objects.equals(left.getText(), right.getText());
    }

    private boolean isBooleanLiteral(Expression expression, boolean value) {
        return expression instanceof BooleanLiteralExp booleanLiteral && booleanLiteral.getValue() == value;
    }
}
