package jd.controlling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import jd.JDUtilities;

/**
 * Alle Interaktionen (Knöpfe, Shortcuts) sollten über diese JDAction stattfinden
 *
 * @author astaldo
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
    public static final int APP_LOG                  = 15;
    public static final int APP_CONFIGURATION        = 16;
    public static final int APP_RECONNECT            = 17;
    public static final int APP_UPDATE               = 18;
    public static final int ITEMS_DND                =  19;

    public static final int APP_SEARCH = 20;

    private ActionListener actionListener;
    private int actionID;
    private KeyStroke accelerator;
    private String ressourceName;
    /**
     * Erstellt ein neues JDAction-Objekt
     * @param actionListener ein ActionListener
     * @param iconName Name des Icons
     * @param ressourceName Name der Resource, aus der die Texte geladen werden sollen
     * @param actionID ID dieser Aktion
     */
    public JDAction(ActionListener actionListener, String iconName, String ressourceName, int actionID){
        super();
        this.ressourceName = ressourceName;
        this.actionID = actionID;
        this.actionListener = actionListener;
     
        ImageIcon icon = new ImageIcon(JDUtilities.getImage(iconName));
        putValue(Action.SMALL_ICON, icon);
        putValue(Action.SHORT_DESCRIPTION, JDUtilities.getResourceString(ressourceName+".desc"));
        putValue(Action.NAME,              JDUtilities.getResourceString(ressourceName+".name"));
        char mnemonic = JDUtilities.getResourceChar(ressourceName+".mnem");
        if (mnemonic!=0)
           putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
        String acceleratorString = JDUtilities.getResourceString(ressourceName+".accel");
        if (acceleratorString!=null && acceleratorString.length()>0)
           accelerator = KeyStroke.getKeyStroke(acceleratorString);
    }
    public void actionPerformed(ActionEvent e) {
        actionListener.actionPerformed(new ActionEvent(e.getSource(),actionID,ressourceName));
    }
    public int getActionID(){
        return actionID;
    }
   public KeyStroke getAccelerator() {
      return accelerator;
   }
}
