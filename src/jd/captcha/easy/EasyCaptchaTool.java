package jd.captcha.easy;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import jd.gui.swing.GuiRunnable;

import jd.utils.JDUtilities;

import jd.nutils.Screen;

import jd.gui.userio.DummyFrame;

public class EasyCaptchaTool {

    private static EasyFile showStartDialog() {
        return new GuiRunnable<EasyFile>() {
            public EasyFile runSave() {
                final EasyFile ef = new EasyFile();
                final JDialog dialog = new JDialog(DummyFrame.getDialogParent());
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setTitle("EasyCaptcha");
                dialog.setModal(true);

                JPanel box = new JPanel(new GridLayout(2, 1));
                JButton btl = new JButton("load methode");
                btl.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        EasyFile[] paths = new EasyFile(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory()).listFiles();
                        EasyFile ef2 = (EasyFile) JOptionPane.showInputDialog(null, "select the methode", "Methodes", JOptionPane.PLAIN_MESSAGE, null, paths, paths[0]);
                        if (ef2 != null) {
                            ef.file=ef2.file;
                            dialog.dispose();
                        }
                    }
                });

                box.add(btl);
                JButton btc = new JButton("create methode");
                btc.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        final JDialog cHosterDialog = new JDialog(DummyFrame.getDialogParent());
                        cHosterDialog.setModal(true);
                        cHosterDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                        cHosterDialog.setTitle("EasyCaptcha");
                        JPanel box = new JPanel(new GridLayout(4, 2));
                        JTextField tfHoster = new JTextField();
                        box.add(new JLabel("Host:"));
                        box.add(tfHoster);
                        JTextField tfName = new JTextField();
                        box.add(new JLabel("Name:"));
                        box.add(tfName);
                        JSpinner spMaxLetters = new JSpinner(new SpinnerNumberModel(4,1,40,1));
                        box.add(new JLabel("Maximal letter number:"));
                        box.add(spMaxLetters);
                        JButton ok = new JButton("OK");
                        box.add(ok);
                        ok.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                cHosterDialog.dispose();
                            }
                        });
                        cHosterDialog.add(box);
                        cHosterDialog.pack();
                        cHosterDialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), cHosterDialog));
                        cHosterDialog.setVisible(true);
                        if (tfHoster.getText() != null) {
                            ef.file=new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory(), tfHoster.getText());
                            dialog.dispose();
                            cHosterDialog.dispose();
                            CreateHoster.create(ef, tfName.getText(), (Integer)spMaxLetters.getValue());
                        }
                    }
                });

                box.add(btc);
                dialog.add(box);
                dialog.pack();
                dialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), dialog));
                dialog.setVisible(true);
                if(ef.file!=null)
                    return ef;
                else
                    return null;
            }
        }.getReturnValue();

    }

    public static void main(String[] args) {

        EasyCaptchaTool.showStartDialog();

    }

}
