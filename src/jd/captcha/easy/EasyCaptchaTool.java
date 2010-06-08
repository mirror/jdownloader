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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import jd.JDInit;
import jd.captcha.JAntiCaptcha;
import jd.captcha.easy.load.LoadCaptchas;
import jd.config.SubConfiguration;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.events.waitcursor.WaitCursorEventQueue;
import jd.gui.swing.jdgui.views.settings.JDLabelListRenderer;
import jd.gui.swing.laf.LookAndFeelController;
import jd.gui.userio.DummyFrame;
import jd.nutils.Screen;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class EasyCaptchaTool {
    public static SubConfiguration config = SubConfiguration.getConfig("EasyCaptcha");
    public static final String CONFIG_LASTSESSION = "CONFIG_LASTSESSION";
    public static final String CONFIG_AUTHOR = "AUTHOR";
    public static JFrame ownerFrame = DummyFrame.getDialogParent();

    public static EasyMethodFile showMethodes() {
        final EasyMethodFile ef = new EasyMethodFile();
        new GuiRunnable<Object>() {
            public Object runSave() {
                final JDialog cHosterDialog = new JDialog(ownerFrame);
                cHosterDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                cHosterDialog.setTitle(JDL.L("easycaptcha.tool.mothodedialog.title", "EasyCaptcha Methodes"));
                cHosterDialog.setModal(true);
                cHosterDialog.setAlwaysOnTop(true);
                Box box = new Box(BoxLayout.Y_AXIS);

                JPanel pa = new JPanel(new GridLayout(2, 1));

                pa.add(new JLabel(JDL.L("easycaptcha.tool.mothodedialog.selectmethode", "select the methode:")));
                EasyMethodFile[] paths = EasyMethodFile.getMethodeList();

                final JComboBox combox = new JComboBox(paths);
                combox.setRenderer(new JDLabelListRenderer());
                combox.setMinimumSize(new Dimension(24, 70));
                pa.add(combox);
                box.add(pa);
                pa = new JPanel(new GridLayout(1, 2));
                JButton ok = new JButton(JDL.L("gui.btn_ok", "OK"));
                pa.add(ok);
                ok.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        EasyMethodFile ef2 = (EasyMethodFile) combox.getSelectedItem();
                        if (ef2 != null) {
                            ef.file = ef2.file;
                            cHosterDialog.dispose();

                        }
                    }
                });

                JButton cancel = new JButton(JDL.L("gui.btn_cancel", "Cancel"));
                pa.add(cancel);
                cancel.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        cHosterDialog.dispose();
                    }
                });
                box.add(pa);

                cHosterDialog.add(box);
                cHosterDialog.pack();
                cHosterDialog.setLocation(Screen.getCenterOfComponent(ownerFrame, cHosterDialog));
                cHosterDialog.setVisible(true);

                return null;
            }
        }.waitForEDT();
        if (ef.file == null) return null;
        return ef;
    }

    private static EasyMethodFile getCaptchaMethode() {
        return new GuiRunnable<EasyMethodFile>() {
            public EasyMethodFile runSave() {
                final EasyMethodFile ef = new EasyMethodFile();
                final JDialog dialog = new JDialog(ownerFrame);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setTitle(JDL.L("easycaptcha.tool.title", "EasyCaptcha"));
                dialog.setModal(true);
                JPanel box = new JPanel(new GridLayout(3, 1));
                JButton btcs = new JButton(JDL.L("easycaptcha.tool.continuelastsession", "Continue Last Session"));
                final EasyMethodFile lastEF = (EasyMethodFile) config.getProperty(CONFIG_LASTSESSION, null);
                if (lastEF == null) btcs.setEnabled(false);
                btcs.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        ef.file = lastEF.file;
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                dialog.dispose();
                                return null;
                            }
                        }.waitForEDT();
                    }
                });
                box.add(btcs);
                JButton btl = new JButton(JDL.L("easycaptcha.tool.loadmethode", "Load Methode"));
                btl.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        EasyMethodFile ef2 = showMethodes();
                        if (ef2 != null) {
                            ef.file = ef2.file;
                            dialog.dispose();
                        }
                    }
                });

                box.add(btl);
                JButton btc = new JButton(JDL.L("easycaptcha.tool.createmethode", "Create Methode"));
                btc.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                final JDialog cHosterDialog = new JDialog(ownerFrame);
                                cHosterDialog.setAlwaysOnTop(true);
                                cHosterDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                                cHosterDialog.setTitle(JDL.L("easycaptcha.tool.title", "EasyCaptcha"));
                                cHosterDialog.setModal(true);
                                JPanel box = new JPanel(new GridLayout(4, 2));
                                final JTextField tfHoster = new JTextField();
                                box.add(new JLabel(JDL.L("gui.column_host", "Host") + ":"));
                                box.add(tfHoster);
                                final JTextField tfAuthor = new JTextField(config.getStringProperty(CONFIG_AUTHOR, "JDTeam"));
                                box.add(new JLabel(JDL.L("gui.config.jac.column.author", "Author") + ":"));
                                box.add(tfAuthor);
                                final JSpinner spMaxLetters = new JSpinner(new SpinnerNumberModel(4, 1, 40, 1));
                                box.add(new JLabel(JDL.L("easycaptcha.tool.maxletternum", "Maximal number of letters") + ":"));
                                box.add(spMaxLetters);
                                JButton ok = new JButton(JDL.L("gui.btn_ok", "OK"));
                                box.add(ok);
                                ok.addActionListener(new ActionListener() {

                                    public void actionPerformed(ActionEvent e) {
                                        if (tfHoster.getText() != null && !tfHoster.getText().matches("\\s*")) {

                                            ef.file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory(), tfHoster.getText());
                                            dialog.dispose();
                                            cHosterDialog.dispose();
                                            if (tfAuthor.getText() != null && !tfAuthor.getText().matches("\\s*")) config.setProperty(CONFIG_AUTHOR, tfAuthor.getText());
                                            CreateHoster.create(new EasyMethodFile("easycaptcha"), ef, tfAuthor.getText(), (Integer) spMaxLetters.getValue());

                                        } else {
                                            JOptionPane.showConfirmDialog(null, JDL.L("easycaptcha.tool.warning.hostnamemissing", "the hostname is missing"), JDL.L("easycaptcha.tool.warning.hostnamemissing", "the hostname is missing"), JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
                                        }
                                    }
                                });
                                JButton cancel = new JButton(JDL.L("gui.btn_cancel", "Cancel"));
                                box.add(cancel);
                                cancel.addActionListener(new ActionListener() {

                                    public void actionPerformed(ActionEvent e) {
                                        cHosterDialog.dispose();
                                    }
                                });
                                cHosterDialog.add(box);
                                cHosterDialog.pack();
                                cHosterDialog.setLocation(Screen.getCenterOfComponent(ownerFrame, cHosterDialog));
                                cHosterDialog.setVisible(true);
                                return null;
                            }
                        }.waitForEDT();
                    }
                });

                box.add(btc);
                dialog.add(box);
                dialog.pack();
                dialog.setLocation(Screen.getCenterOfComponent(ownerFrame, dialog));
                dialog.setVisible(true);
                if (ef.file != null) {
                    config.setProperty(CONFIG_LASTSESSION, ef);
                    saveConfig();
                    return ef;
                } else
                    return null;
            }
        }.getReturnValue();

    }

    public static void saveConfig() {
        config.save();
        JDUtilities.getConfiguration().save();
    }

    public static void checkReadyToTrain(final EasyMethodFile meth, final JButton btnTrain) {
        new GuiRunnable<Object>() {
            public Object runSave() {
                btnTrain.setEnabled(meth.isReadyToTrain());
                return null;
            }
        }.waitForEDT();
    }

    public static void showToolKid(final EasyMethodFile meth) {

        CreateHoster.setImageType(meth);
        File folder = meth.getCaptchaFolder();
        if (!folder.exists() || folder.list().length < 1) {
            System.exit(0);
        }
        final JAntiCaptcha jac = new JAntiCaptcha(meth.getName());
        final JDialog dialog = new JDialog(ownerFrame);
        dialog.addWindowListener(new WindowListener() {
            public void windowActivated(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowOpened(WindowEvent e) {
            }
        });
        dialog.setLocation(Screen.getCenterOfComponent(ownerFrame, dialog));
        dialog.setTitle(JDL.L("easycaptcha.tool.title", "EasyCaptcha"));
        final JPanel box = new JPanel(new GridLayout(5, 1));
        final JButton btnTrain = new JButton(JDL.L("easycaptcha.tool.btn.train", "Train"));
        btnTrain.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    public void run() {
                        jac.trainAllCaptchas(meth.getCaptchaFolder().getAbsolutePath());
                    }
                }).start();
            }
        });
        box.add(btnTrain);
        JButton btnShowLetters = new JButton(JDL.L("easycaptcha.tool.btn.letterdb", "Show Letter Database"));
        btnShowLetters.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                new Thread(new Runnable() {

                    public void run() {
                        jac.displayLibrary();
                    }
                }).start();

            }
        });
        box.add(btnShowLetters);
        final JButton btnColorTrainer = new JButton(JDL.L("easycaptcha.tool.btn.colortrainer", "Train Colors"));
        btnColorTrainer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                new Thread(new Runnable() {

                    public void run() {
                        ColorTrainerGUI.getColor(meth, ownerFrame);
                        checkReadyToTrain(meth, btnTrain);
                    }
                }).start();

            }
        });
        box.add(btnColorTrainer);
        final JButton btnBackGround = new JButton(JDL.L("easycaptcha.tool.btn.background", "Remove Backgrounds"));
        btnBackGround.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                new Thread(new Runnable() {

                    public void run() {
                        new BackGroundImageGUIList(meth, ownerFrame).show();
                    }
                }).start();

            }
        });
        box.add(btnBackGround);
        JButton btnColorLoadCaptchas = new JButton(JDL.L("easycaptcha.tool.btn.loadcaptchas", "Load Captchas"));
        btnColorLoadCaptchas.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                new Thread(new Runnable() {

                    public void run() {
                        new LoadCaptchas(ownerFrame, meth.file.getName()).start();
                    }
                }).start();

            }
        });
        if (!meth.isEasyCaptchaMethode()) {
            btnColorTrainer.setEnabled(false);
            btnBackGround.setEnabled(false);
        } else {
            checkReadyToTrain(meth, btnTrain);
        }

        box.add(btnColorLoadCaptchas);
        dialog.add(box);
        dialog.pack();
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            new JDInit().loadConfiguration();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LookAndFeelController.setUIManager();
        WaitCursorEventQueue.initEventQueue();

        EasyMethodFile meth = EasyCaptchaTool.getCaptchaMethode();

        showToolKid(meth);
    }

}
