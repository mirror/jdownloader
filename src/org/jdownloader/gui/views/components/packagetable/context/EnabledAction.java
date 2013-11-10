package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.ImageIcon;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.uio.UIOManager;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class EnabledAction extends CustomizableSelectionAppAction {
    /**
     * 
     */
    private static final long serialVersionUID = -1733286276459073749L;

    enum State {
        ALL_ENABLED,
        ALL_DISABLED,
        MIXED;
    }

    private State               state = State.MIXED;
    private SelectionInfo<?, ?> selection;

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        selection = getSelection();
        if (selection != null) {

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

    public EnabledAction() {
        super();

    }

    private State getState() {
        if (getSelection().isEmpty()) return State.ALL_DISABLED;
        Boolean first = null;
        for (Object a : selection.getChildren()) {
            AbstractNode node = (AbstractNode) a;
            /*
             * c1 heck a child
             */
            if (first == null) first = node.isEnabled();
            if (node.isEnabled() != first) { return State.MIXED; }

        }
        return first ? State.ALL_ENABLED : State.ALL_DISABLED;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final boolean enable = state.equals(State.ALL_DISABLED);
                if (!enable) {
                    int count = 0;
                    if (DownloadWatchDog.getInstance().isRunning()) {
                        for (Object a : selection.getChildren()) {
                            if (a instanceof DownloadLink) {
                                DownloadLink link = (DownloadLink) a;
                                SingleDownloadController slc = link.getDownloadLinkController();
                                if (slc != null && slc.getDownloadInstance() != null && !link.isResumeable()) {
                                    count++;
                                }
                            }
                        }
                    }
                    if (count > 0) {
                        final int finalCount = count;
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                if (!UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.EnableAction_run_msg_(SizeFormatter.formatBytes(DownloadWatchDog.getInstance().getNonResumableBytes()), finalCount), NewTheme.I().getIcon("stop", 32), _GUI._.lit_yes(), _GUI._.lit_no())) { return; }
                                setEnabled(enable, getSelection().getChildren());
                            }
                        };
                        return null;
                    }
                }
                setEnabled(enable, getSelection().getChildren());
                return null;
            }
        });
    }

    private void setEnabled(final boolean b, final List<?> aggregatedLinkList) {
        if (aggregatedLinkList != null && aggregatedLinkList.size() > 0) {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    for (Object a : aggregatedLinkList) {
                        ((AbstractNode) a).setEnabled(b);
                    }
                    return null;
                }
            });
        }
    }

}
