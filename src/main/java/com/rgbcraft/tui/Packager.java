package com.rgbcraft.tui;

/* This class has the job to read all the parsed data,
 * pass them to the Remapper and, finally, to create a new jar file
 */
public class Packager {
    private final Remapper remapper;

    public Packager(Remapper remapper) {
        this.remapper = remapper;
    }

    public void run() {
        while (remapper.hasMoreClasses()) {
            remapper.mapClass();
        }
    }
}
