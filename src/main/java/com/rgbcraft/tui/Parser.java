package com.rgbcraft.tui;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/* This class has the job to read the jar file and parse its content
 */
public class Parser {
    public Parser() {
        try {
            URL urlSrg = getClass().getResource("/client.srg");
            if (urlSrg == null) throw new NullPointerException("Can't find client.srg");

            URL urlJar = getClass().getResource("/client.jar");
            if (urlJar == null) throw new NullPointerException("Can't find client.jar");

            JarURLConnection url = (JarURLConnection) new URL("jar:file://" + urlJar.getPath() + "!/").openConnection();
            JarFile file = url.getJarFile();
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.toString().endsWith(".java")) {
                    System.out.println(entry);

                    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
                    combinedTypeSolver.add(new ReflectionTypeSolver());
                    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
                    StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
                    JarEntry fileEntry = file.getJarEntry(entry.getName());
                    InputStream input = file.getInputStream(fileEntry);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    String line;
                    StringBuilder fileContent = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line);
                    }
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(fileContent.toString());
                        System.out.println(cu.getImports());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.println("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
