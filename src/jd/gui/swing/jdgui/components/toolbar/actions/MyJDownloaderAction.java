package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.MyJDownloaderError;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderListener;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderAction extends AbstractToolBarAction {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public MyJDownloaderAction() {
        setIconKey("myjdownloader");
        setEnabled(true);
        setTooltipText(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_title_());
    }

    public void actionPerformed(ActionEvent e) {
        ConfigurationView.getInstance().setSelectedSubPanel(MyJDownloaderSettingsPanel.class);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
    }

    @Override
    protected String createTooltip() {
        return _GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_title_();
    }

    public class Button extends ExtButton implements MyJDownloaderListener {
        /**
         *
         */
        private static final long     serialVersionUID  = 1L;
        private final Icon            myJDownloaderIcon = NewTheme.I().getIcon(getIconKey(), 24);
        private final Font            numberFont        = new Font("Arial", 0, 6);
        private final Color           darkerGREEN       = Color.GREEN.darker();
        private final DelayedRunnable iconDelayer;

        public Button() {
            super(MyJDownloaderAction.this);
            setIcon(myJDownloaderIcon);
            setHideActionText(true);
            iconDelayer = new DelayedRunnable(500, 2000) {

                @Override
                public void delayedrun() {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
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
                                    case NO_INTERNET_CONNECTION:
                                        setIcon(new BadgeIcon(myJDownloaderIcon, createNumberColorIcon(connections, Color.YELLOW), 4, 2));
                                        break;
                                    default:
                                        setIcon(new BadgeIcon(myJDownloaderIcon, createNumberColorIcon(connections, darkerGREEN), 4, 2));
                                        break;
                                    }
                                    break;
                                case CONNECTED:
                                    setIcon(new BadgeIcon(myJDownloaderIcon, createNumberColorIcon(connections, Color.GREEN), 4, 2));
                                    break;
                                case UNCONNECTED:
                                    setIcon(myJDownloaderIcon);
                                    break;
                                }
                            } else {
                                setIcon(myJDownloaderIcon);
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
                    case NO_INTERNET_CONNECTION:
                        return _GUI._.MyJDownloaderSettingsPanel_runInEDT_connections(connections);
                    default:
                        return _GUI._.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI._.MyJDownloaderSettingsPanel_runInEDT_connections(connections);
                    }
                case CONNECTED:
                    return _GUI._.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI._.MyJDownloaderSettingsPanel_runInEDT_connections(connections);
                default:
                    break;
                }
            }
            return _GUI._.MyJDownloaderSettingsPanel_runInEDT_disconnected_();

        }

        private Icon createNumberColorIcon(int connections, Color backgroundColor) {
            final int w = 16;
            final int h = 8;
            final Color foregroundColor = Color.BLACK;
            final String string = Integer.toString(connections);
            final BufferedImage image = new BufferedImage(w, h, Transparency.TRANSLUCENT);
            final Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 5, 5);
            g.setColor(backgroundColor);
            g.fill(roundedRectangle);
            g.setColor(backgroundColor.darker());
            g.draw(roundedRectangle);
            g.setColor(foregroundColor);
            final Rectangle2D bounds = g.getFontMetrics().getStringBounds(string, g);
            g.setFont(numberFont);
            g.drawString(string, (int) (w - bounds.getWidth()) - 2, (h) - 2);
            g.dispose();
            return new ImageIcon(image);
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
