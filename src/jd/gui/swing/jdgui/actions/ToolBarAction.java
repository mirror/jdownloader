package jd.gui.swing.jdgui.actions;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import jd.controlling.JDLogger;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public abstract class ToolBarAction extends AbstractAction {
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
    public static final String IMAGE_KEY = "IMAGE_KEY";

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
        super();
        setId(menukey);
        if (iconkey != null) setIcon(iconkey);
        setName(JDL.L("gui.menu." + menukey + ".name", menukey));
        setMnemonic(JDL.L("gui.menu." + menukey + ".mnem", "-"), JDL.L("gui.menu." + menukey + ".name", menukey));
        setAccelerator(JDL.L("gui.menu." + menukey + ".accel", "-"));
        initDefaults();
        ActionController.register(this);
    }

    public boolean equals(Object o) {
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

    public void setIcon(String key) {
        putValue(IMAGE_KEY, key);
        putValue(AbstractAction.SMALL_ICON, JDTheme.II(key, 24, 24));
    }

    /**
     * Sets the tooltip text, or a long description
     * 
     * @param tt
     */
    public void setToolTipText(String tt) {
        putValue(AbstractAction.LONG_DESCRIPTION, tt);
    }

    private void setAccelerator(String acceleratorString) {
        if (acceleratorString != null && acceleratorString.length() > 0) {
            putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(acceleratorString));
        }
    }

    private void setMnemonic(String key, String keyname) {
        char mnemonic = key.charAt(0);
        if (mnemonic != 0 && !key.contentEquals("-")) {
            Class<?> b = KeyEvent.class;
            Field f;
            try {
                f = b.getField("VK_" + Character.toUpperCase(mnemonic));
                int m = (Integer) f.get(null);
                putValue(AbstractAction.MNEMONIC_KEY, m);

                putValue(AbstractAction.DISPLAYED_MNEMONIC_INDEX_KEY, keyname.indexOf(m));
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

    private void setName(String string) {
        putValue(AbstractAction.NAME, string);
    }

    public void setSelected(boolean b) {
        putValue(SELECTED, b);
    }

    /**
     * Returns the key
     * 
     * @return
     */
    public String getID() {
        return this.getValue(ID).toString();
    }

    /**
     * Returns if the action is in "selected" State
     * 
     * @return
     */
    public boolean isSelected() {
        try {
            return (Boolean) this.getValue(SELECTED);
        } catch (Exception e) {
            return false;
        }
    }

    public Types getType() {
        return type;
    }

    public String getTooltipText() {
        try {
            return getValue(AbstractAction.LONG_DESCRIPTION).toString();
        } catch (Exception e) {
            return null;
        }
    }

}
