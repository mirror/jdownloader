package org.jdownloader.gui.jdtrayicon;

import java.awt.GraphicsDevice;
import java.util.ArrayList;

import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;

public class ScreenStack extends ArrayList<AbstractNotifyWindow> {

    private GraphicsDevice screen;

    public GraphicsDevice getScreen() {
        return screen;
    }

    public ScreenStack(GraphicsDevice screen) {
        this.screen = screen;
    }

}
