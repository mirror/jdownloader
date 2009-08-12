package jd.gui.swing.jdgui.actions;

import javax.swing.AbstractAction;

import jd.gui.action.JDAction;
import jd.utils.locale.JDL;

public abstract class ToolBarAction extends JDAction {
    /**
     * Preferred ButtonType
     * 
     * @author Coalado
     * 
     */
    public static enum Types {
        TOGGLE, NORMAL, SEPARATOR
    }

    /**
     * 
     */
    protected boolean inited = false;
    private static final long serialVersionUID = -7856598906795360922L;
    public static final String SELECTED = "SELECTED";
    public static final String PRIORITY = "PRIORITY";
    public static final String ID = "ID";
    public static final String VISIBLE = "VISIBLE";

    protected Types type = Types.NORMAL;

    public void setVisible(boolean b) {
        this.putValue(VISIBLE, b);
    }

    public boolean isVisible() {
        try {
            return (Boolean) this.getValue(VISIBLE);

        } catch (Exception e) {
            return true;
        }
    }

    public void setId(String id) {
        this.putValue(ID, id);
    }

    public void setPriority(int priority) {
        this.putValue(PRIORITY, priority);
    }

    public ToolBarAction(String menukey, String iconkey) {
        super(JDL.L("gui.menu." + menukey + ".name", menukey));
        setId(menukey);
        if (iconkey != null) setIcon(iconkey);
        setMnemonic(JDL.L("gui.menu." + menukey + ".mnem", "-"));
        setAccelerator(JDL.L("gui.menu." + menukey + ".accel", "-"));
        initDefaults();
        ActionController.register(this);
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof ToolBarAction)) return false;
        return getID().equals(((ToolBarAction) o).getID());
    }

    /**
     * Set default values
     */
    public abstract void initDefaults();

    /**
     * Has to be called by gui converter to start the action.
     */
    public abstract void init();

    /**
     * Sets the tooltip text, or a long description
     * 
     * @param tt
     */
    public void setToolTipText(String tt) {
        putValue(AbstractAction.LONG_DESCRIPTION, tt);
    }

    /**
     * Returns the key
     * 
     * @return
     */
    public String getID() {
        return this.getValue(ID).toString();
    }

    public Types getType() {
        return type;
    }

}
