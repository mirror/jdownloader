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
import jd.captcha.utils.Utilities;
import jd.gui.swing.GuiRunnable;
import jd.gui.userio.DummyFrame;
import jd.nutils.Screen;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class ColorTrainerGUI extends ColorTrainer {
    private static final long serialVersionUID = 1L;
    private JPanel panel, images;
    private ImageComponent ic, icColorImage;
    private boolean close = true;
    private JButton back;
    private JLabel colorState;
    private JFrame frame;


    public void removePixelAbsolut(CPoint cp) {
        backUP();
        super.removePixelAbsolut(cp);
    }

    public void backUP() {
        super.backUP();
        back.setEnabled(true);
    }

    private void goBack() {
        loadLastImage();
        back.setEnabled(false);
        ic.image = getScaledCaptchaImage();
        new GuiRunnable<Object>() {
            public Object runSave() {
                panel.repaint();
                panel.revalidate();
                return null;
            }
        }.waitForEDT();

    }

    private void removePixelRelativ(final CPoint pr) {
        CPoint bestPX = getBestPixel(pr);
        if (bestPX != null) {
            removePixelAbsolut(bestPX);
            ic.image = getScaledCaptchaImage();
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
        if (!ret.contains(p)) {
            backUP();
            addPixel(p);
            ic.image = captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);
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
                final CPoint p = getCPointFromMouseEvent(e);

                if (add)
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
        recreateCaptcha();
        final Image ci = getScaledCaptchaImage();
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

    public ColorTrainerGUI() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                frame = new JFrame();
                return null;
            }
        }.waitForEDT();
    }

    private void addImages() {

        new GuiRunnable<Object>() {
            public Object runSave() {
                images = new JPanel();

                images.setBorder(new TitledBorder(JDL.L("easycaptcha.images", "Images:")));

                images.setLayout(new BoxLayout(images, BoxLayout.Y_AXIS));

                images.add(new JLabel(JDL.L("easycaptcha.orginal", "Original:")), getGBC(0, 1, 1, 1));
                ImageComponent ic0 = new ImageComponent(captcha.getImage().getScaledInstance(captcha.getWidth() * zoom / 100, captcha.getHeight() * zoom / 100, Image.SCALE_DEFAULT));

                images.add(ic0, getGBC(0, 1, 1, 1));

                images.add(Box.createRigidArea(new Dimension(0, 10)));

                images.add(new JLabel(JDL.L("easycaptcha.labeled", "Labeled:")), getGBC(0, 1, 1, 1));

                createIc();

                MouseListener icl = getICListener();

                MouseMotionListener mml = new MouseMotionListener() {

                    public void mouseDragged(MouseEvent e) {

                        // TODO Auto-generated method stub

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
        final String statusString = getStatusString(xb, yb);
        new GuiRunnable<Object>() {
            public Object runSave() {
                icColorImage.setImage(colorImage);
                icColorImage.revalidate();
                icColorImage.repaint();
                colorState.setText(statusString);
                return null;
            }
        }.waitForEDT();

    }

    private JPanel addStatus() {
        JPanel box = new JPanel(new GridLayout(2, 1));
        icColorImage = new ImageComponent(colorImage);
        colorState = new JLabel();
        setStatus(1, 1);
        box.add(icColorImage);
        box.add(colorState);
        box.setBorder(new TitledBorder(JDL.L("easycaptcha.color", "Color")));
        return box;
    }

    private JPanel addSettings() {
        final JPanel box = new JPanel(new GridLayout(5, 1));
        new GuiRunnable<JComboBox>() {
            public JComboBox runSave() {
                final JComboBox ret=new JComboBox(ColorMode.cModes);
                box.add(ret);
                ret.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        mode=((ColorMode) ret.getSelectedItem()).mode;
                    }});
                return null;
            }
        }.waitForEDT();

        final JCheckBox ground = new GuiRunnable<JCheckBox>() {
            public JCheckBox runSave() {
                return new JCheckBox(foreground ? JDL.L("easycaptcha.foreground", "foreground") : JDL.L("easycaptcha.background", "background"), foreground);
            }
        }.getReturnValue();
        ground.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                foreground = !foreground;
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        ground.setText(foreground ? JDL.L("easycaptcha.foreground", "foreground") : JDL.L("easycaptcha.background", "background"));
                        return null;
                    }
                }.waitForEDT();
            }
        });

        box.add(ground);
        final JCheckBox addb = new GuiRunnable<JCheckBox>() {
            public JCheckBox runSave() {
                return new JCheckBox(add ? JDL.L("easycaptcha.add", "add") : JDL.L("easycaptcha.remove", "remove"), add);

            }
        }.getReturnValue();
        addb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                add = !add;
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        addb.setText(add ? JDL.L("easycaptcha.add", "add") : JDL.L("easycaptcha.remove", "remove"));
                        return null;
                    }
                }.waitForEDT();
            }
        });
        box.add(addb);

        JCheckBox fst = new GuiRunnable<JCheckBox>() {
            public JCheckBox runSave() {
                return new JCheckBox(JDL.L("easycaptcha.fastselection", "FastSelection:"), fastSelection);
            }
        }.getReturnValue();

        fst.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fastSelection = !fastSelection;
            }
        });
        box.add(fst);
        final ChangeListener cl = new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        tollerance = (Integer) ((JSpinner) e.getSource()).getValue();

                        return null;
                    }
                }.waitForEDT();
            }
        };
        new GuiRunnable<Object>() {
            public Object runSave() {
                JPanel p = new JPanel();
                final JSpinner tolleranceSP = new JSpinner(new SpinnerNumberModel(tollerance, 0, 360, 1));
                tolleranceSP.setToolTipText("Threshold");
                tolleranceSP.addChangeListener(cl);
                p.add(new JLabel(JDL.L("easycaptcha.threshold", "Threshold:")));
                p.add(tolleranceSP);
                box.add(p);
                box.setBorder(new TitledBorder(JDL.L("easycaptcha.settings", "Settings:")));

                return null;
            }
        }.waitForEDT();

        return box;

    }

    private void init(Captcha captcha) {

        this.captcha = captcha;
        new GuiRunnable<Object>() {
            public Object runSave() {
                frame.setAlwaysOnTop(true);
                panel = new JPanel();
                panel.setLayout(new GridBagLayout());
                frame.setLayout(new BorderLayout());
                frame.add(new JScrollPane(panel), BorderLayout.CENTER);

                frame.setTitle(JDL.L("easycaptcha.colorcrainer.title", "Color Trainer"));
                addImages();
                panel.add(images);
                JPanel pen = new JPanel();

                GridBagConstraints gb = Utilities.getGBC(0, 2, 1, 1);
                pen.add(addStatus());
                pen.add(addSettings());
                panel.add(pen, gb);
                gb = Utilities.getGBC(0, 4, 1, 1);

                back = new JButton(JDL.L("easycaptcha.back", "back"));
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
                JButton btf = new JButton(JDL.L("easycaptcha.finished", "finish"));
                btf.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        destroy();
                    }
                });
                box.add(btf);
                gb.anchor = GridBagConstraints.WEST;
                panel.add(box, gb);

                JButton bt = new JButton(JDL.L("gui.btn_ok", "OK"));
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

    public static Vector<CPoint> getColors(File folder, String hoster, Vector<CPoint> c) {
        File file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment() + "/" + JDUtilities.getJACMethodsDirectory() + hoster + "/CPoints.xml");

        File[] list = folder.listFiles();
        Captcha[] cs = new Captcha[15 < list.length ? 15 : list.length];
        JAntiCaptcha jac = new JAntiCaptcha(Utilities.getMethodDir(), "EasyCaptcha");
        if (c == null) c = load(file);
        ColorTrainerGUI lastCC = null;
        for (int i = 0; i < cs.length; i++) {
            File captchafile = list[i];
            Image captchaImage = Utilities.loadImage(captchafile);
            if (captchaImage == null) continue;
            Captcha captcha = jac.createCaptcha(captchaImage);
            if (captcha == null) continue;
            BackGroundImageManager bgit = new BackGroundImageManager(captcha);
            bgit.clearCaptchaAll();
            captcha.setCaptchaFile(captchafile);
            cs[i] = captcha;
            final ColorTrainerGUI cc = new ColorTrainerGUI();
            cc.ret = c;

            if (lastCC != null) {
                cc.fastSelection = lastCC.fastSelection;
                cc.foreground = lastCC.foreground;
                cc.add = lastCC.add;
                cc.tollerance = lastCC.tollerance;
                cc.mode = lastCC.mode;
            }
            cc.init(captcha);
            synchronized (cc) {
                try {
                    cc.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            lastCC = cc;
            c = cc.ret;
            if (cc.close) break;

        }
        if (new GuiRunnable<Boolean>() {
            public Boolean runSave() {
                return JOptionPane.showConfirmDialog(null, JDL.L("gui.btn_save", "Save"), JDL.L("gui.btn_save", "Save"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
            }
        }.getReturnValue()) saveColors(c, file);
        return c;
    }

    public static Vector<CPoint> getColor(EasyFile file) {
        return getColors(file.getCaptchaFolder(), file.getName(), null);
    }

    public static void main(String[] args) {
        String path = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
        String hoster = "canna.to";
        File folder = new File(path + "/captchas/" + hoster);
        getColors(folder, hoster, null);
    }

}
