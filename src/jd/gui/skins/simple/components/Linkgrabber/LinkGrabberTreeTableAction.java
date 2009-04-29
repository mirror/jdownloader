package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import jd.config.Property;

public class LinkGrabberTreeTableAction extends AbstractAction {

    private static final long serialVersionUID = 1220090161204767583L;
    public static final int NEW_PACKAGE = 100;
    public static final int MERGE_PACKAGE = 110;
    public static final int EDIT_DIR = 101;
    public static final int SORT = 103;
    public static final int SORT_ALL = 1031;
    public static final int DOWNLOAD_PRIO = 200;
    public static final int SET_PW = 201;
    public static final int ADD_ALL = 300;
    public static final int ADD_SELECTED = 301;
    public static final int DE_ACTIVATE = 400;
    public static final int SELECT_HOSTER = 500;
    public static final int GUI_ADD = 600;
    public static final int GUI_LOAD = 601;
    public static final int DELETE = 999;
    public static final int DELETE_OFFLINE = 9991;
    public static final int CLEAR = 9999;
    public static final int EXT_FILTER = 99999;

    private int actionID;
    private ActionListener actionListener;

    private Property property;
    private String ressourceName;

    public LinkGrabberTreeTableAction(ActionListener actionListener, ImageIcon icon, String ressourceName, int actionID) {
        this(actionListener, icon, ressourceName, actionID, null);
    }


    public LinkGrabberTreeTableAction(ActionListener actionListener, ImageIcon icon, String ressourceName, int actionID, Property property) {

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