package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import jd.controlling.JDLogger;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

public abstract class StartAction extends AbstractAction {

    private static final long serialVersionUID = -7331375722486190184L;

    public StartAction(String menukey, String iconkey) {
        super();

        setIcon(iconkey);
        setName(JDLocale.L("gui.menu." + menukey + ".name", menukey));
        setMnemonic(JDLocale.L("gui.menu." + menukey + ".mnem", "-"), JDLocale.L("gui.menu." + menukey + ".name", menukey));
        setAccelerator(JDLocale.L("gui.menu." + menukey + ".accel", "-"));
    }

    protected void setIcon(String key) {
        putValue(AbstractAction.SMALL_ICON, JDTheme.II(key, 24, 24));

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

}
