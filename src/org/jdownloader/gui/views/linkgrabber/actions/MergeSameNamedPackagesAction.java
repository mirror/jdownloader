package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.dragdrop.MergePosition;
import org.jdownloader.images.BadgeIcon;

import jd.controlling.TaskQueue;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

public class MergeSameNamedPackagesAction extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> implements ActionContext {
    private boolean caseInsensitive = true;

    public static String getTranslationForMatchPackageNamesCaseInsensitive() {
        return "Match names case insensitive";
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
        setSmallIcon(new BadgeIcon("logo/dlc", "autoMerge", 32, 24, 2, 6));
        setName("Merge packages with the same name");
        // setAccelerator(KeyEvent.VK_M);
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
            final Map<String, List<CrawledPackage>> dupes = new HashMap<String, List<CrawledPackage>>();
            final LinkCollector lc = LinkCollector.getInstance();
            final List<CrawledPackage> pckages = lc.getPackages();
            final boolean caseInsensitive = isMatchPackageNamesCaseInsensitive();
            boolean foundDupes = false;
            final SelectionInfo<CrawledPackage, CrawledLink> sel = getSelection();
            /* If user has selected a package, only collect duplicates of name of selected package. */
            final CrawledPackage selectedPackage = sel != null ? sel.getPackage() : null;
            String selectedPackageCompareName = null;
            if (selectedPackage != null) {
                if (caseInsensitive) {
                    selectedPackageCompareName = selectedPackage.getName().toLowerCase(Locale.ENGLISH);
                } else {
                    selectedPackageCompareName = selectedPackage.getName();
                }
            }
            for (final CrawledPackage pckage : pckages) {
                final String packagename;
                if (caseInsensitive) {
                    packagename = pckage.getName().toLowerCase(Locale.ENGLISH);
                } else {
                    packagename = pckage.getName();
                }
                if (selectedPackageCompareName != null && !packagename.equals(selectedPackageCompareName)) {
                    /* Only search dupes for selected package */
                    continue;
                }
                List<CrawledPackage> thisdupeslist = dupes.get(packagename);
                if (thisdupeslist != null) {
                    foundDupes = true;
                } else {
                    thisdupeslist = new ArrayList<CrawledPackage>();
                    dupes.put(packagename, thisdupeslist);
                }
                thisdupeslist.add(pckage);
            }
            if (!foundDupes) {
                return;
            }
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    /* Merge dupes */
                    final Iterator<Entry<String, List<CrawledPackage>>> dupes_iterator = dupes.entrySet().iterator();
                    while (dupes_iterator.hasNext()) {
                        final Entry<String, List<CrawledPackage>> entry = dupes_iterator.next();
                        final List<CrawledPackage> thisdupes = entry.getValue();
                        if (thisdupes.size() == 1) {
                            /* We need at least two packages to be able to merge them. */
                            continue;
                        }
                        /* Decide which pckage to merge the others into */
                        final CrawledPackage target;
                        if (selectedPackage != null) {
                            /* Package selected by user */
                            target = selectedPackage;
                        } else {
                            /* First hit */
                            target = thisdupes.get(0);
                        }
                        final List<CrawledPackage> packagesToMerge = new ArrayList<CrawledPackage>();
                        for (int i = 1; i < thisdupes.size(); i++) {
                            packagesToMerge.add(thisdupes.get(i));
                        }
                        LinkCollector.getInstance().merge(target, null, packagesToMerge, MergePosition.TOP);
                    }
                    return null;
                }
            });
        } catch (final Throwable ignore) {
        }
    }
}
