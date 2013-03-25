package jd.gui.swing.dialog;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;

import jd.captcha.utils.GifDecoder;
import jd.gui.swing.laf.LAFOptions;
import jd.gui.swing.laf.LookAndFeelController;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.LocationStorage;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.premium.PremiumInfoDialog;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

import sun.swing.SwingUtilities2;

public abstract class AbstractCaptchaDialog extends AbstractDialog<Object> {

    public static Image[] getGifImages(InputStream openStream) {

        try {
            GifDecoder decoder = new GifDecoder();
            decoder.read(openStream);
            BufferedImage[] ret = new BufferedImage[decoder.getFrameCount()];
            for (int i = 0; i < decoder.getFrameCount(); i++) {
                ret[i] = decoder.getFrame(i);
                // ImageIO.write(ret[i], "png", Application.getResource("img_" +
                // i + ".png"));
            }
            return ret;

        } finally {
            try {
                openStream.close();
            } catch (final Throwable e) {
            }
        }

    }

    /**
     * @param url
     * @return
     */
    public static Image[] getGifImages(URL url) {
        InputStream stream = null;
        try {
            return getGifImages(stream = url.openStream());
        } catch (IOException e) {
            Log.exception(e);
        } finally {
            try {
                stream.close();
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    private LocationStorage config;

    protected boolean       hideCaptchasForHost    = false;

    protected boolean       hideCaptchasForPackage = false;

    private final String    explain;

    private int             fps;

    private int             frame                  = 0;
    private boolean         hideAllCaptchas;

    public boolean isHideAllCaptchas() {
        return hideAllCaptchas;
    }

    private DomainInfo hosterInfo;
    protected JPanel   iconPanel;

    private Image[]    images;

    protected Point    offset;

    private Timer      paintTimer;

    private Plugin     plugin;
    protected double   scaleFaktor;

    protected boolean  stopDownloads = false;

    private DialogType type;

    protected boolean  refresh;

    public AbstractCaptchaDialog(int flags, String title, DialogType type, DomainInfo domainInfo, String explain, Image... images) {
        super(flags, title, null, _GUI._.AbstractCaptchaDialog_AbstractCaptchaDialog_continue(), _GUI._.AbstractCaptchaDialog_AbstractCaptchaDialog_cancel());
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isCaptchaDialogUniquePositionByHosterEnabled()) {
            setLocator(new RememberAbsoluteDialogLocator("CaptchaDialog_" + domainInfo.getTld()));
        } else {
            setLocator(new RememberAbsoluteDialogLocator("CaptchaDialog"));
        }
        // if we have gif images, but read them as non indexed images, we try to fix this here.
        java.util.List<Image> ret = new ArrayList<Image>();
        for (int i = 0; i < images.length; i++) {
            if (images[i] instanceof BufferedImage) {
                if (((BufferedImage) images[i]).getColorModel() instanceof IndexColorModel) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        ImageIO.write((BufferedImage) images[i], "gif", os);

                        InputStream is = new ByteArrayInputStream(os.toByteArray());
                        Image[] subImages = getGifImages(is);
                        for (Image ii : subImages) {
                            ret.add(ii);
                        }
                        continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }
            ret.add(images[i]);
        }

        this.images = ret.toArray(new Image[] {});
        fps = 24;
        this.explain = explain;

        this.hosterInfo = domainInfo;
        this.type = type;

    }

    abstract protected JComponent createInputComponent();

    private void createPopup() {
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem mi;
        if (getType() == DialogType.HOSTER) {

            mi = new JMenuItem(new AppAction() {
                {
                    setName(_GUI._.AbstractCaptchaDialog_createPopup_skip_and_disable_all_downloads_from(getHost()));
                    setSmallIcon(getHostIcon());
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
                    setName(_GUI._.AbstractCaptchaDialog_createPopup_skip_and_disable_package(getPackageName()));
                    setSmallIcon(NewTheme.I().getIcon("package_open_error", 16));
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
                    setName(_GUI._.AbstractCaptchaDialog_createPopup_skip_and_hide_all_captchas_download());
                    setSmallIcon(NewTheme.I().getIcon("clear", 16));
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
                    setName(_GUI._.AbstractCaptchaDialog_createPopup_skip_and_stop_all_downloads());
                    setSmallIcon(NewTheme.I().getIcon("stop", 16));
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
                    setName("Skip & Cancel LinkCrawler");
                }

                @Override
                public void actionPerformed(ActionEvent e) {

                    setReturnmask(false);
                    dispose();
                }

            });

            popup.add(mi);
        }

        int[] insets = LookAndFeelController.getInstance().getLAFOptions().getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        pref.height = popup.getComponentCount() * 24 + insets[0] + insets[2];

        popup.setPreferredSize(pref);
        popup.show(cancelButton, +insets[1] - pref.width + cancelButton.getWidth() + 8 + 5, +cancelButton.getHeight());
    }

    @Override
    protected String createReturnValue() {

        return null;
    }

    public void dispose() {
        if (!isInitialized()) return;
        config.setX(getDialog().getWidth());
        config.setValid(true);
        config.setY(getDialog().getHeight());

        super.dispose();
        if (paintTimer != null) {
            paintTimer.stop();
        }
    }

    @Override
    public int getCountdownTime() {
        return JsonConfig.create(GraphicalUserInterfaceSettings.class).getCaptchaDialogTimeout();
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

            @Override
            public void addCancelButton(final JButton cancelButton) {
                super.addCancelButton(cancelButton);

                final JButton bt = new JButton(NewTheme.I().getIcon("popdownButton", -1)) {

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
                setName(_GUI._.CaptchaDialog_getDefaultButtonPanel_premium());
            }

            public void actionPerformed(ActionEvent e) {
                cancel();
                PremiumInfoDialog d = new PremiumInfoDialog(hosterInfo, _GUI._.PremiumInfoDialog_PremiumInfoDialog_(hosterInfo.getTld()), "CaptchaDialog");
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
        if (plg != null && plg.isPremium()) {
            ret.add(premium);
        }
        SwingUtils.setOpaque(premium, false);
        return ret;
    }

    public DomainInfo getDomainInfo() {
        return hosterInfo;
    }

    public String getFilename() {
        if (!JsonConfig.create(GeneralSettings.class).isShowFileNameInCaptchaDialogEnabled()) return null;
        switch (type) {
        case HOSTER:
            if (plugin == null || ((PluginForHost) plugin).getDownloadLink() == null) return null;
            return ((PluginForHost) plugin).getDownloadLink().getName();
        }
        return null;
    }

    public long getFilesize() {
        switch (type) {
        case HOSTER:
            if (plugin == null || ((PluginForHost) plugin).getDownloadLink() == null) return -1;
            return ((PluginForHost) plugin).getDownloadLink().getDownloadMax();
        }
        return -1;
    }

    public String getHelpText() {
        return explain;
    }

    public String getHost() {
        switch (type) {
        case HOSTER:

            return ((PluginForHost) plugin).getDomainInfo().getTld();
        case CRAWLER:
            return ((PluginForDecrypt) plugin).getHost();
        }
        return null;
    }

    protected Icon getHostIcon() {
        try {
            return DomainInfo.getInstance(getHost()).getIcon(16);
        } catch (Exception e) {
        }
        return null;
    }

    public Image[] getImages() {
        return images;
    }

    public Point getOffset() {
        return offset;
    }

    protected String getPackageName() {
        switch (type) {
        case HOSTER:
            if (plugin == null || ((PluginForHost) plugin).getDownloadLink() == null) return null;
            return ((PluginForHost) plugin).getDownloadLink().getFilePackage().getName();

        case CRAWLER:
            return null;
        }
        return null;
    }

    protected int getPreferredHeight() {
        if (!config.isValid()) return super.getPreferredHeight();
        return config.getY();
    }

    @Override
    protected int getPreferredWidth() {
        if (!config.isValid()) return super.getPreferredWidth();
        return config.getX();
    }

    public double getScaleFaktor() {
        return scaleFaktor;
    }

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

        LAFOptions lafOptions = LookAndFeelController.getInstance().getLAFOptions();
        MigPanel field = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill]");
        // field.setOpaque(false);
        getDialog().setMinimumSize(new Dimension(0, 0));
        // getDialog().setIconImage(hosterInfo.getFavIcon().getImage());
        final JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[grow,fill]10[]"));
        final int col = lafOptions.getPanelBackgroundColor();
        if (col >= 0) {

            field.setBackground(new Color(col));
            field.setOpaque(true);
            // getDialog().getContentPane().setBackground(new Color(col));
            // panel.setBackground(new Color(col));
        }

        MigPanel headerPanel = null;
        if (type == DialogType.HOSTER) {

            // setBorder(new JTextField().getBorder());

            headerPanel = new MigPanel("ins 0 0 1 0", "[grow,fill]", "[]");
            headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor())));

            headerPanel.setBackground(new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor()));
            headerPanel.setOpaque(true);

            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            final String headerText;
            if (getFilename() != null) {
                if (getFilesize() > 0) {
                    headerText = (_GUI._.CaptchaDialog_layoutDialogContent_header(getFilename(), SizeFormatter.formatBytes(getFilesize()), hosterInfo.getTld()));
                } else {
                    headerText = (_GUI._.CaptchaDialog_layoutDialogContent_header2(getFilename(), hosterInfo.getTld()));
                }
            } else {
                headerText = null;
            }
            JLabel header = new JLabel() {
                private boolean setting = false;

                protected void paintComponent(Graphics g) {
                    if (headerText != null) {
                        setting = true;

                        setText(SwingUtilities2.clipStringIfNecessary(this, this.getFontMetrics(getFont()), headerText, getWidth() - 30));
                        setting = false;
                    }
                    super.paintComponent(g);

                }

                // public Dimension getPreferredSize() {
                // return new Dimension(10, 10);
                // }

                public void repaint() {

                    if (setting) return;
                    super.repaint();
                }

                public void revalidate() {
                    if (setting) return;
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

            headerPanel = new MigPanel("ins 0 0 1 0", "[grow,fill]", "[grow,fill]");
            headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor())));

            headerPanel.setBackground(new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor()));
            headerPanel.setOpaque(true);

            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            final String headerText;
            if (getCrawlerStatus() == null) {
                headerText = (_GUI._.CaptchaDialog_layoutDialogContent_header_crawler(hosterInfo.getTld()));
            } else {
                headerText = (_GUI._.CaptchaDialog_layoutDialogContent_header_crawler2(getCrawlerStatus(), hosterInfo.getTld()));

            }
            // headerPanel.setOpaque(false);
            JLabel header = new JLabel() {
                private boolean setting = false;

                protected void paintComponent(Graphics g) {
                    if (headerText != null) {
                        setting = true;

                        setText(SwingUtilities2.clipStringIfNecessary(this, this.getFontMetrics(getFont()), headerText, getWidth() - 30));
                        setting = false;
                    }
                    super.paintComponent(g);

                }

                // public Dimension getPreferredSize() {
                // return new Dimension(10, 10);
                // }

                public void repaint() {

                    if (setting) return;
                    super.repaint();
                }

                public void revalidate() {
                    if (setting) return;
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
        config = JsonConfig.create(Application.getResource("cfg/CaptchaDialogDimensions_" + Hash.getMD5(getTitle())), LocationStorage.class);

        HeaderScrollPane sp;
        final JButton refreshBtn = new JButton(_GUI._.CaptchaDialog_layoutDialogContent_refresh(), NewTheme.I().getIcon("refresh", 20));
        refreshBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                refresh = true;
                setReturnmask(false);
                dispose();
            }
        });
        iconPanel = new JPanel(new MigLayout("ins 0", "[grow]", "[grow]")) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                Composite comp = g2.getComposite();
                try {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                    super.paintChildren(g);

                } finally {
                    g2.setComposite(comp);
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                BufferedImage scaled = IconIO.getScaledInstance(images[frame], getWidth() - 10, getHeight() - 10, Interpolation.BICUBIC, true);
                g.drawImage(scaled, (getWidth() - scaled.getWidth()) / 2, (getHeight() - scaled.getHeight()) / 2, new Color(col), null);
                scaleFaktor = images[frame].getWidth(null) / (double) scaled.getWidth();
                offset = new Point((getWidth() - scaled.getWidth()) / 2, (getHeight() - scaled.getHeight()) / 2);

            }

        };
        iconPanel.setOpaque(false);
        iconPanel.add(refreshBtn, "alignx right,aligny bottom");
        iconPanel.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                cancel();
            }
        });

        final int size = org.jdownloader.settings.staticreferences.CFG_GUI.CAPTCHA_SCALE_FACTOR.getValue();
        if (size != 100) {

            iconPanel.setPreferredSize(new Dimension((int) (images[0].getWidth(null) * size / 100.0f), (int) (images[0].getHeight(null) * size / 100.0f)));
        } else {
            iconPanel.setPreferredSize(new Dimension(images[0].getWidth(null), images[0].getHeight(null)));
        }

        field.add(iconPanel);

        sp = new HeaderScrollPane(field);

        panel.add(sp);
        if (headerPanel != null) {
            sp.setColumnHeaderView(headerPanel);
            // sp.setMinimumSize(new Dimension(Math.max(images[0].getWidth(null) + 10, headerPanel.getPreferredSize().width + 10),
            // images[0].getHeight(null) + headerPanel.getPreferredSize().height));
        }
        JComponent b = createInputComponent();
        if (b != null) panel.add(b);

        // if (config.isValid()) {
        //
        // getDialog().setSize(new Dimension(Math.max(config.getX(), images[0].getWidth(null)), Math.max(config.getY(),
        // images[0].getHeight(null))));
        // }
        if (images.length > 1) {
            paintTimer = new Timer(1000 / fps, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame = (frame + 1) % images.length;
                    iconPanel.repaint();
                }
            });
            paintTimer.setRepeats(true);
            paintTimer.start();

        }
        return panel;
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

    public void pack() {
        getDialog().pack();

        getDialog().setMinimumSize(getDialog().getRawPreferredSize());

    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }
}
