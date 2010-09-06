package jd.gui.swing.jdgui.views.downloads;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

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
        this(action.getClass().getSimpleName(), action, i);
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
            popup.add(this.action);
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
            popup.add(this.action);
        } else if (this.internalItem != null) {
            popup.add(this.internalItem);
        } else if (this.internalMenuItem != null) {
            popup.add(this.internalMenuItem);
        } else {
            popup.add(new JSeparator());
        }
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
