package com.rgbcraft.tui;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.TypeSolverBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/* This class has the job to read the jar file and parse its content
 */
public class Parser {
    private final ConcurrentLinkedQueue<ParserData> data;

    public Parser() {
        try {
            URL urlJar = getClass().getResource("/client.jar");
            if (urlJar == null) throw new NullPointerException("Can't find client.jar");

            JarURLConnection url = (JarURLConnection) new URL("jar:file://" + urlJar.getPath() + "!/").openConnection();
            JarFile file = url.getJarFile();
            Enumeration<JarEntry> entries = file.entries();

            CombinedTypeSolver solver = new CombinedTypeSolver();
            solver.add(new ReflectionTypeSolver());
            TypeSolver typeSolver = new TypeSolverBuilder()
                    .withSourceCode(getClass().getResource("/client/").getPath())
//                    .withJAR(urlJar.getPath())
                    .withCurrentJRE()
//                    .withCurrentClassloader()
                    .build();
            solver.add(typeSolver);
            ParserConfiguration parserConfiguration = new ParserConfiguration();
            parserConfiguration.setSymbolResolver(new JavaSymbolSolver(solver));
            parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_7);
            parserConfiguration.setStoreTokens(true);
            parserConfiguration.setLexicalPreservationEnabled(true);
            JavaParser javaParser = new JavaParser(parserConfiguration);

            this.data = new ConcurrentLinkedQueue<>();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.toString().endsWith(".java")) {
                    JarEntry fileEntry = file.getJarEntry(entry.getName());
                    InputStream input = file.getInputStream(fileEntry);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    String line;
                    StringBuilder fileContent = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line);
                    }

                    ParseResult<CompilationUnit> parsed = javaParser.parse(fileContent.toString());
                    if (parsed.isSuccessful() && parsed.getResult().isPresent()) {
                        CompilationUnit cu = parsed.getResult().get();
                        cu.getTypes().parallelStream().forEach(this::parseClass);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseClass(TypeDeclaration<?> type) {
        String className = type.getName().asString();
        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();
        List<ConstructorDeclaration> constructors = new ArrayList<>();
        List<InitializerDeclaration> initializers = new ArrayList<>();

        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member.isFieldDeclaration()) {
                FieldDeclaration field = member.asFieldDeclaration();
                fields.add(field);
            } else if (member.isMethodDeclaration()) {
                MethodDeclaration method = member.asMethodDeclaration();
                methods.add(method);
            } else if (member.isClassOrInterfaceDeclaration()) {
                ClassOrInterfaceDeclaration classOrInterfaceDeclaration = member.asClassOrInterfaceDeclaration();
                parseClass(classOrInterfaceDeclaration.asTypeDeclaration());
            } else if (member.isConstructorDeclaration()) {
                ConstructorDeclaration constructor = member.asConstructorDeclaration();
                constructors.add(constructor);
            } else if (member.isTypeDeclaration()) {
                parseClass(member.asTypeDeclaration());
            } else if (member.isInitializerDeclaration()) {
                InitializerDeclaration initializer = member.asInitializerDeclaration();
                initializers.add(initializer);
            }
        }

        this.data.add(new ParserData(className, "", fields, methods, constructors, initializers));
    }

    public ConcurrentLinkedQueue<ParserData> getData() {
        return this.data;
    }
}
