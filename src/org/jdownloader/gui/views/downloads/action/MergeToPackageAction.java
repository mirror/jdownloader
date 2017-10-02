package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.LocationInList;
import org.jdownloader.gui.views.linkgrabber.contextmenu.NewPackageDialog;
import org.jdownloader.translate._JDT;

public class MergeToPackageAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> implements ActionContext {
    private static final long serialVersionUID = -4468197802870765463L;

    public MergeToPackageAction() {
        setName(_GUI.T.MergeToPackageAction_MergeToPackageAction_());
        setIconKey(IconKey.ICON_PACKAGE_NEW);
        setLastPathDefault(true);
    }

    private boolean expandNewPackage = false;

    public static String getTranslationForExpandNewPackage() {
        return _JDT.T.MergeToPackageAction_getTranslationForExpandNewPackage();
    }

    @Customizer(link = "#getTranslationForExpandNewPackage")
    public boolean isExpandNewPackage() {
        return expandNewPackage;
    }

    public void setExpandNewPackage(boolean expandNewPackage) {
        this.expandNewPackage = expandNewPackage;
    }

    private boolean lastPathDefault = false;

    public static String getTranslationForLastPathDefault() {
        return _JDT.T.MergeToPackageAction_getTranslationForLastPathDefault();
    }

    @Customizer(link = "#getTranslationForLastPathDefault")
    public boolean isLastPathDefault() {
        return lastPathDefault;
    }

    public void setLastPathDefault(boolean lastPathDefault) {
        this.lastPathDefault = lastPathDefault;
    }

    private LocationInList location = LocationInList.END_OF_LIST;

    public static String getTranslationForLocation() {
        return _JDT.T.MergeToPackageAction_getTranslationForLocation();
    }

    @Customizer(link = "#getTranslationForLocation")
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
            final NewPackageDialog d = new NewPackageDialog(sel) {
                @Override
                public String getDontShowAgainKey() {
                    return "ABSTRACTDIALOG_DONT_SHOW_AGAIN_" + MergeToPackageAction.this.getClass().getSimpleName();
                }
            };
            if (isLastPathDefault()) {
                List<String> paths = DownloadPathHistoryManager.getInstance().listPaths((String[]) null);
                if (paths != null && paths.size() > 0) {
                    d.setDownloadFolder(paths.get(0));
                }
            }
            Dialog.getInstance().showDialog(d);
            final String name = d.getName();
            if (StringUtils.isEmpty(name)) {
                return;
            }
            final String downloadFolder = d.getDownloadFolder();
            DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    FilePackage newPackage = FilePackage.getInstance();
                    newPackage.setExpanded(isExpandNewPackage());
                    newPackage.setName(name);
                    newPackage.setDownloadDirectory(PackagizerController.replaceDynamicTags(downloadFolder, name, newPackage));
                    final StringBuilder sb = new StringBuilder();
                    final HashSet<String> commentDups = new HashSet<String>();
                    for (PackageView<FilePackage, DownloadLink> pv : sel.getPackageViews()) {
                        final String comment = pv.getPackage().getComment();
                        if (StringUtils.isNotEmpty(comment)) {
                            final String[] commentLines = Regex.getLines(comment);
                            for (final String commentLine : commentLines) {
                                if (StringUtils.isNotEmpty(commentLine) && commentDups.add(commentLine)) {
                                    if (sb.length() > 0) {
                                        sb.append("\r\n");
                                    }
                                    sb.append(commentLine);
                                }
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        newPackage.setComment(sb.toString());
                    }
                    switch (getLocation()) {
                    case AFTER_SELECTION:
                        int index = -1;
                        for (PackageView<FilePackage, DownloadLink> pv : sel.getPackageViews()) {
                            index = Math.max(index, DownloadController.getInstance().indexOf(pv.getPackage()) + 1);
                        }
                        DownloadController.getInstance().moveOrAddAt(newPackage, sel.getChildren(), 0, index);
                        return null;
                    case BEFORE_SELECTION:
                        index = Integer.MAX_VALUE;
                        for (PackageView<FilePackage, DownloadLink> pv : sel.getPackageViews()) {
                            index = Math.min(index, DownloadController.getInstance().indexOf(pv.getPackage()));
                        }
                        if (index == Integer.MAX_VALUE) {
                            index = 0;
                        }
                        DownloadController.getInstance().moveOrAddAt(newPackage, sel.getChildren(), 0, index);
                        return null;
                    case END_OF_LIST:
                        DownloadController.getInstance().moveOrAddAt(newPackage, sel.getChildren(), 0, -1);
                        return null;
                    case TOP_OF_LIST:
                        DownloadController.getInstance().moveOrAddAt(newPackage, sel.getChildren(), 0, 0);
                        return null;
                    }
                    return null;
                }
            });
        } catch (DialogNoAnswerException e1) {
        }
    }
}
