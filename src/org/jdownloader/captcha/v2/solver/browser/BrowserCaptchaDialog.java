//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
package org.jdownloader.captcha.v2.solver.browser;

import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import jd.gui.swing.dialog.AbstractImageCaptchaDialog;
import jd.gui.swing.dialog.DialogType;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.LocationStorage;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.solver.gui.Header;
import org.jdownloader.captcha.v2.solver.service.BrowserSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.premium.PremiumInfoDialog;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

/**
 * This Dialog is used to display a Inputdialog for the captchas
 */
public class BrowserCaptchaDialog extends AbstractDialog<String> {
    private AbstractBrowserChallenge challenge;

    public BrowserCaptchaDialog(int flag, DialogType type, DomainInfo domainInfo, AbstractBrowserChallenge captchaChallenge) {
        super(flag | Dialog.STYLE_HIDE_ICON, _GUI.T.gui_captchaWindow_askForInput(domainInfo.getTld()), null, null, null);
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isCaptchaDialogUniquePositionByHosterEnabled()) {
            setLocator(new RememberAbsoluteDialogLocator("CaptchaDialog_" + domainInfo.getTld()));
        } else {
            setLocator(new RememberAbsoluteDialogLocator("CaptchaDialog"));
        }
        this.hosterInfo = domainInfo;
        this.type = type;
        this.challenge = captchaChallenge;
        setPlugin(captchaChallenge.getPlugin());
    }

    public static FrameState getWindowState() {
        for (Window w : Window.getWindows()) {
            if (WindowManager.getInstance().hasFocus(w)) {
                return FrameState.TO_FRONT_FOCUSED;
            }
        }
        FrameState ret = (FrameState) CFG_GUI.NEW_DIALOG_FRAME_STATE.getValue();
        if (ret == null) {
            ret = FrameState.TO_FRONT;
        }
        switch (ret) {
        case OS_DEFAULT:
        case TO_BACK:
            JDGui.getInstance().flashTaskbar();
        }
        return ret;
    }

    @Override
    protected FrameState getWindowStateOnVisible() {
        return getWindowState();
    }

    @Override
    public void onSetVisible(boolean b) {
        super.onSetVisible(b);
        if (b) {
            AbstractImageCaptchaDialog.playCaptchaSound();
        }
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    private LocationStorage config;
    protected boolean       hideCaptchasForHost    = false;
    protected boolean       hideCaptchasForPackage = false;
    private boolean         hideAllCaptchas;

    public boolean isHideAllCaptchas() {
        return hideAllCaptchas;
    }

    private DomainInfo                                hosterInfo;
    protected AbstractConfigPanel                     iconPanel;
    protected Point                                   offset;
    private Plugin                                    plugin;
    protected boolean                                 stopDownloads    = false;
    private DialogType                                type;
    protected boolean                                 refresh;
    protected boolean                                 stopCrawling;
    protected boolean                                 stopShowingCrawlerCaptchas;
    protected final AtomicReference<BrowserReference> browserReference = new AtomicReference<BrowserReference>(null);
    private volatile String                           responseCode;
    private WindowFocusListener                       openBrowserFocusListener;

    private void createPopup() {
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem mi;
        if (getType() == DialogType.HOSTER) {
            mi = new JMenuItem(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_skip_and_disable_all_downloads_from(getHost()));
                    try {
                        setSmallIcon(getDomainInfo().getIcon(16));
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    hideCaptchasForHost = true;
                    setReturnmask(false);
                    dispose();
                }
            });
            popup.add(mi);
            mi = new JMenuItem(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_skip_and_disable_package(getPackageName()));
                    setSmallIcon(new BadgeIcon(IconKey.ICON_PACKAGE_OPEN, IconKey.ICON_SKIPPED, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    hideCaptchasForPackage = true;
                    setReturnmask(false);
                    dispose();
                }
            });
            popup.add(mi);
            mi = new JMenuItem(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_skip_and_hide_all_captchas_download());
                    setSmallIcon(NewTheme.I().getIcon(IconKey.ICON_CLEAR, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    hideAllCaptchas = true;
                    setReturnmask(false);
                    dispose();
                }
            });
            popup.add(mi);
            mi = new JMenuItem(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_skip_and_stop_all_downloads());
                    setSmallIcon(new AbstractIcon(IconKey.ICON_STOP, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    stopDownloads = true;
                    setReturnmask(false);
                    dispose();
                }
            });
            popup.add(mi);
        } else {
            mi = new JMenuItem(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_cancel_linkgrabbing());
                    setSmallIcon(new AbstractIcon(IconKey.ICON_STOP, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    stopCrawling = true;
                    setReturnmask(false);
                    dispose();
                }
            });
            popup.add(mi);
            mi = new JMenuItem(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_cancel_stop_showing_crawlercaptchs());
                    setSmallIcon(new AbstractIcon(IconKey.ICON_FIND, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    stopShowingCrawlerCaptchas = true;
                    setReturnmask(false);
                    dispose();
                }
            });
            popup.add(mi);
        }
        Insets insets = LAFOptions.getInstance().getExtension().customizePopupBorderInsets();
        Dimension pref = popup.getPreferredSize();
        pref.height = popup.getComponentCount() * 24 + insets.top + insets.bottom;
        popup.setPreferredSize(pref);
        popup.show(cancelButton, +insets.left - pref.width + cancelButton.getWidth() + 8 + 5, +cancelButton.getHeight());
    }

    public boolean isStopCrawling() {
        return stopCrawling;
    }

    public boolean isStopShowingCrawlerCaptchas() {
        return stopShowingCrawlerCaptchas;
    }

    @Override
    protected String createReturnValue() {
        return responseCode;
    }

    public void dispose() {
        try {
            if (!isInitialized()) {
                return;
            }
            try {
                if (dialog != null) {
                    config.setX(getDialog().getWidth());
                    config.setValid(true);
                    config.setY(getDialog().getHeight());
                    if (openBrowserFocusListener != null) {
                        getDialog().removeWindowFocusListener(openBrowserFocusListener);
                    }
                }
            } finally {
                super.dispose();
            }
        } finally {
            final BrowserReference lBrowserReference = browserReference.getAndSet(null);
            if (lBrowserReference != null) {
                lBrowserReference.dispose();
            }
        }
    }

    public String getCrawlerStatus() {
        switch (type) {
        case CRAWLER:
            return ((PluginForDecrypt) plugin).getCrawlerStatusString();
        default:
            return null;
        }
    }

    @Override
    protected DefaultButtonPanel getDefaultButtonPanel() {
        final DefaultButtonPanel ret = new DefaultButtonPanel("ins 0", "[]", "0[grow,fill]0") {
            // refresh button shouldn't be needed, since you f5 in browser, or in flash tool refresh/get a new image
            // @Override
            // public void addOKButton(JButton okButton) {
            // final ExtButton refreshBtn = new ExtButton(new AppAction() {
            // {
            // setSmallIcon(new AbstractIcon(IconKey.ICON_REFRESH, 18));
            // setTooltipText(_GUI.T.CaptchaDialog_layoutDialogContent_refresh());
            // KeyStroke ks = KeyStroke.getKeyStroke(CFG_GUI.CFG.getShortcutForCaptchaDialogRefresh());
            // if (ks == null) {
            // ks = KeyStroke.getKeyStroke("pressed F5");
            // }
            // setAccelerator(ks);
            //
            // }
            //
            // @Override
            // public void actionPerformed(ActionEvent e) {
            // refresh = true;
            // setReturnmask(false);
            // dispose();
            // }
            // }) {
            //
            // @Override
            // public int getTooltipDelay(Point mousePositionOnScreen) {
            // return 500;
            // }
            //
            // };
            // //
            // refreshBtn.setRolloverEffectEnabled(true);
            // super.add(refreshBtn, "alignx right,width 28!");
            // super.addOKButton(okButton);
            //
            // }
            @Override
            public void addCancelButton(final JButton cancelButton) {
                super.addCancelButton(cancelButton);
                final JButton bt = new JButton(new AbstractIcon(IconKey.ICON_POPDOWNSMALL, -1)) {
                    public void setBounds(int x, int y, int width, int height) {
                        int delta = 5;
                        super.setBounds(x - delta, y, width + delta, height);
                    }
                };
                bt.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        createPopup();
                    }
                });
                super.add(bt, "gapleft 0,width 8!");
            }
        };
        ret.setOpaque(false);
        ExtButton premium = new ExtButton(new AppAction() {
            /**
             *
             */
            private static final long serialVersionUID = -3551320196255605774L;
            {
                setName(_GUI.T.CaptchaDialog_getDefaultButtonPanel_premium());
            }

            public void actionPerformed(ActionEvent e) {
                cancel();
                PremiumInfoDialog d = new PremiumInfoDialog(hosterInfo, _GUI.T.PremiumInfoDialog_PremiumInfoDialog_(hosterInfo.getTld()), "CaptchaDialog");
                try {
                    Dialog.getInstance().showDialog(d);
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        });
        premium.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        premium.setRolloverEffectEnabled(true);
        // premium.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
        // premium.getForeground()));
        LazyHostPlugin plg = HostPluginController.getInstance().get(hosterInfo.getTld());
        if (plg != null && plg.isPremium() && CFG_GUI.CFG.isHateCaptchasTextInCaptchaDialogVisible()) {
            ret.add(premium);
        }
        SwingUtils.setOpaque(premium, false);
        return ret;
    }

    public DomainInfo getDomainInfo() {
        return hosterInfo;
    }

    public String getFilename() {
        if (!JsonConfig.create(GeneralSettings.class).isShowFileNameInCaptchaDialogEnabled()) {
            return null;
        }
        switch (type) {
        case HOSTER:
            if (plugin == null || ((PluginForHost) plugin).getDownloadLink() == null) {
                return null;
            }
            return ((PluginForHost) plugin).getDownloadLink().getView().getDisplayName();
        }
        return null;
    }

    public long getFilesize() {
        switch (type) {
        case HOSTER:
            if (plugin == null || ((PluginForHost) plugin).getDownloadLink() == null) {
                return -1;
            }
            return ((PluginForHost) plugin).getDownloadLink().getView().getBytesTotal();
        }
        return -1;
    }

    public String getHost() {
        switch (type) {
        case HOSTER:
            return ((PluginForHost) plugin).getHost(((PluginForHost) plugin).getDownloadLink(), null);
        case CRAWLER:
            return ((PluginForDecrypt) plugin).getHost();
        }
        return null;
    }

    public Point getOffset() {
        return offset;
    }

    protected String getPackageName() {
        switch (type) {
        case HOSTER:
            if (plugin == null || ((PluginForHost) plugin).getDownloadLink() == null) {
                return null;
            }
            return ((PluginForHost) plugin).getDownloadLink().getFilePackage().getName();
        case CRAWLER:
            return null;
        }
        return null;
    }

    // protected int getPreferredHeight() {
    // if (!config.isValid()) {
    // return super.getPreferredHeight();
    // }
    // return config.getY();
    // }
    //
    // @Override
    // protected int getPreferredWidth() {
    // if (!config.isValid()) {
    // return super.getPreferredWidth();
    // }
    // return config.getX();
    // }
    public DialogType getType() {
        return type;
    }

    public boolean isHideCaptchasForHost() {
        return hideCaptchasForHost;
    }

    public boolean isHideCaptchasForPackage() {
        return hideCaptchasForPackage;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    public boolean isStopDownloads() {
        return stopDownloads;
    }

    @Override
    public JComponent layoutDialogContent() {
        // getDialog().setModalityType(ModalityType.MODELESS);
        final LAFOptions lafOptions = LAFOptions.getInstance();
        MigPanel field = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill]");
        SwingUtils.setOpaque(field, false);
        // field.setOpaque(false);
        getDialog().setMinimumSize(new Dimension(0, 0));
        // getDialog().setIconImage(hosterInfo.getFavIcon().getImage());
        final JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[grow,fill]10[]"));
        SwingUtils.setOpaque(panel, false);
        LAFOptions.getInstance().applyBackground(lafOptions.getColorForPanelBackground(), field);
        Header headerPanel = null;
        if (type == DialogType.HOSTER) {
            // setBorder(new JTextField().getBorder());
            headerPanel = new Header("ins 0 0 1 0", "[grow,fill]", "[]");
            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            final String headerText;
            if (getFilename() != null) {
                if (getFilesize() > 0) {
                    headerText = (_GUI.T.CaptchaDialog_layoutDialogContent_header(getFilename(), SizeFormatter.formatBytes(getFilesize()), hosterInfo.getTld()));
                } else {
                    headerText = (_GUI.T.CaptchaDialog_layoutDialogContent_header2(getFilename(), hosterInfo.getTld()));
                }
            } else {
                headerText = null;
            }
            JLabel header = new JLabel() {
                private boolean setting = false;

                protected void paintComponent(Graphics g) {
                    if (headerText != null) {
                        setting = true;
                        try {
                            setText(org.appwork.sunwrapper.sun.swing.SwingUtilities2Wrapper.clipStringIfNecessary(this, this.getFontMetrics(getFont()), headerText, getWidth() - 30));
                        } catch (Throwable e) {
                            // http://www.oracle.com/technetwork/java/faq-sun-packages-142232.html
                            e.printStackTrace();
                            setText(headerText);
                        }
                        setting = false;
                    }
                    super.paintComponent(g);
                }

                // public Dimension getPreferredSize() {
                // return new Dimension(10, 10);
                // }
                public void repaint() {
                    if (setting) {
                        return;
                    }
                    super.repaint();
                }

                public void revalidate() {
                    if (setting) {
                        return;
                    }
                    super.revalidate();
                }
            };
            header.setIcon(hosterInfo.getFavIcon());
            // if (col >= 0) {
            // header.setBackground(new Color(col));
            // header.setOpaque(false);
            // }
            // header.setOpaque(false);
            // header.setLabelMode(true);
            if (getFilename() != null) {
                headerPanel.add(header);
            } else {
                headerPanel = null;
            }
        } else {
            // setBorder(new JTextField().getBorder());
            headerPanel = new Header("ins 0 0 1 0", "[grow,fill]", "[grow,fill]");
            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            final String headerText;
            if (getCrawlerStatus() == null) {
                headerText = (_GUI.T.CaptchaDialog_layoutDialogContent_header_crawler(hosterInfo.getTld()));
            } else {
                headerText = (_GUI.T.CaptchaDialog_layoutDialogContent_header_crawler2(getCrawlerStatus(), hosterInfo.getTld()));
            }
            // headerPanel.setOpaque(false);
            JLabel header = new JLabel() {
                private boolean setting = false;

                protected void paintComponent(Graphics g) {
                    if (headerText != null) {
                        setting = true;
                        try {
                            setText(org.appwork.sunwrapper.sun.swing.SwingUtilities2Wrapper.clipStringIfNecessary(this, this.getFontMetrics(getFont()), headerText, getWidth() - 30));
                        } catch (Throwable e) {
                            // http://www.oracle.com/technetwork/java/faq-sun-packages-142232.html
                            e.printStackTrace();
                            setText(headerText);
                        }
                        setting = false;
                    }
                    super.paintComponent(g);
                }

                // public Dimension getPreferredSize() {
                // return new Dimension(10, 10);
                // }
                public void repaint() {
                    if (setting) {
                        return;
                    }
                    super.repaint();
                }

                public void revalidate() {
                    if (setting) {
                        return;
                    }
                    super.revalidate();
                }
            };
            header.setIcon(hosterInfo.getFavIcon());
            // if (col >= 0) {
            // header.setBackground(new Color(col));
            // header.setOpaque(false);
            // }
            // header.setOpaque(false);
            // header.setLabelMode(true);
            headerPanel.add(header);
        }
        config = JsonConfig.create(Application.getResource("cfg/CaptchaDialogSize/" + Hash.getMD5(getHost() + "." + challenge.getClass().getSimpleName() + "." + challenge.getTypeID())), LocationStorage.class);
        HeaderScrollPane sp;
        iconPanel = new AbstractConfigPanel(5) {
            @Override
            public Icon getIcon() {
                return null;
            }

            @Override
            public String getLeftGap() {
                return "0";
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public void save() {
            }

            @Override
            public void updateContents() {
            }
        };
        // JLabel lbl = new JLabel("<html>" + _GUI.T.BrowserCaptchaDialog_layoutDialogContent_explain_() + "</html>");
        //
        // iconPanel.add(lbl, "spanx,pushx,growx");
        iconPanel.addDescriptionPlain(_GUI.T.BrowserCaptchaDialog_layoutDialogContent_explain_());
        iconPanel.addPair(_GUI.T.BrowserCaptchaDialog_layoutDialogContent_autoclick(), null, new Checkbox(CFG_BROWSER_CAPTCHA_SOLVER.AUTO_CLICK_ENABLED));
        iconPanel.addPair(_GUI.T.BrowserCaptchaDialog_layoutDialogContent_autoopen(), null, new Checkbox(CFG_BROWSER_CAPTCHA_SOLVER.AUTO_OPEN_BROWSER_ENABLED));
        SwingUtils.setOpaque(iconPanel, false);
        // iconPanel.add(refreshBtn, "alignx right,aligny bottom");
        iconPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                cancel();
            }
        });
        field.add(iconPanel);
        sp = new HeaderScrollPane(field);
        if (!CFG_GUI.CFG.isCaptchaDialogBorderAroundImageEnabled()) {
            sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            SwingUtils.setOpaque(field, false);
        }
        panel.add(sp);
        if (headerPanel != null) {
            sp.setColumnHeaderView(headerPanel);
            // sp.setMinimumSize(new Dimension(Math.max(images[0].getWidth(null) + 10, headerPanel.getPreferredSize().width + 10),
            // images[0].getHeight(null) + headerPanel.getPreferredSize().height));
        }
        if (BrowserSolverService.getInstance().getConfig().isAutoOpenBrowserEnabled()) {
            if (CFG_GUI.CFG.getNewDialogFrameState() != FrameState.TO_BACK) {
                new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            openBrowser();
                        } catch (InterruptedException e) {
                        }
                    };
                }.start();
            } else {
                getDialog().addWindowFocusListener(openBrowserFocusListener = new WindowFocusListener() {
                    @Override
                    public void windowLostFocus(WindowEvent e) {
                    }

                    @Override
                    public void windowGainedFocus(WindowEvent e) {
                        openBrowser();
                        getDialog().removeWindowFocusListener(this);
                    }
                });
            }
        }
        return panel;
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.okButton) {
            openBrowser();
        } else {
            super.actionPerformed(e);
        }
    }

    @Override
    public String getOKButtonText() {
        return _GUI.T.BrowserCaptchaDialog_getOKButtonText_open_browser();
    }

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    public void mouseClicked(final MouseEvent e) {
        this.cancel();
    }

    public void mouseEntered(final MouseEvent e) {
    }

    public void mouseExited(final MouseEvent e) {
    }

    public void mousePressed(final MouseEvent e) {
        this.cancel();
    }

    public void mouseReleased(final MouseEvent e) {
        this.cancel();
    }

    public List<? extends Image> getIconList() {
        return JDGui.getInstance().getMainFrame().getIconImages();
    }

    @Override
    public Window getOwner() {
        return super.getOwner();
    }

    public void pack() {
        getDialog().pack();
        getDialog().setMinimumSize(getDialog().getRawPreferredSize());
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    protected void openBrowser() {
        BrowserReference lBrowserReference = browserReference.getAndSet(null);
        if (lBrowserReference != null) {
            lBrowserReference.dispose();
        }
        lBrowserReference = new BrowserReference(challenge) {
            @Override
            public void onResponse(String parameter) {
                responseCode = parameter;
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        setReturnmask(true);
                        BrowserCaptchaDialog.this.dispose();
                    }
                };
            }
        };
        try {
            if (browserReference.compareAndSet(null, lBrowserReference)) {
                lBrowserReference.open();
            }
        } catch (Throwable e1) {
            UIOManager.I().showException(e1.getMessage(), e1);
            if (browserReference.compareAndSet(lBrowserReference, null)) {
                lBrowserReference.dispose();
            }
        }
    }
}