package org.jdownloader.extensions.infobar;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.locator.RememberAbsoluteLocator;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.controlling.DownloadLinkAggregator;
import org.jdownloader.extensions.infobar.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.images.AbstractIcon;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.JDProgressBar;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;
import jd.nutils.Formatter;
import net.miginfocom.swing.MigLayout;

public class InfoDialog extends JWindow implements ActionListener, MouseListener, MouseMotionListener, GenericConfigEventListener<Integer>, WindowListener {

    private static final long               serialVersionUID = 4715904261105562064L;

    private static final int                DOCKING_DISTANCE = 25;

    private final DragDropHandler           ddh;

    private NullsafeAtomicReference<Thread> updater          = new NullsafeAtomicReference<Thread>(null);
    private Point                           point;

    private JDProgressBar                   prgTotal;
    private JLabel                          lblProgress;
    private JLabel                          lblETA;
    private JLabel                          lblHelp;

    private SpeedMeterPanel                 speedmeter;

    private InfoBarExtension                extension;

    private RememberAbsoluteLocator         locator;

    public InfoDialog(InfoBarExtension infoBarExtension) {
        super();
        extension = infoBarExtension;
        this.ddh = new DragDropHandler();
        locator = new RememberAbsoluteLocator("InfoDialog");
        this.setName("INFODIALOG");
        this.setAlwaysOnTop(true);

        initGui();

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(ShutdownRequest shutdownRequest) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        hideDialog();
                    }
                };
            }
        });
        lblHelp.addMouseMotionListener(this);
        prgTotal.addMouseMotionListener(this);
        lblETA.addMouseMotionListener(this);
        lblProgress.addMouseMotionListener(this);
        speedmeter.addMouseMotionListener(this);
        lblHelp.addMouseListener(this);
        prgTotal.addMouseListener(this);
        lblETA.addMouseListener(this);
        lblProgress.addMouseListener(this);
        speedmeter.addMouseListener(this);
        addWindowListener(this);
        updateTransparency();
        CFG_INFOBAR.TRANSPARENCY.getEventSender().addListener(this, true);
        Point loc = locator.getLocationOnScreen(this);
        setLocation(loc);
    }

    private void updateTransparency() {
        if (Application.getJavaVersion() >= Application.JAVA16) {
            AbstractNotifyWindow.setWindowOpacity(this, (float) (CFG_INFOBAR.TRANSPARENCY.getValue() / 100.0));
        } else {
            Dialog.getInstance().showErrorDialog("Infobar Extension: Transparent Windows are not supported for your java version. Please update Java to 1.7 or higher.");
        }
    }

    @Override
    public void dispose() {
        locator.onClose(this);
        super.dispose();
    }

    @Override
    public void setVisible(boolean b) {
        if (!b) {
            locator.onClose(this);
        }
        super.setVisible(b);
    }

    private void initGui() {
        lblProgress = new JLabel(" ~ ");
        lblProgress.setHorizontalAlignment(JLabel.LEADING);

        lblETA = new JLabel(" ~ ");
        lblETA.setHorizontalAlignment(JLabel.TRAILING);

        prgTotal = new JDProgressBar();
        prgTotal.setMaximum(1);
        prgTotal.setMinimum(0);
        prgTotal.setStringPainted(true);

        lblHelp = new JLabel();
        if (Boolean.TRUE.equals(CFG_INFOBAR.DRAGNDROP_ICON_DISPLAYED.getValue())) {
            lblHelp = new JLabel(T.T.jd_plugins_optional_infobar_InfoDialog_help());
            lblHelp.setIcon(new AbstractIcon(IconKey.ICON_CLIPBOARD, 16));
        }
        lblHelp.setHorizontalTextPosition(JLabel.LEADING);
        lblHelp.setHorizontalAlignment(JLabel.CENTER);
        lblHelp.setToolTipText(T.T.jd_plugins_optional_infobar_InfoDialog_help_tooltip2());

        final JPanel panel = new JPanel(new MigLayout("ins 6, wrap 1", "[grow,fill,250]"));
        panel.setBorder(BorderFactory.createLineBorder(getBackground().darker().darker()));
        panel.add(speedmeter = new SpeedMeterPanel(false, true), "h 30!");

        panel.add(lblProgress, "split 2");
        panel.add(lblETA);
        panel.add(prgTotal);
        panel.add(lblHelp, "hidemode 3");
        if (Boolean.TRUE.equals(CFG_INFOBAR.LINKGRABBER_BUTTON_DISPLAYED.getValue())) {
            final JLabel lblCrawler = new JLabel(_GUI.T.jd_gui_swing_jdgui_views_linkgrabberview_tab_title());
            lblCrawler.setIcon(new AbstractIcon(IconKey.ICON_LINKGRABBER, 16));
            lblCrawler.setHorizontalTextPosition(JLabel.LEADING);
            lblCrawler.setHorizontalAlignment(JLabel.CENTER);
            lblCrawler.setToolTipText(_GUI.T.jd_gui_swing_jdgui_views_linkgrabberview_tab_tooltip());
            lblCrawler.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);
                    JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
                }
            });
            panel.add(lblCrawler);
        }
        this.setLayout(new MigLayout("ins 0", "[grow,fill]"));
        this.add(panel);

        this.pack();
    }

    public void showDialog() {
        if (isVisible()) {
            return;
        }
        final InfoUpdater thread = new InfoUpdater();
        final Thread oldThread = updater.getAndSet(thread);
        if (oldThread != null) {
            oldThread.interrupt();
        }
        thread.start();
        WindowManager.getInstance().setVisible(this, true, FrameState.OS_DEFAULT);
    }

    public void hideDialog() {
        final Thread thread = updater.getAndSet(null);
        if (thread != null) {
            thread.interrupt();
        }
        if (!isVisible()) {
            return;
        }
        setVisible(false);
        dispose();
    }

    public void setEnableDropLocation(final boolean enableDropLocation) {
        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {
                if (enableDropLocation) {
                    setTransferHandler(ddh);
                    lblHelp.setVisible(true);
                } else {
                    setTransferHandler(null);
                    lblHelp.setVisible(false);
                }
                pack();
                return null;
            }
        }.start();
    }

    private final class InfoUpdater extends Thread implements Runnable {

        @Override
        public void run() {
            final Thread thread = Thread.currentThread();
            while (thread == updater.get()) {
                final DownloadLinkAggregator dla = new DownloadLinkAggregator(DownloadsTable.getInstance().getSelectionInfo(false, false));
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (isVisible()) {
                            long totalDl = dla.getTotalBytes();
                            long curDl = dla.getBytesLoaded();
                            lblProgress.setText(Formatter.formatFilesize(curDl, 0) + " / " + Formatter.formatFilesize(totalDl, 0));
                            lblETA.setText(Formatter.formatSeconds(dla.getEta()));
                            prgTotal.setMaximum(Math.max(1, totalDl));
                            prgTotal.setValue(Math.max(0, curDl));
                        } else {
                            updater.compareAndSet(thread, null);
                        }
                    }
                }.waitForEDT();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            JMenuItem mi = new JMenuItem(T.T.jd_plugins_optional_infobar_InfoDialog_hideWindow());
            mi.setIcon(new AbstractIcon(IconKey.ICON_CLOSE, -1));
            mi.addActionListener(this);

            JPopupMenu popup = new JPopupMenu();
            popup.add(mi);
            popup.show(this, e.getPoint().x, e.getPoint().y);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        point = e.getPoint();
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {

        Point window = this.getLocation();

        int x = window.x + e.getPoint().x - point.x;
        int y = window.y + e.getPoint().y - point.y;

        this.setLocation(x, y);

    }

    public void mouseMoved(MouseEvent e) {
    }

    public void actionPerformed(ActionEvent e) {
        extension.setGuiEnable(false);

    }

    @Override
    public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                updateTransparency();
            }
        };
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {
        locator.onClose(this);
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

}