package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.plugins.DownloadLink;
import jd.plugins.download.DownloadInterface;

import org.appwork.uio.UIOManager;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class EnabledAction extends CustomizableTableContextAppAction {
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
                setSmallIcon(getCheckBoxedIcon("select", true, false));
                setName(_GUI._.EnabledAction_EnabledAction_disable());
                break;
            case ALL_DISABLED:
                setSmallIcon(getCheckBoxedIcon("select", false, true));
                setName(_GUI._.EnabledAction_EnabledAction_enable());
                break;
            case ALL_ENABLED:
                setSmallIcon(getCheckBoxedIcon("select", true, true));
                setName(_GUI._.EnabledAction_EnabledAction_disable());
                break;
            }
        } else {
            setSmallIcon(getCheckBoxedIcon("select", false, true));
            setName(_GUI._.EnabledAction_EnabledAction_empty());
        }
    }

    public EnabledAction() {
        super();
        setSmallIcon(getCheckBoxedIcon("select", true, true));
        setName(_GUI._.EnabledAction_EnabledAction_disable());

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

                    long i = 0;
                    if (DownloadWatchDog.getInstance().isRunning()) {
                        for (Object a : selection.getChildren()) {
                            if (a instanceof DownloadLink) {
                                DownloadLink link = (DownloadLink) a;
                                SingleDownloadController slc = link.getDownloadLinkController();
                                if (slc != null && slc.getDownloadInstance() != null && !link.isResumeable()) {
                                    count++;
                                }
                                DownloadInterface dl = slc.getDownloadInstance();
                                if (dl != null && !slc.getDownloadLink().isResumeable()) {
                                    i += slc.getDownloadLink().getDownloadCurrent();
                                }
                            }
                        }
                    }
                    final long bytesToDelete = i;
                    if (count > 0) {
                        final int finalCount = count;
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                if (bytesToDelete > 0) {
                                    if (JDGui.bugme(WarnLevel.SEVERE)) {
                                        if (!UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.EnableAction_run_msg_(SizeFormatter.formatBytes(bytesToDelete), finalCount), NewTheme.I().getIcon("stop", 32), _GUI._.lit_yes(), _GUI._.lit_no())) { return; }

                                    }
                                } else {
                                    if (JDGui.bugme(WarnLevel.LOW)) {
                                        if (!UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.EnableAction_run_msg_(SizeFormatter.formatBytes(bytesToDelete), finalCount), NewTheme.I().getIcon("stop", 32), _GUI._.lit_yes(), _GUI._.lit_no())) { return; }

                                    }
                                }
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
