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

import java.awt.Toolkit;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.synthetica.SyntheticaHelper;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.LAFManagerInterface;
import org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.HexColorString;

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

    private volatile LAFOptions            lafOptions;
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

    private static void initLinux() {
        // set WM Class explicitly
        try {
            // patch by Vampire
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            final Field awtAppClassName = Toolkit.getDefaultToolkit().getClass().getDeclaredField("awtAppClassName");
            awtAppClassName.setAccessible(true);
            awtAppClassName.set(toolkit, "JDownloader");
        } catch (final Exception e) {
            e.printStackTrace();
            // it seems we are not in X, nothing to do for now
        }
    }

    public static final String DEFAULT_PREFIX = "LAF_CFG";
    private static boolean     uiInitated     = false;

    /**
     * setups the correct Look and Feel
     */
    public synchronized void setUIManager() {
        if (uiInitated) return;
        uiInitated = true;

        if (CrossSystem.isLinux()) initLinux();
        long t = System.currentTimeMillis();
        try {
            // de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setLookAndFeel("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel");
            // if (true) return;

            laf = DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL;
            LogController.GL.info("Use Look & Feel: " + laf);
            try {
                if (!StringUtils.isEmpty(config.getLookAndFeel())) {
                    logger.info("Try Custom Look And Feel: " + config.getLookAndFeel());
                    Class<?> cl = Class.forName(config.getLookAndFeel());
                    logger.info("Class: " + cl);
                    if (LookAndFeel.class.isAssignableFrom(cl)) {
                        logger.info("Custom LAF is a LookAndFeelClass");
                        laf = config.getLookAndFeel();

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
                                    JarFile jf = new JarFile(Application.getResource("JDownloader.jar"));
                                    try {
                                        JarEntry je = jf.getJarEntry("cfg/synthetica-license.key");
                                        liz = IO.readInputStreamToString(jf.getInputStream(je));
                                    } finally {

                                        jf.close();
                                    }

                                }

                            }
                        } else {
                            String str = IO.readFileToString(new File(bin.getParent(), ".svn/entries"));
                            if (str.contains("svn://svn.jdownloader.org/jdownloader/trunk")) {
                                str = null;
                                if (Application.getResource("JDownloader.jar").exists()) {
                                    JarFile jf = new JarFile(Application.getResource("JDownloader.jar"));
                                    try {
                                        JarEntry je = jf.getJarEntry("cfg/synthetica-license.key");
                                        liz = IO.readInputStreamToString(jf.getInputStream(je));
                                    } finally {

                                        jf.close();
                                    }

                                }

                            }
                        }

                    }
                } catch (Exception e) {

                }

                SyntheticaHelper.init(laf, liz);
                ExtTooltip.createConfig(ExtTooltip.DEFAULT).setForegroundColor(LAFOptions.createColor(getLAFOptions().getColorForTooltipForeground()).getRGB());

            } else {
                /* init for all other laf */
                UIManager.setLookAndFeel(laf);
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

    public LAFOptions getLAFOptions() {
        if (lafOptions != null) return lafOptions;
        synchronized (this) {
            if (lafOptions != null) return lafOptions;
            String str = null;
            try {
                if (laf != null) {
                    int i = laf.lastIndexOf(".");
                    String path = "laf/" + (i >= 0 ? laf.substring(i + 1) : laf) + "/options.json";
                    str = NewTheme.I().getText(path);
                }
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
            String c = null;
            LAFSettings cfg = JsonConfig.create(LAFSettings.class);
            for (KeyHandler m : cfg._getStorageHandler().getMap().values()) {
                try {
                    if (m.getAnnotation(HexColorString.class) != null) {
                        Object v = m.getValue();

                        if (v != null && LAFOptions.createColor(v.toString()) != null) {

                            logger.info("Use Custom Color for " + m.getKey() + ": " + v);
                            m.getSetter().getMethod().invoke(lafOptions, v);

                        }
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
            }

        }

        return lafOptions;
    }

    @Override
    public void init() {
        setUIManager();
    }
}
