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

package jd.gui.swing.jdgui.views.downloads;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import jd.config.Property;

public class TableAction extends AbstractAction {

    public static final int SORT = 103;
    public static final int SORT_ALL = 1031;
    public static final int DE_ACTIVATE = 400;
    public static final int CHECK = 500;
    public static final int DELETE = 999;
    public static final int DELETEFILE = 9991;
    public static final int NEW_PACKAGE = 100;
    public static final int DOWNLOAD_PRIO = 200;
    public static final int SET_PW = 201;
    public static final int DOWNLOAD_RESET = 4;
    public static final int DOWNLOAD_COPY_URL = 301;
    public static final int DOWNLOAD_DLC = 20;
    public static final int DOWNLOAD_DIR = 1;
    public static final int DOWNLOAD_COPY_PASSWORD = 3;
    public static final int DOWNLOAD_RESUME = 22;
    public static final int DOWNLOAD_BROWSE_LINK = 7;
    public static final int EDIT_NAME = 11;
    public static final int EDIT_DIR = 101;
    public static final int FORCE_DOWNLOAD = 401;
    public static final int STOP_MARK = 600;

    private static final long serialVersionUID = 1L;

    private int actionID;
    private ActionListener actionListener;

    private Property property;
    private String ressourceName;

    public TableAction(ActionListener actionListener, String ressourceName, int actionID) {
        this(actionListener, null, ressourceName, actionID, null);
    }

    public TableAction(ActionListener actionListener, ImageIcon imageIcon, String ressourceName, int actionID, Property property) {
        super();
        this.ressourceName = ressourceName;
        this.actionID = actionID;
        this.actionListener = actionListener;
        this.property = property;

        putValue(AbstractAction.SMALL_ICON, imageIcon);
        putValue(AbstractAction.NAME, ressourceName);
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
