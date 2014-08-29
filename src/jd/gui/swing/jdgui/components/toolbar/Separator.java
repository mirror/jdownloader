package jd.gui.swing.jdgui.components.toolbar;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import jd.gui.swing.jdgui.components.toolbar.actions.AbstractToolbarAction;

public class Separator extends AbstractToolbarAction {
    private static final Separator INSTANCE = new Separator();

    /**
     * get the only existing instance of Separator. This is a singleton
     *
     * @return
     */
    public static Separator getInstance() {
        return Separator.INSTANCE;
    }

    /**
     * Create a new instance of Separator. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private Separator() {

    }

    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public String createIconKey() {
        return null;
    }

    @Override
    protected void doInit() {
    }

    @Override
    protected KeyStroke createAccelerator() {
        return null;
    }

    @Override
    protected String createTooltip() {
        return null;
    }

}
