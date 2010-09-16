package jd.gui.swing.jdgui.views.downloads;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;

public class RatedMenuItem implements ActionListener {
    // delegate for an Action
    protected class ActionWrapper extends AbstractAction {

        /**
         * 
         */
        private static final long    serialVersionUID = 1L;
        private final AbstractAction _action;

        public ActionWrapper(final AbstractAction action) {
            this._action = action;
        }

        public void actionPerformed(final ActionEvent e) {
            // increase rating
            RatedMenuItem.STORAGE.put(RatedMenuItem.this.id, RatedMenuItem.STORAGE.get(RatedMenuItem.this.id, RatedMenuItem.this.rating) + 1);
            RatedMenuItem.this.rating++;
            this._action.actionPerformed(e);
        }

        public synchronized void addPropertyChangeListener(final PropertyChangeListener listener) {
            this._action.addPropertyChangeListener(listener);
        }

        public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
            return this._action.getPropertyChangeListeners();
        }

        public Object getValue(final String key) {
            return this._action.getValue(key);

        }

        public boolean isEnabled() {
            return this._action.isEnabled();
        }

        public void putValue(final String key, final Object newValue) {
            this._action.putValue(key, newValue);
        }

        public synchronized void removePropertyChangeListener(final PropertyChangeListener listener) {
            this._action.removePropertyChangeListener(listener);
        }

        public void setEnabled(final boolean newValue) {
            this._action.setEnabled(newValue);
        }

    }

    private static final Storage STORAGE = JSonStorage.getPlainStorage("RatedMenuItem");

    public static RatedMenuItem createSeparator() {
        // TODO Auto-generated method stub
        return new RatedMenuItem();
    }

    private ActionWrapper action;

    private JMenu         internalItem;
    private JMenuItem     internalMenuItem;
    private int           rating;

    private boolean       sep;
    private String        id;

    public RatedMenuItem() {
        this.sep = true;
    }

    public RatedMenuItem(final AbstractAction action, final int i) {
        this(action instanceof MenuAction ? ((MenuAction) action).getID() : action.getClass().getSimpleName(), action, i);
    }

    public RatedMenuItem(final String id, final AbstractAction action, final int i) {
        this.action = new ActionWrapper(action);
        this.rating = RatedMenuItem.STORAGE.get(id, i);
        this.id = id;
    }

    public RatedMenuItem(final String id, final JMenu createPrioMenu, final int i) {
        this.internalItem = createPrioMenu;
        for (int x = 0; x < createPrioMenu.getItemCount(); x++) {
            createPrioMenu.getItem(x).addActionListener(this);
        }
        this.rating = RatedMenuItem.STORAGE.get(id, i);
        this.id = id;
    }

    public RatedMenuItem(final String id, final JMenuItem menuItem, final int i) {
        this.internalMenuItem = menuItem;
        this.rating = RatedMenuItem.STORAGE.get(id, i);
        menuItem.addActionListener(this);
        this.id = id;
    }

    public void actionPerformed(final ActionEvent e) {
        RatedMenuItem.STORAGE.put(RatedMenuItem.this.id, RatedMenuItem.STORAGE.get(RatedMenuItem.this.id, RatedMenuItem.this.rating) + 1);
        this.rating++;
    }

    /**
     * Adds the item to a popup menu
     * 
     * @param popup
     */
    public void addToPopup(final JMenu popup) {        
        if (this.action != null) {
            if (this.action._action instanceof MenuAction) {

                popup.add(this.getJMenuItem(((MenuAction) this.action._action)));
            } else {
                popup.add(this.action);
            }
        } else if (this.internalItem != null) {
            popup.add(this.internalItem);
        } else if (this.internalMenuItem != null) {
            popup.add(this.internalMenuItem);
        } else {
            popup.add(new JSeparator());
        }
    }

    public void addToPopup(final JPopupMenu popup) {       
        if (this.action != null) {
            if (this.action._action instanceof MenuAction) {
                popup.add(this.getJMenuItem(((MenuAction) this.action._action)));
            } else {
                popup.add(this.action);
            }
        } else if (this.internalItem != null) {
            popup.add(this.internalItem);
        } else if (this.internalMenuItem != null) {
            popup.add(this.internalMenuItem);
        } else {
            popup.add(new JSeparator());
        }
    }

    public JMenuItem getJMenuItem(final MenuAction mi) {
        switch (mi.getType()) {
        case SEPARATOR:
            return null;
        case NORMAL:
            final JMenuItem ret = new JMenuItem(mi);
            ret.addActionListener(this.action);
            return ret;
        case TOGGLE:
            if (JDUtilities.getJavaVersion() >= 1.6) {
                // Togglebuttons for 1.6
                final JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(mi);
                m2.addActionListener(this.action);
                return m2;
            } else {
                // 1.5 togle buttons need a changelistener in the menuitem
                final JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(mi);
                m2.addActionListener(mi);
                mi.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent evt) {
                        m2.setSelected(((ToolBarAction) evt.getSource()).isSelected());
                    }
                });
                return m2;
            }
        case CONTAINER:
            final JMenu m3 = new JMenu(mi.getTitle());
            m3.setIcon(mi.getIcon());
            JMenuItem c;
            if (mi.getSize() > 0) {
                for (int i = 0; i < mi.getSize(); i++) {
                    c = this.getJMenuItem(mi.get(i));
                    if (c == null) {
                        m3.addSeparator();
                    } else {
                        m3.add(c);
                    }
                }
            }
            return m3;
        }
        return null;
    }

    public int getRating() {
        // TODO Auto-generated method stub
        return this.rating;
    }

    public boolean isSeparator() {
        // TODO Auto-generated method stub
        return this.sep;
    }

    public String toString() {
        return this.sep + "," + this.action + "," + this.internalItem + "," + this.internalMenuItem;
    }
}
