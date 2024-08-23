package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.LocationInList;
import org.jdownloader.translate._JDT;

public abstract class AbstractMergeToPackageAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableTableContextAppAction<PackageType, ChildrenType> implements ActionContext {
    /**
     *
     */
    private static final long serialVersionUID = -4468197802870765463L;

    public AbstractMergeToPackageAction() {
        setName(_GUI.T.MergeToPackageAction_MergeToPackageAction_());
        setIconKey(IconKey.ICON_PACKAGE_NEW);
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
        NewPackageDialog d = null;
        final SelectionInfo<PackageType, ChildrenType> sel = getSelection();
        try {
            d = new NewPackageDialog(sel) {
                @Override
                public String getDontShowAgainKey() {
                    return "ABSTRACTDIALOG_DONT_SHOW_AGAIN_" + AbstractMergeToPackageAction.this.getClass().getSimpleName();
                }
            };
            if (isLastPathDefault()) {
                List<String> paths = DownloadPathHistoryManager.getInstance().listPaths((String[]) null);
                if (paths != null && paths.size() > 0) {
                    d.setDownloadFolder(paths.get(0));
                }
            }
            Dialog.getInstance().showDialog(d);
        } catch (DialogNoAnswerException e1) {
            return;
        }
        final String name = d.getName();
        if (StringUtils.isEmpty(name)) {
            return;
        }
        final String downloadFolder = d.getDownloadFolder();
        final PackageController<PackageType, ChildrenType> controller = sel.getController();
        controller.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                final PackageType newPackage = createNewPackage(name, downloadFolder);
                newPackage.setExpanded(isExpandNewPackage());
                final String packageComment = mergePackageViewListComments(sel.getPackageViews());
                if (!StringUtils.isEmpty(packageComment)) {
                    newPackage.setComment(packageComment);
                }
                switch (getLocation()) {
                case AFTER_SELECTION:
                    int index = -1;
                    for (PackageView<PackageType, ChildrenType> pv : sel.getPackageViews()) {
                        index = Math.max(index, controller.indexOf(pv.getPackage()) + 1);
                    }
                    controller.moveOrAddAt(newPackage, sel.getChildren(), 0, index);
                    return null;
                case BEFORE_SELECTION:
                    index = Integer.MAX_VALUE;
                    for (PackageView<PackageType, ChildrenType> pv : sel.getPackageViews()) {
                        index = Math.min(index, controller.indexOf(pv.getPackage()));
                    }
                    if (index == Integer.MAX_VALUE) {
                        index = 0;
                    }
                    controller.moveOrAddAt(newPackage, sel.getChildren(), 0, index);
                    return null;
                case END_OF_LIST:
                    controller.moveOrAddAt(newPackage, sel.getChildren(), 0, -1);
                    return null;
                case TOP_OF_LIST:
                    controller.moveOrAddAt(newPackage, sel.getChildren(), 0, 0);
                    return null;
                }
                return null;
            }
        });
    }

    protected abstract PackageType createNewPackage(final String name, final String downloadFolder);

    /** Merges comments of multiple packages into one string. */
    protected String mergePackageViewListComments(final List<PackageView<PackageType, ChildrenType>> packages) {
        final List<PackageType> crawledpackagelist = new ArrayList<PackageType>();
        for (PackageView<PackageType, ChildrenType> pv : packages) {
            crawledpackagelist.add(pv.getPackage());
        }
        return mergePackageComments(crawledpackagelist);
    }

    /** Merges comments of given packages into one string. */
    protected String mergePackageComments(final List<PackageType> packages) {
        final StringBuilder sb = new StringBuilder();
        final HashSet<String> commentDups = new HashSet<String>();
        for (final PackageType cp : packages) {
            final String comment = cp.getComment();
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
        return sb.toString();
    }
}
