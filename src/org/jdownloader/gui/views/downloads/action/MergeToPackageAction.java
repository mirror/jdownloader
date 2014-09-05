package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.LocationInList;
import org.jdownloader.gui.views.linkgrabber.contextmenu.NewPackageDialog;

public class MergeToPackageAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> implements ActionContext {

    /**
     *
     */
    private static final long serialVersionUID = -4468197802870765463L;

    public MergeToPackageAction() {
        setName(_GUI._.MergeToPackageAction_MergeToPackageAction_());
        setIconKey("package_new");
        setLastPathDefault(true);
    }

    private boolean expandNewPackage = false;

    @Customizer(name = "Expand the new package after creation")
    public boolean isExpandNewPackage() {
        return expandNewPackage;
    }

    public void setExpandNewPackage(boolean expandNewPackage) {
        this.expandNewPackage = expandNewPackage;
    }

    private boolean lastPathDefault = false;

    @Customizer(name = "Use latest selected path as default one")
    public boolean isLastPathDefault() {
        return lastPathDefault;
    }

    public void setLastPathDefault(boolean lastPathDefault) {
        this.lastPathDefault = lastPathDefault;
    }

    private LocationInList location = LocationInList.END_OF_LIST;

    @Customizer(name = "Add package at")
    public LocationInList getLocation() {
        return location;
    }

    public void setLocation(LocationInList location) {
        this.location = location;
    }

    @Override
    public void addContextSetup(ActionContext contextSetup) {
        super.addContextSetup(contextSetup);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        try {
            final SelectionInfo<FilePackage, DownloadLink> sel = getSelection();
            final NewPackageDialog d = new NewPackageDialog(sel);
            if (isLastPathDefault()) {
                List<String> paths = DownloadPathHistoryManager.getInstance().listPaths((String[]) null);
                if (paths != null && paths.size() > 0) {
                    d.setDownloadFolder(paths.get(0));
                }
            }
            Dialog.getInstance().showDialog(d);
            final String name = d.getName();

            if (name == null | name.trim().length() == 0) {
                return;
            }

            DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    FilePackage newPackage = FilePackage.getInstance();
                    newPackage.setExpanded(isExpandNewPackage());
                    newPackage.setName(name);
                    String f = d.getDownloadFolder();

                    newPackage.setDownloadDirectory(PackagizerController.replaceDynamicTags(f, name));

                    switch (getLocation()) {
                    case AFTER_SELECTION:
                        int index = -1;
                        for (PackageView<FilePackage, DownloadLink> pv : sel.getPackageViews()) {
                            index = Math.max(index, DownloadController.getInstance().indexOf(pv.getPackage()) + 1);
                        }

                        DownloadController.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0, index);

                        return null;
                    case BEFORE_SELECTION:
                        index = Integer.MAX_VALUE;
                        for (PackageView<FilePackage, DownloadLink> pv : sel.getPackageViews()) {
                            index = Math.min(index, DownloadController.getInstance().indexOf(pv.getPackage()));
                        }
                        if (index == Integer.MAX_VALUE) {
                            index = 0;
                        }
                        DownloadController.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0, index);

                        return null;

                    case END_OF_LIST:
                        DownloadController.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0, -1);

                        return null;

                    case TOP_OF_LIST:
                        DownloadController.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0, 0);

                        return null;
                    }

                    return null;
                }

            });
        } catch (DialogNoAnswerException e1) {
        }
    }

}
