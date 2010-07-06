package jd.http.ext;

import java.util.ArrayList;

import org.lobobrowser.html.domimpl.HTMLElementImpl;

public class CssUtilities {

    public static boolean isVisible(HTMLElementImpl impl) {

        ArrayList<HTMLElementImpl> styles = getPath(impl);
        for (HTMLElementImpl p : styles) {
            String v = p.getComputedStyle(null).getDisplay();
            v = v;
            if ("none".equalsIgnoreCase(v)) {
                //
                return false;
            }
        }
        return true;
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
