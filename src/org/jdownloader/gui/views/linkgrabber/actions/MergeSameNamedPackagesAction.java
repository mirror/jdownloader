package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.controlling.packagecontroller.PackageController.MergePackageSettings;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;

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
                /* If user has selected package(s), only collect duplicates within selection. */
                final List<PackageView<PgkType, ChildType>> selPackageViews = sel.getPackageViews();
                final Map<String, List<PgkType>> dupes;
                if (sel != null && selPackageViews.size() > 0) {
                    /* Merge duplicates withing users' selection */
                    final List<PgkType> selectedPackages = new ArrayList<PgkType>();
                    for (final PackageView<PgkType, ChildType> pv : selPackageViews) {
                        final PgkType selectedpackage = pv.getPackage();
                        selectedPackages.add(selectedpackage);
                    }
                    if (selectedPackages.isEmpty()) {
                        /* User has only selected items we can't work with -> Do nothing */
                        return null;
                    }
                    dupes = controller.getPackagesWithSameName(selectedPackages, caseInsensitive);
                } else {
                    /* Merge duplicates in whole list */
                    dupes = controller.getPackagesWithSameName(caseInsensitive);
                }
                if (dupes.isEmpty()) {
                    /* Zero results -> Do nothing */
                    return null;
                }
                final Iterator<Entry<String, List<PgkType>>> dupes_iterator = dupes.entrySet().iterator();
                while (dupes_iterator.hasNext()) {
                    final Entry<String, List<PgkType>> entry = dupes_iterator.next();
                    final List<PgkType> thisdupes = entry.getValue();
                    if (thisdupes.size() == 1) {
                        /* We need at least two packages to be able to merge them. */
                        continue;
                    }
                    /* Pick package to merge the others into */
                    final PgkType target = thisdupes.remove(0);
                    final MergePackageSettings mergesettings = new MergePackageSettings();
                    mergesettings.setMergePackageComments(true);
                    controller.merge(target, thisdupes, mergesettings);
                }
                return null;
            }
        });
    }

    @Override
    public boolean isEnabled() {
        final SelectionInfo<PgkType, ChildType> sel = getSelection();
        if (sel == null) {
            /* This shall never happen. */
            return false;
        }
        final PackageController<PgkType, ChildType> controller = sel.getController();
        if (controller == null || controller.getPackages() == null || controller.getPackages().size() == 0) {
            /* Zero items in linkgrabberlist/downloadlist. */
            return false;
        } else {
            return super.isEnabled();
        }
    }
}
