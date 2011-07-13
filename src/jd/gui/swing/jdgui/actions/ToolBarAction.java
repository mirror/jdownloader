//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.gui.action.JDAction;
import jd.gui.swing.GuiRunnable;
import jd.utils.locale.JDL;

import org.appwork.utils.Application;
import org.jdownloader.images.NewTheme;

public abstract class ToolBarAction extends JDAction {
    /**
     * Preferred ButtonType
     * 
     * @author Coalado
     */
    public static enum Types {
        TOGGLE, NORMAL, SEPARATOR, CONTAINER
    }

    private static final long  serialVersionUID = -7856598906795360922L;

    public static final String ID               = "ID";

    private boolean            inited           = false;
    private Types              type             = Types.NORMAL;

    public void setType(Types type) {
        if (type == Types.TOGGLE && getValue(SELECTED_KEY) == null) {
            super.setSelected(false);
        }
        this.type = type;
    }

    public void setId(String id) {
        this.putValue(ID, id);
    }

    public ToolBarAction(String name, String menukey, int id) {
        this(name, menukey, null, id);
    }

    public ToolBarAction(String name, String menukey, String iconkey) {
        this(name, menukey, iconkey, -1);
    }

    public ToolBarAction(String name, String menukey, String iconkey, int id) {
        super(name);

        setId(menukey);
        this.setActionID(id);
        if (iconkey != null) setIcon(iconkey);
        if (!JDL.DEBUG) {
            setMnemonic(createMnemonic());
            setAccelerator(createAccelerator());
        }
        setToolTipText(createTooltip());

        initDefaults();
        ActionController.register(this);
    }

    abstract protected String createMnemonic();

    abstract protected String createAccelerator();

    abstract protected String createTooltip();

    protected ToolBarAction() {
        super("");
    }

    public final void actionPerformed(ActionEvent e) {
        if (this.type == Types.TOGGLE) {
            updateIcon();
            if (Application.getJavaVersion() < 16000000) this.setSelected(!this.isSelected());
        }
        if (getActionListener() != null) {
            getActionListener().actionPerformed(new ActionEvent(this, getActionID(), getTitle()));
        } else {
            onAction(e);
        }
    }

    @Override
    public void setSelected(final boolean selected) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                setSelectedInternal(selected);
                return null;
            }
        }.start();
    }

    private void setSelectedInternal(boolean selected) {
        super.setSelected(selected);
        setType(Types.TOGGLE);
        updateIcon();
    }

    private void updateIcon() {
        // putValue(AbstractAction.SMALL_ICON, JDTheme.getCheckBoxImage((String)
        // getValue(IMAGE_KEY), isSelected(), 16, 16));
        // putValue(AbstractAction.LARGE_ICON_KEY,
        // JDTheme.getCheckBoxImage((String) getValue(IMAGE_KEY), isSelected(),
        // 24, 24));
        if (getValue(IMAGE_KEY) == null) {

            putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("checkbox_" + isSelected(), 16));
            putValue(AbstractAction.LARGE_ICON_KEY, NewTheme.I().getIcon("checkbox_" + isSelected(), 24));
        } else {
            putValue(AbstractAction.SMALL_ICON, NewTheme.I().getCheckBoxImage((String) getValue(IMAGE_KEY), isSelected(), 16));
            putValue(AbstractAction.LARGE_ICON_KEY, NewTheme.I().getCheckBoxImage((String) getValue(IMAGE_KEY), isSelected(), 24));
        }
    }

    @Override
    public void setIcon(String key) {
        if (this.getType() == Types.TOGGLE) {
            if (key.length() >= 3) {
                putValue(IMAGE_KEY, key);
                updateIcon();
            }
        } else {
            super.setIcon(key);
        }
    }

    /**
     * May be overridden acts like actionPerformed, but only of no
     * actionlistener is set
     * 
     * @param e
     */
    public void onAction(ActionEvent e) {
    }

    @Override
    public int hashCode() {
        return this.getID() == null ? 0 : this.getID().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ToolBarAction)) return false;
        if (getID() == null) return false;
        return getID().equals(((ToolBarAction) o).getID());
    }

    /**
     * Set default values
     */
    public abstract void initDefaults();

    /**
     * Has to be called by gui converter to start the action.
     */
    public final void init() {
        if (inited) return;
        initAction();
        inited = true;
    }

    protected void initAction() {
    }

    /**
     * Sets the tooltip text, or a long description
     * 
     * @param tt
     */
    public void setToolTipText(String tt) {
        putValue(AbstractAction.SHORT_DESCRIPTION, tt);
    }

    /**
     * Returns the key
     * 
     * @return
     */
    public String getID() {
        if (getValue(ID) == null) return null;
        return this.getValue(ID).toString();
    }

    public Types getType() {
        return type;
    }

    /**
     * if this method returns true, the action cannot be disabled
     */
    public boolean force() {
        return false;
    }

    @Override
    public String toString() {
        return "[id = " + getID() + ", type = " + getType() + ", icon = " + getValue(ToolBarAction.IMAGE_KEY) + "]";
    }

}
