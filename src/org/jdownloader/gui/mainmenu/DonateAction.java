package org.jdownloader.gui.mainmenu;

import java.awt.event.ActionEvent;

import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.donate.DONATE_EVENT;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class DonateAction extends CustomizableAppAction {
    /**
     *
     */
    private static final long serialVersionUID = -4559644534638870038L;
    final DONATE_EVENT        event;

    public DonateAction() {
        // required by MenuManager
        this(DONATE_EVENT.getNow());
    }

    public DonateAction(DONATE_EVENT event) {
        if (event == null) {
            event = DONATE_EVENT.DEFAULT;
        }
        this.event = event;
        setIconKey(event.getIconKey());
        setName(_GUI.T.DonateAction());
        setTooltipText(_GUI.T.DonateAction_tt());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final ConfirmDialog d = new ConfirmDialog(0, event.getTitleText(), event.getText(), new AbstractIcon(event.getIconKey(), 32), _GUI.T.lit_continue(), null) {
            protected int getPreferredWidth() {
                return 650;
            };
        };
        final ConfirmDialogInterface di = UIOManager.I().show(null, d);
        if (di.getCloseReason() == CloseReason.OK) {
            CrossSystem.openURL("https://my.jdownloader.org/contribute/#/?ref=jdownloader");
        }
    }
}
