package jd.nutils;

import java.awt.Color;

public class Colors {
/**
 * Returns the same color, but with the given alpha channel
 * @param color
 * @param alpha (0-100)
 * @return
 */
    public static Color getColor(Color color, int alpha) {
        alpha*=255;
        alpha/=100;
        
        return new Color(color.getRed(),color.getGreen(),color.getBlue(),alpha);
    }

}
