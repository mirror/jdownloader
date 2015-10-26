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

package org.jdownloader.startup;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.gui.swing.jdgui.menu.actions.sendlogs.LogAction;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonSerializer;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.IO.SYNC;
import org.appwork.utils.IOErrorHandler;
import org.appwork.utils.Regex;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.json.JSonHandler;
import org.jdownloader.myjdownloader.client.json.JsonFactoryInterface;
import org.jdownloader.myjdownloader.client.json.MyJDJsonMapper;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class Main {

    public static ParameterHandler PARAMETER_HANDLER = null;

    static {
        Application.ensureFrameWorkInit();
        // only use ipv4, because debian changed default stack to ipv6
        /*
         * we have to make sure that this property gets set before any network stuff gets loaded!!
         */
        System.setProperty("java.net.preferIPv4Stack", "true");

        /**
         * The sorting algorithm used by java.util.Arrays.sort and (indirectly) by java.util.Collections.sort has been replaced. The new
         * sort implementation may throw an IllegalArgumentException if it detects a Comparable that violates the Comparable contract. The
         * previous implementation silently ignored such a situation. If the previous behavior is desired, you can use the new system
         * property, java.util.Arrays.useLegacyMergeSort, to restore previous mergesort behavior. Nature of Incompatibility: behavioral RFE:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6804124
         *
         * Sorting live data (values changing during sorting) violates the general contract
         *
         * java.lang.IllegalArgumentException: Comparison method violates its general contract!
         */
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        try {
            /*
             * never cache negative answers,workaround for buggy dns servers that can fail and then the cache would be polluted for cache
             * timeout
             */
            java.security.Security.setProperty("networkaddress.cache.negative.ttl", 0 + "");
        } catch (final Throwable e) {
        }
        org.appwork.utils.Application.setApplication(".jd_home");
        org.appwork.utils.Application.getRoot(jd.SecondLevelLaunch.class);
        try {
            copySVNtoHome();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Dialog.getInstance().setLafManager(LookAndFeelController.getInstance());

        IO.setErrorHandler(new IOErrorHandler() {
            private final AtomicBoolean reported = new AtomicBoolean(Application.isHeadless());

            @Override
            public void onWriteException(final Throwable e, final File file, final byte[] data) {
                LogController.getInstance().getLogger("GlobalIOErrors").severe("An error occured while writing " + data.length + " bytes to " + file);
                LogController.getInstance().getLogger("GlobalIOErrors").log(e);
                if (reported.compareAndSet(false, true)) {
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

            }

            @Override
            public void onReadStreamException(final Throwable e, final java.io.InputStream fis) {
                LogController.getInstance().getLogger("GlobalIOErrors").log(e);
                if (reported.compareAndSet(false, true)) {
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

    private static void copySVNtoHome() {
        try {
            if (!Application.isJared(null) && Application.getRessourceURL("org/jdownloader/update/JDUpdateClient.class") == null) {

                File workspace = new File(Main.class.getResource("/").toURI()).getParentFile();
                File svnEntriesFile = new File(workspace, ".svn/entries");
                if (svnEntriesFile.exists()) {
                    long lastMod = svnEntriesFile.lastModified();
                    try {
                        lastMod = Long.parseLong(Regex.getLines(IO.readFileToString(svnEntriesFile))[3].trim());
                    } catch (Throwable e) {

                    }

                    long lastUpdate = -1;
                    File lastSvnUpdateFile = Application.getResource("dev/lastSvnUpdate");
                    if (lastSvnUpdateFile.exists()) {
                        try {
                            lastUpdate = Long.parseLong(IO.readFileToString(lastSvnUpdateFile));
                        } catch (Throwable e) {

                        }
                    }
                    if (lastMod > lastUpdate) {
                        copyResource(workspace, "themes/themes", "themes");
                        copyResource(workspace, "ressourcen/jd", "jd");
                        copyResource(workspace, "ressourcen/tools", "tools");
                        copyResource(workspace, "translations/translations", "translations");
                        File jdJar = Application.getResource("JDownloader.jar");
                        jdJar.delete();
                        IO.copyFile(new File(workspace, "dev/JDownloader.jar"), jdJar);
                        lastSvnUpdateFile.delete();
                        lastSvnUpdateFile.getParentFile().mkdirs();
                        IO.writeStringToFile(lastSvnUpdateFile, lastMod + "");
                    }
                }
                // URL mainClass = Application.getRessourceURL("org", true);
                //
                // File svnJar = new File(new File(mainClass.toURI()).getParentFile().getParentFile(), "dev/JDownloader.jar");
                // FileCreationManager.getInstance().delete(jdjar, null);
                // IO.copyFile(svnJar, jdjar);
                //
                // }

            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void copyResource(File workspace, String from, String to) throws IOException {
        System.out.println("Copy SVN Resources " + new File(workspace, from) + " to " + Application.getResource(to));
        IO.copyFolderRecursive(new File(workspace, from), Application.getResource(to), true, new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.getAbsolutePath().contains(".svn")) {
                    return false;
                } else {
                    System.out.println("Copy " + pathname);
                    return true;
                }

            }

        }, SYNC.NONE);
    }

    public static void main(String[] args) {
        final boolean nativeSwing = !CrossSystem.isRaspberryPi() && System.getProperty("nativeswing") != null && !Application.isHeadless();
        if (nativeSwing) {
            long start = System.currentTimeMillis();
            try {
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.COMPONENTS_DEBUG_PRINTCREATION.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.COMPONENTS_DEBUG_PRINTDISPOSAL.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.COMPONENTS_DEBUG_PRINTFAILEDMESSAGES.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.COMPONENTS_DEBUG_PRINTOPTIONS.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.COMPONENTS_DEBUG_PRINTSHAPECOMPUTING.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.INTERFACE_DEBUG_PRINTMESSAGES.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.PEERVM_DEBUG_PRINTCOMMANDLINE.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.PEERVM_DEBUG_PRINTSTARTMESSAGE.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.PEERVM_DEBUG_PRINTSTOPMESSAGE.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.SWT_DEVICE_DEBUG.set("true");
                chrriis.dj.nativeswing.swtimpl.NSSystemPropertySWT.SWT_DEVICEDATA_DEBUG.set("true");
                chrriis.dj.nativeswing.swtimpl.NativeInterface.open();
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                System.out.println("Native Swing init took " + (System.currentTimeMillis() - start) + " ms");
            }
        }

        // USe Jacksonmapper in this project

        JacksonMapper jm = new JacksonMapper();
        JSonStorage.setMapper(jm);

        // add Serializer to Handle JsonFactoryInterface from MyJDownloaderCLient Project
        jm.addSerializer(JsonFactoryInterface.class, new JsonSerializer<JsonFactoryInterface>() {

            @Override
            public String toJSonString(JsonFactoryInterface list) {
                return list.toJsonString();
            }

            @Override
            public boolean canSerialize(Object arg0) {
                return arg0 instanceof JsonFactoryInterface;
            }
        });
        // set MyJDownloaderCLient JsonHandler
        MyJDJsonMapper.HANDLER = new JSonHandler<Type>() {

            @Override
            public String objectToJSon(Object payload) {
                return JSonStorage.serializeToJson(payload);
            }

            @Override
            public <T> T jsonToObject(String dec, final Type clazz) {
                return (T) JSonStorage.restoreFromString(dec, new TypeRef(clazz) {

                });
            }
        };

        checkLanguageSwitch(args);
        try {
            /* set D3D Property if not already set by user */
            if (CrossSystem.isWindows() && System.getProperty("sun.java2d.d3d") == null) {
                if (JsonConfig.create(org.jdownloader.settings.GraphicalUserInterfaceSettings.class).isUseD3D()) {
                    System.setProperty("sun.java2d.d3d", "true");
                } else {
                    System.setProperty("sun.java2d.d3d", "false");
                    // 4455041 - Even when ddraw is disabled, ddraw.dll is loaded when
                    // pixel format calls are made.
                    System.setProperty("sun.awt.nopixfmt", "true");
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }

        PARAMETER_HANDLER = new ParameterHandler();
        PARAMETER_HANDLER.onStartup(args);

        // Rescan plugincached if required
        ExtensionController.getInstance().invalidateCacheIfRequired();
        HostPluginController.getInstance().invalidateCacheIfRequired();
        CrawlerPluginController.invalidateCacheIfRequired();

        jd.SecondLevelLaunch.mainStart(args);
        if (nativeSwing) {
            try {
                final Class<?> updaterLauncherClass = Class.forName("org.jdownloader.update.launcher.JDLauncher");
                final Field field = updaterLauncherClass.getField("END_OF_MAIN_HANDLER");
                field.set(null, new Runnable() {

                    @Override
                    public void run() {
                        try {
                            chrriis.dj.nativeswing.swtimpl.NativeInterface.runEventPump();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Throwable e1) {

                try {
                    chrriis.dj.nativeswing.swtimpl.NativeInterface.runEventPump();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
}