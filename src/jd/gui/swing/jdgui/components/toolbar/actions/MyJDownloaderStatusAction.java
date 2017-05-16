package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;

import jd.controlling.TaskQueue;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.myjd.MyJDownloaderView;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.MyJDownloaderError;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderStatusAction extends AbstractToolBarAction {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public MyJDownloaderStatusAction() {
        setIconKey(IconKey.ICON_LOGO_MYJDOWNLOADER);
        setEnabled(true);
        setTooltipText(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_title_());
    }

    public void actionPerformed(ActionEvent e) {
        JDGui.getInstance().setContent(MyJDownloaderView.getInstance(), true);
    }

    @Override
    protected String createTooltip() {
        return _GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_title_();
    }

    public static final class NoAPIIconWarnIcon extends BadgeIcon {
        public NoAPIIconWarnIcon(Icon main, Icon badgeIcon, int xOffset, int yOffset) {
            super(main, badgeIcon, xOffset, yOffset);
        }

        protected void idIconCheck(final Entry entry) {
        }
    }

    public class Button extends ExtButton implements MyJDownloaderListener {
        /**
         *
         */
        private static final long     serialVersionUID = 1L;
        private final DelayedRunnable iconDelayer;

        public Button() {
            super(MyJDownloaderStatusAction.this);
            setIcon(new AbstractIcon(getIconKey(), 22));
            setHideActionText(true);
            iconDelayer = new DelayedRunnable(TaskQueue.TIMINGQUEUE, 500, 2000) {
                @Override
                public void delayedrun() {
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            final MyJDownloaderConnectionStatus connectionStatus = MyJDownloaderController.getInstance().getConnectionStatus();
                            final boolean connected = connectionStatus != MyJDownloaderConnectionStatus.UNCONNECTED;
                            if (connected) {
                                switch (connectionStatus) {
                                case PENDING:
                                    final MyJDownloaderError latestError = CFG_MYJD.CFG.getLatestError();
                                    switch (latestError) {
                                    case IO:
                                    case SERVER_DOWN:
                                    case SERVER_OVERLOAD:
                                    case SERVER_MAINTENANCE:
                                    case NO_INTERNET_CONNECTION:
                                        setIcon(new NoAPIIconWarnIcon(new AbstractIcon(getIconKey(), 22), new AbstractIcon(IconKey.ICON_WARNING_RED, 16), 2, 2).crop(24, 24));
                                        break;
                                    default:
                                        setIcon(new NoAPIIconWarnIcon(new AbstractIcon(getIconKey(), 22), new AbstractIcon(IconKey.ICON_WARNING_BLUE, 16), 2, 2).crop(24, 24));
                                        break;
                                    }
                                    break;
                                case CONNECTED:
                                    setIcon(new NoAPIIconWarnIcon(new AbstractIcon(getIconKey(), 22), new AbstractIcon(IconKey.ICON_TRUE, 16), 2, 2).crop(24, 24));
                                    break;
                                case UNCONNECTED:
                                    setIcon(new AbstractIcon(getIconKey(), 22));
                                    break;
                                }
                            } else {
                                setIcon(new AbstractIcon(getIconKey(), 22));
                            }
                        }
                    };
                }
            };
            MyJDownloaderController.getInstance().getEventSender().addListener(this, true);
            iconDelayer.resetAndStart();
        }

        @Override
        public boolean isTooltipWithoutFocusEnabled() {
            return true;
        }

        @Override
        public String getToolTipText() {
            final MyJDownloaderConnectionStatus connectionStatus = MyJDownloaderController.getInstance().getConnectionStatus();
            final int connections = MyJDownloaderController.getInstance().getEstablishedConnections();
            final boolean connected = connectionStatus != MyJDownloaderConnectionStatus.UNCONNECTED;
            if (connected) {
                switch (connectionStatus) {
                case PENDING:
                    final MyJDownloaderError latestError = CFG_MYJD.CFG.getLatestError();
                    switch (latestError) {
                    case IO:
                    case SERVER_DOWN:
                    case SERVER_OVERLOAD:
                    case SERVER_MAINTENANCE:
                    case NO_INTERNET_CONNECTION:
                        return _GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections);
                    default:
                        return _GUI.T.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections);
                    }
                case CONNECTED:
                    return _GUI.T.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections);
                default:
                    break;
                }
            }
            return _GUI.T.MyJDownloaderSettingsPanel_runInEDT_disconnected_();
        }

        @Override
        public void onMyJDownloaderConnectionStatusChanged(final MyJDownloaderConnectionStatus connectionStatus, final int connections) {
            iconDelayer.resetAndStart();
        }
    }

    @Override
    public AbstractButton createButton() {
        return new Button();
    }
}
