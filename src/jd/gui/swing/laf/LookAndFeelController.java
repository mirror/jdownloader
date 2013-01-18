//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//     along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.laf;

import java.awt.Font;
import java.util.Enumeration;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.LAFManagerInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class LookAndFeelController implements LAFManagerInterface {
    private static final String                DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL = "de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel";
    private static final LookAndFeelController INSTANCE                                                      = new LookAndFeelController();

    /**
     * get the only existing instance of LookAndFeelController. This is a singleton
     * 
     * @return
     */
    public static LookAndFeelController getInstance() {
        return LookAndFeelController.INSTANCE;
    }

    private LAFOptions                     lafOptions;
    private GraphicalUserInterfaceSettings config;
    private String                         laf = null;

    /**
     * Create a new instance of LookAndFeelController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private LookAndFeelController() {
        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);
    }

    /**
     * Config parameter to store the users laf selection
     */

    public static final String DEFAULT_PREFIX = "LAF_CFG";
    private static boolean     uiInitated     = false;

    /**
     * setups the correct Look and Feel
     */
    public synchronized void setUIManager() {
        if (uiInitated) return;
        uiInitated = true;
        long t = System.currentTimeMillis();
        try {
            // de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setLookAndFeel("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel");
            // if (true) return;

            laf = DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL;
            LogController.GL.info("Use Look & Feel: " + laf);
            preSetup(laf);
            if (laf.contains("Synthetica")) {
                try {
                    de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setLookAndFeel(laf);
                    de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setExtendedFileChooserEnabled(false);
                } catch (Throwable e) {
                    LogController.CL().log(e);
                }
            } else {
                /* init for all other laf */
                UIManager.setLookAndFeel(laf);
            }
            postSetup(laf);
        } catch (Throwable e) {
            LogController.CL().log(e);
        } finally {
            LogController.GL.info("LAF init: " + (System.currentTimeMillis() - t));
        }
    }

    public LAFOptions getLAFOptions() {
        if (lafOptions != null) return lafOptions;
        synchronized (this) {
            if (lafOptions != null) return lafOptions;
            String str = null;
            try {
                if (laf != null) str = NewTheme.I().getText("lafoptions/" + laf + ".json");
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }
            if (str != null) {
                lafOptions = JSonStorage.restoreFromString(str, new TypeRef<LAFOptions>() {
                }, new LAFOptions());
            } else {
                LogController.GL.info("Not LAF Options found: " + laf + ".json");
                lafOptions = new LAFOptions();
            }
        }
        return lafOptions;
    }

    /**
     * Executes laf dependend commands AFTER setting the laf
     * 
     * @param className
     */
    private void postSetup(String className) {
        String fontName = config.getFontName();
        String fontFromTranslation = _GUI._.config_fontname();
        ExtTooltip.createConfig(ExtTooltip.DEFAULT).setForegroundColor(getLAFOptions().getTooltipForegroundColor());
        if (isSynthetica()) {
            ExtPasswordField.MASK = "*******";
            try {
                String newFontName = null;
                int fontSize = de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.getFont().getSize();
                if (fontFromTranslation != null && !"default".equalsIgnoreCase(fontFromTranslation)) {
                    /* we have customized fontName in translation */
                    /* lower priority than fontName in settings */
                    newFontName = fontFromTranslation;
                }
                if (fontName != null && !"default".equalsIgnoreCase(fontName)) {
                    /* we have customized fontName in settings, it has highest priority */
                    newFontName = fontName;
                }
                if (newFontName == null) {
                    /* default Font */
                    /* nothing to change as fontSize got already set by Synthetica */
                } else {
                    /* change Font */
                    int oldStyle = de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.getFont().getStyle();
                    Font newFont = new Font(newFontName, oldStyle, fontSize);
                    de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setFont(newFont, false);
                }
                UIManager.put("ExtTable.SuggestedFontHeight", fontSize);
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }

        } else {
            try {
                int fontSize = config.getFontScaleFactor();
                boolean sizeSet = false;
                Font font = Font.getFont(fontName);
                for (Enumeration<Object> e = UIManager.getDefaults().keys(); e.hasMoreElements();) {
                    Object key = e.nextElement();
                    Object value = UIManager.get(key);
                    if (value instanceof Font) {
                        Font f = null;
                        if (font != null) {
                            f = font;
                        } else {
                            f = (Font) value;
                        }
                        if (sizeSet == false) {
                            UIManager.put("ExtTable.SuggestedFontHeight", (f.getSize() * fontSize) / 100);
                            sizeSet = true;
                        }
                        UIManager.put(key, new FontUIResource(f.getName(), f.getStyle(), (f.getSize() * fontSize) / 100));
                    }
                }
            } catch (Throwable e) {
                LogController.CL().log(e);
            }
        }
    }

    public boolean isSynthetica() {
        return true;
    }

    /**
     * Executes LAF dependend commands BEFORE initializing the LAF
     */
    private void preSetup(String className) {
        UIManager.put("Synthetica.window.decoration", false);
        UIManager.put("Synthetica.text.antialias", config.isTextAntiAliasEnabled());
        /* http://www.jyloo.com/news/?pubId=1297681728000 */
        /* we want our own FontScaling, not SystemDPI */
        UIManager.put("Synthetica.font.respectSystemDPI", config.isFontRespectsSystemDPI());
        UIManager.put("Synthetica.font.scaleFactor", config.getFontScaleFactor());
        /*
         * fixes http://svn.jdownloader.org/issues/1897 #1 When the Downloads grid or LinkGrabber grid is open and a vertical thumb (the
         * slider) is in the scrollbar, clicking on the blank part of the scrollbar is supposed to move the thumb and document by one page
         * in that direction. Currently, it moves the thumb and document enough to move the thumb under the cursor, one page at a time. That
         * is supposed to happen only if the mouse is held down longer than the time to display the page.
         */
        UIManager.put("Synthetica.scrollBarTrack.hoverAndPressed.enabled", true);
        // UIManager.put("Synthetica.scrollBarTrack.hoverOnButtons.enabled", true);
        if (config.isFontRespectsSystemDPI() && config.getFontScaleFactor() != 100) {
            LogController.CL().warning("SystemDPI might interfere with JD's FontScaling");
        }
        UIManager.put("Synthetica.animation.enabled", config.isAnimationEnabled());
        if (CrossSystem.isWindows()) {
            /* only windows opaque works fine */
            UIManager.put("Synthetica.window.opaque", config.isWindowOpaque());
        } else {
            /* must be true to disable it..strange world ;) */
            UIManager.put("Synthetica.window.opaque", true);
        }
        /*
         * NOTE: This Licensee Information may only be used by AppWork UG. If you like to create derived creation based on this sourcecode,
         * you have to remove this license key. Instead you may use the FREE Version of synthetica found on javasoft.de
         */
        try {
            /* we save around x-400 ms here by not using AES */
            String key = new String(new byte[] { 67, 49, 52, 49, 48, 50, 57, 52, 45, 54, 49, 66, 54, 52, 65, 65, 67, 45, 52, 66, 55, 68, 51, 48, 51, 57, 45, 56, 51, 52, 65, 56, 50, 65, 49, 45, 51, 55, 69, 53, 68, 54, 57, 53 }, "UTF-8");
            if (key != null) {
                String[] li = { "Licensee=AppWork UG", "LicenseRegistrationNumber=289416475", "Product=Synthetica", "LicenseType=Small Business License", "ExpireDate=--.--.----", "MaxVersion=2.999.999" };
                UIManager.put("Synthetica.license.info", li);
                UIManager.put("Synthetica.license.key", key);
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
    }

    @Override
    public void init() {
        setUIManager();
    }
}
