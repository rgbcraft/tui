package com.rgbcraft.tui;

import com.github.javaparser.ast.body.FieldDeclaration;

import java.util.List;

public class ParserData {
    private final String className;
    private final List<FieldDeclaration> fields;

    public ParserData(String className, List<FieldDeclaration> fields) {
        this.className = className;
        this.fields = fields;
    }

    public String getClassName() {
        return className;
    }

    public List<FieldDeclaration> getFields() {
        return fields;
    }
}
