/*
 * PainterUtil.java
 *
 * Created on July 20, 2006, 1:18 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.editors;


import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.ExceptionListener;
import java.beans.Expression;
import java.beans.Introspector;
import java.beans.PersistenceDelegate;
import java.beans.PropertyDescriptor;
import java.beans.Statement;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import org.apache.batik.ext.awt.LinearGradientPaint;
import org.apache.batik.ext.awt.RadialGradientPaint;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.ImagePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.editors.PainterPropertyEditor.*;
import org.jdesktop.swingx.painter.AbstractLayoutPainter;
import org.jdesktop.swingx.painter.RectanglePainter;

/**
 *
 * @author joshy
 */
public class PainterUtil {
    
    /** Creates a new instance of PainterUtil */
    private PainterUtil() {
    }
    /*
    public static void main(String[] args) throws Exception {
        ImagePainter ip = new ImagePainter();
        ip.setImageString("file:/Users/joshy/Pictures/cooltikis.jpg");
        File outfile = new File("/Users/joshy/Desktop/test.xml");
        outfile.createNewFile();
        p("outfile = " + outfile.getAbsolutePath());
        p("exists = " + outfile.exists());
        savePainterToFile(ip, outfile, outfile.getParentFile().toURL());
        p("---------");
        ip = (ImagePainter)loadPainter(outfile);
        p("image = " + ip.getImage());
        ip = (ImagePainter)loadPainter(outfile.toURL());
        p("image = " + ip.getImage());
        p("==================");
        
        
    }*/
    
    public static Painter loadPainter(File file) throws FileNotFoundException, MalformedURLException, IOException {
        return loadPainter(file.toURL(), file.toURL());
    }
    
    private static Painter loadPainter(final URL in, URL baseURL) throws FileNotFoundException, IOException {
        Thread.currentThread().setContextClassLoader(PainterUtil.class.getClassLoader());
        XMLDecoder dec = new XMLDecoder(in.openStream());
//        p("creating a persistence owner with the base url: " + baseURL);
        dec.setOwner(new PersistenceOwner(baseURL));
        dec.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        });
        Object obj = dec.readObject();
        return (Painter)obj;
    }
    
    public static Painter loadPainter(URL url) throws IOException {
        return loadPainter(url,url);
    }
    
    static public void savePainterToFile(Painter compoundPainter, File file) throws IOException {
        savePainterToFile(compoundPainter,file,file.getParentFile().toURL());
    }
    
    static public void savePainterToFile(Painter compoundPainter, File file, URL baseURL) throws IOException {
        //System.setErr(null);
        //        u.p("writing out to: " + file.getCanonicalPath());
        setTransient(ImagePainter.class, "image");
        setTransient(ImagePainter.class, "imageString");
        //setTransient(CompoundPainter.class,"antialiasing");
        //setTransient(AbstractPainter.class,"antialiasing");
        //setTransient(AbstractPainter.class,"renderingHints");
        //setPropertyDelegate();
        
        XMLEncoder e = new XMLEncoder(new FileOutputStream(file));
        e.setOwner(new PersistenceOwner(baseURL));
        //p("owner = " + e.getOwner());
        //e.setOwner(compoundPainter);
        
        // serialize the enums
        //e.setPersistenceDelegate(AbstractPainter.Antialiasing.class, new TypeSafeEnumPersistenceDelegate());
        e.setPersistenceDelegate(AbstractPainter.Interpolation.class, new TypeSafeEnumPersistenceDelegate());
       // e.setPersistenceDelegate(AbstractPainter.FractionalMetrics.class, new TypeSafeEnumPersistenceDelegate());
        e.setPersistenceDelegate(RectanglePainter.Style.class, new TypeSafeEnumPersistenceDelegate());
        e.setPersistenceDelegate(AbstractLayoutPainter.HorizontalAlignment.class, new TypeSafeEnumPersistenceDelegate());
        e.setPersistenceDelegate(AbstractLayoutPainter.VerticalAlignment.class, new TypeSafeEnumPersistenceDelegate());
        
        
        e.setPersistenceDelegate(AbstractPainter.class, new AbstractPainterDelegate());
        e.setPersistenceDelegate(ImagePainter.class, new ImagePainterDelegate());
        e.setPersistenceDelegate(RenderingHints.class, new RenderingHintsDelegate());
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
        
        e.writeObject(compoundPainter);
        e.close();
    }
    
    //private static void setPropertyDelegate(Class clazz, String property, )
    private static void setTransient(Class clazz, String property) {
        try {
            BeanInfo info = Introspector.getBeanInfo(clazz);
            PropertyDescriptor[] propertyDescriptors =
                    info.getPropertyDescriptors();
            for (int i = 0; i < propertyDescriptors.length; ++i) {
                PropertyDescriptor pd = propertyDescriptors[i];
                if (pd.getName().equals(property)) {
                    pd.setValue("transient", Boolean.TRUE);
                    //u.p(pd.attributeNames());
                }
            }
        } catch (Exception ex) {
            //u.p(ex);
        }
    }
    
    static class TypeSafeEnumPersistenceDelegate extends PersistenceDelegate {
        protected boolean mutatesTo( Object oldInstance, Object newInstance ) {
            return oldInstance == newInstance;
        }
        
        protected Expression instantiate( Object oldInstance, Encoder out ) {
            Class type = oldInstance.getClass();
            if ( !Modifier.isPublic( type.getModifiers() ) )
                throw new IllegalArgumentException( "Could not instantiate instance of non-public class: " + oldInstance );
            
            for ( Field field : type.getFields() ) {
                int mod = field.getModifiers();
                if ( Modifier.isPublic( mod ) && Modifier.isStatic( mod ) && Modifier.isFinal( mod ) && ( type == field.getDeclaringClass() ) ) {
                    try {
                        if ( oldInstance == field.get( null ) )
                            return new Expression( oldInstance, field, "get", new Object[]{null} );
                    } catch ( IllegalAccessException exception ) {
                        throw new IllegalArgumentException( "Could not get value of the field: " + field, exception );
                    }
                }
            }
            throw new IllegalArgumentException( "Could not instantiate value: " + oldInstance );
        }
    }
    
    public static final class RenderingHintsDelegate extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            //u.p("rh inst");
            // give it a constructor w/ null as the argument
            return new Expression(oldInstance, oldInstance.getClass(),
                    "new", new Object[] { null });
        }
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            //u.p("rh init ");
            RenderingHints rh = (RenderingHints)oldInstance;
            out.writeStatement(new Statement(oldInstance, "put",
                    new Object[] {RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON}));
            //u.p("done");
        }
    }
    
    public static final class AbstractPainterDelegate extends DefaultPersistenceDelegate {
        protected void initialize(Class type, Object oldInstance,
                Object newInstance, Encoder out) {
//            p("ap delegate called");
            super.initialize(type, oldInstance,  newInstance, out);
        }
    }
    
    public static final class ImagePainterDelegate extends DefaultPersistenceDelegate {
        protected void initialize(Class type, Object oldInstance,
                Object newInstance, Encoder out) {
//            p("image painter delegate called");
            super.initialize(type, oldInstance,  newInstance, out);
            //p("old instance = " + oldInstance);
            //p("owner = " + ((XMLEncoder)out).getOwner());
            PersistenceOwner owner = (PersistenceOwner)((XMLEncoder)out).getOwner();
            ImagePainter ip = (ImagePainter)oldInstance;
//            p("need to convert string: " + ip.getImageString());
            String s = owner.toXMLURL(ip.getImageString());
//            p("converted to: " + s);
                //out.writeExpression(new Expression(oldInstance,owner,"fromXMLURL",new Object[]{ip.getImageString()}));
                //out.writeStatement(new Statement(owner,"fromXMLURL",new Object[]{ip.getImageString()}));
                //out.writeStatement(new Statement(oldInstance,"setImageString",new Object[]{
                //new Expression(oldInstance,owner,"fromXMLURL",new Object[]{ip.getImageString()})
                //}));
                
            out.writeStatement(new Statement(oldInstance,"setResolver",new Object[]{owner}));
            out.writeStatement(new Statement(oldInstance,"setImageString",new Object[]{s}));
        }
    }
    
    public static void savePainterToImage(JComponent testPanel, CompoundPainter compoundPainter, File file) throws IOException {
        BufferedImage img = new BufferedImage(testPanel.getWidth(),testPanel.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setBGP(testPanel,compoundPainter);
        testPanel.paint(g);
        ImageIO.write(img,"png",file);
    }
    
    public static void setBGP(JComponent comp, Painter painter) {
        if(comp instanceof JXPanel) {
            ((JXPanel)comp).setBackgroundPainter(painter);
        }
        if(comp instanceof JXButton) {
            ((JXButton)comp).setBackgroundPainter(painter);
        }
    }
    public static void setFGP(JComponent comp, Painter painter) {
        if(comp instanceof JXLabel) {
            ((JXLabel)comp).setForegroundPainter(painter);
        }
        if(comp instanceof JXButton) {
            ((JXButton)comp).setForegroundPainter(painter);
        }
    }
    
    public static Painter getFGP(JComponent comp) {
        if(comp instanceof JXLabel) {
            return ((JXLabel)comp).getForegroundPainter();
        }
        if(comp instanceof JXButton) {
            return ((JXButton)comp).getForegroundPainter();
        }
        return null;
    }
    
    public static Painter getBGP(JComponent comp) {
        if(comp instanceof JXPanel) {
            return ((JXPanel)comp).getBackgroundPainter();
        }
        if(comp instanceof JXButton) {
            return ((JXButton)comp).getBackgroundPainter();
        }
        return null;
    }
    
    public static class PersistenceOwner {
        private URL baseURL;
        public PersistenceOwner(URL baseURL) {
            this.baseURL = baseURL;
        }
        
        public URL getBaseURL() {
            return baseURL;
        }
        public String toXMLURL(String url) {
//            p("========");
//            p("in toXMLURL()");
//            p("base url = " + baseURL);
//            p("url to convert = " + url);
            //trim off the beginning if the url is relative to the base
            if(url.startsWith(baseURL.toString())) {
//                p("it's a valid substring!!!!!!!!!!!!");
                url = url.substring(baseURL.toString().length());
//                p("subsstring = " + url);
            }
//            p("final url = " + url);
//            p("========");
            return url;
        }
        
        public String fromXMLURL(String url) throws MalformedURLException {
            /*p("========");
            p("in fromXMLURL()");
            p("base url = " + baseURL);
            p("url to convert: " + url);*/
            String s = new URL(baseURL,url).toString();
//            p("returning: " + s);
//            p("========");
            return s;
        }
    }
}
