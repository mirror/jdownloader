//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org  http://jdownloader.org
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
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

package jd;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import jd.gui.swing.jdgui.menu.actions.sendlogs.LogAction;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.IO;
import org.appwork.utils.IOErrorHandler;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.logging.ExtLogManager;
import org.jdownloader.logging.LogController;
import org.jdownloader.startup.ParameterHandler;

public class Main {

    public static ParameterHandler PARAMETER_HANDLER = null;

    static {
        // only use ipv4, because debian changed default stack to ipv6
        /*
         * we have to make sure that this property gets set before any network stuff gets loaded!!
         */
        System.setProperty("java.net.preferIPv4Stack", "true");
        try {
            /*
             * never cache negative answers,workaround for buggy dns servers that can fail and then the cache would be polluted for cache
             * timeout
             */
            java.security.Security.setProperty("networkaddress.cache.negative.ttl", 0 + "");
        } catch (final Throwable e) {
        }
        org.appwork.utils.Application.setApplication(".jd_home");
        org.appwork.utils.Application.getRoot(jd.Launcher.class);

        Dialog.getInstance().setLafManager(LookAndFeelController.getInstance());
        try {
            // the logmanager should not be initialized here. so setting the
            // property should tell the logmanager to init a ExtLogManager
            // instance.
            System.setProperty("java.util.logging.manager", ExtLogManager.class.getName());
            ((ExtLogManager) LogManager.getLogManager()).setLogController(LogController.getInstance());
        } catch (Throwable e) {
            e.printStackTrace();
            LogManager lm = LogManager.getLogManager();
            System.err.println("Logmanager: " + lm);
            try {
                if (lm != null) {
                    // seems like the logmanager has already been set, and is
                    // not of type ExtLogManager. try to fix this here
                    // we experiences this bug once on a mac system. may be
                    // caused by mac jvm, or the mac install4j launcher

                    // 12.11:
                    // a winxp user had this problem with install4j (exe4j) as well.
                    // seems like 4xeej sets a logger before our main is reached.
                    Field field = LogManager.class.getDeclaredField("manager");
                    field.setAccessible(true);
                    ExtLogManager manager = new ExtLogManager();

                    field.set(null, manager);
                    Field rootLogger = LogManager.class.getDeclaredField("rootLogger");
                    rootLogger.setAccessible(true);
                    Logger rootLoggerInstance = (Logger) rootLogger.get(lm);
                    rootLogger.set(manager, rootLoggerInstance);
                    manager.addLogger(rootLoggerInstance);

                    // Adding the global Logger. Doing so in the Logger.<clinit>
                    // would deadlock with the LogManager.<clinit>.

                    Method setLogManager = Logger.class.getDeclaredMethod("setLogManager", new Class[] { LogManager.class });
                    setLogManager.setAccessible(true);
                    setLogManager.invoke(Logger.global, manager);

                    Enumeration<String> names = lm.getLoggerNames();
                    while (names.hasMoreElements()) {
                        manager.addLogger(lm.getLogger(names.nextElement()));

                    }
                }
            } catch (Throwable e1) {
                e1.printStackTrace();
            }

        }
        IO.setErrorHandler(new IOErrorHandler() {
            private boolean reported;

            {
                reported = false;

            }

            @Override
            public void onWriteException(final Throwable e, final File file, final byte[] data) {
                LogController.getInstance().getLogger("GlobalIOErrors").log(e);
                if (reported) return;
                reported = true;
                new Thread() {
                    public void run() {
                        Dialog.getInstance().showExceptionDialog("Write Error occured", "An error occured while writing " + data.length + " bytes to " + file, e);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        LogAction la = new LogAction();
                        la.actionPerformed(null);
                    }
                }.start();

            }

            @Override
            public void onReadStreamException(final Throwable e, final java.io.InputStream fis) {
                LogController.getInstance().getLogger("GlobalIOErrors").log(e);
                if (reported) return;
                reported = true;
                new Thread() {
                    public void run() {
                        Dialog.getInstance().showExceptionDialog("Read Error occured", "An error occured while reading data", e);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        LogAction la = new LogAction();
                        la.actionPerformed(null);
                    }
                }.start();
            }

            @Override
            public void onCopyException(Throwable e, File in, File out) {

            }
        });
    }

    public static void checkLanguageSwitch(final String[] args) {
        try {
            String lng = JSonStorage.restoreFromFile("cfg/language.json", TranslationFactory.getDesiredLanguage());
            TranslationFactory.setDesiredLanguage(lng);

            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-translatortest")) {
                    TranslationFactory.setDesiredLanguage(args[i + 1]);
                }

            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // USe Jacksonmapper in this project
        System.err.println(CrossSystem.class.getClassLoader());
        System.err.println(JacksonMapper.class.getClassLoader());
        System.err.println(Thread.currentThread().getContextClassLoader());
        JacksonMapper jm = new JacksonMapper();
        JSonStorage.setMapper(jm);

        checkLanguageSwitch(args);
        try {
            /* set D3D Property if not already set by user */
            if (CrossSystem.isWindows()) {
                if (JsonConfig.create(org.jdownloader.settings.GraphicalUserInterfaceSettings.class).isUseD3D()) {
                    System.setProperty("sun.java2d.d3d", "true");
                } else {
                    System.setProperty("sun.java2d.d3d", "false");
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }

        PARAMETER_HANDLER = new ParameterHandler();
        PARAMETER_HANDLER.onStartup(args);
        jd.Launcher.mainStart(args);

    }
}