package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;
import org.jdownloader.images.NewTheme;

public class SetDownloadPassword extends AppAction {

    /**
     * 
     */
    private static final long      serialVersionUID = -8280535886054721277L;
    private ArrayList<CrawledLink> selection;

    public SetDownloadPassword(AbstractNode node, ArrayList<AbstractNode> selection2) {

        this.selection = LinkTreeUtils.getSelectedChildren(selection2);
        setName(_GUI._.SetDownloadPassword_SetDownloadPassword_());
        setIconKey("password");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            final String newPW = Dialog.getInstance().showInputDialog(0, _GUI._.SetDownloadPassword_SetDownloadPassword_(), _GUI._.jd_gui_userio_defaulttitle_input(), selection.get(0).getDownloadLink().getDownloadPassword(), NewTheme.I().getIcon("password", 32), null, null);
            IOEQ.add(new Runnable() {

                public void run() {
                    for (CrawledLink l : selection) {
                        DownloadLink dl = l.getDownloadLink();
                        if (dl != null) dl.setDownloadPassword(newPW);
                    }
                    LinkCollector.getInstance().refreshData();
                }

            });
        } catch (DialogNoAnswerException e1) {
        }
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() > 0;
    }

}
