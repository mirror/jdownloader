package jd.http.ext;

import java.util.ArrayList;

import jd.parser.Regex;

import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.style.AbstractCSS2Properties;

public class RendererUtilities {

    /**
     * This method has to evaluate if the element impl is visible on screen.
     * 
     * @param impl
     * @return
     */
    public static boolean isVisible(HTMLElementImpl impl) {

        ArrayList<HTMLElementImpl> styles = getPath(impl);
        int x = 0;
        int y = 0;
        for (HTMLElementImpl p : styles) {
            AbstractCSS2Properties style = p.getComputedStyle(null);

            if ("none".equalsIgnoreCase(style.getDisplay())) {
                //
                return false;
            }
            if ("absolute".equalsIgnoreCase(style.getPosition())) {
                x = y = 0;
            }
            if (style.getTop() != null) {
                y += covertToPixel(style.getTop());
            }
            if (style.getLeft() != null) {
                x += covertToPixel(style.getLeft());
            }

        }
        if (y < 0) return false;
        return true;
    }

    private static int covertToPixel(String top) {
        if (top == null) return 0;
        if (top.toLowerCase().trim().endsWith("px")) { return Integer.parseInt(top.substring(0, top.length() - 2)); }
        String value = new Regex(top, "([\\-\\+]?\\s*\\d+)").getMatch(0);
        if (value == null) return 0;
        return Integer.parseInt(value);
    }

    private static ArrayList<HTMLElementImpl> getPath(HTMLElementImpl impl) {
        ArrayList<HTMLElementImpl> styles = new ArrayList<HTMLElementImpl>();

        HTMLElementImpl p = impl;
        while (p != null) {
            styles.add(0, p);
            p = p.getParent("*");
        }
        return styles;
    }

}
