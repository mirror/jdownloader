/*
 * $Id: PaintUtils.java,v 1.16 2007/11/19 16:20:37 kschaefe Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

/**
 * A collection of utilties for painting visual effects.
 *
 * @author Mark Davidson
 */
public class PaintUtils {
    private static GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    
    //  Utility methods.
    private static Border defaultBorder =
            BorderFactory.createBevelBorder(BevelBorder.RAISED);
    
    
    
    public static final GradientPaint BLUE_EXPERIENCE = new GradientPaint(
            new Point2D.Double(0, 0),
            new Color(168, 204, 241),
            new Point2D.Double(0, 1),
            new Color(44, 61, 146));
    public static final GradientPaint MAC_OSX_SELECTED = new GradientPaint(
            new Point2D.Double(0, 0),
            new Color(81, 141, 236),
            new Point2D.Double(0, 1),
            new Color(36, 96, 192));
    public static final GradientPaint MAC_OSX = new GradientPaint(
            new Point2D.Double(0, 0),
            new Color(167, 210, 250),
            new Point2D.Double(0, 1),
            new Color(99, 147, 206));
    public static final GradientPaint AERITH = new GradientPaint(
            new Point2D.Double(0, 0),
            Color.WHITE,
            new Point2D.Double(0, 1),
            new Color(64, 110, 161));
    public static final GradientPaint GRAY = new GradientPaint(
            new Point2D.Double(0, 0),
            new Color(226, 226, 226),
            new Point2D.Double(0, 1),
            new Color(250, 248, 248));
    public static final GradientPaint RED_XP = new GradientPaint(
            new Point2D.Double(0, 0),
            new Color(236, 81, 81),
            new Point2D.Double(0, 1),
            new Color(192, 36, 36));
    public static final GradientPaint NIGHT_GRAY = new GradientPaint(
            new Point2D.Double(0, 0),
            new Color(102, 111, 127),
            new Point2D.Double(0, 1),
            new Color(38, 45, 61));
    public static final GradientPaint NIGHT_GRAY_LIGHT = new GradientPaint(
            new Point2D.Double(0, 0),
            new Color(129, 138, 155),
            new Point2D.Double(0, 1),
            new Color(58, 66, 82));
    
    
    private PaintUtils() {
    }
    
    public static Border getDefaultBorder() {
        return defaultBorder;
    }
    
    /**
     * Returns the bounds that the text of a label will be drawn into.
     * Takes into account the current font metrics.
     */
    public static Rectangle getTextBounds(Graphics g, JLabel label) {
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D r2d = fm.getStringBounds(label.getText(), g);
        Rectangle rect = r2d.getBounds();
        int xOffset = 0;
        switch (label.getHorizontalAlignment()) {
            case SwingConstants.RIGHT:
            case SwingConstants.TRAILING:
                xOffset = label.getBounds().width - rect.width;
                break;
            case SwingConstants.CENTER:
                xOffset = (label.getBounds().width - rect.width) / 2;
                break;
            default:
            case SwingConstants.LEFT:
            case SwingConstants.LEADING:
                xOffset = 0;
                break;
        }
        int yOffset = 0;
        switch (label.getVerticalAlignment()) {
            case SwingConstants.TOP:
                yOffset = 0;
                break;
            case SwingConstants.CENTER:
                yOffset = (label.getBounds().height - rect.height) / 2;
                break;
            case SwingConstants.BOTTOM:
                yOffset = label.getBounds().height - rect.height;
                break;
        }
        return new Rectangle(xOffset, yOffset, rect.width, rect.height);
    }
    
    /**
     * Paints a top to bottom gradient fill over the component bounds
     * from color1 to color2.
     */
    public static void paintGradient(Graphics g, JComponent comp,
            Color color1, Color color2) {
        GradientPaint paint = new GradientPaint(0, 0, color1,
                0, comp.getHeight(), color2,
                true);
        Graphics2D g2 = (Graphics2D) g;
        Paint oldPaint = g2.getPaint();
        g2.setPaint(paint);
        g2.fillRect(0, 0, comp.getWidth(), comp.getHeight());
        g2.setPaint(oldPaint);
    }
    
    /**
     * Sets the background color for a containment hierarchy.
     */
    public static void setBackgroundColor(Container cont, Color color) {
        cont.setBackground(color);
        Component[] children = cont.getComponents();
        for (Component aChildren : children) {
            if (aChildren instanceof Container) {
                setBackgroundColor((Container) aChildren, color);
            } else {
                aChildren.setBackground(color);
            }
        }
    }
    
    /**
     * Sets the foreground color for a containment hierarchy.
     */
    public static void setForegroundColor(Container cont, Color color) {
        cont.setForeground(color);
        Component[] children = cont.getComponents();
        for (Component aChildren : children) {
            if (aChildren instanceof Container) {
                setForegroundColor((Container) aChildren, color);
            } else {
                aChildren.setForeground(color);
            }
        }
    }
    
    /**
     * Sets the font for a containment hierarchy.
     */
    public static void setFont(Container cont, Font font) {
        cont.setFont(font);
        Component[] children = cont.getComponents();
        for (Component aChildren : children) {
            if (aChildren instanceof Container) {
                setFont((Container) aChildren, font);
            } else {
                aChildren.setFont(font);
            }
        }
    }
    
    /**
     * @param width  the width of the new BufferedImage
     * @param height the height of the new BufferedImage
     *
     * @return Creates and returns a BufferedImage that is "compatible" with this machines
     *         video card and subsystem
     */
    public static BufferedImage createCompatibleImage(int width, int height) {
        GraphicsEnvironment environment =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screenDevice = environment.getDefaultScreenDevice();
        GraphicsConfiguration configuration =
                screenDevice.getDefaultConfiguration();
        return configuration.createCompatibleImage(width, height);
    }
    
    /**
     * @param width         the width of the new BufferedImage
     * @param height        the height of the new BufferedImage
     * @param transparency  one of the values in the Transparency interface
     *
     * @return Creates and returns a BufferedImage that is "compatible" with this machines
     *         video card and subsystem with the given Transparency.
     */
    public static BufferedImage createCompatibleImage(int width, int height,
            int transparency) {
        GraphicsEnvironment environment =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screenDevice = environment.getDefaultScreenDevice();
        GraphicsConfiguration configuration =
                screenDevice.getDefaultConfiguration();
        return configuration.createCompatibleImage(width, height, transparency);
    }
    
    public static BufferedImage convertToBufferedImage(Image img) {
        BufferedImage buff = createCompatibleImage(img.getWidth(null),img.getHeight(null));
        Graphics2D g2 = buff.createGraphics();
        g2.drawImage(img,0,0,null);
        g2.dispose();
        return buff;
    }
    
    /**
     * Loads the image at the URL and makes it compatible with the screen.
     * If loading the url fails then this method will either throw an IOException
     * or return null.
     */
    public static BufferedImage loadCompatibleImage(URL resource) throws IOException {
        BufferedImage image = ImageIO.read(resource);
        if(image == null) return null;
        return toCompatibleImage(image);
    }
    
    public static BufferedImage loadCompatibleImage(InputStream in) throws IOException {
        BufferedImage image = ImageIO.read(in);
        if(image == null) return null;
        return toCompatibleImage(image);
    }
    
    public static BufferedImage toCompatibleImage(BufferedImage image) {
        BufferedImage compatibleImage = configuration.createCompatibleImage(image.getWidth(),
                image.getHeight(), Transparency.TRANSLUCENT);
        Graphics g = compatibleImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return compatibleImage;
    }
    
    
    /** Sets then clip on a graphics object by merging with the existing
     * clip instead of replacing it. The new clip will be an intersection of
     * the old clip and the passed in clip shape.   The old clip shape will
     * be returned
     */
    public static Shape setMergedClip(Graphics2D g, Shape newClip) {
        Shape oldClip = g.getClip();
        if(oldClip == null) {
            g.setClip(newClip);
            return null;
        }
        Area area = new Area(oldClip);
        area.intersect(new Area(newClip));//new Rectangle(0,0,width,height)));
        g.setClip(area);
        return oldClip;
    }
    
    
    /** Resizes a gradient to fill the width and height available. If the
     * gradient is left to right it will be resized to fill the entire width.
     * If the gradient is top to bottom it will be resized to fill the entire
     * height. If the gradient is on an angle it will be resized to go from
     * one corner to the other of the rectangle formed by (0,0 -> width,height).
     *
     * This method can resize java.awt.GradientPaint, java.awt.LinearGradientPaint,
     * and the LinearGradientPaint implementation from Apache's Batik project. Note,
     * this method does not require the MultipleGradientPaint.jar from Apache to
     * compile or to run. MultipleGradientPaint.jar *is* required if you want
     * to resize the LinearGradientPaint from that jar.
     *
     * Any paint passed into this method which is not a kind of gradient paint (like
     * a Color or TexturePaint) will be returned unmodified. It will not throw
     * an exception. If the gradient cannot be resized due to other errors the
     * original paint will be returned unmodified. It will not throw an
     * exception.
     *
     */
    public static Paint resizeGradient(Paint p, int width, int height) {
        if(p == null) return p;
        
        if(p instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint)p;
            Point2D[] pts = new Point2D[2];
            pts[0] = gp.getPoint1();
            pts[1] = gp.getPoint2();
            pts = adjustPoints(pts, width, height);
            return new GradientPaint(pts[0], gp.getColor1(), pts[1], gp.getColor2(), gp.isCyclic());
        }
        
        if("java.awt.LinearGradientPaint".equals(p.getClass().getName()) ||
           "org.apache.batik.ext.awt.LinearGradientPaint".equals(p.getClass().getName())) {
            return resizeLinearGradient(p,width,height);
        }
        return p;
    }
    
    
    private static Paint resizeLinearGradient(Paint p, int width, int height) {
        try {
            Point2D[] pts = new Point2D[2];
            pts[0] = (Point2D) invokeMethod(p,"getStartPoint");
            pts[1] = (Point2D) invokeMethod(p,"getEndPoint");
            pts = adjustPoints(pts, width, height);
            float[] fractions = (float[]) invokeMethod(p,"getFractions");
            Color[] colors = (Color[]) invokeMethod(p,"getColors");
            
            Constructor<?> con = p.getClass().getDeclaredConstructor(
                    Point2D.class, Point2D.class,
                    new float[0].getClass(),
                    new Color[0].getClass());
            return (Paint) con.newInstance(pts[0],pts[1],fractions, colors);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return p;
    }
    
    private static Object invokeMethod(final Object p, final String methodName)
            throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException, SecurityException, IllegalAccessException {
        Method meth = p.getClass().getMethod(methodName);
        return meth.invoke(p);
    }
    
    
    private static Point2D[] adjustPoints(Point2D[] pts, int width, int height) {
        Point2D start = pts[0];
        Point2D end = pts[1];
        
        double angle = calcAngle(start,end);
        double a2 = Math.toDegrees(angle);
        double e = 1;
        
        // if it is near 0 degrees
        if(Math.abs(angle) < Math.toRadians(e) ||
                Math.abs(angle) > Math.toRadians(360-e)) {
            start = new Point2D.Float(0,0);
            end = new Point2D.Float(width,0);
        }
        
        // near 45
        if(isNear(a2, 45, e)) {
            start = new Point2D.Float(0,0);
            end = new Point2D.Float(width,height);
        }
        
        // near 90
        if(isNear(a2,  90, e)) {
            start = new Point2D.Float(0,0);
            end = new Point2D.Float(0,height);
        }
        
        // near 135
        if(isNear(a2, 135, e)) {
            start = new Point2D.Float(width,0);
            end = new Point2D.Float(0,height);
        }
        
        // near 180
        if(isNear(a2, 180, e)) {
            start = new Point2D.Float(width,0);
            end = new Point2D.Float(0,0);
        }
        
        // near 225
        if(isNear(a2, 225, e)) {
            start = new Point2D.Float(width,height);
            end = new Point2D.Float(0,0);
        }
        
        // near 270
        if(isNear(a2, 270, e)) {
            start = new Point2D.Float(0,height);
            end = new Point2D.Float(0,0);
        }
        
        // near 315
        if(isNear(a2, 315, e)) {
            start = new Point2D.Float(0,height);
            end = new Point2D.Float(width,0);
        }
        
        return new Point2D[] { start, end };
    }
    
    private static boolean isNear(double angle, double target, double error) {
        return Math.abs(target - Math.abs(angle)) < error;
    }
    
    private static double calcAngle(Point2D p1, Point2D p2) {
        double x_off = p2.getX() - p1.getX();
        double y_off = p2.getY() - p1.getY();
        double angle = Math.atan(y_off / x_off);
        if (x_off < 0) {
            angle = angle + Math.PI;
        }
        
        if(angle < 0) { angle+= 2*Math.PI; }
        if(angle > 2*Math.PI) { angle -= 2*Math.PI; }
        return angle;
    }
/*    
    public static void main(String ... args) {
        LinearGradientPaint in = new LinearGradientPaint(
                new Point(0,0), new Point(10,0),
                new float[] {0f, 0.5f, 1f},
                new Color[] {Color.RED, Color.GREEN, Color.BLUE});
        System.out.println("in  = " + toString(in));
        Paint out = resizeGradient(in,100,100);
        System.out.println("out = " + toString((MultipleGradientPaint) out));
    }*/
    /*
    private static String toString(MultipleGradientPaint paint) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(paint.getClass().getName());
        Color[] colors = paint.getColors();
        float[] values = paint.getFractions();
        buffer.append("[");
        for(int i=0; i<colors.length; i++) {
            buffer.append("#").append(Integer.toHexString(colors[i].getRGB()));
            buffer.append(":");
            buffer.append(values[i]);
            buffer.append(", ");
        }
        buffer.append("]");
        if(paint instanceof LinearGradientPaint) {
            LinearGradientPaint lgp = (LinearGradientPaint) paint;
            buffer.append(", ");
            buffer.append(""+lgp.getStartPoint().getX() + ", " + lgp.getStartPoint().getY());
            buffer.append("->");
            buffer.append(""+lgp.getEndPoint().getX() + ", " + lgp.getEndPoint().getY());
        }
        
        return buffer.toString();
    }*/
    
//    private static void p(String string) {
//        System.out.println(string);
//    }

}
