/*
 * $Id: JXLabel.java,v 1.20 2008/02/29 08:35:12 rah003 Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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

package org.jdesktop.swingx;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SizeRequirements;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.GlyphView;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.ParagraphView;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.TabableView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.WrappedPlainView;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;

/**
 * <p>
 * A {@link javax.swing.JLabel} subclass which supports {@link org.jdesktop.swingx.painter.Painter}s, multi-line text,
 * and text rotation.
 * </p>
 * 
 * <p>
 * Painter support consists of the <code>foregroundPainter</code> and <code>backgroundpainter</code> properties. The
 * <code>backgroundPainter</code> refers to a painter responsible for painting <i>beneath</i> the text and icon. This
 * painter, if set, will paint regardless of the <code>opaque</code> property. If the background painter does not
 * fully paint each pixel, then you should make sure the <code>opaque</code> property is set to false.
 * </p>
 * 
 * <p>
 * The <code>foregroundPainter</code> is responsible for painting the icon and the text label. If no foregroundPainter
 * is specified, then the look and feel will paint the label. Note that if opaque is set to true and the look and feel
 * is rendering the foreground, then the foreground <i>may</i> paint over the background. Most look and feels will
 * paint a background when <code>opaque</code> is true. To avoid this behavior, set <code>opaque</code> to false.
 * </p>
 * 
 * <p>
 * Since JXLabel is not opaque by default (<code>isOpaque()</code> returns false), neither of these problems
 * typically present themselves.
 * </p>
 * 
 * <p>
 * Multi-line text is enabled via the <code>lineWrap</code> property. Simply set it to true. By default, line wrapping
 * occurs on word boundaries.
 * </p>
 * 
 * <p>
 * The text (actually, the entire foreground and background) of the JXLabel may be rotated. Set the
 * <code>rotation</code> property to specify what the rotation should be.
 * </p>
 * TODO not yet determined what API this will use.
 * 
 * @author joshua.marinacci@sun.com
 * @author rbair
 * @author rah
 * @author mario_cesar
 */
public class JXLabel extends JLabel {
    // textOrientation value declarations...
    public static final double NORMAL = 0;

    public static final double INVERTED = Math.PI;

    public static final double VERTICAL_LEFT = 3 * Math.PI / 2;

    public static final double VERTICAL_RIGHT = Math.PI / 2;

    private double textRotation = NORMAL;

    private boolean painting = false;

    private Painter foregroundPainter;

    private Painter backgroundPainter;

    private boolean multiLine;

    private int pWidth;

    private int pHeight;

    private boolean ignoreRepaint;

    private static final String oldRendererKey = "was" + BasicHTML.propertyKey;

    /**
     * Create a new JXLabel. This has the same semantics as creating a new JLabel.
     */
    public JXLabel() {
        super();
        initPainterSupport();
        initLineWrapSupport();
    }

    public JXLabel(Icon image) {
        super(image);
        initPainterSupport();
        initLineWrapSupport();
    }

    public JXLabel(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
        initPainterSupport();
        initLineWrapSupport();
    }

    /**
     * Create a new JXLabel with the given text as the text for the label. This is shorthand for:
     * 
     * <pre><code>
     * JXLabel label = new JXLabel();
     * label.setText(&quot;Some Text&quot;);
     * </code></pre>
     * 
     * @param text the text to set.
     */
    public JXLabel(String text) {
        super(text);
        initPainterSupport();
        initLineWrapSupport();
    }

    public JXLabel(String text, Icon image, int horizontalAlignment) {
        super(text, image, horizontalAlignment);
        initPainterSupport();
        initLineWrapSupport();
    }

    public JXLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
        initPainterSupport();
        initLineWrapSupport();
    }

    private void initPainterSupport() {
        foregroundPainter = new AbstractPainter() {
            protected void doPaint(Graphics2D g, Object object, int width, int height) {
                Insets i = getInsets();
                g = (Graphics2D) g.create(-i.left, -i.top, width, height);
                JXLabel.super.paintComponent(g);
                g.dispose();
            }
        };
    }

    /**
     * Helper method for initializing multiline support.
     */
    private void initLineWrapSupport() {
        addPropertyChangeListener(new MultiLineSupport());
    }

    /**
     * Returns the current foregroundPainter. This is a bound property. By default the foregroundPainter will be an
     * internal painter which executes the standard painting code (paintComponent()).
     * 
     * @return the current foreground painter.
     */
    public final Painter getForegroundPainter() {
        return foregroundPainter;
    }

    /**
     * Sets a new foregroundPainter on the label. This will replace the existing foreground painter. Existing painters
     * can be wrapped by using a CompoundPainter.
     * 
     * @param painter
     */
    public void setForegroundPainter(Painter painter) {
        Painter old = this.getForegroundPainter();
        this.foregroundPainter = painter;
        firePropertyChange("foregroundPainter", old, getForegroundPainter());
        repaint();
    }

    /**
     * Sets a Painter to use to paint the background of this component By default there is already a single painter
     * installed which draws the normal background for this component according to the current Look and Feel. Calling
     * <CODE>setBackgroundPainter</CODE> will replace that existing painter.
     * 
     * @param p the new painter
     * @see #getBackgroundPainter()
     */
    public void setBackgroundPainter(Painter p) {
        Painter old = getBackgroundPainter();
        backgroundPainter = p;
        firePropertyChange("backgroundPainter", old, getBackgroundPainter());
        repaint();
    }
    
    /**
     * Returns the current background painter. The default value of this property is a painter which draws the normal
     * JPanel background according to the current look and feel.
     * 
     * @return the current painter
     * @see #setBackgroundPainter(Painter)
     */
    public final Painter getBackgroundPainter() {
        return backgroundPainter;
    }

    /**
     * Gets current value of text rotation in rads.
     * 
     * @return a double representing the current rotation of the text
     * @see #setTextRotation(double)
     */
    public double getTextRotation() {
        return textRotation;
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (isPreferredSizeSet()) {
            return size;
        } else if (this.textRotation != NORMAL) {
            // #swingx-680 change the preferred size when rotation is set ... ideally this would be solved in the LabelUI rather then here
            double theta = getTextRotation();
            size.setSize(rotateWidth(size, theta), rotateHeight(size,
            theta));
        } else {
            // #swingx-780 preferred size is not set properly when parent container doesn't enforce the width
            View view = (View) getClientProperty(BasicHTML.propertyKey);
            Rectangle b = super.getBounds();
            Container tla = super.getTopLevelAncestor();
            if (view == null || tla == null) {
                return size;
            }
            Rectangle s = tla.getBounds();
            int newW = Math.min(b.width, s.width);
            if (newW == 0) {
                return size;
            }
            Icon i = getIcon();
            if (i != null) {
                newW -= i.getIconWidth() + getIconTextGap();
                
            }
            if (b.width > 0) {
                size.setSize(newW - 10, view.getPreferredSpan(View.Y_AXIS) );
            } else {
                view.setSize(newW , Integer.MAX_VALUE);
            }
        }
        return size;
    }

    public int getMaxLineSpan() {
        return maxLineSpan ;
    }
    
    public void setMaxLineSpan(int maxLineSpan) {
            int old = getMaxLineSpan();
            this.maxLineSpan = maxLineSpan;
            firePropertyChange("maxLineSpan", old, getMaxLineSpan());
    }

    private static int rotateWidth(Dimension size, double theta) {
        return (int)Math.round(size.width*Math.abs(Math.cos(theta)) +
        size.height*Math.abs(Math.sin(theta)));
    }

    private static int rotateHeight(Dimension size, double theta) {
        return (int)Math.round(size.width*Math.abs(Math.sin(theta)) +
        size.height*Math.abs(Math.cos(theta)));
    }

    /**
     * Sets new value for text rotation. The value can be anything in range <0,2PI>. Note that although property name
     * suggests only text rotation, the whole foreground painter is rotated in fact. Due to various reasons it is
     * strongly discouraged to access any size related properties of the label from other threads then EDT when this
     * property is set.
     * 
     * @param textOrientation Value for text rotation in range <0,2PI>
     * @see #getTextRotation()
     */
    public void setTextRotation(double textOrientation) {
        double old = getTextRotation();
        this.textRotation = textOrientation;
        if (old != getTextRotation()) {
            firePropertyChange("textRotation", old, getTextRotation());
        }
        repaint();
    }

    /**
     * Enables line wrapping support for plain text. By default this support is disabled to mimic default of the JLabel.
     * Value of this property has no effect on HTML text.
     * 
     * @param b the new value
     */
    public void setLineWrap(boolean b) {
        boolean old = isLineWrap();
        this.multiLine = b;
        if (isLineWrap() != old) {
            firePropertyChange("lineWrap", old, isLineWrap());
            if (getForegroundPainter() != null) {
                // XXX There is a bug here. In order to make painter work with this, caching has to be disabled
                ((AbstractPainter) getForegroundPainter()).setCacheable(!b);
            }
            repaint();
        }
    }

    /**
     * Returns the current status of line wrap support. The default value of this property is false to mimic default
     * JLabel behavior. Value of this property has no effect on HTML text.
     * 
     * @return the current multiple line splitting status
     */
    public boolean isLineWrap() {
        return this.multiLine;
    }

    private boolean paintBorderInsets = true;

	private int maxLineSpan = -1;
    
    /**
     * Returns true if the background painter should paint where the border is
     * or false if it should only paint inside the border. This property is 
     * true by default. This property affects the width, height,
     * and intial transform passed to the background painter.
     * @return current value of the paintBorderInsets property
     */
    public boolean isPaintBorderInsets() {
        return paintBorderInsets;
    }
    
    /**
     * Sets the paintBorderInsets property.
     * Set to true if the background painter should paint where the border is
     * or false if it should only paint inside the border. This property is true by default.
     * This property affects the width, height,
     * and intial transform passed to the background painter.
     * 
     * This is a bound property.
     * @param paintBorderInsets new value of the paintBorderInsets property
     */
    public void setPaintBorderInsets(boolean paintBorderInsets) {
        boolean old = this.isPaintBorderInsets();
        this.paintBorderInsets = paintBorderInsets;
        firePropertyChange("paintBorderInsets", old, isPaintBorderInsets());
    }
    
    /**
     * @param g graphics to paint on
     */
    @Override
    protected void paintComponent(Graphics g) {
        // resizing the text view causes recursive callback to the paint down the road. In order to prevent such
        // computationally intensive series of repaints every call to paint is skipped while top most call is being
        // executed.
        if (ignoreRepaint) {
            return;
        }
        if (backgroundPainter == null && foregroundPainter == null) {
            super.paintComponent(g);
        } else {
            pWidth = getWidth();
            pHeight = getHeight();
            Insets i = getInsets();
            if (backgroundPainter != null) {
            	Graphics2D tmp = (Graphics2D) g.create();
                if(!isPaintBorderInsets()) {
                    tmp.translate(i.left, i.top);
                    pWidth = pWidth - i.left - i.right;
                    pHeight = pHeight - i.top - i.bottom;
            	}
                backgroundPainter.paint(tmp, this, pWidth, pHeight);
                tmp.dispose();
            }
            if (foregroundPainter != null) {
                pWidth = getWidth() - i.left - i.right;
                pHeight = getHeight() - i.top - i.bottom;

                Point2D tPoint = calculateT();
                double wx = Math.sin(textRotation) * tPoint.getY() + Math.cos(textRotation) * tPoint.getX();
                double wy = Math.sin(textRotation) * tPoint.getX() + Math.cos(textRotation) * tPoint.getY();
                double x = (getWidth() - wx) / 2 + Math.sin(textRotation) * tPoint.getY();
                double y = (getHeight() - wy) / 2;
            	Graphics2D tmp = (Graphics2D) g.create();
            	if (i != null) {
                    tmp.translate(i.left + x, i.top + y);
            	} else {
            		tmp.translate(x, y);
            	}
                tmp.rotate(textRotation);

                painting = true;
                // uncomment to highlight text area
                // Color c = g2.getColor();
                // g2.setColor(Color.RED);
                // g2.fillRect(0, 0, getWidth(), getHeight());
                // g2.setColor(c);
                foregroundPainter.paint(tmp, this, pWidth, pHeight);
                tmp.dispose();
                painting = false;
                pWidth = 0;
                pHeight = 0;
            }
        }
    }
    
    private Point2D calculateT() {
        double tx = (double) getWidth();
        double ty = (double) getHeight();

        // orthogonal cases are most likely the most often used ones, so give them preferential treatment.
        if ((textRotation > 4.697 && textRotation < 4.727) || (textRotation > 1.555 && textRotation < 1.585)) {
            // vertical
            int tmp = pHeight;
            pHeight = pWidth;
            pWidth = tmp;
            tx = pWidth;
            ty = pHeight;
        } else if ((textRotation > -0.015 && textRotation < 0.015)
                || (textRotation > 3.140 && textRotation < 3.1430)) {
            // normal & inverted
            pHeight = getHeight();
            pWidth = getWidth();
        } else {
            // the rest of it. Calculate best rectangle that fits the bounds. "Best" is considered one that
            // allows whole text to fit in, spanned on preferred axis (X). If that doesn't work, fit the text
            // inside square with diagonal equal min(height, width) (Should be the largest rectangular area that
            // fits in, math proof available upon request)

            ignoreRepaint = true;
            double square = Math.min(getHeight(), getWidth()) * Math.cos(Math.PI / 4d);

            View v = (View) getClientProperty(BasicHTML.propertyKey);
            if (v == null) {
                // no html and no wrapline enabled means no view
                // ... find another way to figure out the heigh
                ty = getFontMetrics(getFont()).getHeight();
                double cw = (getWidth() - Math.abs(ty * Math.sin(textRotation)))
                        / Math.abs(Math.cos(textRotation));
                double ch = (getHeight() - Math.abs(ty * Math.cos(textRotation)))
                        / Math.abs(Math.sin(textRotation));
                // min of whichever is above 0 (!!! no min of abs values)
                tx = cw < 0 ? ch : ch > 0 ? Math.min(cw, ch) : cw;
            } else {
                float w = v.getPreferredSpan(View.X_AXIS);
                float h = v.getPreferredSpan(View.Y_AXIS);
                double c = w;
                double alpha = textRotation;// % (Math.PI/2d);
                boolean ready = false;
                while (!ready) {
                    // shorten the view len until line break is forced
                    while (h == v.getPreferredSpan(View.Y_AXIS)) {
                        w -= 10;
                        v.setSize(w, h);
                    }
                    if (w < square || h > square) {
                        // text is too long to fit no matter what. Revert shape to square since that is the
                        // best option (1st derivation for area size of rotated rect in rect is equal 0 for
                        // rotated rect with equal w and h i.e. for square)
                        w = h = (float) square;
                        // set view height to something big to prevent recursive resize/repaint requests
                        v.setSize(w, 100000);
                        break;
                    }
                    // calc avail width with new view height
                    h = v.getPreferredSpan(View.Y_AXIS);
                    double cw = (getWidth() - Math.abs(h * Math.sin(alpha))) / Math.abs(Math.cos(alpha));
                    double ch = (getHeight() - Math.abs(h * Math.cos(alpha))) / Math.abs(Math.sin(alpha));
                    // min of whichever is above 0 (!!! no min of abs values)
                    c = cw < 0 ? ch : ch > 0 ? Math.min(cw, ch) : cw;
                    // make it one pix smaller to ensure text is not cut on the left
                    c--;
                    if (c > w) {
                        v.setSize((float) c, 10 * h);
                        ready = true;
                    } else {
                        v.setSize((float) c, 10 * h);
                        if (v.getPreferredSpan(View.Y_AXIS) > h) {
                            // set size back to figure out new line break and height after
                            v.setSize(w, 10 * h);
                        } else {
                            w = (float) c;
                            ready = true;
                        }
                    }
                }

                tx = Math.floor(w);// xxx: watch out for first letter on each line missing some pixs!!!
                ty = h;
            }
            pWidth = (int) tx;
            pHeight = (int) ty;
            ignoreRepaint = false;
        }
		return new Point2D.Double(tx,ty);
	}

	@Override
    public void repaint() {
        if (ignoreRepaint) {
            return;
        }
        super.repaint();
    }

    @Override
    public void repaint(int x, int y, int width, int height) {
        if (ignoreRepaint) {
            return;
        }
        super.repaint(x, y, width, height);
    }

    @Override
    public void repaint(long tm) {
        if (ignoreRepaint) {
            return;
        }
        super.repaint(tm);
    }

    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
        if (ignoreRepaint) {
            return;
        }
        super.repaint(tm, x, y, width, height);
    }

    // ----------------------------------------------------------
    // textOrientation magic
    @Override
    public int getHeight() {
        int retValue = super.getHeight();
        if (painting) {
            retValue = pHeight;
        }
        return retValue;
    }

    @Override
    public int getWidth() {
        int retValue = super.getWidth();
        if (painting) {
            retValue = pWidth;
        }
        return retValue;
    }

    protected MultiLineSupport getMultiLineSupport() {
    	return new MultiLineSupport();
    }
    // ----------------------------------------------------------
    // WARNING:
    // Anything below this line is related to lineWrap support and can be safely ignored unless
    // in need to mess around with the implementation details.
    // ----------------------------------------------------------
    // FYI: This class doesn't reinvent line wrapping. Instead it makes use of existing support
    // made for JTextComponent/JEditorPane.
    // All the classes below named Alter* are verbatim copy of swing.text.* classes made to
    // overcome package visibility of some of the code. All other classes here, when their name
    // matches corresponding class from swing.text.* package are copy of the class with removed
    // support for highlighting selection. In case this is ever merged back to JDK all of this
    // can be safely removed as long as corresponding swing.text.* classes make appropriate checks
    // before casting JComponent into JTextComponent to find out selected region since
    // JLabel/JXLabel does not support selection of the text.

    public static class MultiLineSupport implements PropertyChangeListener {

        private static final String HTML = "<html>";

        private static ViewFactory basicViewFactory;

        private static BasicEditorKit basicFactory;

        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            JXLabel src = (JXLabel) evt.getSource();
            if (src.isLineWrap()) {
                if ("font".equals(name) || "foreground".equals(name) || "maxLineSpan".equals(name)) {
                    if (evt.getOldValue() != null && !isHTML(src.getText())) {
                        updateRenderer(src);
                    }
                } else if ("text".equals(name)) {
                    if (isHTML((String) evt.getOldValue()) && evt.getNewValue() != null
                            && !isHTML((String) evt.getNewValue())) {
                        // was html , but is not
                        if (src.getClientProperty(oldRendererKey) == null
                                && src.getClientProperty(BasicHTML.propertyKey) != null) {                       
                            src.putClientProperty(oldRendererKey, src.getClientProperty(BasicHTML.propertyKey));
                        }
                        src.putClientProperty(BasicHTML.propertyKey, createView(src));
                    } else if (!isHTML((String) evt.getOldValue()) && evt.getNewValue() != null
                            && !isHTML((String) evt.getNewValue())) {
                        // wasn't html and isn't
                        updateRenderer(src);
                    } else {
                        // either was html and is html or wasn't html, but is html
                        restoreHtmlRenderer(src);
                    }
                } else if ("lineWrap".equals(name) && !isHTML(src.getText())) {
                    src.putClientProperty(BasicHTML.propertyKey, createView(src));
                }
            } else if ("lineWrap".equals(name)) {
                restoreHtmlRenderer(src);
            }
        }

        private static void restoreHtmlRenderer(JXLabel src) {
            Object current = src.getClientProperty(BasicHTML.propertyKey);
            if (current == null || current instanceof Renderer) {
                src.putClientProperty(BasicHTML.propertyKey, src.getClientProperty(oldRendererKey));
            }
        }

        private static boolean isHTML(String s) {
            return s != null && s.toLowerCase().startsWith(HTML);
        }

        public static View createView(JXLabel c) {
            BasicEditorKit kit = getFactory();
            Document doc = kit.createDefaultDocument(c.getFont(), c.getForeground());
            Reader r = new StringReader(c.getText() == null ? "" : c.getText());
            try {
                kit.read(r, doc, 0);
            } catch (Throwable e) {
            }
            ViewFactory f = kit.getViewFactory();
            View hview = f.create(doc.getDefaultRootElement());
            View v = new Renderer(c, f, hview, true);
            return v;
        }

        public static void updateRenderer(JXLabel c) {
            View value = null;
            View oldValue = (View) c.getClientProperty(BasicHTML.propertyKey);
            if (oldValue == null || oldValue instanceof Renderer) {
                value = createView(c);
            }
            if (value != oldValue && oldValue != null) {
                for (int i = 0; i < oldValue.getViewCount(); i++) {
                    oldValue.getView(i).setParent(null);
                }
            }
            c.putClientProperty(BasicHTML.propertyKey, value);
        }

        private static BasicEditorKit getFactory() {
            if (basicFactory == null) {
                basicViewFactory = new BasicViewFactory();
                basicFactory = new BasicEditorKit();
            }
            return basicFactory;
        }

        private static class BasicEditorKit extends StyledEditorKit {
            public Document createDefaultDocument(Font defaultFont, Color foreground) {
                BasicDocument doc = new BasicDocument(defaultFont, foreground);
                doc.setAsynchronousLoadPriority(Integer.MAX_VALUE);
                return doc;
            }

            public ViewFactory getViewFactory() {
                return basicViewFactory;
            }
        }
    }

    private static class BasicViewFactory implements ViewFactory {
        public View create(Element elem) {

            String kind = elem.getName();
            View view = null;
            if (kind == null) {
                // default to text display
                view = new LabelView(elem);
            } else if (kind.equals(AbstractDocument.ContentElementName)) {
                view = new LabelView(elem);
            } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                view = new ParagraphView(elem);
            } else if (kind.equals(AbstractDocument.SectionElementName)) {
                view = new BoxView(elem, View.Y_AXIS);
            } else if (kind.equals(StyleConstants.ComponentElementName)) {
                view = new ComponentView(elem);
            } else if (kind.equals(StyleConstants.IconElementName)) {
                view = new IconView(elem);
            }
            return view;
        }
    }

    static class BasicDocument extends DefaultStyledDocument {
        BasicDocument(Font defaultFont, Color foreground) {
            setFontAndColor(defaultFont, foreground);
        }

        private void setFontAndColor(Font font, Color fg) {
            if (fg != null) {

                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, fg);
                getStyle("default").addAttributes(attr);
            }

            if (font != null) {
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setFontFamily(attr, font.getFamily());
                getStyle("default").addAttributes(attr);

                attr = new SimpleAttributeSet();
                StyleConstants.setFontSize(attr, font.getSize());
                getStyle("default").addAttributes(attr);

                attr = new SimpleAttributeSet();
                StyleConstants.setBold(attr, font.isBold());
                getStyle("default").addAttributes(attr);

                attr = new SimpleAttributeSet();
                StyleConstants.setItalic(attr, font.isItalic());
                getStyle("default").addAttributes(attr);
            }

            MutableAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setSpaceAbove(attr, 0f);
            getStyle("default").addAttributes(attr);

            // TODO: add rest of the style stuff
            // ... if anyone ever want's this (stuff like justification, etc.)
            // attr = new SimpleAttributeSet();
            // StyleConstants.setLeftIndent(attr,5f);
            // getStyle("default").addAttributes(attr);

            // MutableAttributeSet attr = new SimpleAttributeSet();
            // StyleConstants.setAlignment(attr, StyleConstants.ALIGN_JUSTIFIED);
            // getStyle("default").addAttributes(attr);

        }
    }

    /**
     * Root text view that acts as an renderer.
     */
    static class Renderer extends WrappedPlainView {

        JXLabel host;

        private int width;

        Renderer(JXLabel c, ViewFactory f, View v, boolean wordWrap) {
            super(null, wordWrap);
            factory = f;
            view = v;
            view.setParent(this);
            host = c;
            // initially layout to the preferred size
            setSize(c.getMaxLineSpan() > -1 ? c.getMaxLineSpan() : view.getPreferredSpan(X_AXIS), view.getPreferredSpan(Y_AXIS));
        }

        public void preferenceChanged(View child, boolean width, boolean height) {
            if (host != null) {
                host.revalidate();
                host.repaint();
            }
        }


        /**
         * Fetches the attributes to use when rendering. At the root level there are no attributes. If an attribute is
         * resolved up the view hierarchy this is the end of the line.
         */
        public AttributeSet getAttributes() {
            return null;
        }

        /**
         * Renders the view.
         * 
         * @param g the graphics context
         * @param allocation the region to render into
         */
        public void paint(Graphics g, Shape allocation) {
            Rectangle alloc = allocation.getBounds();
            view.setSize(alloc.width, alloc.height);
            if (g.getClipBounds() == null) {
            	g.setClip(alloc);
                view.paint(g, allocation);
                g.setClip(null);
            } else {
                //g.translate(alloc.x, alloc.y);
                view.paint(g, allocation);
                //g.translate(-alloc.x, -alloc.y);
            }
        }

        /**
         * Sets the view parent.
         * 
         * @param parent the parent view
         */
        public void setParent(View parent) {
            throw new Error("Can't set parent on root view");
        }

        /**
         * Returns the number of views in this view. Since this view simply wraps the root of the view hierarchy it has
         * exactly one child.
         * 
         * @return the number of views
         * @see #getView
         */
        public int getViewCount() {
            return 1;
        }

        /**
         * Gets the n-th view in this container.
         * 
         * @param n the number of the view to get
         * @return the view
         */
        public View getView(int n) {
            return view;
        }

        /**
         * Returns the document model underlying the view.
         * 
         * @return the model
         */
        public Document getDocument() {
            return view == null ? null : view.getDocument();
        }

        /**
         * Sets the view size.
         * 
         * @param width the width
         * @param height the height
         */
        public void setSize(float width, float height) {
            if (width == this.width) {
                return;
            }
            this.width = (int) width;
            view.setSize(width, height);
        }
        
        @Override
        public float getPreferredSpan(int axis) {
            if (axis == X_AXIS && width > 0) {
                // width currently laid out to
                return width;
                }
                return view.getPreferredSpan(axis);
        }

        /**
         * Fetches the container hosting the view. This is useful for things like scheduling a repaint, finding out the
         * host components font, etc. The default implementation of this is to forward the query to the parent view.
         * 
         * @return the container
         */
        public Container getContainer() {
            return host;
        }

        /**
         * Fetches the factory to be used for building the various view fragments that make up the view that represents
         * the model. This is what determines how the model will be represented. This is implemented to fetch the
         * factory provided by the associated EditorKit.
         * 
         * @return the factory
         */
        public ViewFactory getViewFactory() {
            return factory;
        }

        private View view;

        private ViewFactory factory;

    }

    /**
     * <code>AnotherAbstractDocument</code>
     * 
     * @inheritDoc
     */
    private static abstract class AlterAbstractDocument extends AbstractDocument implements Document, Serializable {

        /**
         * Document property that indicates whether internationalization functions such as text reordering or reshaping
         * should be performed. This property should not be publicly exposed, since it is used for implementation
         * convenience only. As a side effect, copies of this property may be in its subclasses that live in different
         * packages (e.g. HTMLDocument as of now), so those copies should also be taken care of when this property needs
         * to be modified.
         */
        static final String I18NProperty = "i18n";

        public AlterAbstractDocument(Content data, AttributeContext context) {
            super(data, context);
        }

        public AlterAbstractDocument(Content data) {
            super(data);
        }

    }

    /**
     * Internally created view that has the purpose of holding the views that represent the children of the paragraph
     * that have been arranged in rows.
     */
    class Row extends AlterBoxView {

        private int justification;

        private float lineSpacing;

        /** Indentation for the first line, from the left inset. */
        protected int firstLineIndent = 0;

        Row(Element elem) {
            super(elem, View.X_AXIS);
        }

        /**
         * This is reimplemented to do nothing since the paragraph fills in the row with its needed children.
         */
        protected void loadChildren(ViewFactory f) {
        }

        /**
         * Fetches the attributes to use when rendering. This view isn't directly responsible for an element so it
         * returns the outer classes attributes.
         */
        public AttributeSet getAttributes() {
            View p = getParent();
            return (p != null) ? p.getAttributes() : null;
        }

        public float getAlignment(int axis) {
            if (axis == View.X_AXIS) {
                switch (justification) {
                case StyleConstants.ALIGN_LEFT:
                    return 0;
                case StyleConstants.ALIGN_RIGHT:
                    return 1;
                case StyleConstants.ALIGN_CENTER:
                    return 0.5f;
                case StyleConstants.ALIGN_JUSTIFIED:
                    float rv = 0.5f;
                    // if we can justifiy the content always align to
                    // the left.
                    if (isJustifiableDocument()) {
                        rv = 0f;
                    }
                    return rv;
                }
            }
            return super.getAlignment(axis);
        }

        /**
         * Provides a mapping from the document model coordinate space to the coordinate space of the view mapped to it.
         * This is implemented to let the superclass find the position along the major axis and the allocation of the
         * row is used along the minor axis, so that even though the children are different heights they all get the
         * same caret height.
         * 
         * @param pos the position to convert
         * @param a the allocated region to render into
         * @return the bounding box of the given position
         * @exception BadLocationException if the given position does not represent a valid location in the associated
         *            document
         * @see View#modelToView
         */
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            Rectangle r = a.getBounds();
            View v = getViewAtPosition(pos, r);
            if ((v != null) && (!v.getElement().isLeaf())) {
                // Don't adjust the height if the view represents a branch.
                return super.modelToView(pos, a, b);
            }
            r = a.getBounds();
            int height = r.height;
            int y = r.y;
            Shape loc = super.modelToView(pos, a, b);
            r = loc.getBounds();
            r.height = height;
            r.y = y;
            return r;
        }

        /**
         * Range represented by a row in the paragraph is only a subset of the total range of the paragraph element.
         * 
         * @see View#getRange
         */
        public int getStartOffset() {
            int offs = Integer.MAX_VALUE;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                offs = Math.min(offs, v.getStartOffset());
            }
            return offs;
        }

        public int getEndOffset() {
            int offs = 0;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                offs = Math.max(offs, v.getEndOffset());
            }
            return offs;
        }

        /**
         * Perform layout for the minor axis of the box (i.e. the axis orthoginal to the axis that it represents). The
         * results of the layout should be placed in the given arrays which represent the allocations to the children
         * along the minor axis.
         * <p>
         * This is implemented to do a baseline layout of the children by calling BoxView.baselineLayout.
         * 
         * @param targetSpan the total span given to the view, which whould be used to layout the children.
         * @param axis the axis being layed out.
         * @param offsets the offsets from the origin of the view for each of the child views. This is a return value
         *        and is filled in by the implementation of this method.
         * @param spans the span of each child view. This is a return value and is filled in by the implementation of
         *        this method.
         * @return the offset and span for each child view in the offsets and spans parameters
         */
        protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            baselineLayout(targetSpan, axis, offsets, spans);
        }

        protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
            return baselineRequirements(axis, r);
        }

        private boolean isLastRow() {
            View parent;
            return ((parent = getParent()) == null || this == parent.getView(parent.getViewCount() - 1));
        }

        private boolean isBrokenRow() {
            boolean rv = false;
            int viewsCount = getViewCount();
            if (viewsCount > 0) {
                View lastView = getView(viewsCount - 1);
                if (lastView.getBreakWeight(X_AXIS, 0, 0) >= ForcedBreakWeight) {
                    rv = true;
                }
            }
            return rv;
        }

        private boolean isJustifiableDocument() {
            return (!Boolean.TRUE.equals(getDocument().getProperty(AlterAbstractDocument.I18NProperty)));
        }

        /**
         * Whether we need to justify this {@code Row}. At this time (jdk1.6) we support justification on for non 18n
         * text.
         * 
         * @return {@code true} if this {@code Row} should be justified.
         */
        private boolean isJustifyEnabled() {
            boolean ret = (justification == StyleConstants.ALIGN_JUSTIFIED);

            // no justification for i18n documents
            ret = ret && isJustifiableDocument();

            // no justification for the last row
            ret = ret && !isLastRow();

            // no justification for the broken rows
            ret = ret && !isBrokenRow();

            return ret;
        }

        // Calls super method after setting spaceAddon to 0.
        // Justification should not affect MajorAxisRequirements
        @Override
        protected SizeRequirements calculateMajorAxisRequirements(int axis, SizeRequirements r) {
            int oldJustficationData[] = justificationData;
            justificationData = null;
            SizeRequirements ret = super.calculateMajorAxisRequirements(axis, r);
            if (isJustifyEnabled()) {
                justificationData = oldJustficationData;
            }
            return ret;
        }

        @Override
        protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            int oldJustficationData[] = justificationData;
            justificationData = null;
            super.layoutMajorAxis(targetSpan, axis, offsets, spans);
            if (!isJustifyEnabled()) {
                return;
            }

            int currentSpan = 0;
            for (int span : spans) {
                currentSpan += span;
            }
            if (currentSpan == targetSpan) {
                // no need to justify
                return;
            }

            // we justify text by enlarging spaces by the {@code spaceAddon}.
            // justification is started to the right of the rightmost TAB.
            // leading and trailing spaces are not extendable.
            //
            // GlyphPainter1 uses
            // justificationData
            // for all painting and measurement.

            int extendableSpaces = 0;
            int startJustifiableContent = -1;
            int endJustifiableContent = -1;
            int lastLeadingSpaces = 0;

            int rowStartOffset = getStartOffset();
            int rowEndOffset = getEndOffset();
            int spaceMap[] = new int[rowEndOffset - rowStartOffset];
            Arrays.fill(spaceMap, 0);
            for (int i = getViewCount() - 1; i >= 0; i--) {
                View view = getView(i);
                if (view instanceof GlyphView) {
                    AlterGlyphView.JustificationInfo justificationInfo = ((AlterGlyphView) view)
                            .getJustificationInfo(rowStartOffset);
                    final int viewStartOffset = view.getStartOffset();
                    final int offset = viewStartOffset - rowStartOffset;
                    for (int j = 0; j < justificationInfo.spaceMap.length(); j++) {
                        if (justificationInfo.spaceMap.get(j)) {
                            spaceMap[j + offset] = 1;
                        }
                    }
                    if (startJustifiableContent > 0) {
                        if (justificationInfo.end >= 0) {
                            extendableSpaces += justificationInfo.trailingSpaces;
                        } else {
                            lastLeadingSpaces += justificationInfo.trailingSpaces;
                        }
                    }
                    if (justificationInfo.start >= 0) {
                        startJustifiableContent = justificationInfo.start + viewStartOffset;
                        extendableSpaces += lastLeadingSpaces;
                    }
                    if (justificationInfo.end >= 0 && endJustifiableContent < 0) {
                        endJustifiableContent = justificationInfo.end + viewStartOffset;
                    }
                    extendableSpaces += justificationInfo.contentSpaces;
                    lastLeadingSpaces = justificationInfo.leadingSpaces;
                    if (justificationInfo.hasTab) {
                        break;
                    }
                }
            }
            if (extendableSpaces <= 0) {
                // there is nothing we can do to justify
                return;
            }
            int adjustment = (targetSpan - currentSpan);
            int spaceAddon = (extendableSpaces > 0) ? adjustment / extendableSpaces : 0;
            int spaceAddonLeftoverEnd = -1;
            for (int i = startJustifiableContent - rowStartOffset, leftover = adjustment - spaceAddon
                    * extendableSpaces; leftover > 0; leftover -= spaceMap[i], i++) {
                spaceAddonLeftoverEnd = i;
            }
            if (spaceAddon > 0 || spaceAddonLeftoverEnd >= 0) {
                justificationData = (oldJustficationData != null) ? oldJustficationData : new int[END_JUSTIFIABLE + 1];
                justificationData[SPACE_ADDON] = spaceAddon;
                justificationData[SPACE_ADDON_LEFTOVER_END] = spaceAddonLeftoverEnd;
                justificationData[START_JUSTIFIABLE] = startJustifiableContent - rowStartOffset;
                justificationData[END_JUSTIFIABLE] = endJustifiableContent - rowStartOffset;
                super.layoutMajorAxis(targetSpan, axis, offsets, spans);
            }
        }

        // for justified row we assume the maximum horizontal span
        // is MAX_VALUE.
        @Override
        public float getMaximumSpan(int axis) {
            float ret;
            if (View.X_AXIS == axis && isJustifyEnabled()) {
                ret = Float.MAX_VALUE;
            } else {
                ret = super.getMaximumSpan(axis);
            }
            return ret;
        }

        /**
         * Fetches the child view index representing the given position in the model.
         * 
         * @param pos the position >= 0
         * @return index of the view representing the given position, or -1 if no view represents that position
         */
        protected int getViewIndexAtPosition(int pos) {
            // This is expensive, but are views are not necessarily layed
            // out in model order.
            if (pos < getStartOffset() || pos >= getEndOffset())
                return -1;
            for (int counter = getViewCount() - 1; counter >= 0; counter--) {
                View v = getView(counter);
                if (pos >= v.getStartOffset() && pos < v.getEndOffset()) {
                    return counter;
                }
            }
            return -1;
        }

        /**
         * Gets the left inset.
         * 
         * @return the inset
         */
        protected short getLeftInset() {
            View parentView;
            int adjustment = 0;
            if ((parentView = getParent()) != null) { // use firstLineIdent for the first row
                if (this == parentView.getView(0)) {
                    adjustment = firstLineIndent;
                }
            }
            return (short) (super.getLeftInset() + adjustment);
        }

        protected short getBottomInset() {
            return (short) (super.getBottomInset() + ((minorRequest != null) ? minorRequest.preferred : 0)
                    * lineSpacing);
        }

        final static int SPACE_ADDON = 0;

        final static int SPACE_ADDON_LEFTOVER_END = 1;

        final static int START_JUSTIFIABLE = 2;

        // this should be the last index in justificationData
        final static int END_JUSTIFIABLE = 3;

        int justificationData[] = null;
    }

    /**
     * <code>AnotherGlyphView</code>
     * 
     * @inheritDoc
     */
    static class AlterGlyphView extends GlyphView implements TabableView, Cloneable {

        /**
         * Constructs a new view wrapped on an element.
         * 
         * @param elem the element
         */
        public AlterGlyphView(Element elem) {
            super(elem);
        }

        /**
         * Class to hold data needed to justify this GlyphView in a PargraphView.Row
         */
        static class JustificationInfo {
            // justifiable content start
            final int start;

            // justifiable content end
            final int end;

            final int leadingSpaces;

            final int contentSpaces;

            final int trailingSpaces;

            final boolean hasTab;

            final BitSet spaceMap;

            JustificationInfo(int start, int end, int leadingSpaces, int contentSpaces, int trailingSpaces,
                    boolean hasTab, BitSet spaceMap) {
                this.start = start;
                this.end = end;
                this.leadingSpaces = leadingSpaces;
                this.contentSpaces = contentSpaces;
                this.trailingSpaces = trailingSpaces;
                this.hasTab = hasTab;
                this.spaceMap = spaceMap;
            }
        }

        JustificationInfo getJustificationInfo(int rowStartOffset) {
            if (justificationInfo != null) {
                return justificationInfo;
            }
            // states for the parsing
            final int TRAILING = 0;
            final int CONTENT = 1;
            final int SPACES = 2;
            int startOffset = getStartOffset();
            int endOffset = getEndOffset();
            Segment segment = getText(startOffset, endOffset);
            int txtOffset = segment.offset;
            int txtEnd = segment.offset + segment.count - 1;
            int startContentPosition = txtEnd + 1;
            int endContentPosition = txtOffset - 1;
            int trailingSpaces = 0;
            int contentSpaces = 0;
            int leadingSpaces = 0;
            boolean hasTab = false;
            BitSet spaceMap = new BitSet(endOffset - startOffset + 1);

            // we parse conent to the right of the rightmost TAB only.
            // we are looking for the trailing and leading spaces.
            // position after the leading spaces (startContentPosition)
            // position before the trailing spaces (endContentPosition)
            for (int i = txtEnd, state = TRAILING; i >= txtOffset; i--) {
                if (' ' == segment.array[i]) {
                    spaceMap.set(i - txtOffset);
                    if (state == TRAILING) {
                        trailingSpaces++;
                    } else if (state == CONTENT) {
                        state = SPACES;
                        leadingSpaces = 1;
                    } else if (state == SPACES) {
                        leadingSpaces++;
                    }
                } else if ('\t' == segment.array[i]) {
                    hasTab = true;
                    break;
                } else {
                    if (state == TRAILING) {
                        if ('\n' != segment.array[i] && '\r' != segment.array[i]) {
                            state = CONTENT;
                            endContentPosition = i;
                        }
                    } else if (state == CONTENT) {
                        // do nothing
                    } else if (state == SPACES) {
                        contentSpaces += leadingSpaces;
                        leadingSpaces = 0;
                    }
                    startContentPosition = i;
                }
            }

            SegmentCache.releaseSharedSegment(segment);

            int startJustifiableContent = -1;
            if (startContentPosition < txtEnd) {
                startJustifiableContent = startContentPosition - txtOffset;
            }
            int endJustifiableContent = -1;
            if (endContentPosition > txtOffset) {
                endJustifiableContent = endContentPosition - txtOffset;
            }
            justificationInfo = new JustificationInfo(startJustifiableContent, endJustifiableContent, leadingSpaces,
                    contentSpaces, trailingSpaces, hasTab, spaceMap);
            return justificationInfo;
        }

        private JustificationInfo justificationInfo = null;
    }

    /**
     * SegmentCache caches <code>Segment</code>s to avoid continually creating and destroying of <code>Segment</code>s.
     * A common use of this class would be:
     * 
     * <pre>
     *   Segment segment = segmentCache.getSegment();
     *   // do something with segment
     *   ...
     *   segmentCache.releaseSegment(segment);
     * </pre>
     * 
     * @version 1.6 11/17/05
     */
    static class SegmentCache {
        /**
         * A global cache.
         */
        private static SegmentCache sharedCache = new SegmentCache();

        /**
         * A list of the currently unused Segments.
         */
        private List<Segment> segments;

        /**
         * Returns the shared SegmentCache.
         */
        public static SegmentCache getSharedInstance() {
            return sharedCache;
        }

        /**
         * A convenience method to get a Segment from the shared <code>SegmentCache</code>.
         */
        public static Segment getSharedSegment() {
            return getSharedInstance().getSegment();
        }

        /**
         * A convenience method to release a Segment to the shared <code>SegmentCache</code>.
         */
        public static void releaseSharedSegment(Segment segment) {
            getSharedInstance().releaseSegment(segment);
        }

        /**
         * Creates and returns a SegmentCache.
         */
        public SegmentCache() {
            segments = new ArrayList<Segment>(11);
        }

        /**
         * Returns a <code>Segment</code>. When done, the <code>Segment</code> should be recycled by invoking
         * <code>releaseSegment</code>.
         */
        public Segment getSegment() {
            synchronized (this) {
                int size = segments.size();

                if (size > 0) {
                    return segments.remove(size - 1);
                }
            }
            return new CachedSegment();
        }

        /**
         * Releases a Segment. You should not use a Segment after you release it, and you should NEVER release the same
         * Segment more than once, eg:
         * 
         * <pre>
         * segmentCache.releaseSegment(segment);
         * segmentCache.releaseSegment(segment);
         * </pre>
         * 
         * Will likely result in very bad things happening!
         */
        public void releaseSegment(Segment segment) {
            if (segment instanceof CachedSegment) {
                synchronized (this) {
                    segment.array = null;
                    segment.count = 0;
                    segments.add(segment);
                }
            }
        }

        /**
         * CachedSegment is used as a tagging interface to determine if a Segment can successfully be shared.
         */
        private static class CachedSegment extends Segment {
        }
    }

    /**
     * <code>AnotherBoxView</code>
     * 
     * @inheritDoc
     */
    static class AlterBoxView extends BoxView {

        /** used in paint. */
        Rectangle tempRect;

        boolean majorAllocValid;

        SizeRequirements minorRequest;

        public AlterBoxView(Element elem, int axis) {
            super(elem, axis);
        }

        @Override
        protected short getRightInset() {
            return super.getRightInset();
        }

        // TODO: override following 2 methods and Renderer.paint() to make this a ShapeView rather then BoxView ;)
        @Override
        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            if (!isAllocationValid()) {
                Rectangle alloc = a.getBounds();
                setSize(alloc.width, alloc.height);
            }
            return super.viewToModel(x, y, a, bias);
        }

    }

    /**
     * This exception is to report the failure of state invarient assertion that was made. This indicates an internal
     * error has occurred.
     * 
     * @author Timothy Prinzing
     * @version 1.18 11/17/05
     */
    static class StateInvariantError extends Error {
        /**
         * Creates a new StateInvariantFailure object.
         * 
         * @param s a string indicating the assertion that failed
         */
        public StateInvariantError(String s) {
            super(s);
        }

    }

}
