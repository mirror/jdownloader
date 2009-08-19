package jd.captcha.easy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import jd.captcha.gui.ImageComponent;
import jd.captcha.utils.Utilities;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.userio.DummyFrame;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.utils.locale.JDL;

public class BackGroundImageDialog {
    private BackGroundImageManager bgim;

    public BackGroundImageDialog(BackGroundImageManager bgim) {
        this.bgim = bgim;
    }

    private ImageComponent bg1, bgv;
    public BackGroundImage dialogImage;
    private BackGroundImage ret = null;
    private JDialog dialog;
    private JComboBox mode;
    private int threshold = 2;
    private byte modeByte = CPoint.LAB_DIFFERENCE;

    JPanel images;

    private void initDialog() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                dialog = new JDialog(DummyFrame.getDialogParent());
                dialog.setTitle(JDL.L("easycaptcha.addbackgroundimagedialog.title", "Add BackgroundImage"));
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setModal(true);
                return null;
            }
        }.waitForEDT();

    }

    private void initCaptchaImages() {
        final Image image = bgim.getScaledCaptchaImage();

        new GuiRunnable<Object>() {
            public Object runSave() {
                images = new JPanel();

                images.setBorder(new TitledBorder(JDL.L("easycaptcha.images", "Images:")));

                images.setLayout(new BoxLayout(images, BoxLayout.Y_AXIS));

                images.add(new JLabel(JDL.L("easycaptcha.orginal", "Original:")));
                bg1 = new ImageComponent(image);

                images.add(bg1);

                images.add(Box.createRigidArea(new Dimension(0, 10)));

                images.add(new JLabel(JDL.L("easycaptcha.addbackgroundimagedialog.imagepreview", "Preview") + ":"));
                if (dialogImage != null) {
                    threshold = dialogImage.getDistance();
                    modeByte = dialogImage.getColorDistanceMode();
                    bgim.add(dialogImage);
                    bgim.clearCaptchaAll();
                    bgim.remove(dialogImage);
                } else
                    bgim.clearCaptchaAll();

                bgv = new ImageComponent(bgim.getScaledCaptchaImage());
                images.add(bgv);
                return null;
            }
        }.waitForEDT();
    }

    public BackGroundImage getNewBackGroundImage() {
        bgim.getCaptchaImage().reset();
        initDialog();
        initCaptchaImages();
        return new GuiRunnable<BackGroundImage>() {
            public BackGroundImage runSave() {
                JPanel box = new JPanel();
                box.setLayout(new GridBagLayout());

                GridBagConstraints gbc = Utilities.getGBC(0, 0, 1, 1);
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1;
                gbc.weightx = 1;

                box.add(images, gbc);

                final JSpinner tolleranceSP = new JSpinner(new SpinnerNumberModel(threshold, 0, 360, 1));
                tolleranceSP.setToolTipText("Threshold");
                Box menu = new Box(BoxLayout.X_AXIS);
                JButton btLoadBackgroundImage = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.loadimage", "Load BackgroundImage"));
                menu.add(btLoadBackgroundImage);
                JButton btCreateBackgroundFilter = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.generate", "Generate Backgroundfilter"));
                menu.add(btCreateBackgroundFilter);
                Color defColor = Color.WHITE;
                if (dialogImage != null) defColor = new Color(dialogImage.getColor());
                final JColorChooser chooser = new JColorChooser(defColor);
                JButton btchoose = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.deletecolor", "Deletecolor"));
                menu.add(btchoose);
                final JButton btPreview = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.imagepreview", "Preview"));
                if (dialogImage == null) btPreview.setEnabled(false);
                menu.add(btPreview);
                menu.add(tolleranceSP);
                mode = new JComboBox(ColorMode.cModes);
                mode.setSelectedItem(new ColorMode(modeByte));
                menu.add(mode);
                JButton btf = new JButton(JDL.L("easycaptcha.finished", "finish"));
                menu.add(btf);
                gbc.gridy = 1;
                box.add(menu, gbc);
                dialog.add(box);
                dialog.pack();

                btchoose.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                JDialog dialog = JColorChooser.createDialog(chooser, JDL.L("easycaptcha.addbackgroundimagedialog.deletecolor", "Deletecolor"), true, chooser, null, null);
                                dialog.setVisible(true);
                                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                                return null;
                            }
                        }.waitForEDT();
                    }
                });
                btCreateBackgroundFilter.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {

                        File fout = BackgroundFilterCreater.create(bgim.methode);
                        dialogImage = new BackGroundImage();
                        dialogImage.setBackgroundImage(fout.getName());
                        dialogImage.setColor(chooser.getColor().getRGB());
                        dialogImage.setDistance((Integer) tolleranceSP.getValue());
                        dialogImage.setColorDistanceMode(((ColorMode) mode.getSelectedItem()).mode);
                        bgim.add(dialogImage);
                        bgim.clearCaptchaAll();
                        bgim.remove(dialogImage);
                        btPreview.setEnabled(true);
                        final Image image2 = bgim.getScaledCaptchaImage();

                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                bgv.image = image2;
                                bgv.repaint();
                                return null;
                            }
                        }.waitForEDT();
                    }
                });
                btLoadBackgroundImage.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        File fch = JDFileChooser.getFile(JDFileChooser.ImagesOnly);
                        File fout = new File(bgim.methode.file, "mask_" + JDHash.getMD5(fch) + "." + JDIO.getFileExtension(fch));
                        JDIO.copyFile(fch, fout);
                        dialogImage = new BackGroundImage();
                        dialogImage.setBackgroundImage(fout.getName());
                        dialogImage.setColor(chooser.getColor().getRGB());
                        dialogImage.setDistance((Integer) tolleranceSP.getValue());
                        dialogImage.setColorDistanceMode(((ColorMode) mode.getSelectedItem()).mode);
                        bgim.add(dialogImage);
                        bgim.clearCaptchaAll();
                        bgim.remove(dialogImage);
                        btPreview.setEnabled(true);
                        final Image image2 = bgim.getScaledCaptchaImage();

                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                bgv.image = image2;
                                bgv.repaint();
                                return null;
                            }
                        }.waitForEDT();
                    }
                });
                btPreview.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        dialogImage.setDistance((Integer) tolleranceSP.getValue());
                        dialogImage.setColorDistanceMode(((ColorMode) mode.getSelectedItem()).mode);
                        dialogImage.setColor(chooser.getColor().getRGB());

                        bgim.add(dialogImage);
                        bgim.clearCaptchaAll();
                        bgim.remove(dialogImage);
                        final Image image2 = bgim.getScaledCaptchaImage();

                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                bgv.image = image2;
                                bgv.repaint();
                                return null;
                            }
                        }.waitForEDT();
                    }
                });

                btf.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        bgim.add(dialogImage);

                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                dialog.dispose();

                                return null;
                            }
                        }.waitForEDT();
                        ret = dialogImage;
                    }
                });
                dialog.setVisible(true);
                return ret;
            }
        }.getReturnValue();
    }

}
