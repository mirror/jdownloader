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

import java.io.File;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.synthetica.SyntheticaHelper;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.LAFManagerInterface;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowsWindowManager;
import org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.LookAndFeelType;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LookAndFeelController implements LAFManagerInterface {
    private static final String                DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL = JDDefaultLookAndFeel.class.getName();
    private static final LookAndFeelController INSTANCE                                                      = new LookAndFeelController();

    /**
     * get the only existing instance of LookAndFeelController. This is a singleton
     * 
     * @return
     */
    public static LookAndFeelController getInstance() {
        return LookAndFeelController.INSTANCE;
    }

    private GraphicalUserInterfaceSettings config;
    private String                         laf = null;
    private LogSource                      logger;

    /**
     * Create a new instance of LookAndFeelController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private LookAndFeelController() {
        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);
        logger = LogController.getInstance().getLogger(getClass().getName());
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
        initWindowManager();
        long t = System.currentTimeMillis();
        try {
            // de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setLookAndFeel("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel");
            // if (true) return;

            laf = DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL;
            LogController.GL.info("Use Look & Feel: " + laf);
            try {
                LookAndFeelType theme = config.getLookAndFeelTheme();
                if (theme == null || !theme.isAvailable()) {
                    theme = LookAndFeelType.DEFAULT;
                }

                laf = theme.getClazz();

                String customLookAndFeel = config.getCustomLookAndFeelClass();
                if (StringUtils.isNotEmpty(customLookAndFeel)) {
                    try {
                        Class.forName(customLookAndFeel);
                        laf = customLookAndFeel;
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
            } catch (Throwable e) {
                logger.log(e);
            }

            if (laf.contains("Synthetica") || laf.equals(DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL)) {

                //
                String liz = null;
                try {
                    if (!Application.isJared(LookAndFeelController.class)) {
                        // enable the synthetica dev license for people working on our offical repo at svn.jdownloader.org
                        // for all other mirror repos: please do not use our license
                        URL url = Application.getRessourceURL("");
                        File bin = new File(url.toURI());
                        File db = new File(bin.getParent(), ".svn/wc.db");
                        if (db.exists()) {
                            String str = IO.readFileToString(db);
                            if (str.contains("svn://svn.jdownloader.org/jdownloader")) {
                                str = null;
                                if (Application.getResource("JDownloader.jar").exists()) {
                                    JarFile jf = null;
                                    try {
                                        jf = new JarFile(Application.getResource("JDownloader.jar"));
                                        JarEntry je = jf.getJarEntry("cfg/synthetica-license.key");
                                        liz = IO.readInputStreamToString(jf.getInputStream(je));
                                    } finally {
                                        if (jf != null) jf.close();
                                    }
                                }
                            }
                        } else {
                            String str = IO.readFileToString(new File(bin.getParent(), ".svn/entries"));
                            if (str != null && str.contains("svn://svn.jdownloader.org/jdownloader/trunk")) {
                                str = null;
                                if (Application.getResource("JDownloader.jar").exists()) {
                                    JarFile jf = null;
                                    try {
                                        jf = new JarFile(Application.getResource("JDownloader.jar"));
                                        JarEntry je = jf.getJarEntry("cfg/synthetica-license.key");
                                        liz = IO.readInputStreamToString(jf.getInputStream(je));
                                    } finally {
                                        if (jf != null) jf.close();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }
                SyntheticaHelper.init(laf, liz);
                LAFOptions.init(laf);
                ExtTooltip.createConfig(ExtTooltip.DEFAULT).setForegroundColor((LAFOptions.getInstance().getColorForTooltipForeground()).getRGB());
            } else {
                /* init for all other laf */

                UIManager.setLookAndFeel(laf);
                LAFOptions.init(laf);
            }

        } catch (Throwable e) {
            LogController.CL().log(e);
            try {
                LookAndFeel currentLaf = UIManager.getLookAndFeel();
                // this may happen if the updater launcher already has set the look and feel.
                if (currentLaf != null && !(currentLaf instanceof MetalLookAndFeel)) {
                    LogController.CL().info("Don't set System look and feel " + currentLaf + " is already set");
                    return;
                }

                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                LAFOptions.init(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            } catch (InstantiationException e1) {
                e1.printStackTrace();
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (UnsupportedLookAndFeelException e1) {
                e1.printStackTrace();
            }
        } finally {
            LogController.GL.info("LAF init: " + (System.currentTimeMillis() - t));
        }
    }

    private void initWindowManager() {
        WindowManager wm = WindowManager.getInstance();
        if (wm instanceof WindowsWindowManager && CrossSystem.isWindows()) {
            final WindowsWindowManager wwm = (WindowsWindowManager) wm;

            wwm.setAltWorkaroundEnabled(CFG_GUI.CFG.isWindowsWindowManagerAltKeyWorkaroundEnabled());
            wwm.setAltWorkaroundKeys(CFG_GUI.CFG.getWindowsWindowManagerAltKeyCombi());
            try {
                CFG_GUI.CFG.setWindowsWindowManagerForegroundLockTimeout(WindowsWindowManager.readForegroundLockTimeout());
            } catch (Exception e) {
                CFG_GUI.CFG.setWindowsWindowManagerForegroundLockTimeout(-1);
                logger.log(e);
            }
            CFG_GUI.WINDOWS_WINDOW_MANAGER_FOREGROUND_LOCK_TIMEOUT.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

                @Override
                public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
                }

                @Override
                public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                    try {
                        if (newValue >= 0 && newValue != WindowsWindowManager.readForegroundLockTimeout()) {

                            WindowsWindowManager.writeForegroundLockTimeout(newValue);
                            Dialog.getInstance().showMessageDialog(_GUI._.LookAndFeelController_onConfigValueModified_reboot_required());
                        }
                    } catch (Exception e) {
                        logger.log(e);
                        Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e.getMessage(), e);
                    }
                }
            });
            CFG_GUI.WINDOWS_WINDOW_MANAGER_ALT_KEY_WORKAROUND_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

                @Override
                public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                }

                @Override
                public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                    wwm.setAltWorkaroundEnabled(CFG_GUI.CFG.isWindowsWindowManagerAltKeyWorkaroundEnabled());
                }
            });
            CFG_GUI.WINDOWS_WINDOW_MANAGER_ALT_KEY_COMBI.getEventSender().addListener(new GenericConfigEventListener<int[]>() {

                @Override
                public void onConfigValueModified(KeyHandler<int[]> keyHandler, int[] newValue) {
                    wwm.setAltWorkaroundKeys(CFG_GUI.CFG.getWindowsWindowManagerAltKeyCombi());
                }

                @Override
                public void onConfigValidatorError(KeyHandler<int[]> keyHandler, int[] invalidValue, ValidationException validateException) {
                }
            });
        }
    }

    @Override
    public void init() {

        setUIManager();
    }
}
