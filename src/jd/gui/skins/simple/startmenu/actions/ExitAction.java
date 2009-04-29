package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.SimpleGuiUtils;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ExitAction extends StartAction {

    private static final long serialVersionUID = -1428029294638573437L;

    public ExitAction() {
        super("action.exit", "gui.images.exit");
    }

    public void actionPerformed(ActionEvent e) {
        boolean doIt = true;
        if (!SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
            doIt = SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?"));
        } else {
            doIt = true;
        }
        if (doIt) {
            SimpleGuiUtils.saveLastLocation(SimpleGUI.CURRENTGUI, null);
            SimpleGuiUtils.saveLastDimension(SimpleGUI.CURRENTGUI, null);
            SimpleGuiConstants.GUI_CONFIG.save();
            JDUtilities.getController().exit();
        }
    }

}
