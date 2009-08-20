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

public class BackGroundImageGUIList implements ActionListener{
    public BackGroundImageGUIList(EasyFile methode) {
        manager = new BackGroundImageManager(methode);
    }

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
    public void addActionListeners()
    {
        btExit.addActionListener(this);
        btAdd.addActionListener(this);
    }
    public void actionPerformed(ActionEvent e) {
        if(e.getSource()==btExit)
        {
        manager.save();
        new GuiRunnable<Object>() {
            public Object runSave() {
                mainDialog.dispose();
                return null;
            }
        }.waitForEDT();
        }
        else if(e.getSource()==btAdd)
        {
            BackGroundImageDialog bgiaDialog = new BackGroundImageDialog(manager);
            showImage(bgiaDialog.getNewBackGroundImage());
        }
    }

}
