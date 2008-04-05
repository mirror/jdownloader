/*
 * ShapeUtils.java
 *
 * Created on October 18, 2006, 10:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.util;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author joshy
 */
public final class ShapeUtils {
    
    /** Creates a new instance of ShapeUtils */
    private ShapeUtils() { }
    
    public static Shape generatePolygon(int sides, int outsideRadius, boolean normalize) {
        return generatePolygon(sides, outsideRadius, 0, normalize);
    }
    
    public static Shape generatePolygon(int sides, int outsideRadius, int insideRadius, boolean normalize) {
        Shape shape = generatePolygon(sides,outsideRadius,insideRadius);
        if(normalize) {
            Rectangle2D bounds = shape.getBounds2D();
            GeneralPath path = new GeneralPath(shape);
            shape = path.createTransformedShape(
                    AffineTransform.getTranslateInstance(-bounds.getX(), -bounds.getY()));
        }
        return shape;
    }
    
    
    public static Shape generatePolygon(int sides, int outsideRadius, int insideRadius) {
        
        if(sides < 3) {
            return new Ellipse2D.Float(0,0,10,10);
        }
        
        AffineTransform trans = new AffineTransform();
        Polygon poly = new Polygon();
        for(int i=0; i<sides; i++) {
            trans.rotate(Math.PI*2/(float)sides/2);
            Point2D out = trans.transform(new Point2D.Float(0,outsideRadius),null);
            poly.addPoint((int)out.getX(), (int)out.getY());
            trans.rotate(Math.PI*2/(float)sides/2);
            if(insideRadius > 0) {
                Point2D in = trans.transform(new Point2D.Float(0,insideRadius),null);
                poly.addPoint((int)in.getX(), (int)in.getY());
            }
        }
        
        return poly;
    }

    public static Shape generateShapeFromText(Font font, char ch) {
        return generateShapeFromText(font, new String(new char[] {ch}));
    }
    
    public static Shape generateShapeFromText(Font font, String string) {
        BufferedImage img = new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        GlyphVector vect = font.createGlyphVector(g2.getFontRenderContext(), string);
        Shape shape = vect.getOutline(0f,(float)-vect.getVisualBounds().getY());
        g2.dispose();
        return shape;
    }
}
