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
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerEvent;
import jd.controlling.linkchecker.LinkCheckerListener;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerEvent;
import jd.controlling.linkcrawler.LinkCrawlerListener;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.swing.jdgui.components.premiumbar.PremiumStatus;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class StatusBarImpl extends JPanel implements StateEventListener {

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

        if (LookAndFeelController.getInstance().getLAFOptions().isPaintStatusbarTopBorder()) {
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
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                Reconnecter.getInstance().getStateMachine().addListener(new StateEventListener() {

                    public void onStateChange(StateEvent event) {
                        boolean r = false;
                        if (event.getNewState() == Reconnecter.RECONNECT_RUNNING) {
                            r = true;
                        }
                        final boolean running = r;
                        new EDTRunner() {
                            @Override
                            protected void runInEDT() {
                                reconnectIndicator.setEnabled(running);
                                reconnectIndicator.setIndeterminate(running);
                            }
                        };
                    }

                    public void onStateUpdate(StateEvent event) {
                    }

                });
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
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                LinkCrawler.getEventSender().addListener(new LinkCrawlerListener() {

                    public void onLinkCrawlerEvent(LinkCrawlerEvent event) {
                        updateLinkGrabberIndicator();
                    }

                });
                LinkChecker.getEventSender().addListener(new LinkCheckerListener() {

                    public void onLinkCheckerEvent(LinkCheckerEvent event) {
                        updateLinkGrabberIndicator();
                    }

                });

            }

        });

        // linkGrabberIndicator.setToolTipText("<html><img src=\"" +
        // NewTheme.I().getImageUrl("linkgrabber") +
        // "\"></img>Crawling for Downloads</html>");

        // extractIndicator.setToolTipText("<html><img src=\"" +
        // NewTheme.I().getImageUrl("archive") +
        // "\"></img>Extracting Archives: 85%</html>");

        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        redoLayout();

        updateDelayer = new DelayedRunnable(ToolTipController.EXECUTER, 1000, 2000) {

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
        // add(extractIndicator, "height 22!,width 22!,hidemode 2");
    }

    private void redoLayout() {

        StringBuilder sb = new StringBuilder();
        sb.append("[left]0[grow,fill]");
        for (int i = -2; i < processIndicators.size(); i++) {
            sb.append("1[fill,22!]");
        }
        sb.append("3");
        setLayout(new MigLayout("ins 0", sb.toString(), "[fill,22!]"));

        super.removeAll();
        super.add(PremiumStatus.getInstance());
        super.add(statusLabel, "height 22!,gapright 10!");

        super.add(reconnectIndicator, "");

        super.add(linkGrabberIndicator, "");
        for (JComponent c : processIndicators) {
            super.add(c, "");
        }
        repaint();
        revalidate();

    }

    public void removeProcessIndicator(JComponent icon) {
        processIndicators.remove(icon);
        updateDelayer.resetAndStart();
    }

    public void addProcessIndicator(JComponent icon) {
        processIndicators.add(icon);
        updateDelayer.resetAndStart();
    }

    public IconedProcessIndicator getLinkGrabberIndicator() {
        return linkGrabberIndicator;
    }

    private ArrayList<JComponent>  processIndicators = new ArrayList<JComponent>();
    private IconedProcessIndicator downloadWatchdogIndicator;
    private HashSet<String>        captchaBlockedHoster;

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

    // public void updateDownloadwatchdogCaptchaIndicator(HashSet<String> captchaBlockedHoster) {
    // // reconnectIndicator = new ReconnectProgress();
    // // // IconedProcessIndicator;
    // // reconnectIndicator.setTitle(_GUI._.StatusBarImpl_initGUI_reconnect());
    // // reconnectIndicator.setIndeterminate(false);
    // // reconnectIndicator.setEnabled(false);
    // this.captchaBlockedHoster = new HashSet<String>(captchaBlockedHoster);
    // if (captchaBlockedHoster.size() == 0 && downloadWatchdogIndicator != null) {
    // removeProcessIndicator(lazyGetDownloadWatchdogIndicator());
    // } else if (captchaBlockedHoster.size() > 0) {
    // addProcessIndicator(lazyGetDownloadWatchdogIndicator());
    //
    // }
    //
    // ;
    // }

    private JComponent lazyGetDownloadWatchdogIndicator() {
        if (downloadWatchdogIndicator != null) return downloadWatchdogIndicator;

        downloadWatchdogIndicator = new IconedProcessIndicator(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("ocr", 16), NewTheme.I().getImage("download", 16), 0, 0, -2, -2)));

        downloadWatchdogIndicator.setTitle(_GUI._.StatusBarImpl_lazyGetDownloadWatchdogIndicator_title());
        downloadWatchdogIndicator.setDescription(_GUI._.StatusBarImpl_lazyGetDownloadWatchdogIndicator_desc());
        downloadWatchdogIndicator.setIndeterminate(false);
        downloadWatchdogIndicator.setEnabled(true);
        downloadWatchdogIndicator.setValue(100);
        downloadWatchdogIndicator.addMouseListener(new MouseListener() {

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    final JPopupMenu popup = new JPopupMenu();
                    // if (captchaBlockedHoster!=null&&captchaBlockedHoster.size()>0){
                    popup.add(new AppAction() {
                        /**
                         * 
                         */
                        private static final long serialVersionUID = -968768342263254431L;

                        {
                            this.setIconKey("ocr");
                            this.setName(_GUI._.StatusBarImpl_lazyGetDownloadWatchdogIndicator_release());

                        }

                        public void actionPerformed(ActionEvent e) {
                            // DownloadWatchDog.getInstance().setCaptchaAllowed(null, CAPTCHA.OK);
                        }

                    });

                    popup.show(downloadWatchdogIndicator, e.getPoint().x, 0 - popup.getPreferredSize().height);
                    // }

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

        return downloadWatchdogIndicator;
    }

    @Override
    public void onStateChange(StateEvent event) {
        if (!DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE)) {
            if (downloadWatchdogIndicator != null) {
                removeProcessIndicator(lazyGetDownloadWatchdogIndicator());
            }
        }

    }

    @Override
    public void onStateUpdate(StateEvent event) {
    }

}