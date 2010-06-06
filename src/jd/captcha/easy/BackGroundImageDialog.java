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
import javax.swing.border.TitledBorder;

import jd.captcha.gui.ImageComponent;
import jd.captcha.utils.Utilities;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.nutils.JDHash;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.utils.locale.JDL;

public class BackGroundImageDialog implements ActionListener {

    public BackGroundImageDialog(BackGroundImageManager bgim, JFrame owner) {
        this.bgim = bgim;
        this.owner = owner;
    }

    private JFrame owner;
    public BackGroundImage workingImage;
    private BackGroundImageManager bgim;
    private ImageComponent bg1, bgv, bgmask;
    private BackGroundImage ret = null;
    private JDialog dialog;
    private JButton btColorChoose, btPreview, btFinished, btLoadBackgroundImage, btCreateBackgroundFilter;
    private JComboBox colorModeBox;
    private JSpinner thresholdSpinner;
    private JColorChooser colorChooser;
    private int threshold = 2;
    private byte colorMode = CPoint.RGB_DIFFERENCE3;
    private JPanel imagePanel;

    public BackGroundImage getNewBackGroundImage() {
        init();
        return ret;
    }

    private void init() {
        initDialog();
        initCaptchaImages();
        initComponents();
        addComponentsToDialog();
    }

    private void addComponentsToDialog() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                JPanel box = new JPanel();
                box.setLayout(new GridBagLayout());
                GridBagConstraints gbc = Utilities.getGBC(0, 0, 1, 1);
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1;
                gbc.weightx = 1;
                box.add(imagePanel, gbc);
                Box menu = new Box(BoxLayout.X_AXIS);
                menu.add(btLoadBackgroundImage);
                menu.add(btCreateBackgroundFilter);
                menu.add(btColorChoose);
                menu.add(btPreview);
                menu.add(thresholdSpinner);
                menu.add(colorModeBox);
                menu.add(btFinished);
                gbc.gridy = 1;
                box.add(menu, gbc);
                dialog.add(box);
                dialog.pack();
                addActionListeners();
                dialog.setVisible(true);
                return null;
            }
        }.waitForEDT();
    }

    private void initDialog() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                dialog = new JDialog(owner);
                dialog.setTitle(JDL.L("easycaptcha.addbackgroundimagedialog.title", "Add BackgroundImage"));
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setModal(true);
                dialog.setAlwaysOnTop(true);
                return null;
            }
        }.waitForEDT();

    }

    private void initCaptchaImages() {
        bgim.resetCaptcha();

        final Image image = bgim.getScaledCaptchaImage();

        new GuiRunnable<Object>() {
            public Object runSave() {
                imagePanel = new JPanel();

                imagePanel.setBorder(new TitledBorder(JDL.L("easycaptcha.images", "Images:")));

                imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
                imagePanel.add(new JLabel(JDL.L("easycaptcha.mask", "Mask:")));
                if (workingImage != null)
                    bgmask = new ImageComponent(workingImage.getImage(bgim.methode));
                else {
                    BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ig2 = bi.createGraphics();
                    ig2.fillRect(0, 0, image.getWidth(null) - 1, image.getHeight(null) - 1);
                    bgmask = new ImageComponent(bi);

                }

                imagePanel.add(bgmask);

                imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));

                imagePanel.add(new JLabel(JDL.L("easycaptcha.orginal", "Original:")));
                bg1 = new ImageComponent(image);

                imagePanel.add(bg1);

                imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));

                imagePanel.add(new JLabel(JDL.L("easycaptcha.addbackgroundimagedialog.imagepreview", "Preview") + ":"));
                if (workingImage != null) {
                    threshold = workingImage.getDistance();
                    colorMode = workingImage.getColorDistanceMode();
                    bgim.clearCaptchaPreview(workingImage);
                }

                bgv = new ImageComponent(bgim.getScaledCaptchaImage());
                imagePanel.add(bgv);
                return null;
            }
        }.waitForEDT();
    }

    private void initComponents() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                thresholdSpinner = new JSpinner(new SpinnerNumberModel(threshold, 0, 360, 1));
                thresholdSpinner.setToolTipText("Threshold");
                btLoadBackgroundImage = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.loadimage", "Load BackgroundImage"));
                btCreateBackgroundFilter = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.generate", "Generate Backgroundfilter"));
                Color defColor = Color.WHITE;
                if (workingImage != null) defColor = new Color(workingImage.getColor());
                colorChooser = new JColorChooser(defColor);
                btColorChoose = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.deletecolor", "Deletecolor"));
                btPreview = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.imagepreview", "Preview"));
                if (workingImage == null) btPreview.setEnabled(false);
                colorModeBox = new JComboBox(ColorMode.cModes);
                colorModeBox.setSelectedItem(new ColorMode(colorMode));
                btFinished = new JButton(JDL.L("easycaptcha.finished", "finish"));
                return null;
            }
        }.waitForEDT();

    }

    private void addActionListeners() {
        btColorChoose.addActionListener(this);
        btPreview.addActionListener(this);
        btFinished.addActionListener(this);
        btLoadBackgroundImage.addActionListener(this);
        btCreateBackgroundFilter.addActionListener(this);
        colorModeBox.addActionListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        new GuiRunnable<Object>() {
            public Object runSave() {
                dialog.setAlwaysOnTop(false);
                return null;
            }
        }.waitForEDT();
        if (e.getSource() == btPreview) {
            workingImage.setDistance((Integer) thresholdSpinner.getValue());
            workingImage.setColorDistanceMode(colorMode);
            workingImage.setColor(colorChooser.getColor().getRGB());
            bgim.clearCaptchaPreview(workingImage);
            final Image image2 = bgim.getScaledCaptchaImage();
            new GuiRunnable<Object>() {
                public Object runSave() {
                    bgv.image = image2;
                    bgv.repaint();
                    return null;
                }
            }.waitForEDT();
        } else if (e.getSource() == btColorChoose) {
            new GuiRunnable<Object>() {
                public Object runSave() {

                    JDialog dialog = JColorChooser.createDialog(colorChooser, JDL.L("easycaptcha.addbackgroundimagedialog.deletecolor", "Deletecolor"), true, colorChooser, null, null);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.setAlwaysOnTop(true);
                    dialog.setVisible(true);
                    return null;
                }
            }.waitForEDT();
        } else if (e.getSource() == btFinished) {
            // wird in der BackGroundImageGUIList gemacht
            // if (dialogImage != null) bgim.add(dialogImage);

            new GuiRunnable<Object>() {
                public Object runSave() {
                    dialog.dispose();

                    return null;
                }
            }.waitForEDT();
            workingImage.setColor(colorChooser.getColor().getRGB());
            workingImage.setDistance((Integer) thresholdSpinner.getValue());
            workingImage.setColorDistanceMode(colorMode);
            ret = workingImage;
            return;
        } else if (e.getSource() == btLoadBackgroundImage) {
            File[] fch = UserIOGui.getInstance().requestFileChooser(null, null, null, new JDFileFilter(null, ".jpg|.png|.gif|.jpeg|.bmp", true), null);
            if (fch == null || fch.length == 0) return;

            File fout = new File(bgim.methode.file, "mask_" + JDHash.getMD5(fch[0]) + "." + JDIO.getFileExtension(fch[0]));
            JDIO.copyFile(fch[0], fout);
            workingImage = new BackGroundImage();
            workingImage.setBackgroundImage(fout.getName());
            workingImage.setColor(colorChooser.getColor().getRGB());
            workingImage.setDistance((Integer) thresholdSpinner.getValue());
            workingImage.setColorDistanceMode(colorMode);
            bgim.clearCaptchaPreview(workingImage);
            btPreview.setEnabled(true);
            final Image image2 = bgim.getScaledCaptchaImage();

            new GuiRunnable<Object>() {
                public Object runSave() {
                    bgv.image = image2;
                    bgmask.image = workingImage.getImage(bgim.methode);
                    bgmask.repaint();
                    bgv.repaint();
                    return null;
                }
            }.waitForEDT();
        } else if (e.getSource() == btCreateBackgroundFilter) {

            File fout = BackgroundFilterCreater.create(bgim.methode);
            if (fout != null && fout.exists()) {
                workingImage = new BackGroundImage();
                workingImage.setBackgroundImage(fout.getName());
                workingImage.setColor(colorChooser.getColor().getRGB());
                workingImage.setDistance((Integer) thresholdSpinner.getValue());
                workingImage.setColorDistanceMode(colorMode);
                bgim.clearCaptchaPreview(workingImage);
                btPreview.setEnabled(true);
                final Image image2 = bgim.getScaledCaptchaImage();

                new GuiRunnable<Object>() {
                    public Object runSave() {
                        bgv.image = image2;
                        bgmask.image = workingImage.getImage(bgim.methode);
                        bgmask.repaint();
                        bgv.repaint();
                        return null;
                    }
                }.waitForEDT();
            }
        } else if (e.getSource() == colorModeBox) {
            colorMode = ((ColorMode) colorModeBox.getSelectedItem()).mode;
        }
        new GuiRunnable<Object>() {
            public Object runSave() {
                dialog.setAlwaysOnTop(true);
                return null;
            }
        }.waitForEDT();
    }
}
