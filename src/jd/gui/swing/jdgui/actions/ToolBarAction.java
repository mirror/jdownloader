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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public abstract class ToolBarAction extends JDAction {
    /**
     * Preferred ButtonType
     * 
     * @author Coalado
     */
    public static enum Types {
        TOGGLE, NORMAL, SEPARATOR, CONTAINER
    }

    protected boolean inited = false;
    private static final long serialVersionUID = -7856598906795360922L;

    public static final String ID = "ID";

    private Types type = Types.NORMAL;

    public void setType(Types type) {
        if (type == Types.TOGGLE && getValue(SELECTED_KEY) == null) {
            super.setSelected(false);
        }
        this.type = type;
    }

    public void setId(String id) {
        this.putValue(ID, id);
    }

    public ToolBarAction(String menukey, int id) {
        this(menukey, null, id);
    }

    public ToolBarAction(String menukey, String iconkey) {
        this(menukey, iconkey, -1);
    }

    public ToolBarAction(String menukey, String iconkey, int id) {
        super(JDL.L("gui.menu." + menukey + ".name", menukey));
        setId(menukey);
        this.setActionID(id);
        if (iconkey != null) setIcon(iconkey);
        if (!JDL.DEBUG) {
            setMnemonic(JDL.L("gui.menu." + menukey + ".mnem", "-"));
            setAccelerator(JDL.L("gui.menu." + menukey + ".accel", "-"));
        }
        setToolTipText(JDL.L("gui.menu." + menukey + ".tooltip", menukey));

        initDefaults();
        ActionController.register(this);
    }

    protected ToolBarAction() {
        super("");
    }

    public final void actionPerformed(ActionEvent e) {
        if (this.type == Types.TOGGLE && JDUtilities.getJavaVersion() < 1.6) {
            this.setSelected(!this.isSelected());
        }
        if (getActionListener() == null) {
            onAction(e);

            return;
        }

        getActionListener().actionPerformed(new ActionEvent(this, getActionID(), getTitle()));
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
    public abstract void init();

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
