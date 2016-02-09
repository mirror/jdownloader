package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.dlc.DLCFactory;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.logging.LogController;

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

public class CreateDLCAction extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> {

    public CreateDLCAction() {

        setName(_GUI.T.gui_table_contextmenu_dlc());
        setIconKey(IconKey.ICON_LOGO_DLC);

    }

    public void actionPerformed(ActionEvent e) {
        final SelectionInfo<CrawledPackage, CrawledLink> selection = getSelection();
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final DLCFactory plugin = new DLCFactory();
                plugin.setLogger(LogController.CL(CreateDLCAction.class));
                plugin.createDLCByCrawledLinks(selection.getChildren());
                return null;
            }
        });
    }

}
