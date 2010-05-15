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

package jd.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import jd.config.Property;
import jd.controlling.JDLogger;
import jd.gui.swing.ShortCuts;
import jd.parser.Regex;
import jd.utils.JDTheme;

/**
 * This abstract class is the parent class for all actions in JDownloader
 * 
 * @author Coalado
 */
public abstract class JDAction extends AbstractAction {

    private static final long serialVersionUID = -2332356042161170120L;
    public static final String IMAGE_KEY = "IMAGE_KEY";
    private ActionListener actionListener;
    private int actionID = -1;

    public void setActionID(final int actionID) {
        this.actionID = actionID;
    }

    private Property properties;

    /**
     * 
     * @param title
     *            name of the action
     * @param actionID
     *            optional action id
     */
    public JDAction(final String title, final int actionID) {
        super(title);
        this.actionID = actionID;
    }

    /**
     * @param l
     *            name of the action
     * @param ii
     *            icon of the action
     */
    public JDAction(final String l, final ImageIcon ii) {
        super(l, ii);
    }

    /**
     * @param l
     *            Name of the Action
     */
    public JDAction(final String l) {
        this(l, -1);
    }

    /**
     * @param key
     *            A JDTHeme Icon Key
     */
    public void setIcon(final String key) {
        if (key.length() >= 3) {
            putValue(AbstractAction.SMALL_ICON, JDTheme.II(key, 16, 16));
            putValue(AbstractAction.LARGE_ICON_KEY, JDTheme.II(key, 24, 24));
            putValue(IMAGE_KEY, key);
        }
    }

    /**
     * Sets the Mnemonic for this icon. Mnemonics are used to activate actions
     * using the keyboard (ALT + Mnemonic) usualy the mnemonic is part of the
     * name, and thus gets underlined in menus.
     * 
     * Always set the Mnemonic AFTER! setting the title
     * 
     * @param key
     */
    public void setMnemonic(final String key) {
        final char mnemonic = key.charAt(0);

        if (mnemonic != 0 && !key.contentEquals("-")) {
            final Class<?> b = KeyEvent.class;
            try {
                final Field f = b.getField("VK_" + Character.toUpperCase(mnemonic));
                final int m = (Integer) f.get(null);
                putValue(AbstractAction.MNEMONIC_KEY, m);
                putValue(AbstractAction.DISPLAYED_MNEMONIC_INDEX_KEY, getTitle().indexOf(m));
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

    /**
     * Returns the actions description
     */
    public String getTooltipText() {
        try {
            return getValue(AbstractAction.SHORT_DESCRIPTION).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the KeyStroke set by setAccelerator;
     * 
     * @return
     */
    public KeyStroke getKeyStroke() {
        final Object ret = getValue(ACCELERATOR_KEY);
        return (ret != null) ? (KeyStroke) ret : null;
    }

    /**
     * Sets the shortcut fort this action. a System dependend behaviour is
     * choosen. e,g. WIndows+ Strg+ Acceleratir
     * 
     * example: action.setAccelerator("ENTER"); defines a Enter shortcut
     * 
     * @param accelerator
     */
    public void setAccelerator(final String accelerator) {
        KeyStroke ks;
        if (accelerator != null && accelerator.length() > 0 && !accelerator.equals("-")) {
            final Class<?> b = KeyEvent.class;
            final String[] split = accelerator.split("\\+");
            int mod = 0;
            try {
                final int splitLength = split.length;
                for (int i = 0; i < splitLength - 1; ++i) {
                    if (new Regex(split[i], "^CTRL$").matches()) {
                        mod = mod | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                    } else if (new Regex(split[i], "^SHIFT$").matches()) {
                        mod = mod | KeyEvent.SHIFT_DOWN_MASK;
                    } else if (new Regex(split[i], "^ALTGR$").matches()) {
                        mod = mod | KeyEvent.ALT_GRAPH_DOWN_MASK;
                    } else if (new Regex(split[i], "^ALT$").matches()) {
                        mod = mod | KeyEvent.ALT_DOWN_MASK;
                    } else if (new Regex(split[i], "^META$").matches()) {
                        mod = mod | KeyEvent.META_DOWN_MASK;
                    } else {
                        JDLogger.getLogger().info(this.getTitle() + " Shortcuts: skipping wrong modifier " + mod + " in " + accelerator);
                    }
                }
                final Field f = b.getField("VK_" + split[splitLength - 1].toUpperCase());
                final int m = (Integer) f.get(null);
                putValue(AbstractAction.ACCELERATOR_KEY, ks = KeyStroke.getKeyStroke(m, mod));
                JDLogger.getLogger().finest(this.getTitle() + " Shortcuts: mapped " + accelerator + " to " + ks);
            } catch (Exception e) {
                JDLogger.exception(e);
                putValue(AbstractAction.ACCELERATOR_KEY, ks = KeyStroke.getKeyStroke(accelerator.charAt(accelerator.length() - 1), Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                JDLogger.getLogger().finest(this.getTitle() + " Shortcuts: mapped " + accelerator + " to " + ks + " (Exception)");
            }
        }
    }

    /**
     * Returns the action's id
     * 
     * @return
     */
    public int getActionID() {
        return actionID;
    }

    /**
     * a action may have a actionlistener defined. alternativly the
     * actionPerformed method may be overridden
     * 
     * @return
     */
    public ActionListener getActionListener() {
        return actionListener;
    }

    /**
     * Returns the action's name
     * 
     * @return
     */
    public String getTitle() {
        return (String) getValue(NAME);
    }

    /**
     * For toggle actions, this method returns if it is currently selected
     * 
     * @return
     */
    public boolean isSelected() {
        final Object value = getValue(SELECTED_KEY);
        return (value == null) ? false : (Boolean) value;
    }

    /**
     * Sets the actionlistener. see getActionListener() for details
     * 
     * @param actionListener
     * @return
     */
    public JDAction setActionListener(final ActionListener actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    /**
     * Sets the action selected. WARNING. Swing usualy handles the selection
     * state
     * 
     * @param selected
     */
    public void setSelected(final boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

    /**
     * Sets the actions title
     * 
     * @param title
     * @return
     */
    public JDAction setTitle(final String title) {
        putValue(NAME, title);
        return this;
    }

    /**
     * Sets the ac tions icon
     * 
     * @param ii
     * @return
     */
    public JDAction setIcon(final ImageIcon ii) {
        putValue(SMALL_ICON, ii);
        return this;

    }

    /**
     * Returns the Actions icon
     * 
     * @return
     */
    public ImageIcon getIcon() {
        return (ImageIcon) getValue(SMALL_ICON);
    }

    public String getShortCutString() {
        final Object value = getValue(Action.ACCELERATOR_KEY);
        return (value == null) ? null : ShortCuts.getAcceleratorString((KeyStroke) getValue(Action.ACCELERATOR_KEY));
    }

    /**
     * A action uses an intern {@link Property} for (re-)storing objects
     * 
     * @param string
     * @param value
     * @see Property
     */
    public void setProperty(final String string, final Object value) {
        if (properties == null) {
            properties = new Property();
        }
        this.firePropertyChange(string, getProperty(string), value);
        properties.setProperty(string, value);

    }

    /**
     * A action uses an intern {@link Property} for (re-)storing objects
     * 
     * @param string
     * @see Property
     */
    public Object getProperty(final String string) {
        if (properties == null) {
            properties = new Property();
        }
        return properties.getProperty(string);
    }

}
