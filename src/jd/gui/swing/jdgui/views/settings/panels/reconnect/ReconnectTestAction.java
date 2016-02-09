package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.event.ActionEvent;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.swing.components.tooltips.TooltipFactory;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class ReconnectTestAction extends BasicAction {
    /**
     * 
     */
    private static final long serialVersionUID = 2580441275315364611L;

    {
        putValue(NAME, _GUI.T.ReconnectTestAction());
        putValue(SMALL_ICON, new AbstractIcon(IconKey.ICON_TEST, 20));

    }

    public TooltipFactory getTooltipFactory() {
        return new BasicTooltipFactory(getName(), _GUI.T.ReconnectTestAction_tt_2(), new AbstractIcon(IconKey.ICON_TEST, 32));
    }

    public void actionPerformed(ActionEvent e) {

        try {
            Dialog.getInstance().showDialog(new ReconnectDialog());
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
