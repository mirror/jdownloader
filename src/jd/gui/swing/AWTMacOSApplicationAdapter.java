package jd.gui.swing;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import javax.swing.JFrame;

import jd.SecondLevelLaunch;
import jd.controlling.TaskQueue;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.gui.swing.dialog.AboutDialog;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public class AWTMacOSApplicationAdapter {
    public static void enableMacSpecial() {
        final Desktop desktop = Desktop.getDesktop();
        if (desktop != null) {
            final AWTMacOSApplicationAdapter adapter = new AWTMacOSApplicationAdapter();
            adapter.setAboutHandler​(desktop);
            adapter.setPreferencesHandler​(desktop);
            adapter.setQuitHandler(desktop);
            adapter.setOpenFileHandler(desktop);
            adapter.setOpenURIHandler​(desktop);
        }
    }

    private void setQuitHandler(Desktop desktop) {
        if (desktop != null) {
            try {
                final Class<?> quitHandlerInterface = Class.forName("java.awt.desktop.QuitHandler");
                final Method setQuitHandler​ = desktop.getClass().getDeclaredMethod("setQuitHandler​", new Class[] { quitHandlerInterface });
                final Object quitHandler = java.lang.reflect.Proxy.newProxyInstance(quitHandlerInterface.getClassLoader(), new Class[] { quitHandlerInterface }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, final Object[] args) throws Throwable {
                        RestartController.getInstance().exitAsynch(new SmartRlyExitRequest() {
                            @Override
                            public void onShutdown() {
                                new Thread() {
                                    public void run() {
                                        /*
                                         * own thread because else it will block, performQuit calls exit again
                                         */
                                        try {
                                            final Object quitResponse = args[1];
                                            final Method performQuit = quitResponse.getClass().getMethod("performQuit​", new Class[0]);
                                            performQuit.invoke(quitResponse, new Object[0]);
                                        } catch (Throwable e) {
                                            e.printStackTrace();
                                        }
                                    };
                                }.start();
                            }

                            @Override
                            public void onShutdownVeto() {
                                new Thread() {
                                    public void run() {
                                        /*
                                         * own thread because else it will block, performQuit calls exit again
                                         */
                                        try {
                                            final Object quitResponse = args[1];
                                            final Method cancelQuit​ = quitResponse.getClass().getMethod("cancelQuit​​", new Class[0]);
                                            cancelQuit​.invoke(quitResponse, new Object[0]);
                                        } catch (Throwable e) {
                                            e.printStackTrace();
                                        }
                                    };
                                }.start();
                            }
                        });
                        return null;
                    }
                });
                setQuitHandler​.invoke(desktop, quitHandler);
            } catch (final UnsupportedOperationException ignore) {
                LoggerFactory.getDefaultLogger().log(ignore);
            } catch (final Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
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
                        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                            @Override
                            protected Void run() throws RuntimeException {
                                try {
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
                                } catch (final Throwable e) {
                                    LoggerFactory.getDefaultLogger().log(e);
                                }
                                return null;
                            }
                        });
                        return null;
                    }
                });
                setOpenURIHandler​.invoke(desktop, openURIHandler);
            } catch (final UnsupportedOperationException ignore) {
                LoggerFactory.getDefaultLogger().log(ignore);
            } catch (final Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
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
                        TaskQueue.getQueue().enqueue(new QueueAction<Void, RuntimeException>() {
                            @Override
                            protected Void run() throws RuntimeException {
                                try {
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
                                } catch (final Throwable e) {
                                    LoggerFactory.getDefaultLogger().log(e);
                                }
                                return null;
                            }
                        });
                        return null;
                    }
                });
                setOpenFileHandler​.invoke(desktop, openFileHandler);
            } catch (final UnsupportedOperationException ignore) {
                LoggerFactory.getDefaultLogger().log(ignore);
            } catch (final Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
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
                        TaskQueue.getQueue().enqueue(new QueueAction<Void, RuntimeException>() {
                            @Override
                            protected Void run() throws RuntimeException {
                                new EDTRunner() {
                                    @Override
                                    protected void runInEDT() {
                                        try {
                                            appReOpened();
                                            JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                                            JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                                        } catch (Throwable e) {
                                            LoggerFactory.getDefaultLogger().log(e);
                                        }
                                    }
                                };
                                return null;
                            }
                        });
                        return null;
                    }
                });
                setPreferencesHandler.invoke(desktop, preferencesHandler);
            } catch (final UnsupportedOperationException ignore) {
                LoggerFactory.getDefaultLogger().log(ignore);
            } catch (final Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
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
                        new Thread() {
                            public void run() {
                                Thread.currentThread().setDaemon(true);
                                try {
                                    Dialog.getInstance().showDialog(new AboutDialog());
                                } catch (DialogNoAnswerException e1) {
                                }
                            };
                        }.start();
                        return null;
                    }
                });
                setAboutHandler​.invoke(desktop, aboutHandler);
            } catch (final UnsupportedOperationException ignore) {
                LoggerFactory.getDefaultLogger().log(ignore);
            } catch (final Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
        }
    }
}
