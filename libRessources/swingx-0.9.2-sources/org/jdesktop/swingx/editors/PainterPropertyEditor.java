/*
 * PainterPropertyEditor.java
 *
 * Created on March 21, 2006, 11:26 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.editors;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.PropertyEditorSupport;
import java.beans.Statement;
import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import org.apache.batik.ext.awt.LinearGradientPaint;
import org.apache.batik.ext.awt.MultipleGradientPaint.ColorSpaceEnum;
import org.apache.batik.ext.awt.RadialGradientPaint;
import org.jdesktop.swingx.JXPanel;
//import org.jdesktop.swingx.painter.BackgroundPainter;
import org.jdesktop.swingx.painter.CheckerboardPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.PinstripePainter;
import org.jdesktop.swingx.painter.ShapePainter;
import org.jdesktop.swingx.painter.TextPainter;
//import org.jdesktop.swingx.painter.gradient.BasicGradientPainter;
//import org.jdesktop.swingx.painter.gradient.LinearGradientPainter;
//import org.jdesktop.swingx.painter.gradient.RadialGradientPainter;

/**
 * Two parts to this property editor. The first part is a simple dropdown.
 * The second part is a complicated UI for constructing multiple "layers" of
 * various different Painters, including gradient painters.
 *
 * @author Richard
 */
public class PainterPropertyEditor extends PropertyEditorSupport {
    /** Creates a new instance of PainterPropertyEditor */
    public PainterPropertyEditor() {
    }
    
    public Painter getValue() {
        return (Painter)super.getValue();
    }
    
    public String getJavaInitializationString() {
        Painter painter = getValue();
        //TODO!!!
        return painter == null ? "null" :
            "new org.jdesktop.swingx.painter.CheckerboardPainter()";
    }
    
    public static void main(String... args) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(300);
        XMLEncoder e = new XMLEncoder(baos);
        
        e.setPersistenceDelegate(GradientPaint.class, new GradientPaintDelegate());
        e.setPersistenceDelegate(Arc2D.Float.class, new Arc2DDelegate());
        e.setPersistenceDelegate(Arc2D.Double.class, new Arc2DDelegate());
        e.setPersistenceDelegate(CubicCurve2D.Float.class, new CubicCurve2DDelegate());
        e.setPersistenceDelegate(CubicCurve2D.Double.class, new CubicCurve2DDelegate());
        e.setPersistenceDelegate(Ellipse2D.Float.class, new Ellipse2DDelegate());
        e.setPersistenceDelegate(Ellipse2D.Double.class, new Ellipse2DDelegate());
        e.setPersistenceDelegate(Line2D.Float.class, new Line2DDelegate());
        e.setPersistenceDelegate(Line2D.Double.class, new Line2DDelegate());
        e.setPersistenceDelegate(Point2D.Float.class, new Point2DDelegate());
        e.setPersistenceDelegate(Point2D.Double.class, new Point2DDelegate());
        e.setPersistenceDelegate(QuadCurve2D.Float.class, new QuadCurve2DDelegate());
        e.setPersistenceDelegate(QuadCurve2D.Double.class, new QuadCurve2DDelegate());
        e.setPersistenceDelegate(Rectangle2D.Float.class, new Rectangle2DDelegate());
        e.setPersistenceDelegate(Rectangle2D.Double.class, new Rectangle2DDelegate());
        e.setPersistenceDelegate(RoundRectangle2D.Float.class, new RoundRectangle2DDelegate());
        e.setPersistenceDelegate(RoundRectangle2D.Double.class, new RoundRectangle2DDelegate());
        e.setPersistenceDelegate(Area.class, new AreaDelegate());
        e.setPersistenceDelegate(GeneralPath.class, new GeneralPathDelegate());
        e.setPersistenceDelegate(AffineTransform.class, new AffineTransformDelegate());
        e.setPersistenceDelegate(RadialGradientPaint.class, new RadialGradientPaintDelegate());
        e.setPersistenceDelegate(LinearGradientPaint.class, new LinearGradientPaintDelegate());
        e.setPersistenceDelegate(Insets.class, new InsetsDelegate());
        
        /*
        Area a = new Area(new RoundRectangle2D.Double(20, 20, 50, 50, 4, 4));
        a.add(new Area(new Ellipse2D.Double(10, 10, 100, 20)));
         
        TextPainter textPainter = new TextPainter("Yo dude");
        textPainter.setFillPaint(Color.WHITE);
        
        e.writeObject(new CompoundPainter(
                new MattePainter(),
                new CheckerboardPainter(),
                new MattePainter(Color.BLACK),
                //new BasicGradientPainter(BasicGradientPainter.RED_XP),
                //new LinearGradientPainter(LinearGradientPainter.BLACK_PERSPECTIVE),
                new MattePainter(new RadialGradientPaint(
                    new Point2D.Double(.5, .5),
                    .2f, new float[] {0f, .5f, 1f},
                    new Color[] {Color.BLACK,Color.WHITE,Color.RED})),
                new ShapePainter(a),
                new PinstripePainter(),
                textPainter,
                new GlossPainter()
        //                new IconPainter(),
        //                new ImagePainter(),
                ));
        e.close();
        System.out.println(baos.toString());
         
         
        XMLDecoder d = new XMLDecoder(new ByteArrayInputStream(baos.toByteArray()));
         
        Painter p = (Painter)d.readObject();
         
        JFrame frame = new JFrame("Yo momma");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JXPanel panel = new JXPanel();
        panel.setBackgroundPainter(p);
        frame.add(panel);
        frame.setSize(800,600);
        frame.setVisible(true);
         */
        
        RadialGradientPaint rp = new RadialGradientPaint(new Point2D.Double(0.5,0.5),
                .2f, new float[] {0f, .5f, 1f},
                new Color[] {Color.BLACK, Color.WHITE, Color.RED}
        );
        baos = new ByteArrayOutputStream(300);
        e = new XMLEncoder(baos);
        TestStuff ts = new TestStuff();
        //ts.setStr("asdfasdf");
        ts.str = "asdfasdf";
        System.out.println("db = " + ts.getDb());
        ts.setDb(new Point2D.Double(1.0,1.0));
        ts.setIns(new Insets(5,5,5,5));
        System.out.println("db = " + ts.getDb());
        e.writeObject(ts);
        e.close();
        System.out.println("more stuff");
        System.out.println(baos.toString());
        if(new Point2D.Double(0,0).equals(new Point2D.Double(1,1))) {
            System.out.println("they are equal");
        } else {
            System.out.println("they are not equal");
        }
    }
    
    public static final class TestStuff {
        public Point2D.Double db = null;// = new Point2D.Double(5,5);
        public TestStuff() {
            
        }
        public TestStuff(Point2D.Double db, String str) {
            setDb(db);
            setStr(str);
        }
        public void setDb(Point2D.Double db) {
            this.db = db;
        }
        public Point2D.Double getDb() {
            //new Exception().printStackTrace();
            return this.db;
        }
        public String str = "asdf";
        public void setStr(String str) {
            this.str = str;
        }
        public String getStr() {
            return this.str;
        }
        private Insets ins = null;
        
        public Insets getIns() {
            return ins;
        }
        
        public void setIns(Insets ins) {
            this.ins = ins;
        }
    }
    
    public static final class GradientPaintDelegate extends DefaultPersistenceDelegate {
        public GradientPaintDelegate() {
            super(new String[] {"point1", "color1", "point2", "color2"});
        }
    }
    public static final class LinearGradientPaintDelegate extends DefaultPersistenceDelegate {
        public LinearGradientPaintDelegate() {
            super(new String[] {"startPoint", "endPoint", "fractions", "colors"});
            //these 3 not yet supported. The problem is the
            //colorspace. I haven't figured out how to transfer that one yet
            //, "cycleMethod", "colorSpace", "transform"});
        }
        
        protected Expression instantiate(Object oldInstance, Encoder out) {
            
            ColorSpaceEnum e = ((LinearGradientPaint)oldInstance).getColorSpace();
            Expression retValue;
            
            retValue = super.instantiate(oldInstance, out);
            return retValue;
        }
    }
    public static final class RadialGradientPaintDelegate extends DefaultPersistenceDelegate {
        public RadialGradientPaintDelegate() {
            super(new String[] {"centerPoint", "radius", "focusPoint", "fractions", "colors"});
            //these 3 not yet supported. The problem is the
            //colorspace. I haven't figured out how to transfer that one yet
            //, "cycleMethod", "colorSpace", "transform"});
        }
    }
    public static final class Arc2DDelegate extends DefaultPersistenceDelegate {
        public Arc2DDelegate() {
            super(new String[] {"x", "y", "width", "height", "angleStart", "angleExtent", "arcType"});
        }
    }
    public static final class CubicCurve2DDelegate extends DefaultPersistenceDelegate {
        public CubicCurve2DDelegate() {
            super(new String[] {"x1", "y1", "ctrlX1", "ctrlY1", "ctrlX2", "ctrlY2", "x2", "y2"});
        }
    }
    public static final class Ellipse2DDelegate extends DefaultPersistenceDelegate {
        public Ellipse2DDelegate() {
            super(new String[] {"x", "y", "width", "height"});
        }
    }
    public static final class Line2DDelegate extends DefaultPersistenceDelegate {
        public Line2DDelegate() {
            super(new String[] {"x1", "y1", "x2", "y2"});
        }
    }
    public static final class Point2DDelegate extends DefaultPersistenceDelegate {
        public Point2DDelegate() {
            super(new String[] {"x", "y"});
        }/*
    protected Expression instantiate(Object oldInstance, Encoder out) {
       Point2D pt = (Point2D)oldInstance;
       Object[] constructorArgs = new Object[]{
       pt.getX(), pt.getY()
       };
       return new Expression(new Point2D.Double(-1,-1), oldInstance.getClass(), "new", constructorArgs);
   } */
   /*
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            super.initialize(type,oldInstance,newInstance,out);
            System.out.println("initialize called: " + type + " " + oldInstance
                    + " " + newInstance + " " + out);
        }*/
        
    }
    public static final class QuadCurve2DDelegate extends DefaultPersistenceDelegate {
        public QuadCurve2DDelegate() {
            super(new String[] {"x1", "y1", "ctrlX", "ctrlY", "x2", "y2"});
        }
    }
    public static final class Rectangle2DDelegate extends DefaultPersistenceDelegate {
        public Rectangle2DDelegate() {
            super(new String[] {"x", "y", "width", "height"});
        }
    }
    public static final class InsetsDelegate extends DefaultPersistenceDelegate {
        public InsetsDelegate() {
            super(new String[] {"top", "left", "bottom", "right"});
        }
        protected Expression instantiate(Object oldInstance,
                Encoder out) {
            Insets ins = (Insets)oldInstance;
            return new Expression(oldInstance,
                    oldInstance.getClass(),
                    "new",
                    new Object[]{ ins.top, ins.left, ins.bottom, ins.right });
        }
    }
    public static final class RoundRectangle2DDelegate extends DefaultPersistenceDelegate {
        public RoundRectangle2DDelegate() {
            super(new String[] {"x", "y", "width", "height", "arcWidth", "arcHeight"});
        }
    }
    public static final class AreaDelegate extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            Area a = (Area)oldInstance;
            
            //use the default constructor
            AffineTransform tx = new AffineTransform();
            PathIterator itr = a.getPathIterator(tx);
            
            GeneralPath path = new GeneralPath();
            out.writeExpression(new Expression(path, GeneralPath.class, "new", new Object[0]));
            
            while (!itr.isDone()) {
                float[] segment = new float[6]; //must use floats because lineTo etc use floats
                int pathType = itr.currentSegment(segment);
                
                switch (pathType) {
                    case PathIterator.SEG_CLOSE:
                        out.writeStatement(new Statement(path, "closePath", new Object[0]));
                        break;
                    case PathIterator.SEG_CUBICTO:
                        out.writeStatement(new Statement(path, "curveTo", new Object[] {segment[0], segment[1], segment[2], segment[3], segment[4], segment[5]}));
                        break;
                    case PathIterator.SEG_LINETO:
                        out.writeStatement(new Statement(path, "lineTo", new Object[] {segment[0], segment[1]}));
                        break;
                    case PathIterator.SEG_MOVETO:
                        out.writeStatement(new Statement(path, "moveTo", new Object[] {segment[0], segment[1]}));
                        break;
                    case PathIterator.SEG_QUADTO:
                        out.writeStatement(new Statement(path, "quadTo", new Object[] {segment[0], segment[1], segment[2], segment[3]}));
                        break;
                }
                itr.next();
            }
            
            return new Expression(a, Area.class, "new", new Object[] {path});
        }
    }
    public static final class AffineTransformDelegate extends DefaultPersistenceDelegate {
        public AffineTransformDelegate() {
            super(new String[] {"scaleX", "shearY", "shearX", "scaleY", "translateX", "translateY"});
        }
    }
    public static final class GeneralPathDelegate extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            return new Expression(oldInstance, GeneralPath.class, "new", new Object[0]);
        }
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            GeneralPath a = (GeneralPath)oldInstance;
            
            AffineTransform tx = new AffineTransform();
            PathIterator itr = a.getPathIterator(tx);
            
            out.writeStatement(new Statement(a, "setWindingRule", new Object[] {a.getWindingRule()}));
            
            while (!itr.isDone()) {
                float[] segment = new float[6]; //must use floats because lineTo etc use floats
                int pathType = itr.currentSegment(segment);
                
                switch (pathType) {
                    case PathIterator.SEG_CLOSE:
                        out.writeStatement(new Statement(a, "closePath", new Object[0]));
                        break;
                    case PathIterator.SEG_CUBICTO:
                        out.writeStatement(new Statement(a, "curveTo", new Object[] {segment[0], segment[1], segment[2], segment[3], segment[4], segment[5]}));
                        break;
                    case PathIterator.SEG_LINETO:
                        out.writeStatement(new Statement(a, "lineTo", new Object[] {segment[0], segment[1]}));
                        break;
                    case PathIterator.SEG_MOVETO:
                        out.writeStatement(new Statement(a, "moveTo", new Object[] {segment[0], segment[1]}));
                        break;
                    case PathIterator.SEG_QUADTO:
                        out.writeStatement(new Statement(a, "quadTo", new Object[] {segment[0], segment[1], segment[2], segment[3]}));
                        break;
                }
                itr.next();
            }
        }
    }
//    public static final class PaintDelegate extends PersistenceDelegate {
//        protected Expression instantiate(Object oldInstance, Encoder out) {
//            if (oldInstance instanceof GradientPaint) {
//
//            }
//        }
//    }
}
