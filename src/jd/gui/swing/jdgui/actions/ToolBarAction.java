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

import javax.swing.AbstractAction;

import jd.config.SubConfiguration;
import jd.gui.action.JDAction;
import jd.utils.locale.JDL;

public abstract class ToolBarAction extends JDAction {
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

    protected Types type = Types.NORMAL;

    public void setVisible(boolean b) {
        this.putValue(VISIBLE, b);
        SubConfiguration.getConfig("Toolbar").setProperty("VISIBLE_"+this.getID(), b);
        SubConfiguration.getConfig("Toolbar").save();
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
        super(JDL.L("gui.menu." + menukey + ".name", menukey));
        setId(menukey);
        if (iconkey != null) setIcon(iconkey);
        setMnemonic(JDL.L("gui.menu." + menukey + ".mnem", "-"));
        setAccelerator(JDL.L("gui.menu." + menukey + ".accel", "-"));
        this.putValue(VISIBLE, SubConfiguration.getConfig("Toolbar").getBooleanProperty("VISIBLE_"+this.getID(), true));
      
        initDefaults();
        ActionController.register(this);
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof ToolBarAction)) return false;
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
        return this.getValue(ID).toString();
    }

    public Types getType() {
        return type;
    }

}
