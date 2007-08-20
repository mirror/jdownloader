package jd.controlling;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import jd.JDUtilities;
import jd.gui.GUIInterface;

/**
 * Alle Interaktionen (Knöpfe, Shortcuts) sollten über diese JDAction stattfinden
 *
 * @author astaldo1
 */
public class JDAction extends AbstractAction{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 7393495345332708426L;

    public static final int ITEMS_MOVE_UP            =  1;
    public static final int ITEMS_MOVE_DOWN          =  2;
    public static final int ITEMS_MOVE_TOP           =  3;
    public static final int ITEMS_MOVE_BOTTOM        =  4;
    public static final int ITEMS_DISABLE            =  5;
    public static final int ITEMS_ENABLE             =  6;
    public static final int ITEMS_ADD                =  7;
    public static final int ITEMS_REMOVE             =  8;
    public static final int APP_START_STOP_DOWNLOADS =  9;
    public static final int APP_SHOW_LOG             = 10;
    public static final int APP_STOP_DOWNLOADS       = 11;
    public static final int APP_SAVE                 = 12;
    public static final int APP_LOAD                 = 13;
    public static final int APP_EXIT                 = 14;
    public static final int VIEW_LOG                 = 1001;

    private GUIInterface guiInterface;
    private int actionID;
    /**
     * Erstellt ein neues JDAction-Objekt
     * @param guiInterface TODO
     * @param iconName
     * @param resourceName Name der Resource, aus der die Texte geladen werden sollen
     * @param actionID ID dieser Aktion
     */
    public JDAction(GUIInterface guiInterface, String iconName, String resourceName, int actionID){
        super();
        ImageIcon icon = new ImageIcon(JDUtilities.getImage(iconName));
        putValue(Action.SMALL_ICON, icon);
        putValue(Action.SHORT_DESCRIPTION, JDUtilities.getResourceString(resourceName+".desc"));
        putValue(Action.NAME,              JDUtilities.getResourceString(resourceName+".name"));
        this.actionID = actionID;
        this.guiInterface = guiInterface;
    }
    public void actionPerformed(ActionEvent e) {
        guiInterface.doAction(actionID);
    }
    public int getActionID(){
        return actionID;
    }
}
