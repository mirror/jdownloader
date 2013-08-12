package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.TaskQueue;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class CheckStatusAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends SelectionAppAction<PackageType, ChildrenType> {

    private static final long serialVersionUID = 6821943398259956694L;

    public CheckStatusAction(SelectionInfo<PackageType, ChildrenType> si) {
        super(si);
        setIconKey("ok");
        setName(_GUI._.gui_table_contextmenu_check());

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                List<ChildrenType> children = getSelection().getChildren();
                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(children.size());
                for (AbstractPackageChildrenNode l : children) {
                    if (l instanceof DownloadLink || l instanceof CrawledLink) {
                        checkableLinks.add(((CheckableLink) l));
                    }
                }
                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                linkChecker.check(checkableLinks);
                return null;
            }
        });
    }

}