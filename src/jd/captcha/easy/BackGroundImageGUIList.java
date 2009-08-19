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

public class BackGroundImageGUIList {
    public BackGroundImageGUIList(EasyFile methode) {
        manager = new BackGroundImageManager(methode);
    }

    private JDialog mainDialog;
    private JPanel panel, imageBox;
    private BackGroundImageManager manager;

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
            bgi.getImage(manager.methode);
            showImage(bgi);
        }
    }

    void showImage(final BackGroundImage bgio) {
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
                                tmpPanel.remove(ic);
                                tmpPanel.remove(edit);
                                tmpPanel.remove(del);

                                BackGroundImage dialogImage = bgio;
                                manager.remove(bgio);
                                tmpPanel.revalidate();
                                mainDialog.pack();
                                BackGroundImageDialog bgiaDialog = new BackGroundImageDialog(manager);
                                bgiaDialog.dialogImage = dialogImage;
                                dialogImage = bgiaDialog.getNewBackGroundImage();
                                showImage(dialogImage);
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
                        BackGroundImageDialog bgiaDialog = new BackGroundImageDialog(manager);
                        showImage(bgiaDialog.getNewBackGroundImage());
                    }

                });
                JButton exit = new JButton(JDL.L("easycaptcha.backgroundimagetrainer.saveandexit", "Save and Exit"));
                exit.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        manager.save();
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

}
