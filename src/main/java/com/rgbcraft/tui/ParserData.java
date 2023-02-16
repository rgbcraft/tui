package com.rgbcraft.tui;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.List;

public class ParserData {
    private final String className;
    private final List<FieldDeclaration> fields;
    private final List<MethodDeclaration> methods;
    private final List<ConstructorDeclaration> constructors;
    private final List<InitializerDeclaration> initializers;

    public ParserData(String className, List<FieldDeclaration> fields, List<MethodDeclaration> methods, List<ConstructorDeclaration> constructors, List<InitializerDeclaration> initializers) {
        this.className = className;
        this.fields = fields;
        this.methods = methods;
        this.constructors = constructors;
        this.initializers = initializers;
    }

    public String getClassName() {
        return className;
    }

    public List<FieldDeclaration> getFields() {
        return fields;
    }

    public List<MethodDeclaration> getMethods() {
        return methods;
    }

    public List<ConstructorDeclaration> getConstructors() {
        return this.constructors;
    }

    public List<InitializerDeclaration> getInitializers() {
        return initializers;
    }
}
