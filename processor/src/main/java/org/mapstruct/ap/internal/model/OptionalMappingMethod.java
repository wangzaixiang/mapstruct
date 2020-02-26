package org.mapstruct.ap.internal.model;

import static org.mapstruct.ap.internal.util.Collections.first;

import java.util.*;

import org.mapstruct.ap.internal.model.assignment.LocalVarWrapper;
import org.mapstruct.ap.internal.model.assignment.SetterWrapper;
import org.mapstruct.ap.internal.model.common.Assignment;
import org.mapstruct.ap.internal.model.common.FormattingParameters;
import org.mapstruct.ap.internal.model.common.SourceRHS;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.source.*;
import org.mapstruct.ap.internal.prism.NullValueMappingStrategyPrism;
import org.mapstruct.ap.internal.util.Strings;

/**
 * A {@link MappingMethod} implemented by a {@link Mapper} class which maps one iterable type to another. The collection
 * elements are mapped either by a {@link TypeConversion} or another mapping method.
 *
 * @author Gunnar Morling
 */
public class OptionalMappingMethod extends ContainerMappingMethod {

    public static class Builder extends ContainerMappingMethodBuilder<Builder, OptionalMappingMethod> {

        public Builder() {
            super( Builder.class, "collection element" );
        }

        @Override
        protected Type getElementType(Type parameterType) {
            return parameterType.isArrayType() ? parameterType.getComponentType() : first(
                    parameterType.determineTypeArguments( Optional.class ) ).getTypeBound();
        }

        @Override
        protected Assignment getWrapper(Assignment assignment, Method method) { // TODO
            Type resultType = method.getResultType();
            // target accessor is setter, so decorate assignment as setter
            if ( resultType.isArrayType() ) {
                return new LocalVarWrapper( assignment, method.getThrownTypes(), resultType, false );
            }
            else {
                return new SetterWrapper( assignment, method.getThrownTypes(), false );
            }
        }

        @Override
        protected OptionalMappingMethod instantiateMappingMethod(Method method, Collection<String> existingVariables,
                                                                 Assignment assignment, MethodReference factoryMethod, boolean mapNullToDefault, String loopVariableName,
                                                                 List<LifecycleCallbackMethodReference> beforeMappingMethods,
                                                                 List<LifecycleCallbackMethodReference> afterMappingMethods, SelectionParameters selectionParameters) {
            return new OptionalMappingMethod(
                    method,
                    existingVariables,
                    assignment,
                    factoryMethod,
                    mapNullToDefault,
                    loopVariableName,
                    beforeMappingMethods,
                    afterMappingMethods,
                    selectionParameters
            );
        }
    }

    public static class Builder2 extends AbstractBaseBuilder<Builder2> {

        private Type sourceType;
        private boolean isSourceOptional;
        private Type sourceElementType;

        private Type targetType;
        private boolean isTargetOptional;
        private Type targetElementType;

        private String errorMessagePart;

        private NullValueMappingStrategyPrism nullValueMappingStrategy;
        private SelectionParameters selectionParameters;
        private FormattingParameters formattingParameters;

        public Builder2() {
            super(Builder2.class);
        }

        public Builder2 sourceType(Type sourceType) {
            this.sourceType = sourceType;
            if(sourceType.isOptionalType()){
                this.isSourceOptional = true;
                this.sourceElementType = sourceType.getTypeParameters().get(0);
            }
            else {
                this.sourceElementType = sourceType;
            }
            return this;
        }

        public Builder2 targetType(Type targetType){
            this.targetType = targetType;
            if (targetType.isOptionalType()) {
                this.isTargetOptional = true;
                this.targetElementType = targetType.getTypeParameters().get(0);
            }
            else {
                this.targetElementType = targetType;
            }
            return this;
        }

        public Builder2 formattingParameters(FormattingParameters formattingParameters) {
            this.formattingParameters = formattingParameters;
            return myself;
        }

        public Type getSourceType() {
            return sourceType;
        }

        public boolean isSourceOptional() {
            return isSourceOptional;
        }

        public Type getSourceElementType() {
            return sourceElementType;
        }

        public Type getTargetType() {
            return targetType;
        }

        public boolean isTargetOptional() {
            return isTargetOptional;
        }

        public Type getTargetElementType() {
            return targetElementType;
        }

        public Builder2 selectionParameters(SelectionParameters selectionParameters) {
            this.selectionParameters = selectionParameters;
            return myself;
        }

        public Builder2 nullValueMappingStrategy(NullValueMappingStrategyPrism nullValueMappingStrategy) {
            this.nullValueMappingStrategy = nullValueMappingStrategy;
            return myself;
        }

        public Assignment build() {

            String loopVariableName =
                    Strings.getSafeVariableName( sourceElementType.getName(), method.getParameterNames() );

            SourceRHS sourceRHS = new SourceRHS(
                    "arg",
                    isSourceOptional? sourceElementType : sourceType,
                    new HashSet<>(),
                    errorMessagePart);


            Assignment assignment = ctx.getMappingResolver().getTargetAssignment(
                    method,
                    targetElementType,
                    null,
                    null,
                    null,
                    sourceRHS,
                    false,
                    null
            );
            if(assignment == null) {
                assignment = forgeMapping(sourceRHS, sourceElementType, targetElementType);
            }

            if(assignment == null){
                if ( method instanceof ForgedMethod ) {
                    // leave messaging to calling property mapping
                    return null;
                }
                else {
                    reportCannotCreateMapping(
                            method,
                            String.format( "%s \"%s\"", sourceRHS.getSourceErrorMessagePart(), sourceRHS.getSourceType() ),
                            sourceRHS.getSourceType(),
                            targetElementType,
                            ""
                    );
                }
            }
            else {
                if ( method instanceof ForgedMethod ) {
                    ForgedMethod forgedMethod = (ForgedMethod) method;
                    forgedMethod.addThrownTypes( assignment.getThrownTypes() );
                }
            }
            assignment = getWrapper( assignment, method );

            return assignment;
        }

        private String getName(Type sourceType, Type targetType) {
            String fromName = getName( sourceType );
            String toName = getName( targetType );
            return Strings.decapitalize( fromName + "To" + toName );
        }

        private String getName(Type type) {
            StringBuilder builder = new StringBuilder();
            for ( Type typeParam : type.getTypeParameters() ) {
                builder.append( typeParam.getIdentification() );
            }
            builder.append( type.getIdentification() );
            return builder.toString();
        }


        Assignment forgeMapping(SourceRHS sourceRHS, Type sourceType, Type targetType) {
            if ( !canGenerateAutoSubMappingBetween( sourceType, targetType ) ) {
                return null;
            }

            String name = getName( sourceType, targetType );
            name = Strings.getSafeVariableName( name, ctx.getNamesOfMappingsToGenerate() );
            ForgedMethodHistory history = null;
            if ( method instanceof ForgedMethod ) {
                history = ( (ForgedMethod) method ).getHistory();
            }
            ForgedMethod forgedMethod = new ForgedMethod(
                    name,
                    sourceType,
                    targetType,
                    method.getMapperConfiguration(),
                    method.getExecutable(),
                    method.getContextParameters(),
                    method.getContextProvidedMethods(),
                    new ForgedMethodHistory(
                            history,
                            Strings.stubPropertyName( sourceRHS.getSourceType().getName() ),
                            Strings.stubPropertyName( targetType.getName() ),
                            sourceRHS.getSourceType(),
                            targetType,
                            shouldUsePropertyNamesInHistory(),
                            sourceRHS.getSourceErrorMessagePart()
                    ),
                    null,
                    true
            );

            return createForgedAssignment(
                    sourceRHS,
                    ctx.getTypeFactory().builderTypeFor( targetType, BeanMapping.builderPrismFor( method ) ),
                    forgedMethod
            );
        }

        protected Assignment getWrapper(Assignment assignment, Method method) { // TODO
            Type resultType = method.getResultType();
            // target accessor is setter, so decorate assignment as setter
            if ( resultType.isArrayType() ) {
                return new LocalVarWrapper( assignment, method.getThrownTypes(), resultType, false );
            }
            else {
                return new SetterWrapper( assignment, method.getThrownTypes(), false );
            }
        }

        protected boolean shouldUsePropertyNamesInHistory() {
            return false;
        }
    }

    private OptionalMappingMethod(Method method, Collection<String> existingVariables, Assignment parameterAssignment,
                                  MethodReference factoryMethod, boolean mapNullToDefault, String loopVariableName,
                                  List<LifecycleCallbackMethodReference> beforeMappingReferences,
                                  List<LifecycleCallbackMethodReference> afterMappingReferences,
                                  SelectionParameters selectionParameters) {
        super(
                method,
                existingVariables,
                parameterAssignment,
                factoryMethod,
                mapNullToDefault,
                loopVariableName,
                beforeMappingReferences,
                afterMappingReferences,
                selectionParameters
        );
    }

    public Type getSourceElementType() {
        Type sourceParameterType = getSourceParameter().getType();

        if ( sourceParameterType.isArrayType() ) {
            return sourceParameterType.getComponentType();
        }
        else {
            return first( sourceParameterType.determineTypeArguments( Optional.class ) ).getTypeBound();
        }
    }

    @Override
    public Type getResultElementType() {
        if ( getResultType().isArrayType() ) {
            return getResultType().getComponentType();
        }
        else {
            return first( getResultType().determineTypeArguments( Optional.class ) );
        }
    }
}
