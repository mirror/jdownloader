package jd.captcha.easy;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import jd.gui.userio.DummyFrame;
import jd.nutils.Screen;
import jd.captcha.gui.ImageComponent;
import jd.utils.locale.JDL;
import jd.gui.swing.GuiRunnable;

public class BackGroundImageGUIList implements ActionListener {
    public BackGroundImageGUIList(EasyMethodeFile methode, JFrame owner) {
        this.manager = new BackGroundImageManager(methode);
        this.owner = owner;
    }

    private JFrame owner;
    private JDialog mainDialog;
    private JPanel panel, imageBox;
    private BackGroundImageManager manager;
    private JButton btExit, btAdd;

    public void showImages() {
        // Images initialisiert
        new GuiRunnable<Object>() {
            public Object runSave() {
                imageBox = new JPanel();
                imageBox.setLayout(new BoxLayout(imageBox, BoxLayout.Y_AXIS));
                panel.add(imageBox);
                return null;
            }
        }.waitForEDT();
        for (BackGroundImage bgi : manager.getBackgroundList()) {
            if (bgi != null) {
                bgi.getImage(manager.methode);
                showImage(bgi);
            }
        }
    }

    void showImage(final BackGroundImage bgio) {
        if (bgio == null) return;
        new GuiRunnable<Object>() {
            public Object runSave() {
                final JPanel tmpPanel = new JPanel();
                final ImageComponent ic = new ImageComponent(bgio.getImage(manager.methode));
                tmpPanel.add(ic, BorderLayout.WEST);

                final JButton edit = new JButton(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit", "Edit"));
                final JButton del = new JButton(JDL.L("gui.component.textarea.context.delete", "Delete"));

                edit.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new GuiRunnable<Object>() {
                            public Object runSave() {

                                BackGroundImage dialogImage = bgio.clone();
                                mainDialog.setAlwaysOnTop(false);
                                BackGroundImageDialog bgiaDialog = new BackGroundImageDialog(manager, owner);
                                bgiaDialog.workingImage = dialogImage;
                                dialogImage = bgiaDialog.getNewBackGroundImage();
                                mainDialog.setAlwaysOnTop(true);

                                if (dialogImage != null) {
                                    if (!bgio.getBackgroundImage().equals(dialogImage.getBackgroundImage())) {
                                        bgio.setBackgroundImage(dialogImage.getBackgroundImage());
                                        ic.image = bgio.getImage(manager.methode);
                                        ic.revalidate();
                                        ic.repaint();
                                    }
                                    bgio.setColor(dialogImage.getColor());
                                    bgio.setColorDistanceMode(dialogImage.getColorDistanceMode());
                                    bgio.setDistance(dialogImage.getDistance());
                                }
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
                                manager.remove(bgio);
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

    public void show() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                mainDialog = new JDialog(owner);
                mainDialog.setAlwaysOnTop(true);
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
                btAdd = new JButton(JDL.L("gui.menu.add", "Add"));
                btExit = new JButton(JDL.L("easycaptcha.backgroundimagetrainer.saveandexit", "Save and Exit"));
                Box box = new Box(BoxLayout.X_AXIS);
                box.add(btAdd);
                box.add(btExit);
                panel.add(box);
                mainDialog.pack();
                mainDialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), mainDialog));
                addActionListeners();
                mainDialog.setVisible(true);
                return null;
            }
        }.waitForEDT();
    }

    public void addActionListeners() {
        btExit.addActionListener(this);
        btAdd.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btExit) {
            manager.save();
            new GuiRunnable<Object>() {
                public Object runSave() {
                    mainDialog.dispose();
                    return null;
                }
            }.waitForEDT();
        } else if (e.getSource() == btAdd) {
            mainDialog.setAlwaysOnTop(false);
            BackGroundImageDialog bgiaDialog = new BackGroundImageDialog(manager, owner);
            BackGroundImage ret = bgiaDialog.getNewBackGroundImage();
            if (ret != null) {
                manager.add(ret);
                showImage(ret);
                
                for (BackGroundImage bg : manager.getBackgroundList()) {
                    System.out.println("bgl:"+bg.getBackgroundImage());
                }
                mainDialog.pack();
            }
            mainDialog.setAlwaysOnTop(true);

        }
    }

}
