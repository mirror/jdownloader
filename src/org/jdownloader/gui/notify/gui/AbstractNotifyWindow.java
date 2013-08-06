package org.jdownloader.gui.notify.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import org.appwork.swing.ExtJWindow;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.Application;
import org.appwork.utils.ColorUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.jdtrayicon.ScreenStack;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class AbstractNotifyWindow extends ExtJWindow implements ActionListener {

    private static final int FADE_SPEED    = 500;
    private static final int BOTTOM_MARGIN = 5;
    private static final int TOP_MARGIN    = 20;
    private MigPanel         content;
    private Timer            timer;
    private Fader            fader;
    private ScreenStack      screenStack;
    private Point            endLocation;

    public Point getEndLocation() {
        return endLocation;
    }

    private int              timeout   = 15000;
    private int              fadeSpeed = FADE_SPEED;
    private Point            startLocation;
    private static final int ROUND     = 5;

    public AbstractNotifyWindow(String caption, JComponent comp) {
        super();

        content = new MigPanel("ins 2 5 10 5,wrap 1", "[grow,fill]", "[][grow,fill]") {

            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();

                int width = getWidth();
                int height = getHeight();

                // Create a soft clipped image for the background
                BufferedImage img = getSoftClipWorkaroundImage(g2d, width, height);
                g2d.drawImage(img, 0, 0, null);

                g2d.dispose();
            }

        };

        setContentPane(content);
        content.add(createHeader(caption));

        content.add(comp);
        pack();

        setWindowOpaque(this, false);

        setWindowOpacity(this, 0f);

        fader = new Fader(this);
    }

    public static void setWindowOpaque(JWindow window, boolean b) {
        try {

            com.sun.awt.AWTUtilities.setWindowOpaque(window, b);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static float getWindowOpacity(JWindow owner) {
        if (Application.getJavaVersion() >= Application.JAVA17) {
            return owner.getOpacity();
        } else {
            return com.sun.awt.AWTUtilities.getWindowOpacity(owner);
        }
    }

    public static void setWindowOpacity(JWindow window, float f) {
        try {
            if (Application.getJavaVersion() >= Application.JAVA17) {
                window.setOpacity(f);
            } else {
                com.sun.awt.AWTUtilities.setWindowOpacity(window, f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setVisible(boolean b) {

        super.setVisible(b);

        if (b) {

            fader.fadeIn(getFadeSpeed());

        }
        if (b && getTimeout() > 0) {
            timer = new Timer(getTimeout(), this);
            timer.setRepeats(false);
            timer.start();
        }
    }

    protected int getFadeSpeed() {
        return fadeSpeed;
    }

    public void setFadeSpeed(int fadeSpeed) {
        this.fadeSpeed = fadeSpeed;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        onClose();
    }

    protected int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    private Component createHeader(String caption) {
        MigPanel ret = new MigPanel("ins 0", "[grow,fill][][]", "[]");
        JLabel lbl = new JLabel(caption);
        lbl.setForeground(ColorUtils.getAlphaInstance(lbl.getForeground(), 160));
        lbl.setHorizontalAlignment(SwingConstants.LEFT);
        ret.add(SwingUtils.toBold(lbl));
        SwingUtils.setOpaque(ret, false);
        SwingUtils.setOpaque(lbl, false);
        ExtButton settings = new ExtButton() {
            {
                setToolTipText(_GUI._.Notify_createHeader_settings_tt());
                setRolloverEffectEnabled(true);
                addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        onSettings();
                        onRollOut();
                    }

                });
            }

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            protected void onRollOut() {
                setContentAreaFilled(false);

                setIcon(IconIO.getTransparentIcon(NewTheme.I().getImage("brightmix/wrench", 16), 0.5f));
                // setIcon(NewTheme.I().getIcon("brightmix/w2", 18));
            }

            /**
             * 
             */
            protected void onRollOver() {
                setIcon(NewTheme.I().getIcon("brightmix/wrench", 16));
            }

        };
        ret.add(settings, "width 16!,height 16!");
        ExtButton closeButton = new ExtButton() {
            {
                setToolTipText(_GUI._.Notify_createHeader_close_tt());
                setRolloverEffectEnabled(true);
                addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        onClose();
                        onRollOut();
                    }

                });
            }

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            protected void onRollOut() {
                setContentAreaFilled(false);
                setIcon(IconIO.getTransparentIcon(NewTheme.I().getImage("16/mimiglyphs/close", 10), 0.5f));

            }

            /**
             * 
             */
            protected void onRollOver() {
                setIcon(NewTheme.I().getIcon("16/mimiglyphs/close", 10));
            }

        };
        ret.add(closeButton, "width 16!,height 16!");
        SwingUtils.setOpaque(ret, false);
        SwingUtils.setOpaque(lbl, false);
        SwingUtils.setOpaque(closeButton, false);
        return ret;
    }

    protected void onSettings() {
    }

    protected void onClose() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        fader.fadeOut(FADE_SPEED);
        fader.moveTo(endLocation.x, endLocation.y, FADE_SPEED);

        // dispose();
    }

    protected BufferedImage getSoftClipWorkaroundImage(Graphics2D g2d, int width, int height) {
        GraphicsConfiguration gc = g2d.getDeviceConfiguration();
        BufferedImage img = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
        Graphics2D g2 = img.createGraphics();
        // clear the full image
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, width, height);

        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        // rendering the shape
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ROUND, ROUND);
        g2.setComposite(AlphaComposite.SrcAtop);
        // rendering the actuall contents
        Paint p = new GradientPaint(0, 0, LAFOptions.getInstance().getColorForPanelHeaderBackground(), 0, TOP_MARGIN, LAFOptions.getInstance().getColorForScrollbarsMouseOverState());
        g2.setPaint(p);

        g2.fillRoundRect(0, 0, width, height, ROUND, ROUND);

        p = new GradientPaint(0, 0, LAFOptions.getInstance().getColorForPanelBackground(), 0, TOP_MARGIN, LAFOptions.getInstance().getColorForPanelBackground().brighter());
        g2.setPaint(p);

        g2.fillRect(0, TOP_MARGIN, width, height - TOP_MARGIN - BOTTOM_MARGIN);

        g2.setColor(LAFOptions.getInstance().getColorForPanelHeaderLine());

        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ROUND, ROUND);
        g2.dispose();
        return img;
    }

    public void setPreferedLocation(int x, int y) {
        fader.moveTo(x, y, FADE_SPEED);
    }

    public void setScreenStack(ScreenStack screenStack) {
        this.screenStack = screenStack;
    }

    public ScreenStack getScreenStack() {
        return screenStack;
    }

    public void setEndLocation(Point p) {

        endLocation = p;
    }

    public void setStartLocation(Point point) {
        if (startLocation == null) {
            setLocation(point);
            startLocation = point;
        }

    }

    public Point getStartLocation() {
        return startLocation;
    }

}
