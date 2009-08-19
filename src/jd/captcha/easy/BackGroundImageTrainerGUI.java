package jd.captcha.easy;

import java.awt.BorderLayout;
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
import jd.captcha.pixelgrid.PixelGrid;
import jd.nutils.JDHash;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.userio.DummyFrame;
import jd.nutils.Screen;
import jd.captcha.gui.ImageComponent;
import jd.utils.locale.JDL;
import jd.captcha.utils.Utilities;
import jd.nutils.io.JDIO;
import jd.gui.swing.GuiRunnable;

public class BackGroundImageTrainerGUI {
    private JDialog mainDialog;
    private JPanel panel, imageBox;
    private ImageComponent bg1, bgv;
    private BackGroundImage dialogImage;
    private BackGroundImageManager backGroundImageManager;
    public int zoom = 400;
    JPanel images;
    private JComboBox mode;

    private void addBackgroundImageDialog() {
        backGroundImageManager.getCaptchaImage().reset();
        backGroundImageManager.getCaptchaImage().setOrgGrid(PixelGrid.getGridCopy(backGroundImageManager.getCaptchaImage().grid));

        final Image image = backGroundImageManager.getScaledCaptchaImage(zoom);

        new GuiRunnable<Object>() {
            public Object runSave() {
                final JDialog dialog = new JDialog(DummyFrame.getDialogParent());
                dialog.setTitle(JDL.L("easycaptcha.addbackgroundimagedialog.title", "Add BackgroundImage"));
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setModal(true);
                JPanel box = new JPanel();
                box.setLayout(new GridBagLayout());

                images = new JPanel();

                images.setBorder(new TitledBorder(JDL.L("easycaptcha.images", "Images:")));

                images.setLayout(new BoxLayout(images, BoxLayout.Y_AXIS));

                images.add(new JLabel(JDL.L("easycaptcha.orginal", "Original:")));
                bg1 = new ImageComponent(image);

                images.add(bg1);

                images.add(Box.createRigidArea(new Dimension(0, 10)));

                images.add(new JLabel(JDL.L("easycaptcha.addbackgroundimagedialog.imagepreview", "Preview") + ":"));
                int tol = 2;
                byte modeByte = CPoint.LAB_DIFFERENCE;
                if (dialogImage != null) {
                    tol = dialogImage.getDistance();
                    modeByte = dialogImage.getColorDistanceMode();
                    backGroundImageManager.add(dialogImage);
                    backGroundImageManager.clearCaptchaAll();
                    backGroundImageManager.remove(dialogImage);
                } else
                    backGroundImageManager.clearCaptchaAll();

                bgv = new ImageComponent(backGroundImageManager.getScaledCaptchaImage(zoom));
                images.add(bgv);
                GridBagConstraints gbc = Utilities.getGBC(0, 0, 1, 1);
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1;
                gbc.weightx = 1;

                box.add(images, gbc);

                final JSpinner tolleranceSP = new JSpinner(new SpinnerNumberModel(tol, 0, 360, 1));
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

                final JButton btPreview = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.imagepreview", "Preview"));
                if (dialogImage == null) btPreview.setEnabled(false);
                btCreateBackgroundFilter.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {

                        File fout = BackgroundFilterCreater.create(backGroundImageManager.methode);
                        dialogImage = new BackGroundImage();
                        dialogImage.setBackgroundImage(fout.getName());
                        dialogImage.setColor(chooser.getColor().getRGB());
                        dialogImage.setDistance((Integer) tolleranceSP.getValue());
                        dialogImage.setColorDistanceMode(((ColorMode) mode.getSelectedItem()).mode);
                        backGroundImageManager.add(dialogImage);
                        backGroundImageManager.clearCaptchaAll();
                        backGroundImageManager.remove(dialogImage);
                        btPreview.setEnabled(true);
                        final Image image2 = backGroundImageManager.getScaledCaptchaImage(zoom);

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
                        File fout = new File(backGroundImageManager.methode.file, "mask_" + JDHash.getMD5(fch) + "." + JDIO.getFileExtension(fch));
                        JDIO.copyFile(fch, fout);
                        dialogImage = new BackGroundImage();
                        dialogImage.setBackgroundImage(fout.getName());
                        dialogImage.setColor(chooser.getColor().getRGB());
                        dialogImage.setDistance((Integer) tolleranceSP.getValue());
                        dialogImage.setColorDistanceMode(((ColorMode) mode.getSelectedItem()).mode);
                        backGroundImageManager.add(dialogImage);
                        backGroundImageManager.clearCaptchaAll();
                        backGroundImageManager.remove(dialogImage);
                        btPreview.setEnabled(true);
                        final Image image2 = backGroundImageManager.getScaledCaptchaImage(zoom);

                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                bgv.image = image2;
                                bgv.repaint();
                                return null;
                            }
                        }.waitForEDT();
                    }
                });

                menu.add(btPreview);
                btPreview.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        dialogImage.setDistance((Integer) tolleranceSP.getValue());
                        dialogImage.setColorDistanceMode(((ColorMode) mode.getSelectedItem()).mode);
                        dialogImage.setColor(chooser.getColor().getRGB());

                        backGroundImageManager.add(dialogImage);
                        backGroundImageManager.clearCaptchaAll();
                        backGroundImageManager.remove(dialogImage);
                        final Image image2 = backGroundImageManager.getScaledCaptchaImage(zoom);

                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                bgv.image = image2;
                                bgv.repaint();
                                return null;
                            }
                        }.waitForEDT();
                    }
                });
                menu.add(tolleranceSP);
                mode = new JComboBox(ColorMode.cModes);
                mode.setSelectedItem(new ColorMode(modeByte));
                menu.add(mode);

                JButton btf = new JButton(JDL.L("easycaptcha.finished", "finish"));
                btf.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        backGroundImageManager.add(dialogImage);

                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                dialog.dispose();

                                return null;
                            }
                        }.waitForEDT();
                        showImage(dialogImage);
                        mainDialog.pack();
                    }
                });
                menu.add(btf);
                gbc.gridy = 1;
                box.add(menu, gbc);
                dialog.add(box);
                dialog.pack();
                dialog.setVisible(true);
                return null;
            }
        }.waitForEDT();
    }

    public void initGui() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                mainDialog = new JDialog(DummyFrame.getDialogParent());
                mainDialog.setModal(true);
                panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                mainDialog.setLayout(new BorderLayout());
                mainDialog.add(new JScrollPane(panel), BorderLayout.CENTER);
                mainDialog.setTitle(JDL.L("easycaptcha.backgroundimagetrainer.title", "BackgroundImage Trainer"));
                return null;
            }
        }.waitForEDT();

        showImages();
        new GuiRunnable<Object>() {
            public Object runSave() {
                final JButton add = new JButton(JDL.L("gui.menu.add", "Add"));
                add.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent arg0) {
                        dialogImage = null;
                        addBackgroundImageDialog();
                    }

                });
                JButton exit = new JButton(JDL.L("easycaptcha.backgroundimagetrainer.saveandexit", "Save and Exit"));
                exit.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        backGroundImageManager.save();
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                mainDialog.dispose();
                                return null;
                            }
                        }.waitForEDT();
                    }
                });
                Box box = new Box(BoxLayout.X_AXIS);
                box.add(add);
                box.add(exit);
                panel.add(box);
                mainDialog.pack();
                mainDialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), mainDialog));
                mainDialog.setVisible(true);
                return null;
            }
        }.waitForEDT();
    }

    private void showImage(final BackGroundImage bgio) {

        new GuiRunnable<Object>() {
            public Object runSave() {
                final JPanel tmpPanel = new JPanel();
                final ImageComponent ic = new ImageComponent(bgio.getImage(backGroundImageManager.methode));
                tmpPanel.add(ic, BorderLayout.WEST);

                final JButton edit = new JButton(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit", "Edit"));
                final JButton del = new JButton(JDL.L("gui.component.textarea.context.delete", "Delete"));

                edit.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                tmpPanel.remove(ic);
                                tmpPanel.remove(edit);
                                tmpPanel.remove(del);

                                dialogImage = bgio;
                                backGroundImageManager.remove(bgio);
                                tmpPanel.revalidate();
                                mainDialog.pack();
                                addBackgroundImageDialog();
                                return null;
                            }
                        }.waitForEDT();

                    }
                });
                del.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                tmpPanel.remove(ic);
                                tmpPanel.remove(del);
                                tmpPanel.remove(edit);
                                backGroundImageManager.remove(bgio);
                                tmpPanel.revalidate();
                                mainDialog.pack();
                                return null;
                            }
                        }.waitForEDT();

                    }
                });
                tmpPanel.add(edit, BorderLayout.SOUTH);
                tmpPanel.add(del, BorderLayout.EAST);
                imageBox.add(tmpPanel);
                return null;
            }
        }.waitForEDT();

    }

    private void showImages() {
        // Images initialisiert
        new GuiRunnable<Object>() {
            public Object runSave() {
                imageBox = new JPanel();
                imageBox.setLayout(new BoxLayout(imageBox, BoxLayout.Y_AXIS));
                panel.add(imageBox);
                return null;
            }
        }.waitForEDT();
        for (BackGroundImage bgi : backGroundImageManager.getBackgroundList()) {
            bgi.getImage(backGroundImageManager.methode);
            showImage(bgi);
        }
    }

    public BackGroundImageTrainerGUI(EasyFile methode) {
        backGroundImageManager = new BackGroundImageManager(methode.getRandomCaptcha());
    }

    public BackGroundImageTrainerGUI(String hoster) {
        this(new EasyFile(hoster));
    }

}
