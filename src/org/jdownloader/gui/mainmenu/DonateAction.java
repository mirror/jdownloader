package org.jdownloader.gui.mainmenu;

import java.awt.event.ActionEvent;

import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class DonateAction extends CustomizableAppAction {

    public DonateAction() {
        setIconKey(IconKey.ICON_HEART);
        setName(_GUI.T.DonateAction());
        setTooltipText(_GUI.T.DonateAction_tt());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final ConfirmDialog d = new ConfirmDialog(0, _GUI.T.DonationDialog_DonationDialog_title_(), _GUI.T.DonationDialog_layoutDialogContent_top_text(), new AbstractIcon(IconKey.ICON_HEART, 32), _GUI.T.lit_continue(), null) {
            protected int getPreferredWidth() {
                return 650;
            };
        };
        UIOManager.I().show(null, d);
        if (d.getCloseReason() == CloseReason.OK) {
            CrossSystem.openURL("https://my.jdownloader.org/contribute/#/?ref=jdownloader");
        }
    }

}
