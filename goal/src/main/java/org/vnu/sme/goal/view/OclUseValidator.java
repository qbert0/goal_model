package org.vnu.sme.goal.view;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tzi.use.uml.mm.MAssociationEnd;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.mm.MNavigableElement;
import org.tzi.use.uml.ocl.type.EnumType;
import org.vnu.sme.goal.mm.Goal;
import org.vnu.sme.goal.mm.IntentionalElement;
import org.vnu.sme.goal.mm.Task;

public class OclUseValidator {
    private static final Pattern ITERATOR_PATTERN = Pattern.compile(
            "((?:self|[A-Za-z_][A-Za-z_0-9]*)(?:\\.[A-Za-z_][A-Za-z_0-9]*(?:@pre)?)*)\\s*->\\s*(?:forAll|exists|select|reject|collect)\\s*\\(\\s*([A-Za-z_][A-Za-z_0-9]*)");
    private static final Pattern PATH_PATTERN = Pattern.compile(
            "\\b(self|[A-Za-z_][A-Za-z_0-9]*)\\s*((?:\\.[A-Za-z_][A-Za-z_0-9]*(?:@pre)?)+)");
    private static final Pattern ENUM_PATTERN = Pattern.compile("\\b([A-Za-z_][A-Za-z_0-9]*)::([A-Za-z_][A-Za-z_0-9]*)\\b");

    private final MModel useModel;
    private final MClass rootClass;

    public OclUseValidator(MModel useModel) {
        this.useModel = useModel;
        this.rootClass = resolveRootClass(useModel);
    }

    public OclValidationReport validate(IntentionalElement element) {
        OclValidationReport report = new OclValidationReport();
        if (useModel == null || rootClass == null) {
            return report;
        }

        if (element instanceof Goal) {
            validateExpression(((Goal) element).getOclExpression(), report);
        } else if (element instanceof Task) {
            Task task = (Task) element;
            validateExpression(task.getPreExpression(), report);
            validateExpression(task.getPostExpression(), report);
        }

        return report;
    }

    private void validateExpression(String expression, OclValidationReport report) {
        if (expression == null || expression.isBlank()) {
            return;
        }

        Map<String, MClass> variables = new HashMap<>();
        variables.put("self", rootClass);

        Matcher iteratorMatcher = ITERATOR_PATTERN.matcher(expression);
        while (iteratorMatcher.find()) {
            MClass collectionElementType = resolvePathType(iteratorMatcher.group(1), variables, report);
            if (collectionElementType != null) {
                variables.put(iteratorMatcher.group(2), collectionElementType);
            }
        }

        Matcher pathMatcher = PATH_PATTERN.matcher(expression);
        Set<String> checked = new HashSet<>();
        while (pathMatcher.find()) {
            String path = pathMatcher.group(1) + pathMatcher.group(2);
            if (checked.add(path)) {
                resolvePathType(path, variables, report);
            }
        }

        Matcher enumMatcher = ENUM_PATTERN.matcher(expression);
        while (enumMatcher.find()) {
            EnumType enumType = useModel.enumType(enumMatcher.group(1));
            if (enumType == null) {
                report.add("Unknown enum type '" + enumMatcher.group(1) + "'");
            } else if (!enumType.contains(enumMatcher.group(2))) {
                report.add("Unknown enum literal '" + enumMatcher.group(1) + "::" + enumMatcher.group(2) + "'");
            }
        }
    }

    private MClass resolvePathType(String path, Map<String, MClass> variables, OclValidationReport report) {
        String[] parts = path.replace("@pre", "").split("\\.");
        if (parts.length == 0) {
            return null;
        }

        MClass current = variables.get(parts[0]);
        int startIndex = 1;

        if (current == null) {
            current = resolveImplicitRootProperty(parts[0], report);
            startIndex = 1;
        }

        if (current == null) {
            return null;
        }

        if (parts.length == 1) {
            return current;
        }

        for (int i = startIndex; i < parts.length; i++) {
            String property = parts[i];

            MAttribute attribute = current.attribute(property, true);
            if (attribute != null) {
                if (attribute.type() instanceof MClass) {
                    current = (MClass) attribute.type();
                } else {
                    current = null;
                }
                continue;
            }

            MClass roleTarget = roleTarget(current, property);
            if (roleTarget != null) {
                current = roleTarget;
                continue;
            }

            report.add("Unknown property '" + property + "' on USE class '" + current.name() + "' in path '" + path + "'");
            return null;
        }

        return current;
    }

    private MClass resolveImplicitRootProperty(String property, OclValidationReport report) {
        if (rootClass == null) {
            return null;
        }

        MAttribute attribute = rootClass.attribute(property, true);
        if (attribute != null && attribute.type() instanceof MClass type) {
            return type;
        }

        MClass roleTarget = roleTarget(rootClass, property);
        if (roleTarget != null) {
            return roleTarget;
        }

        report.add("Unknown root property '" + property + "' on USE class '" + rootClass.name()
                + "'. Use 'self." + property + "' or define the property in the USE model.");
        return null;
    }

    private MClass roleTarget(MClass source, String roleName) {
        MNavigableElement navigableEnd = source.navigableEnd(roleName);
        if (navigableEnd != null) {
            return navigableEnd.cls();
        }

        for (MAssociationEnd end : source.getAssociationEnd(roleName)) {
            return end.cls();
        }
        return null;
    }

    private MClass resolveRootClass(MModel model) {
        if (model == null) {
            return null;
        }

        MClass systemState = model.getClass("SystemState");
        if (systemState != null) {
            return systemState;
        }

        return model.classes().isEmpty() ? null : model.classes().iterator().next();
    }
}
