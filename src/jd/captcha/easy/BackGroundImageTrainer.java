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
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import jd.captcha.pixelgrid.PixelGrid;

import jd.nutils.JDHash;

import jd.gui.swing.components.JDFileChooser;

import jd.gui.userio.DummyFrame;
import jd.nutils.Screen;
import jd.captcha.gui.ImageComponent;
import jd.utils.locale.JDL;
import jd.controlling.JDLogger;
import jd.captcha.utils.Utilities;
import jd.captcha.pixelgrid.Captcha;
import jd.utils.JDUtilities;
import jd.nutils.io.JDIO;
import jd.gui.swing.GuiRunnable;

public class BackGroundImageTrainer {
    private JDialog mainDialog;
    private JPanel panel, imageBox;
    private Vector<BackGroundImage> c;
    private EasyFile methode;
    private ImageComponent bg1, bgv;
    public Captcha captchaImage;
    private BackGroundImage dialogImage;
    public int zoom = 400;
    JPanel images;
    private JComboBox mode;
    
    private File getBgImagesXmlFile() {
        return new File(methode.getJacinfoXml().getParent(), "bgimages.xml");
    }

    private void addBackgroundImageDialog() {
        captchaImage.reset();
        captchaImage.setOrgGrid(PixelGrid.getGridCopy(captchaImage.grid));

        final Image image = captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);

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
                int tol=2;
                byte modeByte = CPoint.LAB_DIFFERENCE;
                if (dialogImage != null) {
                    tol=dialogImage.getDistance();
                    modeByte=dialogImage.getColorDistanceMode();
                    c.add(dialogImage);
                    clearCaptcha();
                    c.remove(dialogImage);
                } else
                    clearCaptcha();

                bgv = new ImageComponent(captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT));
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
                Color defColor=Color.WHITE;
                if(dialogImage!=null)
                    defColor=new Color(dialogImage.getColor());
                final JColorChooser chooser = new JColorChooser(defColor);
                JButton btchoose = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.deletecolor", "Deletecolor"));
                menu.add(btchoose);
                btchoose.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                JDialog dialog = JColorChooser.createDialog(chooser,
                                        JDL.L("easycaptcha.addbackgroundimagedialog.deletecolor", "Deletecolor"), true, chooser,null, null);
                                dialog.setVisible(true);
                                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                                return null;
                            }
                        }.waitForEDT();
                    }});
                
                final JButton btPreview = new JButton(JDL.L("easycaptcha.addbackgroundimagedialog.imagepreview", "Preview"));
                if(dialogImage==null)
                    btPreview.setEnabled(false);
                btCreateBackgroundFilter.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {

                        File fout = BackgroundFilterCreater.create(methode);
                        dialogImage = new BackGroundImage();
                        dialogImage.setBackgroundImage(fout.getName());
                        dialogImage.setColor(chooser.getColor().getRGB());
                        dialogImage.setDistance((Integer) tolleranceSP.getValue());
                        dialogImage.setColorDistanceMode(((ColorMode) mode.getSelectedItem()).mode);
                        c.add(dialogImage);
                        clearCaptcha();
                        c.remove(dialogImage);
                        btPreview.setEnabled(true);
                        final Image image2 = captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);

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
                        File fch = new GuiRunnable<File>() {
                            public File runSave() {
                                JDFileChooser fc = new JDFileChooser();
                                fc.setVisible(true);
                                fc.setFileSelectionMode(JDFileChooser.FILES_ONLY);
                                fc.setFileFilter(new FileFilter() {

                                    @Override
                                    public boolean accept(File f) {
                                        if (f.isDirectory()) return true;
                                        String nt = f.getName().toLowerCase();
                                        if (nt.endsWith(".jpg") || nt.endsWith(".png") || nt.endsWith(".gif") || nt.endsWith(".jpeg") || nt.endsWith(".bmp")) return true;
                                        return false;
                                    }

                                    @Override
                                    public String getDescription() {
                                        // TODO Auto-generated method stub
                                        return null;
                                    }
                                });

                                fc.showOpenDialog(null);
                                File ret = fc.getSelectedFile();
                                return ret;
                            }
                        }.getReturnValue();
                        String ext = (fch.getName().lastIndexOf(".") == -1) ? "" : fch.getName().substring(fch.getName().lastIndexOf(".") + 1, fch.getName().length());

                        File fout = new File(methode.file, "mask_" + JDHash.getMD5(fch) + "." + ext);
                        JDIO.copyFile(fch, fout);
                        dialogImage = new BackGroundImage();
                        dialogImage.setBackgroundImage(fout.getName());
                        dialogImage.setColor(chooser.getColor().getRGB());
                        dialogImage.setDistance((Integer) tolleranceSP.getValue());
                        dialogImage.setColorDistanceMode(((ColorMode) mode.getSelectedItem()).mode);
                        c.add(dialogImage);
                        clearCaptcha();
                        c.remove(dialogImage);
                        btPreview.setEnabled(true);
                        final Image image2 = captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);

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

                        c.add(dialogImage);
                        clearCaptcha();
                        c.remove(dialogImage);
                        final Image image2 = captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);

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
                mode=new JComboBox(ColorMode.cModes);
                mode.setSelectedItem(new ColorMode(modeByte));
                menu.add(mode);

                JButton btf = new JButton(JDL.L("easycaptcha.finished", "finish"));
                btf.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        c.add(dialogImage);

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
        File[] list = methode.getCaptchaFolder().listFiles();
        int id = (int) (Math.random() * (list.length - 1));
        captchaImage = methode.getJac().createCaptcha(Utilities.loadImage(list[id]));
        captchaImage.setOrgGrid(PixelGrid.getGridCopy(captchaImage.grid));

        load();
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
                        save();
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

    @SuppressWarnings("unchecked")
    public void load() {
        File file = getBgImagesXmlFile();
        if (file.exists())
            c = (Vector<BackGroundImage>) JDIO.loadObject(null, file, true);
        else
            c = new Vector<BackGroundImage>();
    }

    public void save() {
        File file = getBgImagesXmlFile();
        file.getParentFile().mkdirs();
        JDIO.saveObject(null, c, file, null, null, true);
    }

    private void showImage(final BackGroundImage bgio) {

        new GuiRunnable<Object>() {
            public Object runSave() {
                final JPanel tmpPanel = new JPanel();
                final ImageComponent ic = new ImageComponent(bgio.getImage(methode));
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
                                c.remove(bgio);
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
                                c.remove(bgio);
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
                imageBox=new JPanel();
                imageBox.setLayout(new BoxLayout(imageBox, BoxLayout.Y_AXIS));
                panel.add(imageBox);
                return null;
            }
        }.waitForEDT();
        for (BackGroundImage bgi : c) {
            bgi.getImage(methode);
            showImage(bgi);
        }
    }

    public void clearCaptcha(BackGroundImage bgi, Captcha cleanImg) {

        if (cleanImg.getWidth() != captchaImage.getWidth() || cleanImg.getHeight() != captchaImage.getHeight()) {
            if (Utilities.isLoggerActive()) {
                JDLogger.getLogger().info("ERROR Maske und Bild passen nicht zusammmen");
            }
            return;
        }
        int color = bgi.getColor();

        for (int x = 0; x < captchaImage.getWidth(); x++) {
            for (int y = 0; y < captchaImage.getHeight(); y++) {
                int pv = captchaImage.getPixelValue(x, y);
                bgi.setColor(cleanImg.getPixelValue(x, y));
                if (bgi.getColorDifference(pv) < bgi.getDistance()) captchaImage.setPixelValue(x, y, color);
            }
        }
        bgi.setColor(color);
    }

    public void clearCaptcha() {
        Captcha best = null;
        int bestVal = -1;
        BackGroundImage bestBgi = null;
        captchaImage.reset();
        captchaImage.setOrgGrid(PixelGrid.getGridCopy(captchaImage.grid));
        for (BackGroundImage bgi : c) {
            int color = bgi.getColor();
            Image bImage = Utilities.loadImage(new File(methode.file, bgi.getBackgroundImage()));
            if (bImage.getWidth(null) != captchaImage.getWidth() || bImage.getHeight(null) != captchaImage.getHeight()) {
                if (Utilities.isLoggerActive()) {
                    JDLogger.getLogger().info("ERROR Maske und Bild passen nicht zusammmen");
                }
                continue;
            }
            Captcha captcha2 = captchaImage.owner.createCaptcha(bImage);
            int val = 0;
            for (int x = 0; x < captchaImage.getWidth(); x++) {
                for (int y = 0; y < captchaImage.getHeight(); y++) {
                    bgi.setColor(captcha2.getPixelValue(x, y));
                    if (bgi.getColorDifference(captchaImage.getPixelValue(x, y)) < bgi.getDistance()) val++;
                }
            }
            bgi.setColor(color);
            if (val > bestVal) {
                best = captcha2;
                bestVal = val;
                bestBgi = bgi;
            }
        }
        if (best != null) {
            clearCaptcha(bestBgi, best);
        }
    }

    public BackGroundImageTrainer(String hoster) {
        methode = new EasyFile(new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory(), hoster));
    }

}
