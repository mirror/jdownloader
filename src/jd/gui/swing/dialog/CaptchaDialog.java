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

package jd.gui.swing.dialog;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import jd.Launcher;
import jd.captcha.utils.GifDecoder;
import jd.controlling.captcha.CaptchaResult;
import jd.gui.swing.laf.LAFOptions;
import jd.gui.swing.laf.LookAndFeelController;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.LocationStorage;
import org.appwork.utils.swing.dialog.RememberAbsoluteLocator;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.premium.PremiumInfoDialog;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

/**
 * This Dialog is used to display a Inputdialog for the captchas
 */
public class CaptchaDialog extends AbstractDialog<String> implements ActionListener, WindowListener, MouseListener, CaptchaDialogInterface {

    private ExtTextField        textField;

    private final CaptchaResult defaultValue;

    private final String        explain;

    private DomainInfo          hosterInfo;

    private DialogType          type;

    private JComponent          iconPanel;

    private Plugin              plugin;
    private Image[]             images;
    private int                 fps;
    protected int               frame         = 0;
    private Timer               paintTimer;
    private Point               resultPoint   = null;

    private CaptchaType         captchaType;

    private CaptchaResult       captchaResult = new CaptchaResult();

    private LocationStorage     config;

    public static void main(String[] args) {
        CaptchaDialog cp;
        try {
            Application.setApplication(".jd_home");
            Launcher.statics();
            cp = new CaptchaDialog(Dialog.LOGIC_COUNTDOWN, DialogType.HOSTER, CaptchaType.TEXT, DomainInfo.getInstance("wupload.com"), getGifImages(new File("C:/Users/Thomas/.BuildServ/applications/beta/sources/JDownloader/src/org/jdownloader/extensions/webinterface/webinterface/themes/main/images/core/load.gif").toURI().toURL()), null, "Enter both words...");

            LookAndFeelController.getInstance().setUIManager();

            Dialog.getInstance().showDialog(cp);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public CaptchaDialog(final int flag, DialogType type, CaptchaType captchaType, final DomainInfo DomainInfo, final Image image, final CaptchaResult defaultValue, final String explain) {
        this(flag, type, captchaType, DomainInfo, new Image[] { image }, defaultValue, explain);
    }

    /**
     * @param url
     * @return
     */
    public static Image[] getGifImages(URL url) {
        InputStream is = null;
        try {
            GifDecoder decoder = new GifDecoder();
            decoder.read(is = url.openStream());
            BufferedImage[] ret = new BufferedImage[decoder.getFrameCount()];
            for (int i = 0; i < decoder.getFrameCount(); i++) {
                ret[i] = decoder.getFrame(i);
                // ImageIO.write(ret[i], "png", Application.getResource("img_" +
                // i + ".png"));
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    public CaptchaDialog(int flag, DialogType type, CaptchaType captchaType, DomainInfo domainInfo, Image[] images, CaptchaResult defaultValue, String explain) {
        super(flag | Dialog.STYLE_HIDE_ICON, _GUI._.gui_captchaWindow_askForInput(domainInfo.getTld()), null, null, null);
        if (CaptchaType.CLICK.equals(captchaType)) {
            super.flagMask = flag | Dialog.BUTTONS_HIDE_OK;
        }

        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isCaptchaDialogUniquePositionByHosterEnabled()) {
            setLocator(new RememberAbsoluteLocator("CaptchaDialog_" + domainInfo.getTld()));
        } else {
            setLocator(new RememberAbsoluteLocator("CaptchaDialog"));
        }

        this.hosterInfo = domainInfo;
        this.images = images;
        fps = 24;
        this.defaultValue = defaultValue;
        this.explain = explain;
        this.type = type;
        this.captchaType = captchaType;
    }

    private void adaptMethod() {

    }

    @Override
    protected JPanel getDefaultButtonPanel() {
        final JPanel ret = new JPanel(new MigLayout("ins 0", "[]", "0[fill,grow]0"));
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

    @Override
    protected void packed() {
        this.textField.requestFocusInWindow();
        textField.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent e) {
                cancel();
            }

            public void focusGained(FocusEvent e) {

            }
        });
    }

    @Override
    protected String createReturnValue() {
        if (Dialog.isOK(this.getReturnmask())) return this.textField.getText();
        return null;
    }

    public void pack() {

        this.getDialog().pack();

    }

    @Override
    public JComponent layoutDialogContent() {
        adaptMethod();
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
        final int size = org.jdownloader.settings.staticreferences.CFG_GUI.CAPTCHA_SCALE_FACTOR.getValue();
        if (size != 100) {
            for (int i = 0; i < images.length; i++) {
                images[i] = IconIO.getScaledInstance(images[i], (int) (images[i].getWidth(null) * size / 100.0f), (int) (images[i].getHeight(null) * size / 100.0f), Interpolation.BICUBIC, false);

            }
        }

        this.textField = new ExtTextField() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void onChanged() {
                cancel();
            }

        };

        textField.setClearHelpTextOnFocus(false);
        textField.setHelpText(explain);
        if (defaultValue != null) this.textField.setText(this.defaultValue.getCaptchaText());

        MigPanel headerPanel = null;
        if (type == DialogType.HOSTER) {

            // setBorder(new JTextField().getBorder());

            headerPanel = new MigPanel("ins 0 0 1 0", "[][grow,fill]", "[grow,fill]");
            headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor())));

            headerPanel.setBackground(new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor()));
            headerPanel.setOpaque(true);

            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            JLabel header = new JLabel();
            header.setIcon(hosterInfo.getFavIcon());

            // if (col >= 0) {
            // header.setBackground(new Color(col));
            // header.setOpaque(false);
            // }
            // header.setOpaque(false);
            // header.setLabelMode(true);
            if (getFilename() != null) {
                if (getFilesize() > 0) {
                    header.setText(_GUI._.CaptchaDialog_layoutDialogContent_header(getFilename(), SizeFormatter.formatBytes(getFilesize()), hosterInfo.getTld()));
                } else {
                    header.setText(_GUI._.CaptchaDialog_layoutDialogContent_header2(getFilename(), hosterInfo.getTld()));
                }
                headerPanel.add(header);
            }

        } else {
            // setBorder(new JTextField().getBorder());

            headerPanel = new MigPanel("ins 0 0 1 0", "[][grow,fill]", "[grow,fill]");
            headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor())));

            headerPanel.setBackground(new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor()));
            headerPanel.setOpaque(true);

            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            // headerPanel.setOpaque(false);
            JLabel header = new JLabel();
            header.setIcon(hosterInfo.getFavIcon());

            // if (col >= 0) {
            // header.setBackground(new Color(col));
            // header.setOpaque(false);
            // }
            // header.setOpaque(false);
            // header.setLabelMode(true);
            if (getCrawlerStatus() == null) {
                header.setText(_GUI._.CaptchaDialog_layoutDialogContent_header_crawler(hosterInfo.getTld()));
            } else {
                header.setText(_GUI._.CaptchaDialog_layoutDialogContent_header_crawler2(getCrawlerStatus(), hosterInfo.getTld()));

            }

            headerPanel.add(header);
        }

        iconPanel = new JComponent() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {

                BufferedImage scaled = IconIO.getScaledInstance(images[frame], getWidth() - 10, getHeight() - 10, Interpolation.BICUBIC, true);
                g.drawImage(scaled, (getWidth() - scaled.getWidth()) / 2, (getHeight() - scaled.getHeight()) / 2, new Color(col), null);

            }

        };
        iconPanel.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                cancel();
            }
        });
        if (CaptchaType.CLICK.equals(captchaType)) {
            iconPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            iconPanel.setToolTipText(this.explain);
            textField.setEditable(false);
            iconPanel.addMouseListener(new MouseListener() {

                @Override
                public void mouseReleased(MouseEvent e) {
                    resultPoint = e.getPoint();
                    final int size = org.jdownloader.settings.staticreferences.CFG_GUI.CAPTCHA_SCALE_FACTOR.getValue();
                    if (size != 100) {
                        resultPoint.setLocation(resultPoint.getX() / (size / 100.0f), resultPoint.getY() / (size / 100.0f));
                    }
                    dispose();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                }
            });
        }
        config = JsonConfig.create(Application.getResource("cfg/CaptchaDialogDimensions_" + Hash.getMD5(getTitle()) + ".json"), LocationStorage.class);

        iconPanel.setPreferredSize(new Dimension(images[0].getWidth(null), images[0].getHeight(null)));

        field.add(iconPanel);

        HeaderScrollPane sp;
        if (headerPanel != null) {
            sp = new HeaderScrollPane(field);

            panel.add(sp);

            sp.setColumnHeaderView(headerPanel);
            sp.setMinimumSize(new Dimension(Math.max(images[0].getWidth(null) + 10, headerPanel.getPreferredSize().width + 10), images[0].getHeight(null) + headerPanel.getPreferredSize().height));
        }
        panel.add(this.textField);
        this.textField.requestFocusInWindow();
        this.textField.selectAll();
        // panel.add(new JLabel("HJ dsf"));
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
        if (config.isValid()) {
            getDialog().setSize(new Dimension(Math.max(config.getX(), images[0].getWidth(null)), Math.max(config.getY(), images[0].getHeight(null))));
        }

        return panel;
    }

    @Override
    protected int getPreferredWidth() {
        if (!config.isValid()) return super.getPreferredWidth();
        return config.getX();
    }

    protected int getPreferredHeight() {
        if (!config.isValid()) return super.getPreferredHeight();
        return config.getY();
    }

    public void setVisible(final boolean b) {

        super.setVisible(b);

    }

    public void dispose() {

        config.setX(getDialog().getWidth());
        config.setValid(true);
        config.setY(getDialog().getHeight());
        super.dispose();
        if (paintTimer != null) {
            paintTimer.stop();
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
    protected boolean isResizable() {
        return true;
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

    public DomainInfo getDomainInfo() {
        return hosterInfo;
    }

    public DialogType getType() {
        return type;
    }

    public Image[] getImages() {
        return images;
    }

    public CaptchaResult getDefaultValue() {
        return defaultValue;
    }

    public String getHelpText() {
        return explain;
    }

    public String getFilename() {
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
            if (((PluginForHost) plugin).getDownloadLink() == null) return -1;
            return ((PluginForHost) plugin).getDownloadLink().getDownloadMax();
        }
        return -1;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CaptchaType getCaptchaType() {
        return captchaType;
    }

    @Override
    public CaptchaResult getCaptchaResult() {
        switch (captchaType) {
        case TEXT:
            captchaResult.setCaptchaText(textField.getText());
            break;
        case CLICK:
            Point position = resultPoint;
            if (position != null) {
                captchaResult.setCaptchaClick(new int[] { position.x, position.y });
            }
            break;
        }
        return captchaResult;
    }

}