//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerEvent;
import jd.controlling.linkchecker.LinkCheckerListener;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerEvent;
import jd.controlling.linkcrawler.LinkCrawlerListener;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ReconnecterEvent;
import jd.controlling.reconnect.ReconnecterListener;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.DownloadLinkWalker;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class StatusBarImpl extends JPanel implements DownloadWatchdogListener {

    private static final long      serialVersionUID = 3676496738341246846L;
    private ReconnectProgress      reconnectIndicator;
    private IconedProcessIndicator linkGrabberIndicator;
    private JLabel                 statusLabel;
    private DelayedRunnable        updateDelayer;

    public StatusBarImpl() {
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        initGUI();
                    }
                };

            }

        });

    }

    private void initGUI() {

        if (LAFOptions.getInstance().isPaintStatusbarTopBorder()) {
            setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().darker()));
        } else {
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, getBackground().darker()));
        }

        statusLabel = new JLabel();
        statusLabel.setEnabled(false);
        reconnectIndicator = new ReconnectProgress();
        // IconedProcessIndicator;
        reconnectIndicator.setTitle(_GUI._.StatusBarImpl_initGUI_reconnect());
        reconnectIndicator.setIndeterminate(false);
        reconnectIndicator.setEnabled(false);
        DownloadWatchDog.getInstance().getEventSender().addListener(this);
        Reconnecter.getInstance().getEventSender().addListener(new ReconnecterListener() {

            @Override
            public void onBeforeReconnect(ReconnecterEvent event) {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        reconnectIndicator.setEnabled(true);
                        reconnectIndicator.setIndeterminate(true);
                    }
                };
            }

            @Override
            public void onAfterReconnect(ReconnecterEvent event) {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        reconnectIndicator.setEnabled(false);
                        reconnectIndicator.setIndeterminate(false);
                    }
                };
            }
        });
        // reconnectIndicator.setToolTipText("<html><img src=\"" +
        // NewTheme.I().getImageUrl("reconnect") +
        // "\"></img>Waiting for new IP - Reconnect in progress</html>");

        linkGrabberIndicator = new IconedProcessIndicator(NewTheme.I().getIcon("linkgrabber", 16));

        linkGrabberIndicator.setTitle(_GUI._.StatusBarImpl_initGUI_linkgrabber());
        linkGrabberIndicator.setDescription(_GUI._.StatusBarImpl_initGUI_linkgrabber_desc_inactive());
        linkGrabberIndicator.setIndeterminate(false);
        linkGrabberIndicator.setEnabled(false);
        linkGrabberIndicator.addMouseListener(new MouseListener() {

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    final JPopupMenu popup = new JPopupMenu();

                    popup.add(new AppAction() {
                        /**
                     * 
                     */
                        private static final long serialVersionUID = -968768342263254431L;

                        {
                            this.setIconKey("cancel");
                            this.setName(_GUI._.StatusBarImpl_initGUI_abort_linkgrabber());
                            this.setEnabled(linkGrabberIndicator.isEnabled());
                        }

                        public void actionPerformed(ActionEvent e) {
                            LinkCollector.getInstance().abort();
                        }

                    });

                    popup.show(linkGrabberIndicator, e.getPoint().x, 0 - popup.getPreferredSize().height);
                }
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
            }
        });
        LinkCrawler.getGlobalEventSender().addListener(new LinkCrawlerListener() {

            public void onLinkCrawlerEvent(LinkCrawlerEvent event) {
                updateLinkGrabberIndicator();
            }

        });
        LinkChecker.getEventSender().addListener(new LinkCheckerListener() {

            public void onLinkCheckerEvent(LinkCheckerEvent event) {
                updateLinkGrabberIndicator();
            }

        });

        // linkGrabberIndicator.setToolTipText("<html><img src=\"" +
        // NewTheme.I().getImageUrl("linkgrabber") +
        // "\"></img>Crawling for Downloads</html>");

        // extractIndicator.setToolTipText("<html><img src=\"" +
        // NewTheme.I().getImageUrl("archive") +
        // "\"></img>Extracting Archives: 85%</html>");

        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        updateDelayer = new DelayedRunnable(ToolTipController.EXECUTER, 1000, 2000) {
            @Override
            public String getID() {
                return "StatusBar";
            }

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        redoLayout();
                    }
                };
            }
        };
        redoLayout();

        // add(extractIndicator, "height 22!,width 22!,hidemode 2");
    }

    private void redoLayout() {

        StringBuilder sb = new StringBuilder();
        sb.append("[left]0[grow,fill]");
        ArrayList<JComponent> lprocessIndicators = new ArrayList<JComponent>(processIndicators);
        for (int i = -2; i < lprocessIndicators.size(); i++) {
            sb.append("1[fill,22!]");
        }
        sb.append("3");
        setLayout(new MigLayout("ins 0", sb.toString(), "[fill,22!]"));

        super.removeAll();
        JScrollPane p = new JScrollPane(ServicePanel.getInstance());
        p.setBorder(null);
        p.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        p.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        super.add(p);
        super.add(statusLabel, "height 22!,gapright 10!");

        super.add(reconnectIndicator, "");

        super.add(linkGrabberIndicator, "");
        for (JComponent c : lprocessIndicators) {
            super.add(c, "");
        }
        repaint();
        revalidate();

    }

    public void removeProcessIndicator(JComponent icon) {
        if (processIndicators.remove(icon)) {
            updateDelayer.resetAndStart();
        }
    }

    public void addProcessIndicator(JComponent icon) {
        if (processIndicators.add(icon)) {
            updateDelayer.resetAndStart();
        }
    }

    public IconedProcessIndicator getLinkGrabberIndicator() {
        return linkGrabberIndicator;
    }

    private CopyOnWriteArraySet<JComponent> processIndicators = new CopyOnWriteArraySet<JComponent>();
    private IconedProcessIndicator          downloadWatchdogIndicator;

    public void remove(Component comp) {
        throw new WTFException("Use #removeProcessIndicator");
    }

    public void add(Component comp, Object constraints) {
        throw new WTFException("Use #addProcessIndicator");
    }

    public Component add(Component comp) {
        throw new WTFException("Use #addProcessIndicator");
    }

    private void updateLinkGrabberIndicator() {
        final boolean enabled = LinkChecker.isChecking() || LinkCrawler.isCrawling();
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                linkGrabberIndicator.setEnabled(enabled);
                linkGrabberIndicator.setIndeterminate(enabled);
                if (enabled) {
                    linkGrabberIndicator.setDescription(_GUI._.StatusBarImpl_initGUI_linkgrabber_desc());
                } else {
                    linkGrabberIndicator.setDescription(_GUI._.StatusBarImpl_initGUI_linkgrabber_desc_inactive());
                }
            }
        };
    }

    private JComponent lazyGetDownloadWatchdogIndicator() {
        if (downloadWatchdogIndicator != null) return downloadWatchdogIndicator;
        downloadWatchdogIndicator = new EDTHelper<IconedProcessIndicator>() {
            @Override
            public IconedProcessIndicator edtRun() {
                if (downloadWatchdogIndicator != null) return downloadWatchdogIndicator;
                IconedProcessIndicator ldownloadWatchdogIndicator = new IconedProcessIndicator(NewTheme.I().getIcon("skipped", 16));

                ldownloadWatchdogIndicator.setTitle(_GUI._.StatusBarImpl_skippedLinksMarker_title());

                ldownloadWatchdogIndicator.setIndeterminate(false);
                ldownloadWatchdogIndicator.setEnabled(true);
                ldownloadWatchdogIndicator.setValue(100);
                ldownloadWatchdogIndicator.addMouseListener(new MouseListener() {

                    public void mouseReleased(MouseEvent e) {
                        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                            @Override
                            protected Void run() throws RuntimeException {
                                DownloadController.getInstance().set(new DownloadLinkWalker() {

                                    @Override
                                    public void handle(DownloadLink link) {
                                        link.setSkipReason(null);
                                    }

                                    @Override
                                    public boolean accept(FilePackage fp) {
                                        return true;
                                    }

                                    @Override
                                    public boolean accept(DownloadLink link) {
                                        return true;
                                    }
                                });
                                return null;
                            }
                        });
                    }

                    public void mousePressed(MouseEvent e) {
                    }

                    public void mouseExited(MouseEvent e) {
                    }

                    public void mouseEntered(MouseEvent e) {
                    }

                    public void mouseClicked(MouseEvent e) {
                    }
                });
                return ldownloadWatchdogIndicator;
            }
        }.getReturnValue();

        return downloadWatchdogIndicator;
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
        if (DownloadWatchDog.getInstance().getSession().getSkipCounter() > 0) {
            if (!hasProcessIndicator(lazyGetDownloadWatchdogIndicator())) {
                addProcessIndicator(lazyGetDownloadWatchdogIndicator());
            }
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    if (downloadWatchdogIndicator != null) downloadWatchdogIndicator.setDescription(_GUI._.StatusBarImpl_skippedLinksMarker_desc(DownloadWatchDog.getInstance().getSession().getSkipCounter()));
                }
            };
        } else if (DownloadWatchDog.getInstance().getSession().getSkipCounter() <= 0) {
            if (downloadWatchdogIndicator != null) {
                removeProcessIndicator(lazyGetDownloadWatchdogIndicator());
            }
        }
    }

    private boolean hasProcessIndicator(JComponent icon) {
        return processIndicators.contains(icon);
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {

    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        if (downloadWatchdogIndicator != null) {
            removeProcessIndicator(lazyGetDownloadWatchdogIndicator());
        }
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController) {
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController) {
    }

}