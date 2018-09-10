package jd.gui.swing;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import javax.swing.JFrame;

import jd.SecondLevelLaunch;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.gui.swing.dialog.AboutDialog;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class AWTMacOSApplicationAdapter {
    public static void enableMacSpecial() {
        final Desktop desktop = Desktop.getDesktop();
        if (desktop != null) {
            final AWTMacOSApplicationAdapter adapter = new AWTMacOSApplicationAdapter();
            adapter.setAboutHandler​(desktop);
            adapter.setPreferencesHandler​(desktop);
            adapter.setOpenFileHandler(desktop);
            adapter.setOpenURIHandler​(desktop);
        }
    }

    private void setOpenURIHandler​(Desktop desktop) {
        if (desktop != null) {
            try {
                final Class<?> openURIHandlerInterface = Class.forName("java.awt.desktop.OpenURIHandler​");
                final Method setOpenURIHandler​ = desktop.getClass().getDeclaredMethod("setOpenURIHandler​​", new Class[] { openURIHandlerInterface });
                final Object openURIHandler = java.lang.reflect.Proxy.newProxyInstance(openURIHandlerInterface.getClassLoader(), new Class[] { openURIHandlerInterface }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        appReOpened();
                        final Object openURIEvent​ = args[0];
                        final Method getURI​ = openURIEvent​.getClass().getMethod("getURI​", new Class[0]);
                        final URI uri = (URI) getURI​.invoke(openURIEvent​, new Object[0]);
                        final String links = uri.toString();
                        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
                            @Override
                            public void run() {
                                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Distribute links: " + links);
                                LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.MAC_DOCK.getLinkOriginDetails(), links));
                            }
                        });
                        return null;
                    }
                });
                setOpenURIHandler​.invoke(desktop, openURIHandler);
            } catch (final UnsupportedOperationException ignore) {
                ignore.printStackTrace();
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void setOpenFileHandler(Desktop desktop) {
        if (desktop != null) {
            try {
                final Class<?> openFilesHandlerInterface = Class.forName("java.awt.desktop.OpenFilesHandler​");
                final Method setOpenFileHandler​ = desktop.getClass().getDeclaredMethod("setOpenFileHandler​", new Class[] { openFilesHandlerInterface });
                final Object openFileHandler = java.lang.reflect.Proxy.newProxyInstance(openFilesHandlerInterface.getClassLoader(), new Class[] { openFilesHandlerInterface }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        appReOpened();
                        final Object openFilesEvent = args[0];
                        Method getFiles = openFilesEvent.getClass().getMethod("getFiles", new Class[0]);
                        final List<File> files = (List<File>) getFiles.invoke(openFilesEvent, new Object[0]);
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Handle open files from Dock " + files.toString());
                        final StringBuilder sb = new StringBuilder();
                        for (final File file : files) {
                            if (sb.length() > 0) {
                                sb.append("\r\n");
                            }
                            sb.append(file.toURI().toString());
                        }
                        final String links = sb.toString();
                        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
                            @Override
                            public void run() {
                                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Distribute links: " + links);
                                LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.MAC_DOCK.getLinkOriginDetails(), links));
                            }
                        });
                        return null;
                    }
                });
                setOpenFileHandler​.invoke(desktop, openFileHandler);
            } catch (final UnsupportedOperationException ignore) {
                ignore.printStackTrace();
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void setPreferencesHandler​(Desktop desktop) {
        if (desktop != null) {
            try {
                final Class<?> preferencesHandlerInterface = Class.forName("java.awt.desktop.PreferencesHandler");
                final Method setPreferencesHandler = desktop.getClass().getDeclaredMethod("setPreferencesHandler", new Class[] { preferencesHandlerInterface });
                final Object preferencesHandler = java.lang.reflect.Proxy.newProxyInstance(preferencesHandlerInterface.getClassLoader(), new Class[] { preferencesHandlerInterface }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        new EDTRunner() {
                            @Override
                            protected void runInEDT() {
                                try {
                                    System.out.println(1);
                                    appReOpened();
                                    System.out.println(2);
                                    JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                                    System.out.println(3);
                                    JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                                } catch (Throwable ignore) {
                                    ignore.printStackTrace();
                                }
                            }
                        };
                        return null;
                    }
                });
                setPreferencesHandler.invoke(desktop, preferencesHandler);
            } catch (final UnsupportedOperationException ignore) {
                ignore.printStackTrace();
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void appReOpened() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                final JDGui swingGui = JDGui.getInstance();
                if (swingGui == null || swingGui.getMainFrame() == null) {
                    return;
                }
                final JFrame mainFrame = swingGui.getMainFrame();
                if (!mainFrame.isVisible()) {
                    WindowManager.getInstance().setVisible(mainFrame, true, FrameState.OS_DEFAULT);
                }
            }
        };
    }

    private void setAboutHandler​(Desktop desktop) {
        if (desktop != null) {
            try {
                final Class<?> aboutHandlerInterface = Class.forName("java.awt.desktop.AboutHandler");
                final Method setAboutHandler​ = desktop.getClass().getDeclaredMethod("setAboutHandler", new Class[] { aboutHandlerInterface });
                final Object aboutHandler = java.lang.reflect.Proxy.newProxyInstance(aboutHandlerInterface.getClassLoader(), new Class[] { aboutHandlerInterface }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        try {
                            Dialog.getInstance().showDialog(new AboutDialog());
                        } catch (DialogNoAnswerException e1) {
                        }
                        return null;
                    }
                });
                setAboutHandler​.invoke(desktop, aboutHandler);
            } catch (final UnsupportedOperationException ignore) {
                ignore.printStackTrace();
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
