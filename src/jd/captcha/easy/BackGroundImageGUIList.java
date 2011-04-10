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


 import jd.captcha.translate.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.captcha.gui.ImageComponent;
import jd.gui.swing.GuiRunnable;
import jd.gui.userio.DummyFrame;
import jd.nutils.Screen;
import jd.utils.locale.JDL;

public class BackGroundImageGUIList implements ActionListener {
    public BackGroundImageGUIList(EasyMethodFile methode, JFrame owner) {
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

                final JButton edit = new JButton(T._.jd_plugins_optional_schedule_maingui_edit());
                final JButton del = new JButton(T._.gui_component_textarea_context_delete());

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
                mainDialog.setTitle(T._.easycaptcha_backgroundimagetrainer_title());
                return null;
            }
        }.waitForEDT();

        showImages();
        new GuiRunnable<Object>() {
            public Object runSave() {
                btAdd = new JButton(T._.gui_menu_add());
                btExit = new JButton(T._.easycaptcha_backgroundimagetrainer_saveandexit());
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
                    System.out.println("bgl:" + bg.getBackgroundImage());
                }
                mainDialog.pack();
            }
            mainDialog.setAlwaysOnTop(true);

        }
    }

}