package org.jdownloader.gui.jdtrayicon;

import java.awt.GraphicsDevice;
import java.util.ArrayList;

public class ScreenStack extends ArrayList<Notify> {

    private GraphicsDevice screen;

    public GraphicsDevice getScreen() {
        return screen;
    }

    public ScreenStack(GraphicsDevice screen) {
        this.screen = screen;
    }

}
