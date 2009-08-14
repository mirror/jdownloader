package jd.captcha.easy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
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

import jd.gui.userio.DummyFrame;
import jd.nutils.Screen;

import jd.gui.swing.GuiRunnable;

import jd.utils.locale.JDL;

import jd.nutils.io.JDIO;

import jd.utils.JDUtilities;

import jd.nutils.Colors;

import jd.captcha.gui.ImageComponent;
import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;

public class ColorTrainer {
    private static final long serialVersionUID = 1L;
    private JPanel panel, images;
    private ImageComponent ic, icColorImage;
    private Vector<CPoint> ret = new Vector<CPoint>(), lastRet = new Vector<CPoint>();
    private int tollerance = 25;
    protected Captcha captcha;
    private boolean foreground = true, add = true, close = true, fastSelection = false;
    public int zoom = 400;
    protected Captcha captchaImage, lastCaptcha;
    JButton back;
    BufferedImage colorImage;
    private JLabel colorState;
    private JFrame frame;
    private int foregroundColor1 = 0xff00ff, foregroundColor2 = 0xFF99FF, backgroundColor1 = 0x0000ff, backgroundColor2 = 0x00ffff;

    class ColorMode {
        short mode;
        String modeString;

        public ColorMode(short mode, String modestString) {
            this.mode = mode;
            this.modeString = modestString;
        }

        @Override
        public boolean equals(Object arg0) {
            if ((arg0 == null) || !(arg0 instanceof ColorMode)) return false;
            return mode == ((ColorMode) arg0).mode;
        }

        public String toString() {
            return modeString;
        }
    }

    final ColorMode[] cModes = new ColorMode[] { new ColorMode(CPoint.LAB_DIFFERENCE, "LAB Difference"), new ColorMode(CPoint.RGB_DIFFERENCE1, "RGB1 Difference"), new ColorMode(CPoint.RGB_DIFFERENCE2, "RGB2 Difference"), new ColorMode(CPoint.HUE_DIFFERENCE, "Hue Difference"), new ColorMode(CPoint.SATURATION_DIFFERENCE, "Saturation Difference"), new ColorMode(CPoint.BRIGHTNESS_DIFFERENCE, "Brightness Difference"), new ColorMode(CPoint.RED_DIFFERENCE, "Red Difference"), new ColorMode(CPoint.GREEN_DIFFERENCE, "Green Difference"), new ColorMode(CPoint.BLUE_DIFFERENCE, "Blue Difference") };
    final JComboBox mode = new GuiRunnable<JComboBox>() {
        public JComboBox runSave() {
            return new JComboBox(cModes);
        }
    }.getReturnValue();

    private void removePixelAbsolut(CPoint cp) {
        backUP();
        ret.remove(cp);
        if (fastSelection) {
            for (int x = 0; x < captchaImage.getWidth(); x++) {
                for (int y = 0; y < captchaImage.getHeight(); y++) {
                    double dist = Colors.getColorDifference(captcha.getPixelValue(x, y), cp.getColor());
                    if (dist < cp.getDistance()) {
                        captchaImage.grid[x][y] = captcha.getPixelValue(x, y);
                    }

                }

            }
        } else {
            paintImage();
        }
    }

    @SuppressWarnings("unchecked")
    private void backUP() {
        lastRet = (Vector<CPoint>) ret.clone();
        lastCaptcha = new Captcha(captchaImage.getHeight(), captchaImage.getWidth());
        lastCaptcha.grid = new int[captchaImage.getWidth()][captchaImage.getHeight()];
        for (int a = 0; a < captchaImage.grid.length; a++) {

            lastCaptcha.grid[a] = captchaImage.grid[a].clone();
        }
        back.setEnabled(true);
    }

    private void goBack() {
        ret = lastRet;
        captchaImage = lastCaptcha;
        back.setEnabled(false);
        ic.image = captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);

        new GuiRunnable<Object>() {
            public Object runSave() {
                panel.repaint();
                panel.revalidate();
                return null;
            }
        }.waitForEDT();

    }

    private void removePixelRelativ(final CPoint pr) {
        int co = pr.getColor();
        double bestDist = Integer.MAX_VALUE;
        CPoint bestPX = null;
        for (Iterator<CPoint> iterator = ret.iterator(); iterator.hasNext();) {
            CPoint p = (CPoint) iterator.next();
            double dist = 0;
            if (p.getDistance() == 0) {
                if (co == p.getColor()) {
                    bestPX = p;
                    break;
                }

            } else if ((dist = p.getColorDifference(co)) < p.getDistance()) {
                if (dist < bestDist) {
                    bestPX = p;
                    bestDist = dist;
                }
            }
        }

        if (bestPX != null) {
            removePixelAbsolut(bestPX);

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

    private void addPixel(final CPoint p) {
        if (!ret.contains(p)) {
            backUP();
            ret.add(p);
            if (fastSelection) {
                for (int x = 0; x < captchaImage.getWidth(); x++) {
                    for (int y = 0; y < captchaImage.getHeight(); y++) {
                        captchaImage.grid[x][y] = captchaImage.getPixelValue(x, y);
                        if (p.getColorDifference(captcha.getPixelValue(x, y)) < p.getDistance()) {
                            captchaImage.grid[x][y] = p.isForeground() ? foregroundColor1 : backgroundColor1;
                        }

                    }

                }
            } else {
                paintImage();
            }
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
                final CPoint p = new CPoint(e.getX() * 100 / zoom, e.getY() * 100 / zoom, (Integer) tollerance, captcha);
                p.setColorDistanceMode(((ColorMode) mode.getSelectedItem()).mode);
                p.setForeground(foreground);
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

    private void paintImage() {
        for (int x = 0; x < captchaImage.getWidth(); x++) {
            for (int y = 0; y < captchaImage.getHeight(); y++) {
                captchaImage.grid[x][y] = captcha.getPixelValue(x, y);
                double bestDist1 = Double.MAX_VALUE;
                CPoint cpBestDist1 = null;
                double bestDist2 = Double.MAX_VALUE;
                CPoint cpBestDist2 = null;
                for (CPoint cp : ret) {
                    double dist = cp.getColorDifference(captcha.getPixelValue(x, y));
                    if (bestDist1 > dist) {
                        bestDist1 = dist;
                        cpBestDist1 = cp;
                    }
                    if (dist < cp.getDistance()) {
                        if (bestDist2 > dist) {
                            bestDist2 = 0;
                            cpBestDist2 = cp;
                        }
                    }
                }
                if (cpBestDist2 != null) {
                    captchaImage.grid[x][y] = cpBestDist2.isForeground() ? foregroundColor1 : backgroundColor1;
                } else if (cpBestDist1 != null) {
                    captchaImage.grid[x][y] = cpBestDist1.isForeground() ? foregroundColor2 : backgroundColor2;
                }

            }

        }
    }

    private void createIc() {
        captchaImage = new Captcha(captcha.getWidth(), captcha.getHeight());

        captchaImage.grid = new int[captcha.getWidth()][captcha.getHeight()];

        paintImage();
        final Image ci = captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);
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

    private ColorTrainer() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                frame=new JFrame();
                return null;
            }
        }.waitForEDT();
    }

    private void addImages() {

        new GuiRunnable<Object>() {
            public Object runSave() {
                images = new JPanel();

                images.setBorder(new TitledBorder(JDL.L("easycaptcha.image", "Image:")));

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

    private void setStatus(int xb, int yb) {
        Graphics2D graphics = colorImage.createGraphics();
        final int xc = xb * 100 / zoom;
        final int yc = yb * 100 / zoom;

        final Color c = new Color(captcha.getPixelValue(xc, yc));
        for (int y = 0; y < colorImage.getHeight(); y++) {

            for (int x = 0; x < colorImage.getWidth(); x++) {

                graphics.setColor(c);

                graphics.fillRect(x, y, 1, 1);

            }

        }
        for (int x = 0; x < colorImage.getWidth(); x++) {

            graphics.setColor(Color.black);

            graphics.fillRect(x, 0, 1, 1);

            graphics.fillRect(x, colorImage.getHeight(), 1, 1);

        }

        for (int y = 0; y < colorImage.getHeight(); y++) {

            graphics.setColor(Color.black);

            graphics.fillRect(0, y, 1, 1);

            graphics.fillRect(colorImage.getWidth(), y, 1, 1);

        }
        final float[] hsb = Colors.rgb2hsb(c.getRed(), c.getGreen(), c.getBlue());

        new GuiRunnable<Object>() {
            public Object runSave() {
                icColorImage.setImage(colorImage);
                icColorImage.revalidate();
                icColorImage.repaint();
                colorState.setText("<HTML><BODY>" + JDL.L("easycaptcha.color", "Color") + ":#" + Integer.toHexString(c.getRGB() & 0x00ffffff) + "<BR>\r\n" + xc + ":" + yc + "<BR>\r\n" + "<span style=\"color:#" + Integer.toHexString(new Color(c.getRed(), 0, 0).getRGB() & 0x00ffffff) + "\">R:" + getDigit(c.getRed()) + "</span><span style=\"color:#" + Integer.toHexString(new Color(0, c.getGreen(), 0).getRGB() & 0x00ffffff) + "\"> G:" + getDigit(c.getGreen()) + "</span><span style=\"color:#" + Integer.toHexString(new Color(0, 0, c.getBlue()).getRGB() & 0x00ffffff) + "\"> B:" + getDigit(c.getBlue()) + "</span><BR>\r\n" + "H:" + getDigit(Math.round(hsb[0] * 360)) + " S:" + getDigit(Math.round(hsb[1] * 100)) + " B:" + getDigit(Math.round(hsb[2] * 100)) + "\r\n</BODY></HTML>");
                return null;
            }
        }.waitForEDT();

    }

    private String getDigit(int i) {
        String ret = "";
        if (i < 10)
            ret = i + "&nbsp;&nbsp;&nbsp;&nbsp;";
        else if (i < 100)
            ret = i + "&nbsp;&nbsp;";
        else
            ret += i;
        return ret;
    }

    private JPanel addStatus() {
        JPanel box = new JPanel(new GridLayout(2, 1));
        colorImage = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
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
        box.add(mode);

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
        ColorTrainer lastCC = null;
        for (int i = 0; i < cs.length; i++) {
            File captchafile = list[i];
            Image captchaImage = Utilities.loadImage(captchafile);
            if (captchaImage == null) continue;
            Captcha captcha = jac.createCaptcha(captchaImage);
            if (captcha == null) continue;

            captcha.setCaptchaFile(captchafile);
            cs[i] = captcha;
            final ColorTrainer cc = new ColorTrainer();
            if (lastCC != null) {
                final ColorTrainer last = lastCC;
                cc.fastSelection = lastCC.fastSelection;
                cc.foreground = lastCC.foreground;
                cc.add = lastCC.add;
                cc.tollerance = lastCC.tollerance;
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        cc.mode.setSelectedItem(last.mode.getSelectedItem());

                    return null;
                    }
                }.waitForEDT();
            }
            cc.ret = c;
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
            if (cc.close) break;

        }
        if (new GuiRunnable<Boolean>() {
            public Boolean runSave() {
                return JOptionPane.showConfirmDialog(null, JDL.L("gui.btn_save", "Save"), JDL.L("gui.btn_save", "Save"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
            }
        }.getReturnValue()) saveColors(c, file);
        return c;
    }

    @SuppressWarnings("unchecked")
    public static Vector<CPoint> load(File file) {
        if (file.exists()) { return (Vector<CPoint>) JDIO.loadObject(null, file, true); }
        return new Vector<CPoint>();
    }

    public static void saveColors(Vector<CPoint> cc, File file) {
        file.getParentFile().mkdirs();
        JDIO.saveObject(null, cc, file, null, null, true);
    }

    public static Vector<CPoint> getColor(EasyFile file) {
        return getColors(file.getCaptchaFolder(), file.getName(), null);
    }

    public static void main(String[] args) {
        String path = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
        String hoster = "relfreaks.com";
        File folder = new File(path + "/captchas/" + hoster);
        getColors(folder, hoster, null);
    }

}
