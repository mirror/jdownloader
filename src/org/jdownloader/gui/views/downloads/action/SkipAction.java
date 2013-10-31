package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.uio.UIOManager;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.SkipReason;

public class SkipAction extends AbstractSelectionContextAction<FilePackage, DownloadLink> {

    enum State {
        ALL_SKIPPED,
        ALL_UNSKIPPED,
        MIXED;
    }

    private State             state            = State.MIXED;

    private static final long serialVersionUID = 7107840091963427544L;

    @Override
    public void setSelection(SelectionInfo<FilePackage, DownloadLink> selection) {
        super.setSelection(selection);
        if (getSelection() != null) {
            switch (state = getState()) {
            case MIXED:
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("skipped", 18), NewTheme.I().getImage("checkbox_undefined", 14), 0, 0, 9, 8)));
                setName(_GUI._.ForceDownloadAction_UnskipDownloadAction());
                break;
            case ALL_UNSKIPPED:
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("skipped", 18), NewTheme.I().getImage("disabled", 14), 0, 0, 9, 8)));
                setName(_GUI._.ForceDownloadAction_SkipDownloadAction());
                break;
            case ALL_SKIPPED:
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("skipped", 18), NewTheme.I().getImage("enabled", 14), 0, 0, 9, 8)));
                setName(_GUI._.ForceDownloadAction_UnskipDownloadAction());
                break;
            }
        } else {
            setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("skipped", 18), NewTheme.I().getImage("disabled", 14), 0, 0, 9, 8)));
            setName(_GUI._.ForceDownloadAction_SkipDownloadAction());
        }
    }

    public SkipAction(final SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);

    }

    private State getState() {
        if (getSelection().isEmpty()) return State.ALL_UNSKIPPED;
        Boolean first = null;
        for (DownloadLink a : getSelection().getChildren()) {

            /* check a child */
            if (first == null) first = a.isSkipped();
            if (a.isSkipped() != first) { return State.MIXED; }

        }
        return first ? State.ALL_SKIPPED : State.ALL_UNSKIPPED;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                boolean enable = state.equals(State.ALL_UNSKIPPED);
                if (enable) {
                    int count = 0;
                    if (DownloadWatchDog.getInstance().isRunning()) {
                        for (DownloadLink a : getSelection().getChildren()) {
                            DownloadLink link = (DownloadLink) a;
                            SingleDownloadController slc = link.getDownloadLinkController();
                            if (slc != null && slc.getDownloadInstance() != null && !link.isResumeable()) {
                                count++;
                            }
                        }
                    }
                    if (count > 0) {
                        if (!UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.SkipAction_run_msg_(SizeFormatter.formatBytes(DownloadWatchDog.getInstance().getNonResumableBytes()), count), NewTheme.I().getIcon("skipped", 32), _GUI._.lit_yes(), _GUI._.lit_no())) { return null; }
                    }
                }
                for (DownloadLink a : getSelection().getChildren()) {
                    // keep skipreason if a reason is set
                    if (enable) {
                        if (!a.isSkipped()) {
                            a.setSkipReason(SkipReason.MANUAL);
                        }
                    } else {
                        a.setSkipReason(null);
                    }
                }
                return null;
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE);
    }

}
