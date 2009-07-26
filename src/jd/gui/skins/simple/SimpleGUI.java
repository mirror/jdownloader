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

package jd.gui.skins.simple;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import jd.config.ConfigContainer;
import jd.config.Configuration;
import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.skins.SwingGui;
import jd.gui.skins.jdgui.GUIUtils;
import jd.gui.skins.jdgui.JDGuiConstants;
import jd.gui.skins.jdgui.TabProgress;
import jd.gui.skins.jdgui.components.Balloon;
import jd.gui.skins.jdgui.components.JDCollapser;
import jd.gui.skins.jdgui.components.JDStatusBar;
import jd.gui.skins.jdgui.components.linkbutton.JLink;
import jd.gui.skins.jdgui.components.toolbar.MainToolBar;
import jd.gui.skins.jdgui.interfaces.JDMouseAdapter;
import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import jd.gui.skins.jdgui.menu.AboutMenu;
import jd.gui.skins.jdgui.menu.AddLinksMenu;
import jd.gui.skins.jdgui.menu.AddonsMenu;
import jd.gui.skins.jdgui.menu.CleanupMenu;
import jd.gui.skins.jdgui.menu.JDStartMenu;
import jd.gui.skins.jdgui.menu.JStartMenu;
import jd.gui.skins.jdgui.menu.PremiumMenu;
import jd.gui.skins.jdgui.menu.SaveMenu;
import jd.gui.skins.jdgui.menu.actions.ExitAction;
import jd.gui.skins.jdgui.menu.actions.RestartAction;
import jd.gui.skins.jdgui.views.downloadview.DownloadLinksPanel;
import jd.gui.swing.laf.LookAndFeelController;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingworker.SwingWorker;

public class SimpleGUI extends SwingGui {

    public static SimpleGUI CURRENTGUI = null;

    private static SimpleGUI INSTANCE;

    private static final long serialVersionUID = 3966433144683787356L;

    /**
     * Komponente, die alle Downloads anzeigt
     */
    private DownloadLinksPanel linkListPane;

    private Logger logger = JDLogger.getLogger();

    private TabProgress progressBar;

    private ContentPanel contentPanel;

    private MainToolBar toolBar;

    private JDStatusBar statusBar;

    private JDSeparator sep;

    private Image mainMenuIconRollOver;

    private Image mainMenuIcon;

    private JViewport taskPaneView;

    private SwingWorker<Object, Object> cursorworker;

    private JPopupMenu startMenu;

    private JLabel startbutton;

    private JMenuBar menuBar;

    /**
     * Das Hauptfenster wird erstellt. Singleton. Use SimpleGUI.createGUI
     */
    private SimpleGUI() {
        super("");

        updateDecoration();
        LookAndFeelController.setUIManager();

        /**
         * Init panels
         */

        menuBar = createMenuBar();
        this.setJMenuBar(menuBar);
        statusBar = new JDStatusBar();
        this.setEnabled(false);
        this.setWaiting(true);

        if (isSubstance() && GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.DECORATION_ENABLED, true)) {
            mainMenuIcon = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54_trans"), 54, 54);
            mainMenuIconRollOver = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54"), 54, 54);
        } else {
            mainMenuIcon = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54_trans"), 32, 32);
            mainMenuIconRollOver = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54"), 32, 32);
        }

        if (isSubstance()) this.getRootPane().setUI(new JDSubstanceUI());

        toolBar = MainToolBar.getInstance();

        // System.out.println(ui);
        addWindowListener(this);

        ArrayList<Image> list = new ArrayList<Image>();

        list.add(JDImage.getImage("logo/logo_14_14"));
        list.add(JDImage.getImage("logo/logo_15_15"));
        list.add(JDImage.getImage("logo/logo_16_16"));
        list.add(JDImage.getImage("logo/logo_17_17"));
        list.add(JDImage.getImage("logo/logo_18_18"));
        list.add(JDImage.getImage("logo/logo_19_19"));
        list.add(JDImage.getImage("logo/logo_20_20"));
        list.add(JDImage.getImage("logo/jd_logo_64_64"));
        if (JDUtilities.getJavaVersion() >= 1.6) {
            this.setIconImages(list);
        } else {
            this.setIconImage(list.get(3));
        }

        // this.setIconImage(JDImage.getImage("empty"));
        setTitle(JDUtilities.getJDTitle());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // initActions();
        // initMenuBar();
        JDL.initLocalisation();

        buildUI();

        setName("MAINFRAME");
        Dimension dim = GUIUtils.getLastDimension(this, null);
        if (dim == null) {
            dim = new Dimension(800, 600);
        }
        setPreferredSize(dim);
        setMinimumSize(new Dimension(400, 100));
        setLocation(GUIUtils.getLastLocation(null, null, this));
        pack();

        setExtendedState(GUIUtils.getConfig().getIntegerProperty("MAXIMIZED_STATE_OF_" + this.getName(), JFrame.NORMAL));

        this.hideSideBar(GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_SIDEBAR_COLLAPSED, false));

        setVisible(true);

        // Why this?
        // Because we want to start clipboardwatcher first, when gui is finished
        // with init, not before!
        ClipboardHandler.getClipboard().setTempDisabled(false);

        new Thread("guiworker") {
            public void run() {
                while (true) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            interval();
                        }
                    });
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        JDLogger.exception(e);
                    }
                }
            }
        }.start();

        startbutton = new JLabel(new ImageIcon(mainMenuIcon));
        startbutton.setToolTipText(JDL.L("gui.menu.tooltip", "Click here to open main menu"));
        startbutton.addMouseListener(new JDMouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                startbutton.setIcon(new ImageIcon(mainMenuIconRollOver));
            }

            public void mouseExited(MouseEvent e) {
                startbutton.setIcon(new ImageIcon(mainMenuIcon));
            }

            public void mouseClicked(MouseEvent e) {
                setWaiting(true);

                startMenu = new JPopupMenu() {

                    private static final long serialVersionUID = 3510198302982639068L;

                    public void paint(Graphics g) {
                        super.paint(g);
                        setWaiting(false);
                    }

                    public void setVisible(boolean b) {
                        super.setVisible(b);
                        if (b) startMenu = null;
                    }

                };

                JDStartMenu.createMenu(startMenu);

                startMenu.show(e.getComponent(), startMenu.getLocation().x, startMenu.getLocation().y + mainMenuIcon.getHeight(null));
            }

        });

    }

    private JMenuBar createMenuBar() {
        JMenuBar ret = new JMenuBar();

        JMenu file = new JMenu(JDL.L("jd.gui.skins.simple.simplegui.menubar.filemenu", "File"));

        file.add(new SaveMenu());
        file.addSeparator();
        file.add(new RestartAction());
        file.add(new ExitAction());

        JMenu edit = new JMenu(JDL.L("jd.gui.skins.simple.simplegui.menubar.linksmenu", "Links"));

        edit.add(new AddLinksMenu());
        edit.add(new CleanupMenu());
        ret.add(file);
        ret.add(edit);
        JStartMenu m;
        ret.add(m = new PremiumMenu());
        m.setIcon(null);
        ret.add(m = new AddonsMenu());
        m.setIcon(null);
        ret.add(m = new AboutMenu());
        m.setIcon(null);

        return ret;
    }

    public void setWaiting(boolean b) {
        if (b) {
            this.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            this.getGlassPane().setCursor(null);
        }
    }

    /**
     * Workaround the substance bug, that the resizecursor does not get resetted
     * if the movement is fast.
     */
    public void setCursor(Cursor c) {
        // System.out.println("set cursor " + c);
        if (this.getCursor() == c) return;
        if (isSubstance()) {
            switch (c.getType()) {
            case Cursor.E_RESIZE_CURSOR:
            case Cursor.N_RESIZE_CURSOR:
            case Cursor.S_RESIZE_CURSOR:
            case Cursor.W_RESIZE_CURSOR:
            case Cursor.NW_RESIZE_CURSOR:
            case Cursor.NE_RESIZE_CURSOR:
            case Cursor.SE_RESIZE_CURSOR:
            case Cursor.SW_RESIZE_CURSOR:
                final Cursor cc = c;
                if (cursorworker != null) {
                    cursorworker.cancel(true);
                    cursorworker = null;
                }
                this.cursorworker = new SwingWorker<Object, Object>() {

                    @Override
                    protected Object doInBackground() throws Exception {
                        Thread.sleep(2000);

                        return null;
                    }

                    public void done() {
                        if (cursorworker == this) {
                            if (getCursor() == cc) {
                                System.out.println("Reset cursor");
                                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            }
                            cursorworker = null;
                        }
                    }

                };
                cursorworker.execute();
            }
        }
        super.setCursor(c);

    }

    public void updateDecoration() {
        if (UIManager.getLookAndFeel().getSupportsWindowDecorations() && GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.DECORATION_ENABLED, true)) {
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        } else {
            setUndecorated(false);
            JFrame.setDefaultLookAndFeelDecorated(false);
            JDialog.setDefaultLookAndFeelDecorated(false);
        }
    }

    /**
     * Factorymethode. Erzeugt eine INstanc der Gui oder gibt eine bereits
     * existierende zurück
     * 
     * @return
     */
    public static SimpleGUI createGUI() {
        if (INSTANCE == null) INSTANCE = new SimpleGUI();
        return INSTANCE;
    }

    /**
     * Hier wird die komplette Oberfläche der Applikation zusammengestrickt
     */
    private void buildUI() {
        CURRENTGUI = this;
        linkListPane = new DownloadLinksPanel();
        contentPanel = new ContentPanel();

        progressBar = new TabProgress();

        contentPanel.display(linkListPane);

        JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]0[]0[]0[]"));

        setContentPane(panel);

        add(toolBar, "dock north");
        JPanel center = new JPanel(new MigLayout("ins 0,wrap 3", "[fill]0[shrink]0[fill,grow 100]", "[grow,fill]0[]"));
        sep = new JDSeparator();

        center.add(sep, "width 6!,gapright 2,spany 2,growy, pushy,hidemode 1");
        sep.setVisible(false);
        center.add(contentPanel, "");
        center.add(JDCollapser.getInstance(), "hidemode 3,gaptop 15,growx,pushx,growy,pushy");

        panel.add(center);

        panel.add(progressBar, "spanx,hidemode 3");
        panel.add(this.statusBar, "spanx, dock south");

    }

    public void controlEvent(final ControlEvent event) {
        // Moved the whole content of this method into a Runnable run by
        // invokeLater(). Ensures that everything inside is executed on the EDT.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                switch (event.getID()) {
                case ControlEvent.CONTROL_INIT_COMPLETE:
                    logger.info("Init complete");

                    SimpleGUI.this.setWaiting(false);
                    SimpleGUI.this.setEnabled(true);
                    if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, false)) {
                        new Thread() {
                            public void run() {
                                this.setName("Autostart counter");
                                final ProgressController pc = new ProgressController(JDL.L("gui.autostart", "Autostart downloads in few secounds..."));
                                pc.getBroadcaster().addListener(new ProgressControllerListener() {
                                    public void onProgressControllerEvent(ProgressControllerEvent event) {
                                        pc.setStatusText("Autostart aborted!");
                                    }
                                });
                                pc.finalize(10 * 1000l);
                                while (!pc.isFinished()) {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        break;
                                    }
                                }
                                if (!pc.isAbort()) JDUtilities.getController().startDownloads();
                            }
                        }.start();
                    }
                    break;
                case ControlEvent.CONTROL_PLUGIN_ACTIVE:
                    logger.info("Module started: " + event.getSource());
                    setTitle(JDUtilities.getJDTitle());
                    break;
                case ControlEvent.CONTROL_SYSTEM_EXIT:
                    SimpleGUI.this.setVisible(false);
                    SimpleGUI.this.dispose();
                    break;
                case ControlEvent.CONTROL_PLUGIN_INACTIVE:
                    logger.info("Module finished: " + event.getSource());
                    setTitle(JDUtilities.getJDTitle());
                    break;
                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                    for (DownloadLink link : DownloadController.getInstance().getAllDownloadLinks()) {
                        if (link.getLinkStatus().hasStatus(LinkStatus.TODO)) {
                            logger.info("Downloads stopped");
                            return;
                        }
                    }
                    logger.info("All downloads finished");

                    break;
                case ControlEvent.CONTROL_DOWNLOAD_START:
                    Balloon.showIfHidden(JDL.L("ballon.download.title", "Download"), JDTheme.II("gui.images.next", 32, 32), JDL.L("ballon.download.finished.started", "Download started"));
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                    Balloon.showIfHidden(JDL.L("ballon.download.title", "Download"), JDTheme.II("gui.images.next", 32, 32), JDL.L("ballon.download.finished.stopped", "Download stopped"));
                    break;
                }
            }
        });
    }

    /**
     * Diese Funktion wird in einem 1000 ms interval aufgerufen und kann dazu
     * verwendet werden die GUI zu aktuelisieren TODO
     */
    private void interval() {
        setTitle(JDUtilities.getJDTitle());
    }

    public static void displayConfig(final ConfigContainer container, final boolean toLastTab) {
        // new GuiRunnable<Object>() {
        //
        // // @Override
        // public Object runSave() {
        // ConfigEntriesPanel cep;
        //
        // JDCollapser.getInstance().setContentPanel(cep = new
        // ConfigEntriesPanel(container));
        // if (toLastTab) {
        // Component comp = cep.getComponent(0);
        // if (comp instanceof JTabbedPane) {
        // ((JTabbedPane) comp).setSelectedIndex(((JTabbedPane)
        // comp).getTabCount() - 1);
        // }
        // }
        // if (container.getGroup() != null) {
        // JDCollapser.getInstance().setTitle(container.getGroup().getName());
        // // JDCollapser.getInstance().setIcon(container.getGroup().
        // // getIcon());
        // } else {
        // JDCollapser.getInstance().setTitle(JDL.L("gui.panels.collapsibleconfig",
        // "Settings"));
        // JDCollapser.getInstance().setIcon(JDTheme.II("gui.images.config.addons",
        // 24, 24));
        // }
        //
        // InfoPanelHandler.setPanel(JDCollapser.getInstance());
        //
        // return null;
        // }
        //
        // }.start();
    }

    public void closeWindow() {
        if (JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(0, JDL.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?")), UserIO.RETURN_OK)) {
            contentPanel.getRightPanel().setHidden();
            GUIUtils.saveLastLocation(this, null);
            GUIUtils.saveLastDimension(this, null);
            GUIUtils.getConfig().save();
            JDUtilities.getController().exit();
        }
    }

    public void windowClosing(WindowEvent e) {
        if (e.getComponent() == this) closeWindow();
    }

    public static void showChangelogDialog() {
        int status = UserIO.getInstance().requestHelpDialog(UserIO.NO_CANCEL_OPTION, JDL.LF("system.update.message.title", "Updated to version %s", JDUtilities.getRevision()), JDL.L("system.update.message", "Update successfull"), JDL.L("system.update.showchangelogv2", "What's new?"), "http://jdownloader.org/changes/index");
        if (JDFlags.hasAllFlags(status, UserIO.RETURN_OK) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_SHOW_CHANGELOG, true)) {
            try {
                JLink.openURL("http://jdownloader.org/changes/index");
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

    public ContentPanel getContentPane() {
        return this.contentPanel;
    }

    /**
     * Returns of Substance LAF is active
     * 
     * @return
     */
    public static boolean isSubstance() {
        return LookAndFeelController.getPlaf().isSubstance();
    }

    public void hideSideBar(boolean b) {
        if (this.taskPaneView == null || taskPaneView.isVisible() == !b) return;
        if (b) {
            if (this.sep != null) this.sep.setMinimized(b);
            taskPaneView.setVisible(!b);
            // this.contentPanel.display(linkListPane);

        } else {
            if (this.sep != null) this.sep.setMinimized(b);
            taskPaneView.setVisible(!b);

        }

        GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_SIDEBAR_COLLAPSED, b);
        GUIUtils.getConfig().save();

    }

    @Override
    public void setContent(SwitchPanel tabbedPanel) {
        this.getContentPane().display(tabbedPanel);
    }

    @Override
    public void disposeView(SwitchPanel view) {
        // TODO Auto-generated method stub

    }

}
