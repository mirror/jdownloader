//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.nutils.nativeintegration;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.dialog.DnDWebBrowser;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.nutils.Executer;
import jd.nutils.OSDetector;
import jd.parser.Regex;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public abstract class LocalBrowser implements Serializable {

    private static final long serialVersionUID = 7153058016440180347L;
    private static LocalBrowser[] BROWSERLIST = null;
    private String name;

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        return toString().equals(o.toString());
    }

    public void setName(String name) {
        this.name = name;
    }

    private LocalBrowser(String name) {
        this.name = name;
    }

    public synchronized static LocalBrowser[] getBrowserList() {
        if (BROWSERLIST != null) return BROWSERLIST;
        ArrayList<LocalBrowser> ret = new ArrayList<LocalBrowser>();
        BrowserLauncher launcher;
        try {
            launcher = new BrowserLauncher();

            for (Object o : launcher.getBrowserList()) {
                ret.add(new LocalBrowser(o.toString()) {
                    private static final long serialVersionUID = 7078868188658406674L;

                    @Override
                    public void openURL(URL url) throws Exception {
                        if (url == null) return;
                        BrowserLauncher launcher = new BrowserLauncher();
                        launcher.openURLinBrowser(this.getName(), url.toString());
                    }

                });
            }
        } catch (BrowserLaunchingInitializingException e) {
            e.printStackTrace();
        } catch (UnsupportedOperatingSystemException e) {
            e.printStackTrace();
        }

        if (OSDetector.isMac()) {
            ret.add(new LocalBrowser("MAC Default") {
                private static final long serialVersionUID = 914161109428877932L;

                @Override
                public void openURL(URL url) throws IOException {
                    if (url == null) return;
                    com.apple.eio.FileManager.openURL(url.toString());
                }

            });

            if (new File("/Applications/Firefox.app").exists()) {
                ret.add(new LocalBrowser("Firefox") {
                    private static final long serialVersionUID = 2089733398098794579L;

                    @Override
                    public void openURL(URL url) throws Exception {
                        if (url == null) return;
                        Executer exec = new Executer("open");
                        exec.setLogger(JDLogger.getLogger());
                        exec.addParameters(new String[] { "/Applications/Firefox.app", "-new-tab", url.toString() });
                        exec.setWaitTimeout(10);
                        exec.start();
                        exec.waitTimeout();
                        if (exec.getException() != null) throw exec.getException();
                    }

                });

            } else {

                ret.add(new LocalBrowser("Firefox") {
                    private static final long serialVersionUID = -558662621604100570L;

                    @Override
                    public void openURL(URL url) throws Exception {
                        if (url == null) return;
                        Executer exec = new Executer("open");
                        exec.setLogger(JDLogger.getLogger());
                        exec.addParameters(new String[] { "/Applications/Safari.app", "-new-tab", url.toString() });
                        exec.setWaitTimeout(10);
                        exec.start();
                        exec.waitTimeout();
                        if (exec.getException() != null) throw exec.getException();
                    }

                });
            }

        }
        if (OSDetector.isLinux() && ret.size() == 0) {
            Executer exec = new Executer("firefox");
            exec.addParameter("-v");
            exec.setWaitTimeout(10);
            exec.start();
            exec.waitTimeout();
            if (exec.getException() == null) {
                ret.add(new LocalBrowser("Firefox") {
                    private static final long serialVersionUID = 6186304252605346654L;

                    @Override
                    public void openURL(URL url) throws Exception {
                        if (url == null) return;
                        Executer exec = new Executer("firefox");
                        exec.setLogger(JDLogger.getLogger());
                        exec.addParameters(new String[] { "-new-tab", url.toString() });
                        exec.setWaitTimeout(10);
                        exec.start();
                        exec.waitTimeout();
                        if (exec.getException() != null) throw exec.getException();
                    }

                });

            }

        }

        if (OSDetector.isWindows()) {
            ret.add(new LocalBrowser("Win Default") {
                private static final long serialVersionUID = 6862234646985946728L;

                @Override
                public void openURL(URL url) throws Exception {
                    Executer exec = new Executer("cmd");
                    exec.setLogger(JDLogger.getLogger());
                    exec.addParameters(new String[] { "/c", "start " + url });
                    exec.setWaitTimeout(10);
                    exec.start();
                    exec.waitTimeout();
                    if (exec.getException() != null) throw exec.getException();
                }

            });
        }
        /**
         * NUr wenn bisher kein anderer Browser gefunden wurde
         */
        if (ret.size() == 0) {

            ret.add(new LocalBrowser("Java Browser") {
                private static final long serialVersionUID = 1L;

                @Override
                public void openURL(final URL url) throws Exception {
                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {
                            new DnDWebBrowser(url);
                            return null;
                        }

                    }.start();
                }

            });

        }
        BROWSERLIST = ret.toArray(new LocalBrowser[] {});
        return BROWSERLIST;

    }

    public void openURL(String url) throws Exception {
        if (url == null) return;
        openURL(new URL(url));
    }

    abstract public void openURL(URL url) throws Exception;

    public static void openURL(String browser, URL url) throws Exception {

        if (url == null) return;
        LocalBrowser[] browsers = getBrowserList();
        if (browsers == null || browsers.length == 0) return;
        if (browser != null) {
            for (LocalBrowser b : browsers) {
                if (browser.equalsIgnoreCase(b.toString())) {
                    b.openURL(url);
                    return;
                }
            }
        }
        browsers[0].openURL(url);
    }

    public static void openDefaultURL(URL url) throws Exception {
        LocalBrowser[] browsers = getBrowserList();
        if (browsers == null || browsers.length == 0) return;
        browsers[0].openURL(url);
    }

    /**
     * can be used to e.g. install a firefox addon
     **/
    public static void openinFirefox(String url) {
        /* first try custom browser if it seems to be firefox */
        SubConfiguration cfg = SubConfiguration.getConfig(JDGuiConstants.CONFIG_PARAMETER);
        if (cfg.getBooleanProperty(JDGuiConstants.PARAM_CUSTOM_BROWSER_USE, false) && cfg.getStringProperty(JDGuiConstants.PARAM_CUSTOM_BROWSER).contains("firefox")) {
            Executer exec = new Executer(cfg.getStringProperty(JDGuiConstants.PARAM_CUSTOM_BROWSER));
            exec.setLogger(JDLogger.getLogger());
            String params = cfg.getStringProperty(JDGuiConstants.PARAM_CUSTOM_BROWSER_PARAM).replace("%url", url + "");
            exec.addParameters(Regex.getLines(params));
            exec.start();
            exec.setWaitTimeout(5);
            exec.waitTimeout();
            return;
        }
        String path = null;
        if (OSDetector.isWindows()) {
            if (new File("C:\\Program Files\\Mozilla Firefox\\firefox.exe").exists()) {
                path = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";
            } else if (new File("C:\\Programme\\Mozilla Firefox\\firefox.exe").exists()) {
                path = "C:\\Programme\\Mozilla Firefox\\firefox.exe";
            } else if (new File("C:\\Programme (x86)\\Mozilla Firefox\\firefox.exe").exists()) {
                path = "C:\\Programme (x86)\\Mozilla Firefox\\firefox.exe";
            } else if (new File("C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe").exists()) {
                path = "C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe";
            }
            if (path != null) {
                Executer exec = new Executer(path);
                exec.addParameters(new String[] { url });
                exec.start();
            }
        } else if (OSDetector.isMac()) {
            if (new File("/Applications/Firefox.app").exists()) {
                path = "/Applications/Firefox.app";
                Executer exec = new Executer("open");
                exec.addParameters(new String[] { path, url });
                exec.start();
            }
        } else if (OSDetector.isLinux()) {
            Executer exec = new Executer("firefox");
            exec.addParameters(new String[] { url });
            exec.start();
        }
    }
}
