package org.jdownloader.gui.mainmenu;

import java.awt.event.ActionEvent;

import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.donate.DONATE_EVENT;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class DonateAction extends CustomizableAppAction {
    /**
     *
     */
    private static final long serialVersionUID = -4559644534638870038L;
    final String              iconKey;

    public DonateAction() {
        // required by MenuManager
        this(DONATE_EVENT.getNow());
    }

    public DonateAction(final DONATE_EVENT event) {
        if (event != null && event.getIconKey() != null) {
            iconKey = event.getIconKey();
        } else {
            iconKey = IconKey.ICON_HEART;
        }
        setIconKey(iconKey);
        setName(_GUI.T.DonateAction());
        setTooltipText(_GUI.T.DonateAction_tt());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final ConfirmDialog d = new ConfirmDialog(0, _GUI.T.DonationDialog_DonationDialog_title_(), _GUI.T.DonationDialog_layoutDialogContent_top_text(), new AbstractIcon(iconKey, 32), _GUI.T.lit_continue(), null) {
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
