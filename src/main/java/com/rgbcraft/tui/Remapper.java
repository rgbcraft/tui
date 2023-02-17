package com.rgbcraft.tui;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.MappingFormats;
import org.cadixdev.lorenz.io.MappingsReader;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.MethodMapping;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/* This class has the job to remap what's given into it
 */
public class Remapper {
    private final ConcurrentLinkedQueue<ParserData> data;
    private final MappingSet mappings;

    public Remapper(Parser parser) {
        try {
            URL urlSrg = getClass().getResource("/client.srg");
            if (urlSrg == null) throw new NullPointerException("Can't find client.srg");

            MappingsReader reader = MappingFormats.SRG.createReader(urlSrg.openStream());

            this.data = parser.getData();
            this.mappings = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasMoreClasses() {
        return this.data.size() > 0;
    }

    public void mapClass() {
        ParserData parserData = this.data.poll();
        if (parserData == null) return;

        this.mappings.getClassMapping(parserData.getClassName()).ifPresent((classMapping) -> {
            parserData.setClassName(classMapping.getDeobfuscatedName());
            parserData.setPackageName(classMapping.getDeobfuscatedPackage());

            parserData.setFields(mapFields(parserData.getFields(), classMapping));
        });
    }

    private List<FieldDeclaration> mapFields(List<FieldDeclaration> fields, ClassMapping<?, ?> classMapping) {
        return fields.stream().peek((field) -> {
            VariableDeclarator variable = field.getVariables().getFirst().get();
            Optional<Expression> initializerExpression = variable.getInitializer();

            initializerExpression.ifPresent(this::mapExpression);

            System.out.println(variable.getInitializer());
            String name = variable.getNameAsString();
            String type = variable.getTypeAsString();
            boolean isArray = type.endsWith("[]");

            if (isArray) type = type.replace("[]", "");

            String finalType = type;
            mappings.getClassMapping(type).flatMap(fieldClassMapping -> classMapping.getFieldMapping(name)).ifPresent((fieldMapping -> {
                Optional<? extends ClassMapping<?, ?>> innerClassMapping = mappings.getClassMapping(finalType);

                if (innerClassMapping.isPresent()) {
                    if (isArray) variable.setType(innerClassMapping.get().getSimpleDeobfuscatedName() + "[]");
                    else variable.setType(innerClassMapping.get().getSimpleDeobfuscatedName());
                }

                variable.setName(fieldMapping.getSimpleDeobfuscatedName());
            }));
        }).collect(Collectors.toList());
    }

    private void mapExpression(Expression expression) {
        if (expression.isCastExpr()) {
            CastExpr castExpr = expression.asCastExpr();
            Optional<? extends ClassMapping<?, ?>> classMapping = mappings.getClassMapping(castExpr.getTypeAsString());
            classMapping.ifPresent(mapping -> castExpr.setType(mapping.getSimpleDeobfuscatedName()));
        } else if (expression.isMethodCallExpr()) {
            // TODO: this is so messy + map it
            MethodCallExpr methodCallExpr = expression.asMethodCallExpr();

            try {
                if (methodCallExpr.getScope().isPresent()) {
                    Optional<? extends ClassMapping<?, ?>> classMapping = mappings.getClassMapping(methodCallExpr.getScope().get().calculateResolvedType().describe());
                    if (classMapping.isPresent()) {
                        Optional<MethodMapping> methodMapping = classMapping.get().getMethodMapping(methodCallExpr.getName().asString(), methodCallExpr.resolve().toDescriptor());
                        if (methodMapping.isPresent()) {
                            methodCallExpr.setName(methodMapping.get().getSimpleDeobfuscatedName());
                        }
                    }
                    mapExpression(methodCallExpr.getScope().get());
                }
            } catch (IllegalStateException | UnsolvedSymbolException ignored) {
            }

            for (Expression arg : methodCallExpr.getArguments()) {
                mapExpression(arg);
            }

            System.out.println("Scope: " + methodCallExpr.getScope() + "; Name: " + methodCallExpr.getName() + "!!!");
        } else if (expression.isObjectCreationExpr()) {
            ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
            if (objectCreationExpr.getScope().isPresent()) {
                mapExpression(objectCreationExpr.getScope().get());
            }

            for (Expression arg : objectCreationExpr.getArguments()) {
                mapExpression(arg);
            }

            String name = objectCreationExpr.getType().getNameAsString();

            Optional<? extends ClassMapping<?, ?>> classMapping = mappings.getClassMapping(name);
            classMapping.ifPresent(mapping -> objectCreationExpr.setType(mapping.getSimpleDeobfuscatedName()));

            System.out.println("Object: " + objectCreationExpr + "???");
        } else if (expression.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = expression.asFieldAccessExpr();

            try {
                Optional<? extends ClassMapping<?, ?>> classMapping = mappings.getClassMapping(fieldAccessExpr.getScope().calculateResolvedType().describe());
                classMapping.ifPresent(mapping -> {
                    Optional<FieldMapping> fieldMapping = mapping.getFieldMapping(fieldAccessExpr.getName().getIdentifier());
                    fieldMapping.ifPresent(value -> fieldAccessExpr.setName(value.getDeobfuscatedName()));
                });
            } catch (UnsolvedSymbolException e) {
                e.printStackTrace();
            }

            mapExpression(fieldAccessExpr.getScope());
        } else if (expression.isClassExpr()) {
            ClassExpr classExpr = expression.asClassExpr();

            mappings.getClassMapping(classExpr.getTypeAsString())
                    .ifPresent((classMapping -> classExpr.setType(classMapping.getSimpleDeobfuscatedName())));
        } else if (expression.isNameExpr()) {
            NameExpr nameExpr = expression.asNameExpr();

            mappings.getClassMapping(nameExpr.getNameAsString())
                    .ifPresent((classMapping -> nameExpr.setName(classMapping.getSimpleDeobfuscatedName())));
        } else if (expression.isEnclosedExpr()) {
            EnclosedExpr enclosedExpr = expression.asEnclosedExpr();
            mapExpression(enclosedExpr.getInner());
        } else if (expression.isArrayCreationExpr()) {
            ArrayCreationExpr arrayCreationExpr = expression.asArrayCreationExpr();
            arrayCreationExpr.getInitializer().ifPresent(this::mapExpression);
            mappings.getClassMapping(arrayCreationExpr.getElementType().asString())
                    .ifPresent(classMapping -> arrayCreationExpr.setElementType(classMapping.getSimpleDeobfuscatedName()));
        } else if (expression.isArrayInitializerExpr()) {
            ArrayInitializerExpr arrayInitializerExpr = expression.asArrayInitializerExpr();
            for (Expression value : arrayInitializerExpr.getValues()) {
                mapExpression(value);
            }
            System.out.println("IniArr: " + arrayInitializerExpr);
        } else if (expression.isMethodReferenceExpr()) {
            MethodReferenceExpr methodReferenceExpr = expression.asMethodReferenceExpr();
            System.out.println("Reference: " + methodReferenceExpr);
        } else if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();
            System.out.println("Assign: " + assignExpr);
        } else if (expression.isBinaryExpr()) {
            BinaryExpr binaryExpr = expression.asBinaryExpr();
            mapExpression(binaryExpr.getLeft());
            mapExpression(binaryExpr.getRight());
            System.out.println("Binary: " + binaryExpr);
        }
    }

    private String resolveExpression(Expression expression) {
        if (expression.isClassExpr()) {
            ClassExpr classExpr = expression.asClassExpr();
            return classExpr.getType().asString();
        } else if (expression.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = expression.asFieldAccessExpr();
            Optional<? extends ClassMapping<?, ?>> classMapping = this.mappings.getClassMapping(resolveExpression(fieldAccessExpr.getScope()));

            if (classMapping.isPresent()) {
                List<?> fields = classMapping.get().getFieldMappings().stream().filter((fieldMapping -> Objects.equals(fieldMapping.getObfuscatedName(), fieldAccessExpr.getName().getIdentifier()))).collect(Collectors.toList());
                if (fields.size() > 0) {
                    return classMapping.get().getSimpleDeobfuscatedName();
                }

            }
        }

        return "";
    }
}
