package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import jd.utils.JDLocale;
import jd.utils.JDTheme;

public abstract class StartAction extends AbstractAction {

    private static final long serialVersionUID = -7331375722486190184L;
    private Dimension dimension;

    public StartAction() {
        super();
        dimension = new Dimension(16, 16);
        this.init();
    }

    protected void setIcon(String key) {
        putValue(Action.SMALL_ICON, JDTheme.II(key, (int) dimension.getWidth(), (int) dimension.getHeight()));

    }

    protected void setAccelerator(String key) {
        String acceleratorString = JDLocale.L(key, "-");
        if (acceleratorString != null && acceleratorString.length() > 0) {
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(acceleratorString));
        }

    }

    protected void setMnemonic(String key, String kename) {
        char mnemonic = JDLocale.L(key, "-").charAt(0);

        if (mnemonic != 0 && JDLocale.L(key, "-").contentEquals("-") == false) {
            Class<?> b = KeyEvent.class;
            Field f;
            try {
                f = b.getField("VK_" + Character.toUpperCase(mnemonic));
                int m = (Integer) f.get(null);
                putValue(Action.MNEMONIC_KEY, m);

                putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, JDLocale.L(kename, kename).indexOf(m));
            } catch (Exception e) {
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
            }
        }

    }

    protected void setName(String string) {
        putValue(Action.NAME, JDLocale.L(string, string));

    }

    protected void setIconDim(Dimension dimension) {
        this.dimension = dimension;

    }

    protected void setShortDescription(String string) {
        putValue(Action.SHORT_DESCRIPTION, JDLocale.L(string, string));

    }

    abstract public void init();
}
