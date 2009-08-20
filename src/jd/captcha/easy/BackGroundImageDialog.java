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

public class BackGroundImageDialog implements ActionListener {

    public BackGroundImageDialog(BackGroundImageManager bgim) {
        this.bgim = bgim;
    }

    public BackGroundImage dialogImage;
    private BackGroundImageManager bgim;
    private ImageComponent bg1, bgv;
    private BackGroundImage ret = null;
    private JDialog dialog;
    private JButton btColorChoose, btPreview, btFinished, btLoadBackgroundImage, btCreateBackgroundFilter;
    private JComboBox mode;
    private JSpinner thresholdSpinner;
    private JColorChooser colorChooser;
    private int threshold = 2;
    private byte modeByte = CPoint.LAB_DIFFERENCE;
    private JPanel images;

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
        bgim.getCaptchaImage().reset();

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

    private void initComponents() {
        initCaptchaImages();
        initDialog();
        new GuiRunnable<Object>() {
            public Object runSave() {
                thresholdSpinner = new JSpinner(new SpinnerNumberModel(threshold, 0, 360, 1));
                thresholdSpinner.setToolTipText("Threshold");
                btLoadBackgroundImage = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.loadimage", "Load BackgroundImage"));
                btCreateBackgroundFilter = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.generate", "Generate Backgroundfilter"));
                Color defColor = Color.WHITE;
                if (dialogImage != null) defColor = new Color(dialogImage.getColor());
                colorChooser = new JColorChooser(defColor);
                btColorChoose = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.deletecolor", "Deletecolor"));
                btPreview = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.imagepreview", "Preview"));
                if (dialogImage == null) btPreview.setEnabled(false);
                mode = new JComboBox(ColorMode.cModes);
                mode.setSelectedItem(new ColorMode(modeByte));
                btFinished = new JButton(JDL.L("easycaptcha.finished", "finish"));
                return null;
            }
        }.waitForEDT();

    }

    private void init() {
        initComponents();

        new GuiRunnable<Object>() {
            public Object runSave() {
                JPanel box = new JPanel();
                box.setLayout(new GridBagLayout());
                GridBagConstraints gbc = Utilities.getGBC(0, 0, 1, 1);
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1;
                gbc.weightx = 1;
                box.add(images, gbc);
                Box menu = new Box(BoxLayout.X_AXIS);
                menu.add(btLoadBackgroundImage);
                menu.add(btCreateBackgroundFilter);
                menu.add(btColorChoose);
                menu.add(btPreview);
                menu.add(thresholdSpinner);
                menu.add(mode);
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

    public BackGroundImage getNewBackGroundImage() {
        init();
        return ret;
    }

    private void addActionListeners() {
        btColorChoose.addActionListener(this);
        btPreview.addActionListener(this);
        btFinished.addActionListener(this);
        btLoadBackgroundImage.addActionListener(this);
        btCreateBackgroundFilter.addActionListener(this);
        mode.addActionListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == btPreview) {
            dialogImage.setDistance((Integer) thresholdSpinner.getValue());
            dialogImage.setColorDistanceMode(modeByte);
            dialogImage.setColor(colorChooser.getColor().getRGB());

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
        } else if (e.getSource() == btColorChoose) {
            new GuiRunnable<Object>() {
                public Object runSave() {

                    JDialog dialog = JColorChooser.createDialog(colorChooser, JDL.L("easycaptcha.addbackgroundimagedialog.deletecolor", "Deletecolor"), true, colorChooser, null, null);
                    dialog.setVisible(true);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                    return null;
                }
            }.waitForEDT();
        } else if (e.getSource() == btFinished) {
            bgim.add(dialogImage);

            new GuiRunnable<Object>() {
                public Object runSave() {
                    dialog.dispose();

                    return null;
                }
            }.waitForEDT();
            ret = dialogImage;
        } else if (e.getSource() == btLoadBackgroundImage) {
            File fch = JDFileChooser.getFile(JDFileChooser.ImagesOnly);
            File fout = new File(bgim.methode.file, "mask_" + JDHash.getMD5(fch) + "." + JDIO.getFileExtension(fch));
            JDIO.copyFile(fch, fout);
            dialogImage = new BackGroundImage();
            dialogImage.setBackgroundImage(fout.getName());
            dialogImage.setColor(colorChooser.getColor().getRGB());
            dialogImage.setDistance((Integer) thresholdSpinner.getValue());
            dialogImage.setColorDistanceMode(modeByte);
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
        } else if (e.getSource() == btCreateBackgroundFilter) {

            File fout = BackgroundFilterCreater.create(bgim.methode);
            dialogImage = new BackGroundImage();
            dialogImage.setBackgroundImage(fout.getName());
            dialogImage.setColor(colorChooser.getColor().getRGB());
            dialogImage.setDistance((Integer) thresholdSpinner.getValue());
            dialogImage.setColorDistanceMode(modeByte);
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
        } else if (e.getSource() == mode) {
            modeByte = ((ColorMode) mode.getSelectedItem()).mode;
        }
    }

}
