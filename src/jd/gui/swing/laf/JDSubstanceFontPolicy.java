package jd.gui.swing.laf;

import javax.swing.UIDefaults;

import org.pushingpixels.substance.api.fonts.FontPolicy;
import org.pushingpixels.substance.api.fonts.FontSet;

public class JDSubstanceFontPolicy implements FontPolicy {

    private final FontSet            defaultFontSet;
    private final JDSubstanceFontSet fontSet;

    public JDSubstanceFontPolicy(final FontSet fontSet, final String fontName, final int fontSize) {
        this.defaultFontSet = fontSet;
        this.fontSet = new JDSubstanceFontSet(this.defaultFontSet, fontName, fontSize);
    }

    public FontSet getFontSet(final String arg0, final UIDefaults arg1) {
        return this.fontSet;
    }

}
