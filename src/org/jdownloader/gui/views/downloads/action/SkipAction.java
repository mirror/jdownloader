package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.SkipReason;

public class SkipAction extends AppAction {

    enum State {
        ALL_SKIPPED,
        ALL_UNSKIPPED,
        MIXED;
    }

    private State                                          state            = State.MIXED;

    private static final long                              serialVersionUID = 7107840091963427544L;

    private final SelectionInfo<FilePackage, DownloadLink> si;

    public SkipAction(final SelectionInfo<FilePackage, DownloadLink> si) {
        this.si = si;
        if (si != null) {
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

    private State getState() {
        if (si.isEmpty()) return State.ALL_UNSKIPPED;
        Boolean first = null;
        for (DownloadLink a : si.getChildren()) {

            /* check a child */
            if (first == null) first = a.isSkipped();
            if (a.isSkipped() != first) { return State.MIXED; }

        }
        return first ? State.ALL_SKIPPED : State.ALL_UNSKIPPED;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
                boolean enable = state.equals(State.ALL_UNSKIPPED);
                for (DownloadLink a : si.getChildren()) {
                    // keep skipreason if a reason is set
                    if (enable) {
                        if (!a.isSkipped()) {
                            a.setSkipReason(SkipReason.MANUAL);
                        }
                    } else {
                        a.setSkipReason(SkipReason.MANUAL);
                    }

                }
            }
        }, true);
    }

    @Override
    public boolean isEnabled() {
        return si != null && !si.isEmpty() && DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE);
    }

}
