package com.rgbcraft;

import com.rgbcraft.tui.Packager;
import com.rgbcraft.tui.Parser;
import com.rgbcraft.tui.Remapper;

public class Main {
    public static void main(String[] args) {
        // TODO: create a Parser instance
        // TODO: parse the jar file
        Parser parser = new Parser();

        // TODO: create a Remapper instance
//        Remapper remapper = new Remapper(parser);

        // TODO: create a Packager instance
        Packager packager = new Packager();
        // TODO: pass the Remapper instance to the Packager one (possibly in the constructor)
        // TODO: call the Packager instance to do all the work

    }
}