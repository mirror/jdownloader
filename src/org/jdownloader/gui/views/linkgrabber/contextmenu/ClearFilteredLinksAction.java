package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import jd.controlling.linkcollector.LinkCollector;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;

public class ClearFilteredLinksAction extends AppAction implements CachableInterface {

    /**
     * 
     */
    private static final long serialVersionUID = -6341297356888158708L;

    public ClearFilteredLinksAction() {

        setName(_GUI._.ClearFilteredLinksAction());
        setIconKey("filter");
        setEnabled(LinkCollector.getInstance().getfilteredStuffSize() > 0);
    }

    public void actionPerformed(ActionEvent e) {
        if (LinkCollector.getInstance().getfilteredStuffSize() == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try {
            Dialog.getInstance().showConfirmDialog(0, _GUI._.literally_are_you_sure(), _GUI._.ClearFilteredLinksAction_msg(LinkCollector.getInstance().getfilteredStuffSize()), null, _GUI._.literally_yes(), _GUI._.literall_no());
            LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkCollector.getInstance().getFilteredStuff(true);
                    return null;
                }
            });
        } catch (DialogNoAnswerException e1) {
        }
    }

    @Override
    public void setData(String data) {
    }

}
