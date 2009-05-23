package jd.nutils.nativeintegration;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.DnDWebBrowser;
import jd.nutils.Executer;
import jd.nutils.OSDetector;
import jd.utils.JDUtilities;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

abstract public class LocaleBrowser implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 7153058016440180347L;
    private static LocaleBrowser[] BROWSERLIST=null;
    private String name;

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private LocaleBrowser(String name) {
        this.name = name;
    }

    public synchronized static LocaleBrowser[] getBrowserList() {
        if (BROWSERLIST != null) return BROWSERLIST;
        ArrayList<LocaleBrowser> ret = new ArrayList<LocaleBrowser>();
        BrowserLauncher launcher;
        try {
            launcher = new BrowserLauncher();

            for (Object o : launcher.getBrowserList()) {
                ret.add(new LocaleBrowser(o.toString()) {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 7078868188658406674L;

                    @Override
                    public void openURL(URL url) throws Exception {
                        if(url==null)return;
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
            ret.add(new LocaleBrowser("MAC Default") {
                /**
                 * 
                 */
                private static final long serialVersionUID = 914161109428877932L;

                @Override
                public void openURL(URL url) throws IOException {
                    if(url==null)return;
                    com.apple.eio.FileManager.openURL(url.toString());
                }

            });

            if (new File("/Applications/Firefox.app").exists()) {
                ret.add(new LocaleBrowser("Firefox") {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 2089733398098794579L;

                    @Override
                    public void openURL(URL url) throws Exception {
                        if(url==null)return;
                        Executer exec = new Executer("open");
                        exec.addParameters(new String[] { "/Applications/Firefox.app", "-new-tab", url.toString() });
                        exec.setWaitTimeout(10);
                        exec.start();
                        exec.waitTimeout();
                        if (exec.getException() != null) throw exec.getException();
                    }

                });

            } else {

                ret.add(new LocaleBrowser("Firefox") {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -558662621604100570L;

                    @Override
                    public void openURL(URL url) throws Exception {
                        if(url==null)return;
                        Executer exec = new Executer("open");
                        exec.addParameters(new String[] { "/Applications/Safari.app", "-new-tab", url.toString() });
                        exec.setWaitTimeout(10);
                        exec.start();
                        exec.waitTimeout();
                        if (exec.getException() != null) throw exec.getException();
                    }

                });
            }

        }
        if (OSDetector.isLinux()&&ret.size()==0) {
            Executer exec = new Executer("firefox");
            exec.addParameter("-v");
            exec.setWaitTimeout(10);
            exec.start();
            exec.waitTimeout();
            if (exec.getException() == null) {
                ret.add(new LocaleBrowser("Firefox") {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 6186304252605346654L;
                 

                    @Override
                    public void openURL(URL url) throws Exception {
                        if(url==null)return;
                        Executer exec = new Executer("firefox");
                        exec.addParameters(new String[] {"-new-tab", url.toString() });
                        exec.setWaitTimeout(10);
                        exec.start();
                        exec.waitTimeout();
                        if (exec.getException() != null) throw exec.getException();
                    }

                });

            }

        }

        if (OSDetector.isWindows()) {
            ret.add(new LocaleBrowser("Win Default") {
                /**
                 * 
                 */
                private static final long serialVersionUID = 6862234646985946728L;

                @Override
                public void openURL(URL url) throws Exception {
                    Executer exec = new Executer("cmd");
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

            ret.add(new LocaleBrowser("Java Browser") {
                /**
                 * 
                 */
                private static final long serialVersionUID = 1L;

                @Override
                public void openURL(final URL url) throws Exception {
                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {
                            DnDWebBrowser javaBrowser = new DnDWebBrowser(((SimpleGUI) JDUtilities.getGUI()));
                            javaBrowser.goTo(url);
                            javaBrowser.setDefaultCloseOperation(DnDWebBrowser.DISPOSE_ON_CLOSE);
                            javaBrowser.setSize(800, 600);
                            javaBrowser.setVisible(true);
                            return null;
                        }

                    }.start();
                }

            });

        }
        BROWSERLIST = ret.toArray(new LocaleBrowser[] {});
        return BROWSERLIST;

    }

    public void openURL(String url) throws Exception {
        openURL(new URL(url));
    }

    abstract public void openURL(URL url) throws Exception;

    public static void openURL(String browser, URL url) throws Exception {
        LocaleBrowser[] browsers = getBrowserList();
        if (browsers == null || browsers.length == 0) return;
        for (LocaleBrowser b : browsers) {

            if (browser.equalsIgnoreCase(b.toString())) {
                b.openURL(url);
                return;
            }
        }
        browsers[0].openURL(url);

    }

    public static void openDefaultURL(URL url) throws Exception {
        LocaleBrowser[] browsers = getBrowserList();
        if (browsers == null || browsers.length == 0) return;
        browsers[0].openURL(url);
    }
}
