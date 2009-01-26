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

package jd.gui.skins.simple.components.treetable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import jd.config.Property;

public class TreeTableAction extends AbstractAction {

    public static final int DOWNLOAD_ABORT = 18;
    public static final int DOWNLOAD_BROWSE_LINK = 7;
    // public static final int DOWNLOAD_CHECK_AVAILABILITY = 24;
    public static final int DOWNLOAD_COPY_PASSWORD = 3;;
    public static final int DOWNLOAD_DISABLE = 6;
    public static final int DOWNLOAD_DLC = 20;
    public static final int DOWNLOAD_DOWNLOAD_DIR = 1;
    public static final int DOWNLOAD_ENABLE = 5;
    public static final int DOWNLOAD_INFO = 0;
    public static final int DOWNLOAD_NEW_PACKAGE = 8;
    public static final int DOWNLOAD_RESET = 4;
    public static final int DOWNLOAD_RESUME = 22;
    public static final int DOWNLOAD_PRIO = 900;
    public static final int PACKAGE_ABORT = 19;
    // public static final int PACKAGE_CHECK_AVAILABILITY = 25;
    public static final int PACKAGE_COPY_PASSWORD = 23;
    public static final int PACKAGE_DISABLE = 15;
    public static final int PACKAGE_DLC = 21;
    public static final int PACKAGE_DOWNLOAD_DIR = 12;
    public static final int PACKAGE_EDIT_DIR = 10;
    public static final int PACKAGE_EDIT_NAME = 11;
    public static final int PACKAGE_ENABLE = 14;
    public static final int PACKAGE_INFO = 9;
    public static final int PACKAGE_RESET = 16;
    public static final int PACKAGE_SORT = 17;
    public static final int PACKAGE_PRIO = 901;

    public static final int SET_PW = 910;
    public static final int DELETE = 800;

    private static final long serialVersionUID = 1L;

    private int actionID;
    private ActionListener actionListener;

    private Property property;
    private String ressourceName;

    public TreeTableAction(ActionListener actionListener, String ressourceName, int actionID) {
        this(actionListener, ressourceName, actionID, null);
    }

    public TreeTableAction(ActionListener actionListener, String ressourceName, int actionID, Property obj) {
        super();
        this.ressourceName = ressourceName;
        this.actionID = actionID;
        this.actionListener = actionListener;
        property = obj;

        putValue(Action.NAME, ressourceName);

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
