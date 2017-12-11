package com.skydoves.processor;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

/**
 * Developed by skydoves on 2017-12-10.
 * Copyright (c) 2017 skydoves rights reserved.
 */

public class ScopeClassGenerator {

    private final MethodScopeAnnotatedClass annotatedClazz;
    private final String packageName;
    private final String scopeName;

    private static final String SCOPE_PREFIX = "Scope";
    private final String INITIALIZE_IMPL = "initializeScopes";
    private static final String SCOPE_INITIALIZE = "Init";

    public ScopeClassGenerator(MethodScopeAnnotatedClass annotatedClazz, String packageName, String scopeName) {
        this.annotatedClazz = annotatedClazz;
        this.packageName = packageName;
        this.scopeName = scopeName;
    }

    public TypeSpec generate() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(getClazzPrefixName(scopeName))
                .addJavadoc("Generated by PreferenceRoom. (https://github.com/skydoves/PreferenceRoom).\n")
                .addModifiers(Modifier.PUBLIC)
                .superclass(TypeName.get(annotatedClazz.annotatedElement.asType()));

        annotatedClazz.annotatedElement.getEnclosedElements().stream()
                .filter(element -> element instanceof ExecutableElement)
                .map(element -> (ExecutableElement) element)
                .forEach(method -> {
                    method.getAnnotationMirrors().stream()
                            .filter(annotationMirror -> annotationMirror.toString().equals(getAnnotationName()))
                            .forEach(annotation -> builder.addMethod(getInitializeMethod(method)));

                    if(method.getSimpleName().toString().equals(INITIALIZE_IMPL))
                        builder.addMethod(getInitializeScopesMethod(method));
                });

        return builder.build();
    }

    private MethodSpec getInitializeMethod(ExecutableElement method) {
        return MethodSpec.overriding(method)
                .addStatement("super.$N()", method.getSimpleName())
                .build();
    }

    private MethodSpec getInitializeScopesMethod(ExecutableElement initializeScopeMethod) {
        MethodSpec.Builder builder =  MethodSpec.overriding(initializeScopeMethod);

        annotatedClazz.annotatedElement.getEnclosedElements().stream()
                .filter(element -> element instanceof ExecutableElement)
                .map(element -> (ExecutableElement) element)
                .forEach(method -> {
                    method.getAnnotationMirrors().stream()
                            .filter(annotationMirror -> annotationMirror.toString().equals(getAnnotationName()))
                            .forEach(annotation ->builder.addStatement("$N()", method.getSimpleName().toString()));
                });

        return builder.build();
    }

    private String getClazzPrefixName(String scopeName) {
        return this.annotatedClazz.clazzName + SCOPE_PREFIX + scopeName;
    }

    private String getAnnotationName() {
        return "@" + SCOPE_INITIALIZE + scopeName + SCOPE_PREFIX;
    }
}