//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.captcha;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import jd.captcha.translate.T;
import jd.captcha.utils.Utilities;
import jd.gui.swing.components.BrowseFile;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.WindowManager;
import org.appwork.utils.swing.WindowManager.FrameState;

public class JACController {
    private static BrowseFile chooser;
    private static JComboBox  methods;

    public static void showDialog(final boolean isTrain) {
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new MigLayout("wrap 2"));
        if (isTrain) {
            frame.setTitle(T._.train_chooser_title());
        } else {
            frame.setTitle(T._.showcaptcha_chooser_title());
        }
        frame.setAlwaysOnTop(true);
        frame.setLocation(20, 20);
        final File[] meths = new File(Utilities.getMethodDir()).listFiles(new FileFilter() {

            public boolean accept(final File pathname) {
                return pathname.isDirectory();
            }
        });
        methods = new JComboBox(meths);
        chooser = new BrowseFile();
        if (isTrain) {
            chooser.setFileSelectionMode(BrowseFile.DIRECTORIES_ONLY);
        }
        chooser.setEditable(true);

        String chooserText = "";
        if (isTrain) {
            chooserText = T._.train_choose_folder();
        } else {
            chooserText = T._.showcaptcha_choose_file();
        }
        frame.add(new JLabel(T._.train_method()));
        frame.add(methods, "growx, spanx");
        frame.add(new JLabel(chooserText));
        frame.add(chooser, "growx, spanx");
        final JButton btnOK = new JButton(T._.gui_btn_ok());
        btnOK.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                new Thread(new Runnable() {

                    public void run() {
                        final JACController jacc = new JACController(new File(chooser.getText()), ((File) methods.getSelectedItem()).getName());
                        if (isTrain) {
                            try {
                                jacc.train();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                jacc.showCaptcha();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                }).start();

            }

        });
        frame.add(btnOK, "span, right");
        frame.pack();
        frame.setResizable(false);
        WindowManager.getInstance().setVisible(frame, true,FrameState.OS_DEFAULT);
    }

    private final JAntiCaptcha jac;
    private final File         path;

    public JACController(final File path, final String methode) {
        this.path = path;
        jac = new JAntiCaptcha(methode);
    }

    public static void main(String[] args) throws InterruptedException {
        File file = new File("C:\\Users\\Thomas\\.appwork\\captchas\\pybckd\\1337856728166.png");
        new JACController(file, "payback.de").showCaptcha();
    }

    public void showCaptcha() throws InterruptedException {
        jac.showPreparedCaptcha(path);
    }

    public void train() throws InterruptedException {
        jac.trainAllCaptchas(path.getAbsolutePath());
        jac.saveMTHFile();
    }
}