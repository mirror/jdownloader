package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.ImageIcon;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.gui.swing.jdgui.interfaces.View;
import jd.plugins.DownloadLink;
import jd.plugins.download.DownloadInterface;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.AbstractIcon;

public class EnabledAction extends CustomizableTableContextAppAction implements GUIListener, ActionContext {
    /**
     *
     */
    private static final long serialVersionUID = -1733286276459073749L;

    public static enum EnableActionMode implements LabelInterface {
        ENABLE() {
            @Override
            public String getLabel() {
                return _GUI.T.EnabledAction_EnabledAction_enable();
            }
        },
        DISABLE() {
            @Override
            public String getLabel() {
                return _GUI.T.EnabledAction_EnabledAction_disable();
            }
        },
        AUTO();
        @Override
        public String getLabel() {
            return this.name();
        }
    }

    public static enum State {
        ALL_ENABLED(false, getCheckBoxedIcon("select", true, true)),
        ALL_DISABLED(true, getCheckBoxedIcon("select", false, true)),
        MIXED_ENABLE(true, getCheckBoxedIcon("select", true, false)),
        MIXED_DISABLE(false, getCheckBoxedIcon("select", false, false));
        private final ImageIcon icon;
        private final boolean   enable;

        private State(boolean enable, ImageIcon icon) {
            this.icon = icon;
            this.enable = enable;
        }
    }

    private State state = State.MIXED_ENABLE;

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        updateStateAndLabelAndIcon();
    }

    private void updateStateAndLabelAndIcon() {
        final SelectionInfo<?, ?> selectionInfo = getSelection();
        if (selectionInfo != null) {
            // final EnableActionMode mode = getMode();
            switch (state = getState(selectionInfo)) {
            case MIXED_DISABLE:
                setSmallIcon(state.icon);
                setName(_GUI.T.EnabledAction_EnabledAction_disable());
                // setEnabled(true);
                break;
            case MIXED_ENABLE:
                setSmallIcon(state.icon);
                setName(_GUI.T.EnabledAction_EnabledAction_enable());
                // setEnabled(true);
                break;
            case ALL_DISABLED:
                setSmallIcon(state.icon);
                setName(_GUI.T.EnabledAction_EnabledAction_enable());
                // setEnabled(!EnableActionMode.DISABLE.equals(mode));
                break;
            case ALL_ENABLED:
                setSmallIcon(state.icon);
                setName(_GUI.T.EnabledAction_EnabledAction_disable());
                // setEnabled(!EnableActionMode.ENABLE.equals(mode));
                break;
            }
        } else {
            setSmallIcon(State.MIXED_ENABLE.icon);
            setName(_GUI.T.EnabledAction_EnabledAction_empty());
            // setEnabled(true);
        }
    }

    private boolean          metaCtrl = false;
    private EnableActionMode mode     = EnableActionMode.AUTO;

    public static String getTranslationForMode() {
        return _GUI.T.EnabledAction_EnabledAction_mode();
    }

    @Customizer(link = "#getTranslationForMode")
    public EnableActionMode getMode() {
        return mode;
    }

    public void setMode(EnableActionMode mode) {
        if (mode == null) {
            this.mode = EnableActionMode.AUTO;
        } else {
            this.mode = mode;
        }
    }

    public EnabledAction() {
        super();
        setSmallIcon(getCheckBoxedIcon("select", true, true));
        setName(_GUI.T.EnabledAction_EnabledAction_disable());
        GUIEventSender.getInstance().addListener(this, true);
        metaCtrl = KeyObserver.getInstance().isMetaDown(true) || KeyObserver.getInstance().isControlDown(true);
    }

    @Override
    public void onKeyModifier(int parameter) {
        final boolean before = metaCtrl;
        if (KeyObserver.getInstance().isControlDown(false) || KeyObserver.getInstance().isMetaDown(false)) {
            metaCtrl = true;
        } else {
            metaCtrl = false;
        }
        if (before != metaCtrl) {
            updateStateAndLabelAndIcon();
        }
    }

    private State getState(final SelectionInfo<?, ?> selection) {
        switch (getMode()) {
        default:
        case AUTO:
            if (selection.isEmpty()) {
                return State.ALL_DISABLED;
            }
            Boolean first = null;
            final List<?> children = selection.getChildren();
            for (Object a : children) {
                AbstractNode node = (AbstractNode) a;
                if (first == null) {
                    first = node.isEnabled();
                } else if (node.isEnabled() != first) {
                    if (selection.getRawContext() != null) {
                        node = selection.getRawContext();
                        if (metaCtrl) {
                            return node.isEnabled() ? State.MIXED_DISABLE : State.MIXED_ENABLE;
                        } else {
                            return node.isEnabled() ? State.MIXED_ENABLE : State.MIXED_DISABLE;
                        }
                    } else {
                        break;
                    }
                }
            }
            return first ? State.ALL_ENABLED : State.ALL_DISABLED;
        case DISABLE:
            return metaCtrl ? State.ALL_DISABLED : State.ALL_ENABLED;
        case ENABLE:
            return metaCtrl ? State.ALL_ENABLED : State.ALL_DISABLED;
        }
    }

    public void actionPerformed(ActionEvent e) {
        final SelectionInfo<?, ?> lSelection = getSelection();
        if (!isEnabled() && hasSelection(lSelection)) {
            return;
        }
        final State lState = state;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                final boolean enable = lState.enable;
                if (!enable) {
                    int count = 0;
                    long i = 0;
                    if (DownloadWatchDog.getInstance().isRunning()) {
                        for (Object a : lSelection.getChildren()) {
                            if (a instanceof DownloadLink) {
                                DownloadLink link = (DownloadLink) a;
                                SingleDownloadController slc = link.getDownloadLinkController();
                                if (slc != null && slc.getDownloadInstance() != null && !link.isResumeable()) {
                                    count++;
                                    DownloadInterface dl = slc.getDownloadInstance();
                                    if (dl != null && !slc.getDownloadLink().isResumeable()) {
                                        i += slc.getDownloadLink().getView().getBytesLoaded();
                                    }
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
                                        if (!UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.lit_are_you_sure(), _GUI.T.EnableAction_run_msg_(SizeFormatter.formatBytes(bytesToDelete), finalCount), new AbstractIcon(IconKey.ICON_STOP, 32), _GUI.T.lit_yes(), _GUI.T.lit_no())) {
                                            return;
                                        }
                                    }
                                } else {
                                    if (JDGui.bugme(WarnLevel.LOW)) {
                                        if (!UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.lit_are_you_sure(), _GUI.T.EnableAction_run_msg_(SizeFormatter.formatBytes(bytesToDelete), finalCount), new AbstractIcon(IconKey.ICON_STOP, 32), _GUI.T.lit_yes(), _GUI.T.lit_no())) {
                                            return;
                                        }
                                    }
                                }
                                setEnabled(enable, lSelection.getChildren());
                            }
                        };
                        return null;
                    }
                }
                setEnabled(enable, lSelection.getChildren());
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

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
    }
}
