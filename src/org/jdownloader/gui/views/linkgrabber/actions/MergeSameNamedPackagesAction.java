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
        // TODO: Find a suitable symbol
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
        // TODO: Remove uglyness
        try {
            final Map<String, List<Object>> dupes = new HashMap<String, List<Object>>();
            final List<?> pckages;
            final boolean isLinkgrabber;
            if (MainTabbedPane.getInstance().isDownloadView()) {
                pckages = DownloadController.getInstance().getPackages();
                isLinkgrabber = false;
            } else if (MainTabbedPane.getInstance().isLinkgrabberView()) {
                final LinkCollector lc = LinkCollector.getInstance();
                pckages = lc.getPackages();
                isLinkgrabber = true;
            } else {
                return;
            }
            final boolean caseInsensitive = isMatchPackageNamesCaseInsensitive();
            boolean foundDupes = false;
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
            }
            for (Object pckage : pckages) {
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
                List<Object> thisdupeslist = dupes.get(packagename);
                if (thisdupeslist != null) {
                    /* We got at least two packages with the same name */
                    foundDupes = true;
                } else {
                    thisdupeslist = new ArrayList<Object>();
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
                    final Iterator<Entry<String, List<Object>>> dupes_iterator = dupes.entrySet().iterator();
                    while (dupes_iterator.hasNext()) {
                        final Entry<String, List<Object>> entry = dupes_iterator.next();
                        // final String packagename = entry.getKey();
                        final List<Object> thisdupes = entry.getValue();
                        if (thisdupes.size() == 1) {
                            /* We need at least two packages to be able to merge them. */
                            continue;
                        }
                        /* Merge comments of all packages so we don't lose any information. */
                        if (isLinkgrabber) {
                            final List<CrawledPackage> linkgrabberdupes = new ArrayList<CrawledPackage>();
                            for (final Object thisdupe : thisdupes) {
                                linkgrabberdupes.add((CrawledPackage) thisdupe);
                            }
                            final String mergedComments = MergeToPackageAction.mergeCrawledPackageListComments(linkgrabberdupes);
                            /* Pick package to merge the others into */
                            final CrawledPackage target = (CrawledPackage) thisdupes.remove(0);
                            if (!StringUtils.isEmpty(mergedComments)) {
                                target.setComment(mergedComments);
                            }
                            LinkCollector.getInstance().merge(target, linkgrabberdupes, MergePosition.BOTTOM);
                        } else {
                            // TODO
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
