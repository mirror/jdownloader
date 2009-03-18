package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import jd.config.Property;

public class LinkGrabberV2TreeTableAction extends AbstractAction {

    private static final long serialVersionUID = 1220090161204767583L;
    public static final int NEW_PACKAGE = 100;
    public static final int PACKAGE_EDIT_DIR = 101;
    public static final int PACKAGE_EDIT_NAME = 102;
    public static final int PACKAGE_SORT = 103;
    public static final int PACKAGE_PRIO = 104;
    public static final int DOWNLOAD_PRIO = 200;
    public static final int SET_PW = 201;
    public static final int ADD_ALL = 300;
    public static final int ADD_SELECTED = 301;
    public static final int DELETE = 999;

    private int actionID;
    private ActionListener actionListener;

    private Property property;
    private String ressourceName;

    public LinkGrabberV2TreeTableAction(ActionListener actionListener, String ressourceName, int actionID) {
        this(actionListener, ressourceName, actionID, null);
    }

    public LinkGrabberV2TreeTableAction(ActionListener actionListener, String ressourceName, int actionID, Property obj) {
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