package jd.captcha.easy;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import jd.JDInit;

import jd.captcha.JAntiCaptcha;
import jd.captcha.utils.Utilities;

import jd.gui.swing.jdgui.events.EDTEventQueue;
import jd.gui.swing.laf.LookAndFeelController;

import jd.config.SubConfiguration;
import jd.utils.locale.JDL;
import jd.gui.swing.jdgui.settings.JDLabelListRenderer;
import jd.gui.swing.GuiRunnable;
import jd.utils.JDUtilities;
import jd.nutils.Screen;
import jd.gui.userio.DummyFrame;

public class EasyCaptchaTool {
    public static SubConfiguration config = SubConfiguration.getConfig("EasyCaptcha");
    public static final String CONFIG_LASTSESSION = "CONFIG_LASTSESSION";
    public static final String CONFIG_AUTHOR = "AUTHOR";

    public static EasyFile showMethodes() {
        final EasyFile ef = new EasyFile();
        new GuiRunnable<Object>() {
            public Object runSave() {
                final JDialog cHosterDialog = new JDialog(DummyFrame.getDialogParent());
                cHosterDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                cHosterDialog.setTitle(JDL.L("easycaptcha.tool.mothodedialog.title", "EasyCaptcha Methodes"));
                cHosterDialog.setModal(true);
                Box box = new Box(BoxLayout.Y_AXIS);

                JPanel pa = new JPanel(new GridLayout(2, 1));

                pa.add(new JLabel(JDL.L("easycaptcha.tool.mothodedialog.selectmethode", "select the methode:")));
                EasyFile[] paths = new EasyFile(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory()).listFiles();

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
                        EasyFile ef2 = (EasyFile) combox.getSelectedItem();
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
                cHosterDialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), cHosterDialog));
                cHosterDialog.setVisible(true);

                return null;
            }
        }.waitForEDT();
        if (ef.file == null) return null;
        return ef;
    }

    private static EasyFile getCaptchaMethode() {
        return new GuiRunnable<EasyFile>() {
            public EasyFile runSave() {
                final EasyFile ef = new EasyFile();
                final JDialog dialog = new JDialog(DummyFrame.getDialogParent());
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setTitle(JDL.L("easycaptcha.tool.title", "EasyCaptcha"));
                dialog.setModal(true);

                JPanel box = new JPanel(new GridLayout(3, 1));
                JButton btcs = new JButton(JDL.L("easycaptcha.tool.continuelastsession", "Continue Last Session"));
                final EasyFile lastEF = (EasyFile) config.getProperty(CONFIG_LASTSESSION, null);
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
                        EasyFile ef2 = showMethodes();
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
                                final JDialog cHosterDialog = new JDialog(DummyFrame.getDialogParent());
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
                                            CreateHoster.create(new EasyFile(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory() + "/" + "easycaptcha"), ef, tfAuthor.getText(), (Integer) spMaxLetters.getValue());

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
                                cHosterDialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), cHosterDialog));
                                cHosterDialog.setVisible(true);
                                return null;
                            }
                        }.waitForEDT();
                    }
                });

                box.add(btc);
                dialog.add(box);
                dialog.pack();
                dialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), dialog));
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

    public static void showToolKid(final EasyFile meth) {

        CreateHoster.setImageType(meth);
        File folder = meth.getCaptchaFolder();
        if (!folder.exists() || folder.list().length < 1) return;
        final JAntiCaptcha jac = new JAntiCaptcha(Utilities.getMethodDir(), meth.getName());
        final JDialog dialog = new GuiRunnable<JDialog>() {
            // @Override
            public JDialog runSave() {
                return new JDialog(DummyFrame.getDialogParent());
            }
        }.getReturnValue();
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), dialog));
        dialog.setTitle(JDL.L("easycaptcha.tool.title", "EasyCaptcha"));
        final JPanel box = new GuiRunnable<JPanel>() {
            public JPanel runSave() {
                return new JPanel(new GridLayout(3, 1));
            }
        }.getReturnValue();
        JButton btnTrain = new GuiRunnable<JButton>() {
            public JButton runSave() {
                return new JButton(JDL.L("easycaptcha.tool.btn.train", "Train"));
            }
        }.getReturnValue();
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
        JButton btnShowLetters = new GuiRunnable<JButton>() {
            public JButton runSave() {
                return new JButton(JDL.L("easycaptcha.tool.btn.letterdb", "Show Letter Database"));
            }
        }.getReturnValue();
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
        JButton btnColorTrainer = new GuiRunnable<JButton>() {
            public JButton runSave() {
                return new JButton(JDL.L("easycaptcha.tool.btn.colortrainer", "Train Colors"));
            }
        }.getReturnValue();
        btnColorTrainer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                new Thread(new Runnable() {

                    public void run() {
                        ColorTrainer.getColor(meth);

                    }
                }).start();

            }
        });
        box.add(btnColorTrainer);
        dialog.add(box);
        new GuiRunnable<Object>() {
            public Object runSave() {
                dialog.pack();
                dialog.setVisible(true);
                return null;
            }
        }.waitForEDT();

    }

    public static void main(String[] args) {
        new JDInit().loadConfiguration();
        LookAndFeelController.setUIManager();

        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EDTEventQueue());
                return null;
            }
        }.waitForEDT();
        EasyFile meth = EasyCaptchaTool.getCaptchaMethode();

        showToolKid(meth);

        // System.exit(0);

    }

}
