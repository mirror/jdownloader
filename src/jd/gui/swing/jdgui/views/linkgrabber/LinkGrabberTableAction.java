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

package jd.gui.swing.jdgui.views.linkgrabber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import jd.config.Property;

public class LinkGrabberTableAction extends AbstractAction {

    private static final long serialVersionUID = 1220090161204767583L;
    public static final int NEW_PACKAGE = 100;
    public static final int MERGE_PACKAGE = 110;
    public static final int EDIT_DIR = 101;
    public static final int DOWNLOAD_PRIO = 200;
    public static final int SET_PW = 201;
    public static final int ADD_SELECTED_PACKAGES = 301;
    public static final int ADD_SELECTED_LINKS = 302;
    public static final int DE_ACTIVATE = 400;
    public static final int SELECT_HOSTER = 500;
    public static final int SPLIT_HOSTER = 501;
    public static final int GUI_ADD = 600;
    public static final int GUI_LOAD = 601;
    public static final int DELETE = 999;
    public static final int DELETE_OFFLINE = 9991;
    public static final int DELETE_DUPS = 9992;
    public static final int CLEAR = 9999;
    public static final int EXT_FILTER = 99999;
    public static final int SAVE_DLC = 20;
    public static final int BROWSE_LINK = 7;
    public static final int COPY_LINK = 8;
    public static final int CHECK_LINK = 120;

    private int actionID;
    private ActionListener actionListener;

    private Property property;
    private String ressourceName;

    public LinkGrabberTableAction(ActionListener actionListener, ImageIcon icon, String ressourceName, int actionID) {
        this(actionListener, icon, ressourceName, actionID, null);
    }

    public LinkGrabberTableAction(ActionListener actionListener, ImageIcon icon, String ressourceName, int actionID, Property property) {

        super();
        this.ressourceName = ressourceName;
        this.actionID = actionID;
        this.actionListener = actionListener;
        this.property = property;

        putValue(AbstractAction.NAME, ressourceName);
        putValue(AbstractAction.SMALL_ICON, icon);
    }

    public void actionPerformed(ActionEvent e) {
        actionListener.actionPerformed(new ActionEvent(e.getSource(), actionID, ressourceName));
    }

    public int getActionID() {
        return actionID;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }
}