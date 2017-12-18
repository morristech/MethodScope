/*
 * Copyright (C) 2017 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

public class ScopeClassGenerator {

    private final MethodScopeAnnotatedClass annotatedClazz;
    private final String packageName;
    private final String scopeName;
    private final Elements elementUtils;

    private static final String SCOPE_PREFIX = "Scope";
    private final String INITIALIZE_IMPL = "initializeScopes";
    private static final String SCOPE_INITIALIZE = "Init";

    private static final String VALUE_SCOPES = "scopes";
    private static final String VALUE_VALUES = "values";

    public ScopeClassGenerator(MethodScopeAnnotatedClass annotatedClazz, String packageName, String scopeName, Elements elementUtils) {
        this.annotatedClazz = annotatedClazz;
        this.packageName = packageName;
        this.scopeName = scopeName;
        this.elementUtils = elementUtils;
    }

    public TypeSpec generate() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(getClazzPrefixName(scopeName))
                .addJavadoc("Generated by MethodScope. (https://github.com/skydoves/MethodScope).\n")
                .addModifiers(Modifier.PUBLIC)
                .superclass(TypeName.get(annotatedClazz.annotatedElement.asType()));

        addScopeAnnotations(builder);
        addInitializeScopesMethod(builder);

        builder.addMethods(getScopedMethodScopes());

        return builder.build();
    }

    private void addScopeAnnotations(TypeSpec.Builder builder) {
        this.annotatedClazz.scopeAnnotationList.forEach(annotationMirror -> {
            AnnotationSpec annotationSpec = AnnotationSpec.get(annotationMirror);
            annotationSpec.members.get(VALUE_SCOPES).stream()
                    .filter(scope -> scope.toString().replace("\"", "").equals(this.scopeName))
                    .forEach(scope -> {
                        int scopePosition = annotationSpec.members.get(VALUE_SCOPES).indexOf(scope);
                        for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> element : annotationMirror.getElementValues().entrySet()) {
                            if (element.getKey().getSimpleName().toString().equals(VALUE_VALUES)) {
                                List valueList = (List) element.getValue().getValue();
                                AnnotationMirror scopeAnnotationMirror = (AnnotationMirror) valueList.get(scopePosition);
                                AnnotationSpec scopeAnnotationSpec = AnnotationSpec.get(scopeAnnotationMirror);
                                builder.addAnnotation(scopeAnnotationSpec);
                                break;
                            }
                        }
            });
        });
    }

    private void addInitializeScopesMethod(TypeSpec.Builder builder) {
        this.annotatedClazz.annotatedElement.getEnclosedElements().stream()
                .filter(element -> element instanceof ExecutableElement)
                .map(element -> (ExecutableElement) element)
                .forEach(method -> {
                    method.getAnnotationMirrors().stream()
                            .filter(annotationMirror -> annotationMirror.toString().equals(getInitializeAnnotationName()))
                            .forEach(annotation -> builder.addMethod(getInitializeMethod(method)));

                    if(method.getSimpleName().toString().equals(INITIALIZE_IMPL))
                        builder.addMethod(getInitializeScopesMethod(method));
                });
    }

    private MethodSpec getInitializeMethod(ExecutableElement method) {
        return MethodSpec.overriding(method)
                .addStatement("super.$N()", method.getSimpleName())
                .build();
    }

    private MethodSpec getInitializeScopesMethod(ExecutableElement initializeScopeMethod) {
        MethodSpec.Builder builder =  MethodSpec.overriding(initializeScopeMethod)
                .addStatement("super.$N()", INITIALIZE_IMPL);

        annotatedClazz.annotatedElement.getEnclosedElements().stream()
                .filter(element -> element instanceof ExecutableElement)
                .map(element -> (ExecutableElement) element)
                .forEach(method -> {
                    method.getAnnotationMirrors().stream()
                            .filter(annotationMirror -> annotationMirror.toString().equals(getInitializeAnnotationName()))
                            .forEach(annotation -> builder.addStatement("$N()", method.getSimpleName().toString()));
                });

        return builder.build();
    }

    private List<MethodSpec> getScopedMethodScopes() {
        List<MethodSpec> methodSpecList = new ArrayList<>();

        annotatedClazz.annotatedElement.getEnclosedElements().stream()
                .filter(element -> element instanceof ExecutableElement)
                .map(element -> (ExecutableElement) element)
                .forEach(method -> {
                    method.getAnnotationMirrors().stream()
                            .filter(annotationMirror -> annotationMirror.toString().equals(getScopeAnnotationName()))
                            .forEach(annotation -> {
                                MethodSpec overrideSpec = MethodSpec.overriding(method)
                                        .addStatement("super.$N()", method.getSimpleName())
                                        .build();
                                methodSpecList.add(overrideSpec);
                            });
                });

        return methodSpecList;
    }

    private String getClazzPrefixName(String scopeName) {
        return this.annotatedClazz.clazzName + SCOPE_PREFIX + scopeName;
    }

    private String getInitializeAnnotationName() {
        return "@" + SCOPE_INITIALIZE + scopeName + SCOPE_PREFIX;
    }

    private String getScopeAnnotationName() {
        return "@" + scopeName + SCOPE_PREFIX;
    }
}
