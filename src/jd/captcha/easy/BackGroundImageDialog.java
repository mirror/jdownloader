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

package jd.captcha.easy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import jd.captcha.gui.ImageComponent;
import jd.captcha.translate.T;
import jd.captcha.utils.Utilities;
import jd.gui.UserIO;
import jd.nutils.JDHash;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;

import org.appwork.utils.Files;
import org.appwork.utils.swing.EDTHelper;

public class BackGroundImageDialog implements ActionListener {

    private final JFrame                 owner;

    public BackGroundImage               workingImage;
    private final BackGroundImageManager bgim;
    private ImageComponent               bg1, bgv, bgmask;
    private BackGroundImage              ret       = null;
    private JDialog                      dialog;
    private JButton                      btColorChoose, btPreview, btFinished, btLoadBackgroundImage, btCreateBackgroundFilter;
    private JComboBox                    colorModeBox;
    private JSpinner                     thresholdSpinner;
    private JColorChooser                colorChooser;
    private int                          threshold = 2;
    private byte                         colorMode = CPoint.RGB_DIFFERENCE3;
    private JPanel                       imagePanel;

    public BackGroundImageDialog(final BackGroundImageManager bgim, final JFrame owner) {
        this.bgim = bgim;
        this.owner = owner;
    }

    public void actionPerformed(final ActionEvent e) {
        new EDTHelper<Object>() {
            public Object edtRun() {
                BackGroundImageDialog.this.dialog.setAlwaysOnTop(false);
                return null;
            }
        }.waitForEDT();
        if (e.getSource() == this.btPreview) {
            this.workingImage.setDistance((Integer) this.thresholdSpinner.getValue());
            this.workingImage.setColorDistanceMode(this.colorMode);
            this.workingImage.setColor(this.colorChooser.getColor().getRGB());
            this.bgim.clearCaptchaPreview(this.workingImage);
            final Image image2 = this.bgim.getScaledCaptchaImage();
            new EDTHelper<Object>() {
                public Object edtRun() {
                    BackGroundImageDialog.this.bgv.image = image2;
                    BackGroundImageDialog.this.bgv.repaint();
                    return null;
                }
            }.waitForEDT();
        } else if (e.getSource() == this.btColorChoose) {
            new EDTHelper<Object>() {
                public Object edtRun() {

                    final JDialog dialog = JColorChooser.createDialog(BackGroundImageDialog.this.colorChooser, T._.easycaptcha_addbackgroundimagedialog_deletecolor(), true, BackGroundImageDialog.this.colorChooser, null, null);
                    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    dialog.setAlwaysOnTop(true);
                    dialog.setVisible(true);
                    return null;
                }
            }.waitForEDT();
        } else if (e.getSource() == this.btFinished) {
            // wird in der BackGroundImageGUIList gemacht
            // if (dialogImage != null) bgim.add(dialogImage);

            new EDTHelper<Object>() {
                public Object edtRun() {
                    BackGroundImageDialog.this.dialog.dispose();

                    return null;
                }
            }.waitForEDT();
            this.workingImage.setColor(this.colorChooser.getColor().getRGB());
            this.workingImage.setDistance((Integer) this.thresholdSpinner.getValue());
            this.workingImage.setColorDistanceMode(this.colorMode);
            this.ret = this.workingImage;
            return;
        } else if (e.getSource() == this.btLoadBackgroundImage) {
            final File[] fch = UserIO.getInstance().requestFileChooser(null, null, null, new JDFileFilter(null, ".jpg|.png|.gif|.jpeg|.bmp", true), null);
            if (fch == null) { return; }

            final File fout = new File(this.bgim.methode.file, "mask_" + JDHash.getMD5(fch[0]) + "." + Files.getExtension(fch[0].getName()));
            JDIO.copyFile(fch[0], fout);
            this.workingImage = new BackGroundImage();
            this.workingImage.setBackgroundImage(fout.getName());
            this.workingImage.setColor(this.colorChooser.getColor().getRGB());
            this.workingImage.setDistance((Integer) this.thresholdSpinner.getValue());
            this.workingImage.setColorDistanceMode(this.colorMode);
            this.bgim.clearCaptchaPreview(this.workingImage);
            this.btPreview.setEnabled(true);
            final Image image2 = this.bgim.getScaledCaptchaImage();

            new EDTHelper<Object>() {
                public Object edtRun() {
                    BackGroundImageDialog.this.bgv.image = image2;
                    BackGroundImageDialog.this.bgmask.image = BackGroundImageDialog.this.workingImage.getImage(BackGroundImageDialog.this.bgim.methode);
                    BackGroundImageDialog.this.bgmask.repaint();
                    BackGroundImageDialog.this.bgv.repaint();
                    return null;
                }
            }.waitForEDT();
        } else if (e.getSource() == this.btCreateBackgroundFilter) {

            final File fout = BackgroundFilterCreater.create(this.bgim.methode);
            if (fout != null && fout.exists()) {
                this.workingImage = new BackGroundImage();
                this.workingImage.setBackgroundImage(fout.getName());
                this.workingImage.setColor(this.colorChooser.getColor().getRGB());
                this.workingImage.setDistance((Integer) this.thresholdSpinner.getValue());
                this.workingImage.setColorDistanceMode(this.colorMode);
                this.bgim.clearCaptchaPreview(this.workingImage);
                this.btPreview.setEnabled(true);
                final Image image2 = this.bgim.getScaledCaptchaImage();

                new EDTHelper<Object>() {
                    public Object edtRun() {
                        BackGroundImageDialog.this.bgv.image = image2;
                        BackGroundImageDialog.this.bgmask.image = BackGroundImageDialog.this.workingImage.getImage(BackGroundImageDialog.this.bgim.methode);
                        BackGroundImageDialog.this.bgmask.repaint();
                        BackGroundImageDialog.this.bgv.repaint();
                        return null;
                    }
                }.waitForEDT();
            }
        } else if (e.getSource() == this.colorModeBox) {
            this.colorMode = ((ColorMode) this.colorModeBox.getSelectedItem()).mode;
        }
        new EDTHelper<Object>() {
            public Object edtRun() {
                BackGroundImageDialog.this.dialog.setAlwaysOnTop(true);
                return null;
            }
        }.waitForEDT();
    }

    private void addActionListeners() {
        this.btColorChoose.addActionListener(this);
        this.btPreview.addActionListener(this);
        this.btFinished.addActionListener(this);
        this.btLoadBackgroundImage.addActionListener(this);
        this.btCreateBackgroundFilter.addActionListener(this);
        this.colorModeBox.addActionListener(this);
    }

    private void addComponentsToDialog() {
        new EDTHelper<Object>() {
            public Object edtRun() {
                final JPanel box = new JPanel();
                box.setLayout(new GridBagLayout());
                final GridBagConstraints gbc = Utilities.getGBC(0, 0, 1, 1);
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1;
                gbc.weightx = 1;
                box.add(BackGroundImageDialog.this.imagePanel, gbc);
                final Box menu = new Box(BoxLayout.X_AXIS);
                menu.add(BackGroundImageDialog.this.btLoadBackgroundImage);
                menu.add(BackGroundImageDialog.this.btCreateBackgroundFilter);
                menu.add(BackGroundImageDialog.this.btColorChoose);
                menu.add(BackGroundImageDialog.this.btPreview);
                menu.add(BackGroundImageDialog.this.thresholdSpinner);
                menu.add(BackGroundImageDialog.this.colorModeBox);
                menu.add(BackGroundImageDialog.this.btFinished);
                gbc.gridy = 1;
                box.add(menu, gbc);
                BackGroundImageDialog.this.dialog.add(box);
                BackGroundImageDialog.this.dialog.pack();
                BackGroundImageDialog.this.addActionListeners();
                BackGroundImageDialog.this.dialog.setVisible(true);
                return null;
            }
        }.waitForEDT();
    }

    public BackGroundImage getNewBackGroundImage() {
        this.init();
        return this.ret;
    }

    private void init() {
        this.initDialog();
        this.initCaptchaImages();
        this.initComponents();
        this.addComponentsToDialog();
    }

    private void initCaptchaImages() {
        this.bgim.resetCaptcha();

        final Image image = this.bgim.getScaledCaptchaImage();

        new EDTHelper<Object>() {
            public Object edtRun() {
                BackGroundImageDialog.this.imagePanel = new JPanel();

                BackGroundImageDialog.this.imagePanel.setBorder(new TitledBorder(T._.easycaptcha_images()));

                BackGroundImageDialog.this.imagePanel.setLayout(new BoxLayout(BackGroundImageDialog.this.imagePanel, BoxLayout.Y_AXIS));
                BackGroundImageDialog.this.imagePanel.add(new JLabel(T._.easycaptcha_mask()));
                if (BackGroundImageDialog.this.workingImage != null) {
                    BackGroundImageDialog.this.bgmask = new ImageComponent(BackGroundImageDialog.this.workingImage.getImage(BackGroundImageDialog.this.bgim.methode));
                } else {
                    final BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    final Graphics2D ig2 = bi.createGraphics();
                    ig2.fillRect(0, 0, image.getWidth(null) - 1, image.getHeight(null) - 1);
                    BackGroundImageDialog.this.bgmask = new ImageComponent(bi);

                }

                BackGroundImageDialog.this.imagePanel.add(BackGroundImageDialog.this.bgmask);

                BackGroundImageDialog.this.imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));

                BackGroundImageDialog.this.imagePanel.add(new JLabel(T._.easycaptcha_orginal()));
                BackGroundImageDialog.this.bg1 = new ImageComponent(image);

                BackGroundImageDialog.this.imagePanel.add(BackGroundImageDialog.this.bg1);

                BackGroundImageDialog.this.imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));

                BackGroundImageDialog.this.imagePanel.add(new JLabel(T._.easycaptcha_addbackgroundimagedialog_imagepreview() + ":"));
                if (BackGroundImageDialog.this.workingImage != null) {
                    BackGroundImageDialog.this.threshold = BackGroundImageDialog.this.workingImage.getDistance();
                    BackGroundImageDialog.this.colorMode = BackGroundImageDialog.this.workingImage.getColorDistanceMode();
                    BackGroundImageDialog.this.bgim.clearCaptchaPreview(BackGroundImageDialog.this.workingImage);
                }

                BackGroundImageDialog.this.bgv = new ImageComponent(BackGroundImageDialog.this.bgim.getScaledCaptchaImage());
                BackGroundImageDialog.this.imagePanel.add(BackGroundImageDialog.this.bgv);
                return null;
            }
        }.waitForEDT();
    }

    private void initComponents() {
        new EDTHelper<Object>() {
            public Object edtRun() {
                BackGroundImageDialog.this.thresholdSpinner = new JSpinner(new SpinnerNumberModel(BackGroundImageDialog.this.threshold, 0, 360, 1));
                BackGroundImageDialog.this.thresholdSpinner.setToolTipText("Threshold");
                BackGroundImageDialog.this.btLoadBackgroundImage = new JButton(T._.easycaptcha_addbackgroundimagedialog_loadimage());
                BackGroundImageDialog.this.btCreateBackgroundFilter = new JButton(T._.easycaptcha_addbackgroundimagedialog_generate());
                Color defColor = Color.WHITE;
                if (BackGroundImageDialog.this.workingImage != null) {
                    defColor = new Color(BackGroundImageDialog.this.workingImage.getColor());
                }
                BackGroundImageDialog.this.colorChooser = new JColorChooser(defColor);
                BackGroundImageDialog.this.btColorChoose = new JButton(T._.easycaptcha_addbackgroundimagedialog_deletecolor());
                BackGroundImageDialog.this.btPreview = new JButton(T._.easycaptcha_addbackgroundimagedialog_imagepreview());
                if (BackGroundImageDialog.this.workingImage == null) {
                    BackGroundImageDialog.this.btPreview.setEnabled(false);
                }
                BackGroundImageDialog.this.colorModeBox = new JComboBox(ColorMode.cModes);
                BackGroundImageDialog.this.colorModeBox.setSelectedItem(new ColorMode(BackGroundImageDialog.this.colorMode));
                BackGroundImageDialog.this.btFinished = new JButton(T._.easycaptcha_finished());
                return null;
            }
        }.waitForEDT();

    }

    private void initDialog() {
        new EDTHelper<Object>() {
            public Object edtRun() {
                BackGroundImageDialog.this.dialog = new JDialog(BackGroundImageDialog.this.owner);
                BackGroundImageDialog.this.dialog.setTitle(T._.easycaptcha_addbackgroundimagedialog_title());
                BackGroundImageDialog.this.dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                BackGroundImageDialog.this.dialog.setModal(true);
                BackGroundImageDialog.this.dialog.setAlwaysOnTop(true);
                return null;
            }
        }.waitForEDT();

    }
}