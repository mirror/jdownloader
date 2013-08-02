package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class ClearFilteredLinksAction extends SelectionAppAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = -6341297356888158708L;

    public ClearFilteredLinksAction(SelectionInfo<CrawledPackage, CrawledLink> selection) {
        super(selection);
        setName(_GUI._.ClearFilteredLinksAction());
        setIconKey("filter");
        setEnabled(LinkCollector.getInstance().getfilteredStuffSize() > 0);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            Dialog.getInstance().showConfirmDialog(0, _GUI._.literally_are_you_sure(), _GUI._.ClearFilteredLinksAction_msg(LinkCollector.getInstance().getfilteredStuffSize()), null, _GUI._.literally_yes(), _GUI._.literall_no());
            IOEQ.add(new Runnable() {

                public void run() {

                    LinkCollector.getInstance().getFilteredStuff(true);
                }

            }, true);
        } catch (DialogNoAnswerException e1) {
        }
    }

}
