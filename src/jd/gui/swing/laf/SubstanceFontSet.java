package jd.gui.swing.laf;

import javax.swing.UIDefaults;
import javax.swing.plaf.FontUIResource;

import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.fonts.FontPolicy;
import org.jvnet.substance.fonts.FontSet;

public class SubstanceFontSet {
    /**
     * Wrapper around the base Substance font set. Is used to create larger /
     * smaller font sets.
     * 
     * @author Kirill Grouchnikov
     */
    private static class WrapperFontSet implements FontSet {

        /**
         * The base Substance font set.
         */
        private FontSet delegate;

        private String font;

        private int zoom;

        /**
         * Creates a wrapper font set.
         * 
         * @param delegate
         *            The base Substance font set.
         * @param extra
         *            Extra size in pixels. Can be positive or negative.
         */
        public WrapperFontSet(FontSet delegate, String fontname, int fontzoom) {
            super();
            this.delegate = delegate;
            this.font = fontname;
            this.zoom = fontzoom;
        }

        /**
         * Returns the wrapped font.
         * 
         * @param systemFont
         *            Original font.
         * @return Wrapped font.
         */
        private FontUIResource getWrappedFont(FontUIResource systemFont) {
            return new FontUIResource(font, systemFont.getStyle(), (systemFont.getSize() * zoom) / 100);
        }

        public FontUIResource getControlFont() {
            return this.getWrappedFont(this.delegate.getControlFont());
        }

        public FontUIResource getMenuFont() {
            return this.getWrappedFont(this.delegate.getMenuFont());
        }

        public FontUIResource getMessageFont() {
            return this.getWrappedFont(this.delegate.getMessageFont());
        }

        public FontUIResource getSmallFont() {
            return this.getWrappedFont(this.delegate.getSmallFont());
        }

        public FontUIResource getTitleFont() {
            return this.getWrappedFont(this.delegate.getTitleFont());
        }

        public FontUIResource getWindowTitleFont() {
            return this.getWrappedFont(this.delegate.getWindowTitleFont());
        }
    }

    public static void postSetup() {
        final String font = GUIUtils.getConfig().getStringProperty(JDGuiConstants.PARAM_GENERAL_FONT_NAME, "Dialog");
        final int fontzoom = GUIUtils.getConfig().getIntegerProperty(JDGuiConstants.PARAM_GENERAL_FONT_SIZE, 100);
        SubstanceLookAndFeel.setFontPolicy(null);
        // Get the default font set
        final FontSet substanceCoreFontSet = SubstanceLookAndFeel.getFontPolicy().getFontSet("Substance", null);
        // Create the wrapper font set
        FontPolicy newFontPolicy = new FontPolicy() {
            public FontSet getFontSet(String lafName, UIDefaults table) {
                return new WrapperFontSet(substanceCoreFontSet, font, fontzoom);
            }
        };
        try {
            // set the new font policy
            SubstanceLookAndFeel.setFontPolicy(newFontPolicy);
        } catch (Throwable exc) {
            JDLogger.exception(exc);
        }
    }
}
