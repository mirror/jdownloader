package jd.gui.skins;

import jd.gui.UIInterface;
import jd.gui.skins.jdgui.WindowAdapter;
import jd.gui.skins.jdgui.interfaces.SwitchPanel;

public abstract class SwingGui extends WindowAdapter implements UIInterface {

    private static SwingGui INSTANCE = null;

    public SwingGui(String string) {
        super(string);
        // TODO Auto-generated constructor stub
    }

    /**
     * Has to return the current used gui
     * 
     * @return
     */
    public static SwingGui getInstance() {

        return INSTANCE;
    }

    /**
     * Sets the currently used GUI. IS not! thouight to be used to change gui at runtime
     * 
     * @param ins
     */
    public static void setInstance(SwingGui ins) {

        INSTANCE = ins;
    }

    // TODO
    abstract public void setWaiting(boolean b);

    abstract public void closeWindow();

    abstract public void setContent(SwitchPanel tabbedPanel);

}
