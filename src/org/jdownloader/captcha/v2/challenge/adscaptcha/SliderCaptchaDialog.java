package org.jdownloader.captcha.v2.challenge.adscaptcha;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.swing.jdgui.JDGui;
import jd.nutils.Screen;
import net.miginfocom.swing.MigLayout;

import org.appwork.uio.UIOManager;
import org.appwork.utils.URLStream;
import org.appwork.utils.locale._AWU;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;

class SliderCaptchaDialog extends AbstractDialog<String> {
    private JSlider       slider;
    private JPanel        p;
    private int           images          = -1;
    private URL           imageUrls[];
    private int           pos             = 0;
    private JPanel        picture;
    private BufferedImage image[];
    private Image         finalImage;
    private Thread        download;
    private JLabel        dLabel;
    private JLabel        cLabel;
    private JProgressBar  bar;
    private final JButton dynamicOkButton = new JButton(_AWU.T.ABSTRACTDIALOG_BUTTON_OK());

    public SliderCaptchaDialog(int flag, String title, URL[] imageUrls, int count) {
        super(flag | Dialog.STYLE_HIDE_ICON | UIOManager.LOGIC_COUNTDOWN | UIOManager.BUTTONS_HIDE_OK, title, null, null, null);
        setCountdownTime(120);
        this.images = imageUrls.length - 1;
        this.imageUrls = imageUrls;
        if (images == 0) {
            images = count--;
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        bar = new JProgressBar(0, images);
        dLabel = new JLabel("Please wait while downloading captchas: " + bar.getValue() + "/" + images);
        cLabel = new JLabel("Slide to fit");

        download = new Thread("Captcha download") {
            public void run() {
                try {
                    if (images < 0) {
                        image = new BufferedImage[imageUrls.length];
                        for (int i = 0; i < image.length; i++) {
                            sleep(50);
                            InputStream stream = null;
                            try {
                                stream = URLStream.openStream(imageUrls[i]);
                                image[i] = ImageIO.read(stream);
                                bar.setValue(i + 1);
                            } finally {
                                try {
                                    stream.close();
                                } catch (final Throwable e) {
                                }
                            }
                        }
                    } else {
                        image = new BufferedImage[images];
                        InputStream stream = null;
                        try {
                            stream = URLStream.openStream(imageUrls[0]);
                            BufferedImage tmpImage = ImageIO.read(stream);
                            int w = tmpImage.getWidth();
                            int h = tmpImage.getHeight() / images;
                            for (int i = 0; i < image.length; i++) {
                                image[i] = tmpImage.getSubimage(0, i * h, w, h);
                                bar.setValue(i + 1);
                            }
                        } finally {
                            try {
                                stream.close();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                } catch (IOException e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                } catch (InterruptedException e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
        };
        download.start();

        slider = new JSlider(0, images, 0);
        slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                pos = ((JSlider) e.getSource()).getValue();
                resizeImage();
                p.repaint();
            }
        });

        /* setup panels */
        p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
        picture = new JPanel() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                if (finalImage != null) {
                    return new Dimension(finalImage.getWidth(dialog), finalImage.getHeight(dialog));
                } else {
                    return super.getPreferredSize();
                }
            }

            @Override
            protected void paintComponent(final Graphics g) {
                super.paintComponent(g);
                if (finalImage != null) {
                    g.setColor(Color.WHITE);
                    g.drawImage(finalImage, 0, 0, this);
                }
            }
        };

        bar.setStringPainted(true);
        bar.addChangeListener(new ChangeListener() {
            private int pos = 0;

            @Override
            public void stateChanged(ChangeEvent e) {
                pos = ((JProgressBar) e.getSource()).getValue();
                if (pos < images) {
                    dLabel.setText("Please wait while downloading captchas: " + bar.getValue() + "/" + images);
                    dLabel.paintImmediately(dLabel.getVisibleRect());
                } else {
                    resizeImage();
                    dialog.setSize(315, 350);
                    if (JDGui.getInstance() == null) {
                        dialog.setLocation(Screen.getCenterOfComponent(null, dialog));
                    } else if (JDGui.getInstance().getMainFrame().getExtendedState() == 1 || !JDGui.getInstance().getMainFrame().isVisible()) {
                        dialog.setLocation(Screen.getDockBottomRight(dialog));
                    } else {
                        dialog.setLocation(Screen.getCenterOfComponent(JDGui.getInstance().getMainFrame(), dialog));
                    }
                    setupCaptchaDialog();
                }
            }

        });

        p.add(dLabel);
        p.add(bar);

        return p;
    }

    private void setupCaptchaDialog() {
        p.remove(dLabel);
        p.remove(bar);
        p.add(cLabel);
        p.add(picture);
        p.add(slider);
        p.repaint();
    }

    private void resizeImage() {
        if (image == null || image.length == 0) {
            finalImage = null;
        } else {
            finalImage = image[pos].getScaledInstance(300, 250, Image.SCALE_SMOOTH);
        }
    }

    @Override
    protected void addButtons(final JPanel buttonBar) {
        dynamicOkButton.addActionListener(this);
        p.addContainerListener(new ContainerListener() {

            @Override
            public void componentAdded(ContainerEvent e) {
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (e.getChild().getClass().getName().endsWith("JProgressBar")) {
                    buttonBar.add(dynamicOkButton, "cell 0 0,tag ok,sizegroup confirms");
                }
            }
        });

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == dynamicOkButton) {
                  org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Answer: Button<OK:" + dynamicOkButton.getText() + ">");
            setReturnmask(true);
        } else if (e.getActionCommand().equals("enterPushed")) {
            return;
        }
        super.actionPerformed(e);
    }

    @Override
    protected String createReturnValue() {
        if (!download.isInterrupted()) {
            download.interrupt();
        }
        if (Dialog.isOK(getReturnmask())) {
            return String.valueOf(pos);
        }
        return null;
    }

}