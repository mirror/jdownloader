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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.beans.PropertyEditorSupport;
import java.util.HashMap;
import java.util.Map;
//import org.jdesktop.swingx.painter.gradient.BasicGradientPainter;
//import org.jdesktop.swingx.painter.gradient.LinearGradientPainter;

/**
 * Two parts to this property editor. The first part is a simple dropdown.
 * The second part is a complicated UI for constructing multiple "layers" of
 * various different Painters, including gradient paints.
 *
 * @author Richard
 */
public class PaintPropertyEditor extends PropertyEditorSupport {
    private static Map<Paint, String> DEFAULT_PAINTS = new HashMap<Paint, String>();
    static {
        //add the default paints
        DEFAULT_PAINTS.put(Color.BLACK, "Black");
        DEFAULT_PAINTS.put(Color.BLUE, "Blue");
        DEFAULT_PAINTS.put(Color.CYAN, "Cyan");
        DEFAULT_PAINTS.put(Color.DARK_GRAY, "Dark Gray");
        DEFAULT_PAINTS.put(Color.GRAY, "Gray");
        DEFAULT_PAINTS.put(Color.GREEN, "Green");
        DEFAULT_PAINTS.put(Color.LIGHT_GRAY, "Light Gray");
        DEFAULT_PAINTS.put(Color.MAGENTA, "Magenta");
        DEFAULT_PAINTS.put(Color.ORANGE, "Orange");
        DEFAULT_PAINTS.put(Color.PINK, "Pink");
        DEFAULT_PAINTS.put(Color.RED, "Red");
        DEFAULT_PAINTS.put(Color.WHITE, "White");
        DEFAULT_PAINTS.put(Color.YELLOW, "Yellow");
        DEFAULT_PAINTS.put(new Color(1f, 1f, 1f, 0f), "Transparent");
//        DEFAULT_PAINTS.put(
//            BasicGradientPainter.WHITE_TO_CONTROL_HORZONTAL, "White->Control (horizontal)");
//        DEFAULT_PAINTS.put(
//            BasicGradientPainter.WHITE_TO_CONTROL_VERTICAL, "White->Control (vertical)");
        /* josh: this should be replaced with matte painters
        DEFAULT_PAINTS.put(
            BasicGradientPainter.AERITH, "Aerith");
        DEFAULT_PAINTS.put(
            BasicGradientPainter.BLUE_EXPERIENCE, "Blue Experience");
        DEFAULT_PAINTS.put(
            BasicGradientPainter.GRAY, "Gray Gradient");
        DEFAULT_PAINTS.put(
            BasicGradientPainter.MAC_OSX, "Mac OSX");
        DEFAULT_PAINTS.put(
            BasicGradientPainter.MAC_OSX_SELECTED, "Max OSX Selected");
        DEFAULT_PAINTS.put(
            BasicGradientPainter.NIGHT_GRAY, "Night Gray");
        DEFAULT_PAINTS.put(
            BasicGradientPainter.NIGHT_GRAY_LIGHT, "Night Gray Light");
        DEFAULT_PAINTS.put(
            BasicGradientPainter.RED_XP, "Red XP");
        DEFAULT_PAINTS.put(
            LinearGradientPainter.BLACK_STAR, "Black Star");
        DEFAULT_PAINTS.put(
            LinearGradientPainter.ORANGE_DELIGHT, "Orange Delight");*/
    }
    
    /** Creates a new instance of PainterPropertyEditor */
    public PaintPropertyEditor() {
    }
    
    public String[] getTags() {
        String[] names = DEFAULT_PAINTS.values().toArray(new String[0]);
        String[] results = new String[names.length+1];
        results[0] = "<none>";
        System.arraycopy(names, 0, results, 1, names.length);
        return results;
    }
    
    public Paint getValue() {
        return (Paint)super.getValue();
    }

    public String getJavaInitializationString() {
        Paint paint = getValue();
        //TODO!!!
        return paint == null ? "null" : 
            "org.jdesktop.swingx.painter.gradient.LinearGradientPainter.BLACK_STAR";
    }

    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().equals("") || text.trim().equalsIgnoreCase("none")
                || text.trim().equalsIgnoreCase("<none>")
                || text.trim().equalsIgnoreCase("[none]")) {
            setValue(null);
            return;
        }
        
        if (text.trim().equalsIgnoreCase("<custom>")) {
            //do nothing
        }
        
        for (Map.Entry<Paint, String> entry : DEFAULT_PAINTS.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(text)) {
                setValue(entry.getKey());
                return;
            }
        }
        
        throw new IllegalArgumentException("The input value " + text + " does" +
                " not match one of the names of the standard paints");
    }

    public String getAsText() {
        Paint p = getValue();
        if (p == null) {
            return null;
        } else if (DEFAULT_PAINTS.containsKey(p)) {
            return DEFAULT_PAINTS.get(p);
        } else {
            return "<custom>";
        }
    }

    public void paintValue(Graphics gfx, Rectangle box) {
        Paint p = getValue();
        if (p == null) {
            //do nothing -- in the future draw the checkerboard or something
        } else {
            ((Graphics2D)gfx).setPaint(p);
            gfx.fillRect(box.x, box.y, box.width, box.height);
        }
    }

    public boolean isPaintable() {
        return true;
    }
}
