package com.rgbcraft.tui;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.MappingFormats;
import org.cadixdev.lorenz.io.MappingsReader;
import org.cadixdev.lorenz.model.ClassMapping;

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

    public boolean mapClass() {
        ParserData parserData = this.data.poll();
        if (parserData == null) return false;

        this.mappings.getClassMapping(parserData.getClassName()).ifPresent((classMapping) -> {
            parserData.setClassName(classMapping.getSimpleDeobfuscatedName());

            parserData.setFields(mapFields(parserData.getFields(), classMapping));
        });
        return true;
    }

    private List<FieldDeclaration> mapFields(List<FieldDeclaration> fields, ClassMapping<?, ?> classMapping) {
        return fields.stream().peek((field) -> {
            VariableDeclarator variable = field.getVariables().getFirst().get();
            Optional<Expression> initializerExpression = variable.getInitializer();

            if (initializerExpression.isPresent()) {
                this.mapExpression(initializerExpression.get());
            }

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
            if (classMapping.isPresent()) {
                System.out.println("HERE CHANGED");
                castExpr.setType(classMapping.get().getSimpleDeobfuscatedName());
            }
        } else if (expression.isMethodCallExpr()) {
            // TODO: this is so messy + map it

            MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
            if (methodCallExpr.getScope().isPresent()) {
                mapExpression(methodCallExpr.getScope().get());
            }
            System.out.println("Scope: " + methodCallExpr.getScope() + "; Name: " + methodCallExpr.getName() + "!!!");
        }
    }
}
