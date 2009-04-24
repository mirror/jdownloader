package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.SimpleGuiUtils;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RestartAction extends StartAction {

    private static final long serialVersionUID = 1333126351380171619L;

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.restart");
        this.setShortDescription("gui.menu.action.restart.desc");
        this.setName("gui.menu.action.restart.name");
        this.setMnemonic("gui.menu.action.restart.mnem", "gui.menu.restart.name");
        this.setAccelerator("gui.menu.action.restart.accel");
    }

    public void actionPerformed(ActionEvent e) {
        boolean doIt = true;
        if (!SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
            doIt = SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("sys.ask.rlyrestart", "Wollen Sie jDownloader wirklich neustarten?"));
        } else {
            doIt = true;
        }
        if (doIt) {
            SimpleGuiUtils.saveLastLocation(SimpleGUI.CURRENTGUI, null);
            SimpleGuiUtils.saveLastDimension(SimpleGUI.CURRENTGUI, null);
            SimpleGuiConstants.GUI_CONFIG.save();
            JDUtilities.getController().restart();
        }
    }

}
