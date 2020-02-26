package org.mapstruct.ap.internal.model.assignment;

import org.mapstruct.ap.internal.model.MethodReference;
import org.mapstruct.ap.internal.model.TypeConversion;
import org.mapstruct.ap.internal.model.common.Assignment;
import org.mapstruct.ap.internal.model.common.SourceRHS;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.common.TypeFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// Optional<A> -> Optional<B>
// A -> Optional<B>
// Optional<A> -> B
public class OptionalWrapper extends AssignmentWrapper {

    private final TypeFactory typeFactory;

    private final Type sourceType;
    private final boolean sourceOptional;
    private final Type sourcElementType; // if optional

    private final Type targetType;
    private final boolean targetOptional;
    private final Type targetElementType; // if optional

    private final Assignment elementAssignment;

    public OptionalWrapper(
            TypeFactory typeFactory,
            Type sourceType, boolean sourceOptional, Type sourceElementType,
            Type targetType, boolean targetOptional, Type targetElementType,
            Assignment sourceAssignment,
            Assignment elementAssignment,
            boolean fieldAssignment
    ) {
        super(sourceAssignment, fieldAssignment);

        this.typeFactory = typeFactory;
        this.sourceType = sourceType;
        this.sourceOptional = sourceOptional;
        this.sourcElementType = sourceElementType;

        this.targetType = targetType;
        this.targetOptional = targetOptional;
        this.targetElementType = targetElementType;

        this.elementAssignment = elementAssignment;
    }

    @Override
    public Set<Type> getImportTypes() {
        HashSet<Type> results = new HashSet<>();
        results.addAll( super.getImportTypes() );
        results.add(typeFactory.getType(Optional.class));
        return results;
    }

    @Override
    public Type getSourceType() {
        return sourceType;
    }

    public boolean isSourceOptional() {
        return sourceOptional;
    }

    public Type getSourcElementType() {
        return sourcElementType;
    }

    public Type getTargetType() {
        return targetType;
    }

    public boolean isTargetOptional() {
        return targetOptional;
    }

    public Type getTargetElementType() {
        return targetElementType;
    }

    public Assignment getElementAssignment() {
        return elementAssignment;
    }

    public String conversion(String paraName) {
        SetterWrapper wrapper = (SetterWrapper) elementAssignment;
        Assignment assign = wrapper.getAssignment();
        if(assign instanceof MethodReference) {
            return String.format("%s(%s)", ((MethodReference) assign).getName(),  paraName);
        }
        else if(assign instanceof SourceRHS) {
            return paraName;
        }
        else if(assign instanceof TypeConversion) {
            TypeConversion tc = (TypeConversion) assign;
            return String.format("%s%s%s", tc.getOpenExpression(), paraName, tc.getCloseExpression());
        }
        else throw new IllegalArgumentException("TODO:" + assign.getClass());
    }

    public String getDefaultValue(){
        return targetType.getNull();
    }
}
