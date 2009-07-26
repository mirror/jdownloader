package jd.gui.skins;

import jd.JDInitFlags;
import jd.controlling.JDLogger;
import jd.event.ControlListener;
import jd.gui.skins.jdgui.WindowAdapter;
import jd.gui.skins.jdgui.interfaces.SwitchPanel;

public abstract class SwingGui extends WindowAdapter implements ControlListener  {

    private static final long serialVersionUID = 7164420260634468080L;

    private static SwingGui INSTANCE = null;

    public SwingGui(String string) {
        super(string);        
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
     * Sets the currently used GUI. IS not! thouight to be used to change gui at
     * runtime
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

    /**
     * Throws an RuntimeException if the current thread is not the edt
     */
    public static void checkEDT() {
        if (!JDInitFlags.SWITCH_DEBUG) return;
        Thread th = Thread.currentThread();
        String name = th.toString();
        if (!name.contains("EventQueue")) {
            JDLogger.exception(new RuntimeException("EDT Violation! Runs in " + th));
        }

    }

    /**
     * remove a panel completly.. e.g. unloading an plugin.
     * 
     * @param view
     */
    abstract public void disposeView(SwitchPanel view);

}
