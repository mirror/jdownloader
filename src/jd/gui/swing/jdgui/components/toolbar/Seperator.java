package jd.gui.swing.jdgui.components.toolbar;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.components.toolbar.actions.AbstractToolbarAction;

public class Seperator extends AbstractToolbarAction {
    private static final Seperator INSTANCE = new Seperator();

    /**
     * get the only existing instance of Seperator. This is a singleton
     * 
     * @return
     */
    public static Seperator getInstance() {
        return Seperator.INSTANCE;
    }

    /**
     * Create a new instance of Seperator. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private Seperator() {

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
    protected String createAccelerator() {
        return null;
    }

    @Override
    protected String createTooltip() {
        return null;
    }

}
