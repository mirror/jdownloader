package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.dragdrop.MergePosition;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MergeToPackageAction;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.plugins.FilePackage;

public class MergeSameNamedPackagesAction extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> implements ActionContext {
    private boolean caseInsensitive = true;

    public static String getTranslationForMatchPackageNamesCaseInsensitive() {
        return _GUI.T.MergeSameNamedPackagesAction_Case_Insensitive();
    }

    @Customizer(link = "#getTranslationForMatchPackageNamesCaseInsensitive")
    public boolean isMatchPackageNamesCaseInsensitive() {
        return caseInsensitive;
    }

    public void setMatchPackageNamesCaseInsensitive(boolean val) {
        this.caseInsensitive = val;
    }

    /**
     * @param selection
     *            TODO
     *
     */
    public MergeSameNamedPackagesAction() {
        super(true, true);
        // setSmallIcon(new BadgeIcon("logo/dlc", "autoMerge", 32, 24, 2, 6));
        setName(_GUI.T.MergeSameNamedPackagesAction_());
        // setAccelerator(KeyEvent.VK_M);
        setIconKey(IconKey.ICON_REMOVE_DUPES);
    }

    private static final long serialVersionUID = -1758454550263991987L;

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        try {
            final boolean caseInsensitive = isMatchPackageNamesCaseInsensitive();
            final SelectionInfo<?, ?> sel = getSelection();
            /* If user has selected a package, only collect duplicates of name of selected package. */
            Map<String, Object> selectedPackagesMap = null;
            final List<?> selPackageViews = sel.getPackageViews();
            if (sel != null && selPackageViews.size() > 0) {
                selectedPackagesMap = new HashMap<>();
                for (final PackageView<?, ?> pv : sel.getPackageViews()) {
                    final AbstractPackageNode<?, ?> crawledpackage = pv.getPackage();
                    // final CrawledPackage crawledpackage = pv.getPackage();
                    final String compareName;
                    if (caseInsensitive) {
                        compareName = crawledpackage.getName().toLowerCase(Locale.ENGLISH);
                    } else {
                        compareName = crawledpackage.getName();
                    }
                    if (selectedPackagesMap.containsKey(compareName)) {
                        /* Item is already contained in map - we want to merge all dupes into first package we find. */
                        continue;
                    }
                    selectedPackagesMap.put(compareName, crawledpackage);
                }
                if (selectedPackagesMap.isEmpty()) {
                    /* Do nothing */
                    return;
                }
            }
            final Map<String, List<AbstractNode>> dupes = new HashMap<String, List<AbstractNode>>();
            final List<AbstractNode> packages = new ArrayList<AbstractNode>();
            final boolean isLinkgrabber;
            if (MainTabbedPane.getInstance().isDownloadView()) {
                isLinkgrabber = false;
                for (final FilePackage item : DownloadController.getInstance().getPackages()) {
                    packages.add(item);
                }
            } else if (MainTabbedPane.getInstance().isLinkgrabberView()) {
                isLinkgrabber = true;
                for (final CrawledPackage item : LinkCollector.getInstance().getPackages()) {
                    packages.add(item);
                }
            } else {
                /* This should never happen */
                return;
            }
            boolean foundDupes = false;
            for (final AbstractNode pckage : packages) {
                String packagename;
                if (isLinkgrabber) {
                    packagename = ((CrawledPackage) pckage).getName();
                } else {
                    packagename = ((FilePackage) pckage).getName();
                }
                if (caseInsensitive) {
                    packagename = packagename.toLowerCase(Locale.ENGLISH);
                }
                if (selectedPackagesMap != null && !selectedPackagesMap.containsKey(packagename)) {
                    /* Only search dupes for selected package(s) */
                    continue;
                }
                List<AbstractNode> thisdupeslist = dupes.get(packagename);
                if (thisdupeslist != null) {
                    /* We got at least two packages with the same name */
                    foundDupes = true;
                } else {
                    thisdupeslist = new ArrayList<AbstractNode>();
                    dupes.put(packagename, thisdupeslist);
                }
                thisdupeslist.add(pckage);
            }
            if (!foundDupes) {
                // TODO: Add logger
                System.out.println("Failed to find any duplicated packages to merge");
                return;
            }
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    /* Merge dupes */
                    final Iterator<Entry<String, List<AbstractNode>>> dupes_iterator = dupes.entrySet().iterator();
                    while (dupes_iterator.hasNext()) {
                        final Entry<String, List<AbstractNode>> entry = dupes_iterator.next();
                        // final String packagename = entry.getKey();
                        final List<AbstractNode> thisdupes = entry.getValue();
                        if (thisdupes.size() == 1) {
                            /* We need at least two packages to be able to merge them. */
                            continue;
                        }
                        /* Merge comments of all packages so we don't lose any information. */
                        if (isLinkgrabber) {
                            final List<CrawledPackage> casteddupes = new ArrayList<CrawledPackage>();
                            for (final AbstractNode thisdupe : thisdupes) {
                                casteddupes.add((CrawledPackage) thisdupe);
                            }
                            final String mergedComments = MergeToPackageAction.mergePackageComments(thisdupes);
                            /* Pick package to merge the others into */
                            final CrawledPackage target = (CrawledPackage) thisdupes.remove(0);
                            if (!StringUtils.isEmpty(mergedComments)) {
                                target.setComment(mergedComments);
                            }
                            LinkCollector.getInstance().merge(target, casteddupes, MergePosition.BOTTOM);
                        } else {
                            final List<FilePackage> casteddupes = new ArrayList<FilePackage>();
                            for (final AbstractNode thisdupe : thisdupes) {
                                casteddupes.add((FilePackage) thisdupe);
                            }
                            final String mergedComments = MergeToPackageAction.mergePackageComments(thisdupes);
                            /* Pick package to merge the others into */
                            final FilePackage target = (FilePackage) thisdupes.remove(0);
                            if (!StringUtils.isEmpty(mergedComments)) {
                                target.setComment(mergedComments);
                            }
                            DownloadController.getInstance().merge(target, casteddupes, MergePosition.BOTTOM);
                        }
                    }
                    return null;
                }
            });
        } catch (final Throwable ignore) {
            System.out.println("wtf");
        }
    }
}
