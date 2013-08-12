package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class RemoveDisabledLinksFromLinkgrabberAction extends SelectionAppAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = -6341297356888158708L;

    public RemoveDisabledLinksFromLinkgrabberAction(SelectionInfo<CrawledPackage, CrawledLink> selection) {
        super(selection);
        setName(_GUI._.RemoveDisabledAction_RemoveDisabledAction_object_());
        setIconKey(IconKey.ICON_REMOVE_DISABLED);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        List<CrawledLink> filtered = new ArrayList<CrawledLink>();
        for (CrawledLink cl : getSelection().getChildren()) {
            if (!cl.isEnabled()) filtered.add(cl);
        }
        try {
            if (filtered.size() > 0) {
                Dialog.getInstance().showConfirmDialog(0, _GUI._.literally_are_you_sure(), _GUI._.RemoveDisabledAction_actionPerformed_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());
                LinkCollector.getInstance().removeChildren(filtered);
            }
        } catch (DialogNoAnswerException e1) {
        }
    }

}
