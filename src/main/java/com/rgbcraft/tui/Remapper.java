package com.rgbcraft.tui;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.MappingFormats;
import org.cadixdev.lorenz.io.MappingsReader;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Remapper {
    public Remapper() {
        try {
            MappingsReader reader = MappingFormats.SRG.createReader(getClass().getResource("/client.srg").openStream());
            MappingSet mappings = reader.read();
            JarURLConnection url = (JarURLConnection) new URL("jar:file://" + Objects.requireNonNull(getClass().getResource("/client.jar")).getPath() + "!/").openConnection();
            JarFile file = url.getJarFile();
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                System.out.println(entry);
            }

            System.out.println(mappings.getClassMapping("alv"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
