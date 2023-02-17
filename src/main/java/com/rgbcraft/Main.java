package com.rgbcraft;

import com.rgbcraft.tui.Packager;
import com.rgbcraft.tui.Parser;
import com.rgbcraft.tui.Remapper;

public class Main {
    public static void main(String[] args) {
        Parser parser = new Parser();
        Remapper remapper = new Remapper(parser);
        Packager packager = new Packager(remapper);

        packager.run();
    }
}