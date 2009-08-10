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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.nutils.io.JDIO;

import jd.utils.JDUtilities;

import jd.nutils.Colors;

import jd.captcha.gui.ImageComponent;

import jd.captcha.gui.BasicWindow;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;

public class ChooseC extends BasicWindow {
    private static final long serialVersionUID = 1L;
    private JPanel panel, images;
    private ImageComponent ic, icColorImage;
    private Vector<CPoint> ret = new Vector<CPoint>(), lastRet = new Vector<CPoint>();
    private int tollerance = 25;
    protected Captcha captcha;
    private boolean foreground = true, add = true, close = true;
    public int zoom = 400;
    protected Captcha captchaImage, lastCaptcha;
    JButton back;
    BufferedImage colorImage;
    private JLabel colorState;
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
            // TODO Auto-generated method stub
            return mode == ((ColorMode) arg0).mode;
        }

        public String toString() {
            return modeString;
        }
    }

    final ColorMode[] cModes = new ColorMode[] { new ColorMode(CPoint.LAB_DIFFERENCE, "LAB Difference"), new ColorMode(CPoint.RGB_DIFFERENCE1, "RGB1 Difference"), new ColorMode(CPoint.RGB_DIFFERENCE2, "RGB2 Difference"), new ColorMode(CPoint.HUE_DIFFERENCE, "Hue Difference"), new ColorMode(CPoint.SATURATION_DIFFERENCE, "Saturation Difference"), new ColorMode(CPoint.BRIGHTNESS_DIFFERENCE, "Brightness Difference"), new ColorMode(CPoint.RED_DIFFERENCE, "Red Difference"), new ColorMode(CPoint.GREEN_DIFFERENCE, "Green Difference"), new ColorMode(CPoint.BLUE_DIFFERENCE, "Blue Difference") };
    final JComboBox mode = new JComboBox(cModes);

    private void removePixelAbsolut(CPoint cp) {
        backUP();
        ret.remove(cp);

        for (int x = 0; x < captchaImage.getWidth(); x++) {
            for (int y = 0; y < captchaImage.getHeight(); y++) {
                double dist = Colors.getColorDifference(captcha.getPixelValue(x, y), cp.getColor());
                if (dist < cp.getDistance()) {
                    captchaImage.grid[x][y] = captcha.getPixelValue(x, y);
                }

            }

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
        panel.repaint();
        panel.revalidate();
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
            panel.repaint();
            panel.revalidate();
        }
    }

    private void addPixel(final CPoint p) {
        if (!ret.contains(p)) {
            backUP();
            ret.add(p);
            for (int x = 0; x < captchaImage.getWidth(); x++) {
                for (int y = 0; y < captchaImage.getHeight(); y++) {
                    captchaImage.grid[x][y] = captchaImage.getPixelValue(x, y);
                    if (p.getColorDifference(captcha.getPixelValue(x, y)) < p.getDistance()) {
                        captchaImage.grid[x][y] = p.isForeground() ? foregroundColor1 : backgroundColor1;
                    }

                }

            }
            ic.image = captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);
            panel.repaint();
            panel.revalidate();
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

    private void createIc() {
        captchaImage = new Captcha(captcha.getWidth(), captcha.getHeight());

        captchaImage.grid = new int[captcha.getWidth()][captcha.getHeight()];

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

        ic = new ImageComponent(captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT));
        images.add(ic, getGBC(0, 2, 1, 1));
    }

    private ChooseC() {
        super();
    }

    private void addImages() {
        images = new JPanel();
        images.setBorder(new TitledBorder("Image:"));

        images.setLayout(new BoxLayout(images, BoxLayout.Y_AXIS));
        images.add(new JLabel("Original:"), getGBC(0, 1, 1, 1));

        ImageComponent ic0 = new ImageComponent(captcha.getImage().getScaledInstance(captcha.getWidth() * zoom / 100, captcha.getHeight() * zoom / 100, Image.SCALE_DEFAULT));
        images.add(ic0, getGBC(0, 1, 1, 1));
        images.add(Box.createRigidArea(new Dimension(0, 10)));

        images.add(new JLabel("Labeled:"), getGBC(0, 1, 1, 1));

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

    }

    private void setStatus(int xc, int yc) {
        Graphics2D graphics = colorImage.createGraphics();
        xc = xc * 100 / zoom;
        yc = yc * 100 / zoom;

        Color c = new Color(captcha.getPixelValue(xc, yc));
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
        icColorImage.setImage(colorImage);
        icColorImage.revalidate();
        icColorImage.repaint();
        float[] hsb = Colors.rgb2hsb(c.getRed(), c.getGreen(), c.getBlue());
        colorState.setText("<HTML><BODY>Color:#" + Integer.toHexString(c.getRGB() & 0x00ffffff) + "<BR>\r\n" + "Position:" + xc + ":" + yc + "<BR>\r\n" + "<span style=\"color:#" + Integer.toHexString(new Color(c.getRed(), 0, 0).getRGB() & 0x00ffffff) + "\">R:" + getDigit(c.getRed()) + "</span><span style=\"color:#" + Integer.toHexString(new Color(0, c.getGreen(), 0).getRGB() & 0x00ffffff) + "\"> G:" + getDigit(c.getGreen()) + "</span><span style=\"color:#" + Integer.toHexString(new Color(0, 0, c.getBlue()).getRGB() & 0x00ffffff) + "\"> B:" + getDigit(c.getBlue()) + "</span><BR>\r\n" + "H:" + getDigit(Math.round(hsb[0] * 360)) + " S:" + getDigit(Math.round(hsb[1] * 100)) + " B:" + getDigit(Math.round(hsb[2] * 100)) + "\r\n</BODY></HTML>");
    }

    private String getDigit(int i) {
        String ret = "";
        if (i < 10)
            ret = "&nbsp;&nbsp;&nbsp;&nbsp;" + i;
        else if (i < 100)
            ret = "&nbsp;&nbsp;" + i;
        else
            ret += i;
        return ret;
    }

    private Box addStatus() {
        Box box = new Box(BoxLayout.Y_AXIS);
        colorImage = new BufferedImage(14, 14, BufferedImage.TYPE_INT_RGB);
        icColorImage = new ImageComponent(colorImage);
        colorState = new JLabel();
        setStatus(1, 1);
        box.add(icColorImage);
        box.add(colorState);
        box.setBorder(new TitledBorder("Color:"));
        return box;
    }

    private JPanel getTools() {
        JPanel box = new JPanel(new GridLayout(4, 1));
        box.add(mode);

        final JCheckBox ground = new JCheckBox(foreground ? "foreground" : "background", foreground);
        ground.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                foreground = !foreground;
                ground.setText(foreground ? "foreground" : "background");
            }
        });
        box.setAlignmentX(Component.LEFT_ALIGNMENT);

        box.add(ground);
        final JCheckBox addb = new JCheckBox(add ? "add" : "remove", foreground);
        addb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                add = !add;
                addb.setText(add ? "add" : "remove");
            }
        });

        box.add(addb);

        JPanel pen = new JPanel();
        final JSpinner tolleranceSP = new JSpinner(new SpinnerNumberModel(tollerance, 0, 360, 1));
        tolleranceSP.setToolTipText("Threshold");
        tolleranceSP.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                tollerance = (Integer) tolleranceSP.getValue();
            }
        });
        pen.add(new JLabel("Threshold:"));
        pen.add(tolleranceSP);

        box.add(pen);
        box.setBorder(new TitledBorder("Tools:"));

        return box;

    }

    private void init(Captcha captcha) {

        this.captcha = captcha;
        this.setAlwaysOnTop(true);
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        setLayout(new BorderLayout());
        add(new JScrollPane(panel), BorderLayout.CENTER);
        setLocation(0, 0);

        setTitle("Layerrecognition Trainer");
        addImages();
        panel.add(images);
        JPanel pen = new JPanel();

        GridBagConstraints gb = Utilities.getGBC(0, 2, 1, 1);
        pen.add(addStatus());
        pen.add(getTools());
        panel.add(pen, gb);
        gb = Utilities.getGBC(0, 4, 1, 1);

        back = new JButton("back");
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
        JButton btf = new JButton("Finish");
        btf.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                destroy();
            }
        });
        box.add(btf);
        gb.anchor = GridBagConstraints.WEST;
        panel.add(box, gb);

        JButton bt = new JButton("OK");
        bt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                close = false;
                destroy();
            }
        });
        gb.anchor = GridBagConstraints.EAST;
        panel.add(bt, gb);

        // refreshUI();
        this.pack();

        setVisible(true);
    }

    public void destroy() {
        super.destroy();
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
        ChooseC lastCC = null;
        for (int i = 0; i < cs.length; i++) {
            File captchafile = list[i];
            Image captchaImage = Utilities.loadImage(captchafile);

            Captcha captcha = jac.createCaptcha(captchaImage);
            captcha.setCaptchaFile(captchafile);
            cs[i] = captcha;
            ChooseC cc = new ChooseC();
            if (lastCC != null) {
                cc.foreground = lastCC.foreground;
                cc.add = lastCC.add;
                cc.tollerance = lastCC.tollerance;
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
        if (JOptionPane.showConfirmDialog(null, "Save?", "Save?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) saveColors(c, file);
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

    public static void main(String[] args) {
        String path = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
        String hoster = "ugotfile.com";
        File folder = new File(path + "/captchas/" + hoster);
        getColors(folder, hoster, null);
    }

}
