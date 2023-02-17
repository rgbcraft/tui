package com.rgbcraft.tui;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.List;

public class ParserData {
    private String className;
    private String packageName;
    private List<FieldDeclaration> fields;
    private final List<MethodDeclaration> methods;
    private final List<ConstructorDeclaration> constructors;
    private final List<InitializerDeclaration> initializers;

    public ParserData(String className, String packageName, List<FieldDeclaration> fields, List<MethodDeclaration> methods, List<ConstructorDeclaration> constructors, List<InitializerDeclaration> initializers) {
        this.className = className;
        this.fields = fields;
        this.methods = methods;
        this.constructors = constructors;
        this.initializers = initializers;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<FieldDeclaration> getFields() {
        return fields;
    }

    public void setFields(List<FieldDeclaration> fields) {
        this.fields = fields;
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
