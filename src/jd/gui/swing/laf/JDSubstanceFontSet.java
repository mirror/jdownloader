package jd.gui.swing.laf;

import javax.swing.plaf.FontUIResource;

import org.pushingpixels.substance.api.fonts.FontSet;

public class JDSubstanceFontSet implements FontSet {
    private final FontSet        defaultFontSet;
    private final FontUIResource controlFont;
    private final FontUIResource menuFont;
    private final FontUIResource messageFont;
    private final FontUIResource smallFont;
    private final FontUIResource titleFont;
    private final FontUIResource windowTitleFont;

    public JDSubstanceFontSet(final FontSet defaultFontSet2, String fontName, int fontSize) {
        this.defaultFontSet = defaultFontSet2;
        if ("default".equalsIgnoreCase(fontName)) {
            this.controlFont = new FontUIResource(this.defaultFontSet.getControlFont().getFontName(), this.defaultFontSet.getControlFont().getStyle(), (this.defaultFontSet.getControlFont().getSize() * fontSize) / 100);
            this.menuFont = new FontUIResource(this.defaultFontSet.getMenuFont().getFontName(), this.defaultFontSet.getMenuFont().getStyle(), (this.defaultFontSet.getMenuFont().getSize() * fontSize) / 100);
            this.messageFont = new FontUIResource(this.defaultFontSet.getMessageFont().getFontName(), this.defaultFontSet.getMessageFont().getStyle(), (this.defaultFontSet.getMessageFont().getSize() * fontSize) / 100);
            this.smallFont = new FontUIResource(this.defaultFontSet.getSmallFont().getFontName(), this.defaultFontSet.getSmallFont().getStyle(), (this.defaultFontSet.getSmallFont().getSize() * fontSize) / 100);
            this.titleFont = new FontUIResource(this.defaultFontSet.getTitleFont().getFontName(), this.defaultFontSet.getTitleFont().getStyle(), (this.defaultFontSet.getTitleFont().getSize() * fontSize) / 100);
            this.windowTitleFont = new FontUIResource(this.defaultFontSet.getWindowTitleFont().getFontName(), this.defaultFontSet.getWindowTitleFont().getStyle(), (this.defaultFontSet.getWindowTitleFont().getSize() * fontSize) / 100);
        } else {
            this.controlFont = new FontUIResource(fontName, this.defaultFontSet.getControlFont().getStyle(), (this.defaultFontSet.getControlFont().getSize() * fontSize) / 100);
            this.menuFont = new FontUIResource(fontName, this.defaultFontSet.getMenuFont().getStyle(), (this.defaultFontSet.getMenuFont().getSize() * fontSize) / 100);
            this.messageFont = new FontUIResource(fontName, this.defaultFontSet.getMessageFont().getStyle(), (this.defaultFontSet.getMessageFont().getSize() * fontSize) / 100);
            this.smallFont = new FontUIResource(fontName, this.defaultFontSet.getSmallFont().getStyle(), (this.defaultFontSet.getSmallFont().getSize() * fontSize) / 100);
            this.titleFont = new FontUIResource(fontName, this.defaultFontSet.getTitleFont().getStyle(), (this.defaultFontSet.getTitleFont().getSize() * fontSize) / 100);
            this.windowTitleFont = new FontUIResource(fontName, this.defaultFontSet.getWindowTitleFont().getStyle(), (this.defaultFontSet.getWindowTitleFont().getSize() * fontSize) / 100);
        }
    }

    public FontUIResource getControlFont() {
        return this.controlFont;
    }

    public FontUIResource getMenuFont() {
        return this.menuFont;
    }

    public FontUIResource getMessageFont() {
        return this.messageFont;
    }

    public FontUIResource getSmallFont() {
        return this.smallFont;
    }

    public FontUIResource getTitleFont() {
        return this.titleFont;
    }

    public FontUIResource getWindowTitleFont() {
        return this.windowTitleFont;
    }

}
