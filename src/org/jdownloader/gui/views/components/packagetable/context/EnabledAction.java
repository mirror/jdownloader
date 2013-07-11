package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.download.DownloadInterface;

import org.appwork.uio.UIOManager;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class EnabledAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends SelectionAppAction<PackageType, ChildrenType> {
    /**
     * 
     */
    private static final long serialVersionUID = -1733286276459073749L;

    enum State {
        ALL_ENABLED,
        ALL_DISABLED,
        MIXED;
    }

    private State state = State.MIXED;

    @Override
    public void setSelection(SelectionInfo<PackageType, ChildrenType> selection) {
        super.setSelection(selection);
        if (getSelection() != null) {

            switch (state = getState()) {
            case MIXED:
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("select", 18), NewTheme.I().getImage("checkbox_undefined", 14), 0, 0, 9, 8)));
                setName(_GUI._.EnabledAction_EnabledAction_disable());
                break;
            case ALL_DISABLED:
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("select", 18), NewTheme.I().getImage("disabled", 14), 0, 0, 9, 8)));
                setName(_GUI._.EnabledAction_EnabledAction_enable());
                break;
            case ALL_ENABLED:
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("select", 18), NewTheme.I().getImage("enabled", 14), 0, 0, 9, 8)));
                setName(_GUI._.EnabledAction_EnabledAction_disable());
                break;
            }
        } else {
            setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("select", 18), NewTheme.I().getImage("disabled", 14), 0, 0, 9, 8)));
            setName(_GUI._.EnabledAction_EnabledAction_empty());
        }

    }

    public EnabledAction(SelectionInfo<PackageType, ChildrenType> si) {
        super(si);

    }

    private State getState() {
        if (getSelection().isEmpty()) return State.ALL_DISABLED;
        Boolean first = null;
        for (ChildrenType a : getSelection().getChildren()) {

            /* check a child */
            if (first == null) first = a.isEnabled();
            if (a.isEnabled() != first) { return State.MIXED; }

        }
        return first ? State.ALL_ENABLED : State.ALL_DISABLED;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
                boolean enable = state.equals(State.ALL_DISABLED);

                if (!enable) {
                    int count = 0;
                    if (DownloadWatchDog.getInstance().isRunning()) {
                        for (ChildrenType a : getSelection().getChildren()) {
                            if (a instanceof DownloadLink) {
                                DownloadLink link = (DownloadLink) a;
                                if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                                    DownloadInterface dl = link.getDownloadInstance();
                                    if (dl != null && !dl.isResumable()) {
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                    if (count > 0) {
                        if (!UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.EnableAction_run_msg_(SizeFormatter.formatBytes(DownloadWatchDog.getInstance().getNonResumableBytes()), count), NewTheme.I().getIcon("stop", 32), _GUI._.lit_yes(), _GUI._.lit_no())) {

                        return; }
                    }

                }
                for (ChildrenType a : getSelection().getChildren()) {
                    a.setEnabled(enable);
                }
            }
        }, true);
    }

}
