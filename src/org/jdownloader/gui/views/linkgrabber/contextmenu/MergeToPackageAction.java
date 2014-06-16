package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;

public class MergeToPackageAction extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> implements ActionContext {

    /**
     * 
     */
    private static final long serialVersionUID = -4468197802870765463L;

    public MergeToPackageAction() {
        setName(_GUI._.MergeToPackageAction_MergeToPackageAction_());
        setIconKey("package_new");
        setLastPathDefault(true);
    }

    private boolean lastPathDefault = false;

    @Customizer(name = "Use latest selected path as default one")
    public boolean isLastPathDefault() {
        return lastPathDefault;
    }

    public void setLastPathDefault(boolean lastPathDefault) {
        this.lastPathDefault = lastPathDefault;
    }

    public static enum Location {
        @EnumLabel("The end of the list")
        END_OF_LIST,
        @EnumLabel("The top of the list")
        TOP_OF_LIST,
        @EnumLabel("After selection")
        AFTER_SELECTION,
        @EnumLabel("Before selection")
        BEFORE_SELECTION;
    }

    private Location location = Location.END_OF_LIST;

    @Customizer(name = "Add package at")
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        try {
            final SelectionInfo<CrawledPackage, CrawledLink> sel = getSelection();
            final NewPackageDialog d = new NewPackageDialog(sel);
            if (isLastPathDefault()) {
                List<String> pathes = DownloadPathHistoryManager.getInstance().listPathes((String[]) null);
                if (pathes != null && pathes.size() > 0) {
                    d.setDownloadFolder(pathes.get(0));
                }
            }
            Dialog.getInstance().showDialog(d);
            final String name = d.getName();

            if (name == null | name.trim().length() == 0) {
                return;
            }

            LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    CrawledPackage newPackage = new CrawledPackage();
                    newPackage.setName(name);
                    String f = d.getDownloadFolder();
                    newPackage.setDownloadFolder(f);

                    switch (getLocation()) {
                    case AFTER_SELECTION:
                        int index = -1;
                        for (PackageView<CrawledPackage, CrawledLink> pv : sel.getPackageViews()) {
                            index = Math.max(index, LinkCollector.getInstance().indexOf(pv.getPackage()) + 1);
                        }
                        LinkCollector.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0, index);
                        return null;
                    case BEFORE_SELECTION:
                        index = Integer.MAX_VALUE;
                        for (PackageView<CrawledPackage, CrawledLink> pv : sel.getPackageViews()) {
                            index = Math.min(index, LinkCollector.getInstance().indexOf(pv.getPackage()));
                        }
                        if (index == Integer.MAX_VALUE) {
                            index = 0;
                        }
                        LinkCollector.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0, index);
                        return null;

                    case END_OF_LIST:
                        LinkCollector.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0, -1);
                        return null;

                    case TOP_OF_LIST:
                        LinkCollector.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0, 0);
                        return null;
                    }

                    return null;
                }

            });
        } catch (DialogNoAnswerException e1) {
        }
    }

}
