//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
