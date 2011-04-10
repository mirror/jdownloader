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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.captcha.JAntiCaptcha;
import jd.captcha.gui.ImageComponent;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.translate.T;
import jd.captcha.utils.Utilities;
import jd.gui.swing.GuiRunnable;
import jd.gui.userio.DummyFrame;
import jd.nutils.Screen;
import jd.utils.JDUtilities;

public class ColorTrainerGUI {
    private static final long serialVersionUID = 1L;
    private JPanel            panel, images;
    private ImageComponent    ic, icColorImage;
    private boolean           close            = true;
    private JButton           back;
    private JLabel            colorState;
    private JDialog           frame;
    private ColorTrainer      colorTrainer     = new ColorTrainer();

    public ColorTrainerGUI(final JFrame owner) {
        new GuiRunnable<Object>() {
            public Object runSave() {
                frame = new JDialog(owner);
                return null;
            }
        }.waitForEDT();
    }

    public void removePixelAbsolut(CPoint cp) {
        backUP();
        colorTrainer.removeCPoint(cp);
    }

    public void backUP() {
        colorTrainer.backUP();
        back.setEnabled(true);
    }

    private void goBack() {
        colorTrainer.loadLastImage();
        back.setEnabled(false);
        ic.image = colorTrainer.getScaledWorkingCaptchaImage();
        new GuiRunnable<Object>() {
            public Object runSave() {
                panel.repaint();
                panel.revalidate();
                return null;
            }
        }.waitForEDT();

    }

    private void removePixelRelativ(final CPoint pr) {
        CPoint bestPX = colorTrainer.searchCPoint(pr);
        if (bestPX != null) {
            removePixelAbsolut(bestPX);
            ic.image = colorTrainer.getScaledWorkingCaptchaImage();
            new GuiRunnable<Object>() {
                public Object runSave() {
                    panel.repaint();
                    panel.revalidate();
                    return null;
                }
            }.waitForEDT();
        }
    }

    public void addPixel(final CPoint p) {
        if (!colorTrainer.colorPointList.contains(p)) {
            backUP();
            colorTrainer.addCPoint(p);
            ic.image = colorTrainer.getScaledWorkingCaptchaImage();
            new GuiRunnable<Object>() {
                public Object runSave() {
                    panel.repaint();
                    panel.revalidate();
                    return null;
                }
            }.waitForEDT();
        }
    }

    private MouseListener getICListener() {
        return new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                final CPoint p = colorTrainer.getCPointFromMouseEvent(e);

                if (colorTrainer.add)
                    addPixel(p);
                else
                    removePixelRelativ(p);
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }
        };
    }

    private void createIc() {
        colorTrainer.createWorkingCaptcha();
        final Image ci = colorTrainer.getScaledWorkingCaptchaImage();
        new GuiRunnable<Object>() {
            public Object runSave() {
                ic = new ImageComponent(ci);
                return null;
            }
        }.waitForEDT();
        images.add(ic, getGBC(0, 2, 1, 1));
    }

    /**
     * Gibt die default GridbagConstants zurück
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     * @return Default GridBagConstraints
     */
    public GridBagConstraints getGBC(int x, int y, int width, int height) {

        GridBagConstraints gbc = Utilities.getGBC(x, y, width, height);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;

        return gbc;
    }

    private void addImages() {

        new GuiRunnable<Object>() {
            public Object runSave() {
                images = new JPanel();

                images.setBorder(new TitledBorder(T._.easycaptcha_images()));

                images.setLayout(new BoxLayout(images, BoxLayout.Y_AXIS));

                images.add(new JLabel(T._.easycaptcha_orginal()), getGBC(0, 1, 1, 1));
                ImageComponent ic0 = new ImageComponent(colorTrainer.getScaledOriginalCaptchaImage());

                images.add(ic0, getGBC(0, 1, 1, 1));

                images.add(Box.createRigidArea(new Dimension(0, 10)));

                images.add(new JLabel(T._.easycaptcha_labeled()), getGBC(0, 1, 1, 1));

                createIc();

                MouseListener icl = getICListener();

                MouseMotionListener mml = new MouseMotionListener() {

                    public void mouseDragged(MouseEvent e) {
                    }

                    public void mouseMoved(MouseEvent e) {
                        setStatus(e.getX(), e.getY());
                    }

                };

                ic.addMouseListener(icl);

                ic0.addMouseListener(icl);

                ic.addMouseMotionListener(mml);

                ic0.addMouseMotionListener(mml);
                return null;
            }
        }.waitForEDT();

    }

    public void setStatus(int xb, int yb) {
        final String statusString = colorTrainer.getStatusString(xb, yb);
        new GuiRunnable<Object>() {
            public Object runSave() {
                icColorImage.setImage(colorTrainer.colorImage);
                icColorImage.revalidate();
                icColorImage.repaint();
                colorState.setText(statusString);
                return null;
            }
        }.waitForEDT();

    }

    private JPanel addStatus() {
        JPanel box = new JPanel(new GridLayout(2, 1));
        icColorImage = new ImageComponent(colorTrainer.colorImage);
        colorState = new JLabel();
        setStatus(1, 1);
        box.add(icColorImage);
        box.add(colorState);
        box.setBorder(new TitledBorder(T._.easycaptcha_color()));
        return box;
    }

    private JPanel addSettings() {
        final JPanel box = new JPanel(new GridLayout(5, 1));
        new GuiRunnable<JComboBox>() {
            public JComboBox runSave() {
                final JComboBox ret = new JComboBox(ColorMode.cModes);
                box.add(ret);
                ret.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        colorTrainer.colorDifferenceMode = ((ColorMode) ret.getSelectedItem()).mode;
                    }
                });
                return null;
            }
        }.waitForEDT();

        final JCheckBox ground = new GuiRunnable<JCheckBox>() {
            public JCheckBox runSave() {
                return new JCheckBox(colorTrainer.foreground ? T._.easycaptcha_foreground() : T._.easycaptcha_background(), colorTrainer.foreground);
            }
        }.getReturnValue();
        ground.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                colorTrainer.foreground = !colorTrainer.foreground;
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        ground.setText(colorTrainer.foreground ? T._.easycaptcha_foreground() : T._.easycaptcha_background());
                        return null;
                    }
                }.waitForEDT();
            }
        });

        box.add(ground);
        final JCheckBox addb = new GuiRunnable<JCheckBox>() {
            public JCheckBox runSave() {
                return new JCheckBox(colorTrainer.add ? T._.easycaptcha_add() : T._.easycaptcha_remove(), colorTrainer.add);

            }
        }.getReturnValue();
        addb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                colorTrainer.add = !colorTrainer.add;
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        addb.setText(colorTrainer.add ? T._.easycaptcha_add() : T._.easycaptcha_remove());
                        return null;
                    }
                }.waitForEDT();
            }
        });
        box.add(addb);
        /*
         * JCheckBox fst = new GuiRunnable<JCheckBox>() { public JCheckBox
         * runSave() { return new JCheckBox(JDL.L("easycaptcha.fastselection",
         * "FastSelection:"), colorTrainer.fastSelection); } }.getReturnValue();
         * 
         * fst.addActionListener(new ActionListener() {
         * 
         * public void actionPerformed(ActionEvent e) {
         * colorTrainer.fastSelection = !colorTrainer.fastSelection; } });
         * box.add(fst);
         */
        final ChangeListener cl = new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        colorTrainer.threshold = (Integer) ((JSpinner) e.getSource()).getValue();

                        return null;
                    }
                }.waitForEDT();
            }
        };
        new GuiRunnable<Object>() {
            public Object runSave() {
                JPanel p = new JPanel();
                final JSpinner tolleranceSP = new JSpinner(new SpinnerNumberModel(colorTrainer.threshold, 0, 360, 1));
                tolleranceSP.setToolTipText("Threshold");
                tolleranceSP.addChangeListener(cl);
                p.add(new JLabel(T._.easycaptcha_threshold()));
                p.add(tolleranceSP);
                box.add(p);
                box.setBorder(new TitledBorder(T._.easycaptcha_settings()));

                return null;
            }
        }.waitForEDT();

        return box;

    }

    private void init() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                frame.setAlwaysOnTop(true);
                panel = new JPanel();
                panel.setLayout(new GridBagLayout());
                frame.setLayout(new BorderLayout());
                frame.add(new JScrollPane(panel), BorderLayout.CENTER);

                frame.setTitle(T._.easycaptcha_colorcrainer_title());
                addImages();
                panel.add(images);
                JPanel pen = new JPanel();

                GridBagConstraints gb = Utilities.getGBC(0, 2, 1, 1);
                pen.add(addStatus());
                pen.add(addSettings());
                panel.add(pen, gb);
                gb = Utilities.getGBC(0, 4, 1, 1);

                back = new JButton(T._.easycaptcha_back());
                back.setEnabled(false);
                back.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        goBack();
                    }
                });
                Box box = new Box(BoxLayout.X_AXIS);
                box.add(back);
                Component glue = Box.createGlue();
                glue.setSize(10, 1);
                box.add(glue);
                JButton btf = new JButton(T._.easycaptcha_finished());
                btf.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        destroy();
                    }
                });
                box.add(btf);
                gb.anchor = GridBagConstraints.WEST;
                panel.add(box, gb);

                JButton bt = new JButton(T._.gui_btn_ok());
                bt.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        close = false;
                        destroy();
                    }
                });
                gb.anchor = GridBagConstraints.EAST;
                panel.add(bt, gb);

                // refreshUI();
                frame.pack();
                frame.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), frame));

                frame.setVisible(true);
                return null;
            }
        }.waitForEDT();

    }

    public void destroy() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                frame.dispose();
                return null;
            }
        }.waitForEDT();
        synchronized (this) {
            this.notify();
        }
    }

    public static Vector<CPoint> getColors(File folder, String hoster, Vector<CPoint> colorPoints, JFrame owner) {
        JAntiCaptcha jac = new JAntiCaptcha(hoster);

        File file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment() + "/" + JDUtilities.getJACMethodsDirectory() + jac.getMethodDirName() + "/CPoints.xml");
        File[] list = folder.listFiles();
        Captcha[] cs = new Captcha[15 < list.length ? 15 : list.length];
        if (colorPoints == null) colorPoints = ColorTrainer.load(file);
        ColorTrainerGUI lastCC = null;
        for (int i = 0; i < cs.length; i++) {
            File captchafile = list[i];
            Image captchaImage = Utilities.loadImage(captchafile);
            if (captchaImage == null) continue;

            Captcha captcha = jac.createCaptcha(captchaImage);
            if (captcha == null) continue;
            BackGroundImageManager bgit = new BackGroundImageManager(captcha);
            bgit.clearCaptchaAll();
            captcha.setOrgGrid(PixelGrid.getGridCopy(captcha.grid));
            captcha.owner.jas.executePrepareCommands(captcha.getCaptchaFile(), captcha);
            captcha.setCaptchaFile(captchafile);
            cs[i] = captcha;
            ColorTrainerGUI cc = new ColorTrainerGUI(owner);
            cc.colorTrainer.colorPointList = colorPoints;
            cc.colorTrainer.originalCaptcha = captcha;
            if (lastCC != null) {
                lastCC.colorTrainer.copySettingsTo(cc.colorTrainer);
            }
            cc.init();
            synchronized (cc) {
                try {
                    cc.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lastCC = cc;
            colorPoints = cc.colorTrainer.colorPointList;
            if (cc.close) break;

        }
        if (new GuiRunnable<Boolean>() {
            public Boolean runSave() {
                return JOptionPane.showConfirmDialog(null, T._.gui_btn_save(), T._.gui_btn_save(), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
            }
        }.getReturnValue()) ColorTrainer.saveColors(colorPoints, file);
        return colorPoints;
    }

    /**
     * Startet einen ColorTrainer und gibt die Liste mit den Trainierten CPoints
     * zurück
     * 
     * @param file
     * @return
     */
    public static Vector<CPoint> getColor(EasyMethodFile file, JFrame owner) {
        return getColors(file.getCaptchaFolder(), file.getName(), null, owner);
    }

    public static void main(String[] args) {
        String path = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
        String hoster = "canna.to";
        File folder = new File(path + "/captchas/" + hoster);
        getColors(folder, hoster, null, new JFrame());
    }

}