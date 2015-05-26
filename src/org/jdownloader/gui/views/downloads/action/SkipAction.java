package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.uio.UIOManager;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.SkipReason;

public class SkipAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    enum State {
        ALL_SKIPPED,
        ALL_UNSKIPPED,
        MIXED;
    }

    private State                                    state            = State.MIXED;

    private SelectionInfo<FilePackage, DownloadLink> selection        = null;

    private static final long                        serialVersionUID = 7107840091963427544L;

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        selection = getSelection();
        if (hasSelection(selection)) {
            switch (state = getState()) {
            case MIXED:
                setSmallIcon(TRUE_TRUE);
                setName(NAME_UNSKIP);
                break;
            case ALL_UNSKIPPED:
                setSmallIcon(FALSE_TRUE);
                setName(NAME_SKIP);
                break;
            case ALL_SKIPPED:
                setSmallIcon(TRUE_TRUE);
                setName(NAME_UNSKIP);
                break;
            }
        } else {
            setSmallIcon(FALSE_TRUE);
            setName(NAME_SKIP);
        }
    }

    private final static String NAME_UNSKIP = _GUI._.ForceDownloadAction_UnskipDownloadAction();
    private final static String NAME_SKIP   = _GUI._.ForceDownloadAction_SkipDownloadAction();
    private final static Icon   TRUE_TRUE   = getCheckBoxedIcon("skipped", true, true);
    private final static Icon   FALSE_TRUE  = getCheckBoxedIcon("skipped", false, true);

    public SkipAction() {
        setSmallIcon(TRUE_TRUE);
        setName(NAME_UNSKIP);
    }

    private State getState() {
        if (!hasSelection(selection)) {
            return State.ALL_UNSKIPPED;
        }
        Boolean first = null;
        for (DownloadLink a : selection.getChildren()) {

            /* check a child */
            if (first == null) {
                first = a.isSkipped();
            }
            if (a.isSkipped() != first) {
                return State.MIXED;
            }

        }
        return first ? State.ALL_SKIPPED : State.ALL_UNSKIPPED;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        final SelectionInfo<FilePackage, DownloadLink> lSelection = getSelection();
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                boolean enable = state.equals(State.ALL_UNSKIPPED);
                if (enable) {
                    int count = 0;
                    if (DownloadWatchDog.getInstance().isRunning()) {
                        for (DownloadLink a : lSelection.getChildren()) {
                            DownloadLink link = a;
                            SingleDownloadController slc = link.getDownloadLinkController();
                            if (slc != null && slc.getDownloadInstance() != null && !link.isResumeable()) {
                                count++;
                            }
                        }
                    }
                    if (count > 0 && DownloadWatchDog.getInstance().getNonResumableBytes(lSelection) > 0) {
                        if (JDGui.bugme(WarnLevel.SEVERE)) {
                            if (!UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.SkipAction_run_msg_(SizeFormatter.formatBytes(DownloadWatchDog.getInstance().getNonResumableBytes(selection)), count), NewTheme.I().getIcon("skipped", 32), _GUI._.lit_yes(), _GUI._.lit_no())) {
                                return null;
                            }

                        }
                    }
                }
                List<DownloadLink> unSkip = new ArrayList<DownloadLink>();
                for (DownloadLink a : lSelection.getChildren()) {
                    // keep skipreason if a reason is set
                    if (enable) {
                        if (!a.isSkipped()) {
                            a.setSkipReason(SkipReason.MANUAL);
                        }
                    } else {
                        unSkip.add(a);
                    }
                }
                DownloadWatchDog.getInstance().unSkip(unSkip);
                return null;
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && DownloadWatchDog.getInstance().isRunning();
    }

}
