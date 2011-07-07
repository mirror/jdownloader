package org.jdownloader.actions;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import jd.controlling.JDLogger;
import jd.gui.swing.ShortCuts;

import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;

/**
 * This abstract class is the parent class for all actions in JDownloader
 * 
 * @author thomas
 * 
 */
public abstract class AppAction extends AbstractAction {

    public AppAction() {
        super();

    }

    public void setSmallIcon(ImageIcon icon) {
        putValue(SMALL_ICON, icon);
    }

    public ImageIcon getSmallIcon() {
        return (ImageIcon) getValue(SMALL_ICON);
    }

    public String getName() {
        return (String) getValue(NAME);
    }

    /**
     * Sets the tooltip text, or a long description
     * 
     * @param tt
     */
    public void setToolTipText(String tt) {
        putValue(AbstractAction.SHORT_DESCRIPTION, tt);
    }

    public void setName(String name) {
        putValue(NAME, name);

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
                        Log.L.info(this.getName() + " Shortcuts: skipping wrong modifier " + mod + " in " + accelerator);
                    }
                }
                final Field f = b.getField("VK_" + split[splitLength - 1].toUpperCase());
                final int m = (Integer) f.get(null);
                putValue(AbstractAction.ACCELERATOR_KEY, ks = KeyStroke.getKeyStroke(m, mod));
                Log.L.finest(this.getName() + " Shortcuts: mapped " + accelerator + " to " + ks);
            } catch (Exception e) {
                // JDLogger.exception(e);
                putValue(AbstractAction.ACCELERATOR_KEY, ks = KeyStroke.getKeyStroke(accelerator.charAt(accelerator.length() - 1), Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                Log.L.finest(this.getName() + " Shortcuts: mapped " + accelerator + " to " + ks + " (Exception)");
            }
        }
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

    public String getShortCutString() {
        final Object value = getValue(Action.ACCELERATOR_KEY);
        return (value == null) ? null : ShortCuts.getAcceleratorString((KeyStroke) getValue(Action.ACCELERATOR_KEY));
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
     * Sets the Mnemonic for this icon. Mnemonics are used to activate actions
     * using the keyboard (ALT + Mnemonic) usualy the mnemonic is part of the
     * name, and thus gets underlined in menus.
     * 
     * Always set the Mnemonic AFTER! setting the title
     * 
     * @param key
     */
    public void setMnemonic(String key) {
        if (getName() == null) throw new IllegalStateException("First set Name");
        if (key == null) key = "-";
        final char mnemonic = key.charAt(0);

        if (mnemonic != 0 && !key.contentEquals("-")) {
            final Class<?> b = KeyEvent.class;
            try {
                final Field f = b.getField("VK_" + Character.toUpperCase(mnemonic));
                final int m = (Integer) f.get(null);
                putValue(AbstractAction.MNEMONIC_KEY, m);
                putValue(AbstractAction.DISPLAYED_MNEMONIC_INDEX_KEY, getName().indexOf(m));
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }
}
