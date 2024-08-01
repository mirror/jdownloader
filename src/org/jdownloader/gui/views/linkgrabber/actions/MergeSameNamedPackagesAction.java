package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.plugins.FilePackage;

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

public class MergeSameNamedPackagesAction<PgkType extends AbstractPackageNode<ChildType, PgkType>, ChildType extends AbstractPackageChildrenNode<PgkType>> extends CustomizableTableContextAppAction implements ActionContext {
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
        final SelectionInfo<PgkType, ChildType> sel = getSelection();
        final PackageController<PgkType, ChildType> controller = sel.getController();
        controller.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final boolean caseInsensitive = isMatchPackageNamesCaseInsensitive();

                /* If user has selected a package, only collect duplicates of name of selected package. */
                final Map<String, AbstractPackageNode> selectedPackagesMap;
                final List<PackageView<PgkType, ChildType>> selPackageViews = sel.getPackageViews();
                if (sel != null && selPackageViews.size() > 0) {
                    selectedPackagesMap = new HashMap<String, AbstractPackageNode>();
                    for (final PackageView<PgkType, ChildType> pv : selPackageViews) {
                        final AbstractPackageNode<?, ?> crawledpackage = pv.getPackage();
                        // final CrawledPackage crawledpackage = pv.getPackage();
                        String compareName = crawledpackage.getName();
                        if (caseInsensitive) {
                            compareName = compareName.toLowerCase(Locale.ENGLISH);
                        }
                        if (selectedPackagesMap.containsKey(compareName)) {
                            /* Item is already contained in map - we want to merge all dupes into first package we find. */
                            continue;
                        }
                        selectedPackagesMap.put(compareName, crawledpackage);
                    }
                    if (selectedPackagesMap.isEmpty()) {
                        /* Do nothing */
                        return null;
                    }
                } else {
                    selectedPackagesMap = null;
                }
                final Map<String, List<PgkType>> dupes = new HashMap<String, List<PgkType>>();
                boolean foundDupes = false;
                final boolean readL = controller.readLock();
                try {
                    for (final PgkType packageNode : controller.getPackages()) {
                        String packagename = packageNode.getName();
                        if (caseInsensitive) {
                            packagename = packagename.toLowerCase(Locale.ENGLISH);
                        }
                        if (selectedPackagesMap != null && !selectedPackagesMap.containsKey(packagename)) {
                            /* Only search dupes for selected package(s) */
                            continue;
                        }
                        List<PgkType> thisdupeslist = dupes.get(packagename);
                        if (thisdupeslist != null) {
                            /* We got at least two packages with the same name */
                            foundDupes = true;
                        } else {
                            thisdupeslist = new ArrayList<PgkType>();
                            dupes.put(packagename, thisdupeslist);
                        }
                        thisdupeslist.add(packageNode);
                    }
                } finally {
                    controller.readUnlock(readL);
                }
                if (!foundDupes) {
                    // TODO: Add logger
                    System.out.println("Failed to find any duplicated packages to merge");
                    return null;
                }
                final Iterator<Entry<String, List<PgkType>>> dupes_iterator = dupes.entrySet().iterator();
                while (dupes_iterator.hasNext()) {
                    final Entry<String, List<PgkType>> entry = dupes_iterator.next();
                    // final String packagename = entry.getKey();
                    final List<PgkType> thisdupes = entry.getValue();
                    if (thisdupes.size() == 1) {
                        /* We need at least two packages to be able to merge them. */
                        continue;
                    }
                    final String mergedComments = org.jdownloader.gui.views.linkgrabber.contextmenu.MergeToPackageAction.mergePackageComments(thisdupes);
                    /* Pick package to merge the others into */
                    final PgkType target = thisdupes.remove(0);
                    if (!StringUtils.isEmpty(mergedComments)) {
                        if (target instanceof CrawledPackage) {
                            ((CrawledPackage) target).setComment(mergedComments);
                        } else {
                            ((FilePackage) target).setComment(mergedComments);
                        }
                    }
                    controller.merge(target, thisdupes, MergePosition.BOTTOM);
                }
                return null;
            }
        });

    }
}
