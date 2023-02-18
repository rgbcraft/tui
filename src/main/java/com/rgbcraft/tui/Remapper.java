package com.rgbcraft.tui;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.MappingFormats;
import org.cadixdev.lorenz.io.MappingsReader;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.MethodMapping;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/* This class has the job to remap what's given into it
 */
public class Remapper {
    private final ConcurrentLinkedQueue<ParserData> data;
    private final MappingSet mappings;

    private int idx;

    public Remapper(Parser parser) {
        try {
            URL urlSrg = getClass().getResource("/client.srg");
            if (urlSrg == null) throw new NullPointerException("Can't find client.srg");

            MappingsReader reader = MappingFormats.SRG.createReader(urlSrg.openStream());

            this.data = parser.getData();
            this.mappings = reader.read();
            this.idx = 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasMoreClasses() {
        return this.idx < this.data.size();
    }

    public void mapClass() {
        ParserData parserData = (ParserData) this.data.toArray()[this.idx++];
        if (parserData == null) return;

        this.mappings.getClassMapping(parserData.getClassName()).ifPresent((classMapping) -> {
            parserData.setClassName(classMapping.getDeobfuscatedName());
            parserData.setPackageName(classMapping.getDeobfuscatedPackage());

            parserData.setFields(mapFields(parserData.getFields(), classMapping));
        });
    }

    private List<FieldDeclaration> mapFields(List<FieldDeclaration> fields, ClassMapping<?, ?> classMapping) {
        return fields.stream().peek(field -> {
            VariableDeclarator variable = field.getVariables().get(0);
            VariableDeclarator clone = variable.clone();
            Optional<Expression> initializerExpression = variable.getInitializer();

            if (initializerExpression.isPresent()) {
                clone.setInitializer(mapExpression(initializerExpression.get()));
            }

            System.out.println(clone.getInitializer());
            String name = variable.getNameAsString();
            String type = variable.getTypeAsString();
            boolean isArray = type.endsWith("[]");

            if (isArray) type = type.replace("[]", "");

            String finalType = type;
            mappings.getClassMapping(type).flatMap(fieldClassMapping -> classMapping.getFieldMapping(name)).ifPresent((fieldMapping -> {
                Optional<? extends ClassMapping<?, ?>> innerClassMapping = mappings.getClassMapping(finalType);

                if (innerClassMapping.isPresent()) {
                    if (isArray) clone.setType(innerClassMapping.get().getSimpleDeobfuscatedName() + "[]");
                    else clone.setType(innerClassMapping.get().getSimpleDeobfuscatedName());
                }

                clone.setName(fieldMapping.getSimpleDeobfuscatedName());
            }));
        }).collect(Collectors.toList());
    }

    private Expression mapExpression(Expression expression) {
        if (expression.isCastExpr()) {
            CastExpr castExpr = expression.asCastExpr();
            CastExpr clone = castExpr.clone();
            Optional<? extends ClassMapping<?, ?>> classMapping = mappings.getClassMapping(castExpr.getTypeAsString());
            classMapping.ifPresent(mapping -> clone.setType(mapping.getSimpleDeobfuscatedName()));

            return clone;
        } else if (expression.isMethodCallExpr()) {
            // TODO: this is so messy + map it
            MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
            MethodCallExpr clone = methodCallExpr.clone();

            Optional<Expression> scope = methodCallExpr.getScope();

            try {
                if (scope.isPresent()) {
                    Expression scopeExpr = scope.get();
                    ResolvedType resolvedType = scopeExpr.calculateResolvedType();
                    System.out.println("Test: " + resolvedType.describe() + " " + resolvedType.isReferenceType());
                    Optional<? extends ClassMapping<?, ?>> classMapping = mappings.getClassMapping(resolvedType.describe());
                    if (classMapping.isPresent()) {
                        System.out.println("STILL HERE1");
                        Optional<MethodMapping> methodMapping = classMapping.get().getMethodMapping(methodCallExpr.getName().asString(), methodCallExpr.resolve().toDescriptor());
                        System.out.println("STILL HERE2");
                        if (methodMapping.isPresent()) {
                            clone.setName(methodMapping.get().getSimpleDeobfuscatedName());
                        }
                    }
                }
                System.out.println("PASSED");
            } catch (IllegalStateException e) {
                System.out.println("ERRR");
                e.printStackTrace();
            }

            if (scope.isPresent()) {
                clone.setScope(mapExpression(scope.get()));
            }

            NodeList<Expression> args = new NodeList<>();
            for (Expression arg : methodCallExpr.getArguments()) {
                args.add(mapExpression(arg));
            }
            clone.setArguments(args);

            System.out.println("Scope: " + clone.getScope() + "; Name: " + clone.getName() + "!!!");
            return clone;
        } else if (expression.isObjectCreationExpr()) {
            ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
            ObjectCreationExpr clone = objectCreationExpr.clone();

            if (objectCreationExpr.getScope().isPresent()) {
                clone.setScope(mapExpression(objectCreationExpr.getScope().get()));
            }

            NodeList<Expression> args = new NodeList<>();
            for (Expression arg : objectCreationExpr.getArguments()) {
                args.add(mapExpression(arg));
            }
            clone.setArguments(args);

            String name = objectCreationExpr.getType().getNameAsString();

            Optional<? extends ClassMapping<?, ?>> classMapping = mappings.getClassMapping(name);
            classMapping.ifPresent(mapping -> clone.setType(mapping.getSimpleDeobfuscatedName()));

            System.out.println("Object: " + clone + "???");
            return clone;
        } else if (expression.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = expression.asFieldAccessExpr();
            FieldAccessExpr clone = fieldAccessExpr.clone();

            try {
                Optional<? extends ClassMapping<?, ?>> classMapping = mappings.getClassMapping(fieldAccessExpr.getScope().calculateResolvedType().describe());
                classMapping.ifPresent(mapping -> {
                    Optional<FieldMapping> fieldMapping = mapping.getFieldMapping(fieldAccessExpr.getName().getIdentifier());
                    fieldMapping.ifPresent(value -> clone.setName(value.getDeobfuscatedName()));
                });
            } catch (UnsolvedSymbolException e) {
                e.printStackTrace();
            }

            clone.setScope(mapExpression(fieldAccessExpr.getScope()));

            return clone;
        } else if (expression.isClassExpr()) {
            ClassExpr classExpr = expression.asClassExpr();
            ClassExpr clone = classExpr.clone();

            mappings.getClassMapping(classExpr.getTypeAsString())
                    .ifPresent((classMapping -> clone.setType(classMapping.getSimpleDeobfuscatedName())));

            return clone;
        } else if (expression.isNameExpr()) {
            NameExpr nameExpr = expression.asNameExpr();
            NameExpr clone = nameExpr.clone();

            mappings.getClassMapping(nameExpr.getNameAsString())
                    .ifPresent((classMapping -> clone.setName(classMapping.getSimpleDeobfuscatedName())));

            return clone;
        } else if (expression.isEnclosedExpr()) {
            EnclosedExpr enclosedExpr = expression.asEnclosedExpr();
            EnclosedExpr clone = enclosedExpr.clone();

            clone.setInner(mapExpression(enclosedExpr.getInner()));

            return clone;
        } else if (expression.isArrayCreationExpr()) {
            ArrayCreationExpr arrayCreationExpr = expression.asArrayCreationExpr();
            ArrayCreationExpr clone = arrayCreationExpr.clone();

            Optional<ArrayInitializerExpr> arrayInitializerExpr = arrayCreationExpr.getInitializer();

            if (arrayInitializerExpr.isPresent()) {
                clone.setInitializer((ArrayInitializerExpr) mapExpression(arrayInitializerExpr.get()));
            }

            mappings.getClassMapping(arrayCreationExpr.getElementType().asString())
                    .ifPresent(classMapping -> clone.setElementType(classMapping.getSimpleDeobfuscatedName()));

            return clone;
        } else if (expression.isArrayInitializerExpr()) {
            ArrayInitializerExpr arrayInitializerExpr = expression.asArrayInitializerExpr();
            ArrayInitializerExpr clone = arrayInitializerExpr.clone();

            NodeList<Expression> values = new NodeList<>();
            for (Expression value : arrayInitializerExpr.getValues()) {
                values.add(mapExpression(value));
            }
            clone.setValues(values);

            System.out.println("IniArr: " + clone);
            return clone;
        } else if (expression.isMethodReferenceExpr()) {
            MethodReferenceExpr methodReferenceExpr = expression.asMethodReferenceExpr();
            System.out.println("Reference: " + methodReferenceExpr);
        } else if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();
            System.out.println("Assign: " + assignExpr);
        } else if (expression.isBinaryExpr()) {
            BinaryExpr binaryExpr = expression.asBinaryExpr();
            BinaryExpr clone = binaryExpr.clone();

            clone.setLeft(mapExpression(binaryExpr.getLeft()));
            clone.setRight(mapExpression(binaryExpr.getRight()));

            System.out.println("Binary: " + clone);
            return clone;
        }

        return expression;
    }
}
