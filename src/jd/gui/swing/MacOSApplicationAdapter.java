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
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.io.File;

import javax.swing.JFrame;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.gui.swing.dialog.AboutDialog;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.menu.actions.SettingsAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.IconPainter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.overviewpanel.AggregatedNumbers;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.RestartController;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.AppReOpenedEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.AppReOpenedListener;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.OpenURIHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

public class MacOSApplicationAdapter implements QuitHandler, AboutHandler, PreferencesHandler, AppReOpenedListener, OpenFilesHandler, OpenURIHandler {

    public static void enableMacSpecial() {
        Application macApplication = Application.getApplication();
        final MacOSApplicationAdapter adapter = new MacOSApplicationAdapter();
        macApplication.setAboutHandler(adapter);
        macApplication.setPreferencesHandler(adapter);
        macApplication.setQuitHandler(adapter);
        macApplication.addAppEventListener(adapter);
        macApplication.setOpenFileHandler(adapter);
        macApplication.setOpenURIHandler(adapter);
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                try {
                    com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(JDGui.getInstance().getMainFrame(), true);
                    LogController.GL.info("MacOS FullScreen Support activated");
                } catch (Throwable e) {
                    LogController.GL.log(e);
                }
                if (adapter.openURIlinks != null) {
                    LogController.GL.info("Distribute links: " + adapter.openURIlinks);
                    LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(adapter.openURIlinks));
                    adapter.openURIlinks = null;
                }
            }

        });

        new Thread("DockUpdater") {
            public void run() {
                while (true) {
                    final AggregatedNumbers aggn = new AggregatedNumbers(new SelectionInfo<FilePackage, DownloadLink>(null, DownloadController.getInstance().getAllDownloadLinks()));
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {

                            // com.apple.eawt.Application.getApplication().setDockIconBadge(DownloadInformations.getInstance().getPercent()
                            // + "%");
                            CircledProgressBar m = new CircledProgressBar();

                            m.setValueClipPainter(new IconPainter() {

                                public void paint(final CircledProgressBar bar, final Graphics2D g2, final Shape shape, final int diameter, final double progress) {
                                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                    ((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                                    final Area a = new Area(shape);

                                    g2.draw(a);
                                    g2.setColor(Color.GREEN);
                                    a.intersect(new Area(new Ellipse2D.Float(0, 0, diameter, diameter)));

                                    g2.fill(a);

                                    g2.setClip(null);

                                    // g2.draw(shape);
                                    g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
                                    g2.setColor(Color.GRAY);
                                    g2.draw(a);

                                }

                                private Dimension dimension;
                                {
                                    dimension = new Dimension(75, 75);
                                }

                                @Override
                                public Dimension getPreferredSize() {
                                    return dimension;
                                }
                            });

                            m.setNonvalueClipPainter(new IconPainter() {

                                public void paint(final CircledProgressBar bar, final Graphics2D g2, final Shape shape, final int diameter, final double progress) {
                                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                    ((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                                    final Area a = new Area(shape);

                                    g2.setColor(Color.WHITE);
                                    a.intersect(new Area(new Ellipse2D.Float(0, 0, diameter, diameter)));

                                    g2.fill(a);
                                    g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
                                    g2.setColor(Color.GRAY);
                                    g2.draw(a);

                                }

                                private Dimension dimension;
                                {
                                    dimension = new Dimension(75, 75);
                                }

                                @Override
                                public Dimension getPreferredSize() {
                                    return dimension;
                                }
                            });
                            m.setMaximum(100);

                            int percent = 0;
                            if (aggn.getTotalBytes() > 0) {
                                percent = (int) ((aggn.getLoadedBytes() * 100) / aggn.getTotalBytes());
                            }
                            m.setValue(percent);
                            // System.out.println(m.getValue());
                            m.setSize(75, 75);
                            Image img = NewTheme.I().getImage("logo/jd_logo_128_128", 128);
                            Graphics g = img.getGraphics();
                            g.translate(128 - m.getWidth(), 128 - m.getHeight());
                            ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                            m.paint(g);
                            // g.fillRect(0, 0, 40, 40);
                            g.dispose();
                            com.apple.eawt.Application.getApplication().setDockIconImage(img);
                            // JDGui.getInstance().getMainFrame().setIconImage(img);
                        }

                    };

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private QuitResponse quitResponse = null;
    private String       openURIlinks;

    private MacOSApplicationAdapter() {
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                new Thread() {
                    public void run() {
                        /*
                         * own thread because else it will block, performQuit calls exit again
                         */
                        if (quitResponse != null) quitResponse.performQuit();
                    };
                }.start();
            }
        });
    }

    public void handleQuitRequestWith(QuitEvent e, final QuitResponse response) {
        quitResponse = response;
        RestartController.getInstance().exitAsynch();
    }

    public void handlePreferences(PreferencesEvent e) {
        new SettingsAction().actionPerformed(null);

        appReOpened(null);
    }

    public void handleAbout(AboutEvent e) {
        try {
            Dialog.getInstance().showDialog(new AboutDialog());
        } catch (DialogNoAnswerException e1) {
        }
    }

    public void appReOpened(AppReOpenedEvent e) {
        final SwingGui swingGui = SwingGui.getInstance();
        if (swingGui == null || swingGui.getMainFrame() == null) return;
        final JFrame mainFrame = swingGui.getMainFrame();
        if (!mainFrame.isVisible()) {
            mainFrame.setVisible(true);
        }
    }

    public void openFiles(OpenFilesEvent e) {
        appReOpened(null);
        LogController.GL.info("Handle open files from Dock " + e.getFiles().toString());
        StringBuilder sb = new StringBuilder();
        for (final File file : e.getFiles()) {
            if (sb.length() > 0) {
                sb.append("\r\n");
            }
            sb.append("file://");
            sb.append(file.getPath());
        }
        LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(sb.toString()));
    }

    public void openURI(AppEvent.OpenURIEvent e) {
        appReOpened(null);
        LogController.GL.info("Handle open uri from Dock " + e.getURI().toString());
        String links = e.getURI().toString();
        if (SecondLevelLaunch.GUI_COMPLETE.isReached()) {
            LogController.GL.info("Distribute links: " + links);
            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(links));
        } else {
            openURIlinks = links;
        }
    }
}
