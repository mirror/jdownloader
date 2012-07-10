package org.jdownloader.extensions.infobar;

import java.awt.DisplayMode;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import jd.controlling.downloadcontroller.DownloadInformations;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.components.JDProgressBar;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;
import jd.nutils.Formatter;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.infobar.translate.T;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class InfoDialog extends JWindow implements ActionListener, MouseListener, MouseMotionListener {

    private static InfoDialog INSTANCE = null;

    public static InfoDialog getInstance(AppAction action) {
        if (INSTANCE == null) INSTANCE = new InfoDialog(action);
        return INSTANCE;
    }

    private static final long          serialVersionUID = 4715904261105562064L;

    private static final int           DOCKING_DISTANCE = 25;

    private final DownloadInformations ds;
    private final AppAction            action;
    private final DragDropHandler      ddh;
    private boolean                    enableDocking;

    private InfoUpdater                updater          = null;
    private Point                      point;

    private JDProgressBar              prgTotal;
    private JLabel                     lblProgress;
    private JLabel                     lblETA;
    private JLabel                     lblHelp;

    private SpeedMeterPanel            speedmeter;

    private InfoDialog(AppAction action) {
        super();

        this.ds = DownloadInformations.getInstance();
        this.action = action;
        this.ddh = new DragDropHandler();

        this.setName("INFODIALOG");
        this.setAlwaysOnTop(true);
        this.setLocation(GUIUtils.getLastLocation(SwingGui.getInstance().getMainFrame(), this));

        initGui();

        this.addMouseListener(this);
        this.addMouseMotionListener(this);

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

    }

    private void initGui() {
        lblProgress = new JLabel(" ~ ");
        lblProgress.setHorizontalAlignment(JLabel.LEADING);

        lblETA = new JLabel(" ~ ");
        lblETA.setHorizontalAlignment(JLabel.TRAILING);

        prgTotal = new JDProgressBar();
        prgTotal.setStringPainted(true);

        lblHelp = new JLabel(T._.jd_plugins_optional_infobar_InfoDialog_help());
        lblHelp.setIcon(NewTheme.I().getIcon("clipboard", 16));
        lblHelp.setHorizontalTextPosition(JLabel.LEADING);
        lblHelp.setHorizontalAlignment(JLabel.CENTER);
        lblHelp.setToolTipText(T._.jd_plugins_optional_infobar_InfoDialog_help_tooltip2());

        JPanel panel = new JPanel(new MigLayout("ins 5, wrap 1", "[grow,fill,200]"));
        panel.setBorder(BorderFactory.createLineBorder(getBackground().darker().darker()));
        panel.add(speedmeter = new SpeedMeterPanel(false, true), "h 30!");
        speedmeter.setAntiAliasing(JsonConfig.create(GraphicalUserInterfaceSettings.class).isTextAntiAliasEnabled());
        panel.add(lblProgress, "split 2");
        panel.add(lblETA);
        panel.add(prgTotal);
        panel.add(lblHelp, "hidemode 3");

        this.setLayout(new MigLayout("ins 0", "[grow,fill]"));
        this.add(panel);

        this.pack();
    }

    public void showDialog() {
        if (isVisible()) return;

        if (updater != null) updater.interrupt();
        updater = new InfoUpdater();
        updater.start();

        setVisible(true);
    }

    public void hideDialog() {
        if (!isVisible()) return;

        GUIUtils.saveLastLocation(this);
        dispose();
    }

    public void setEnableDocking(boolean enableDocking) {
        this.enableDocking = enableDocking;
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

    private void updateInfos() {
        ds.updateInformations();

        lblProgress.setText(Formatter.formatFilesize(ds.getCurrentDownloadSize(), 0) + " / " + Formatter.formatFilesize(ds.getTotalDownloadSize(), 0));
        lblETA.setText(Formatter.formatSeconds(ds.getETA()));

        long totalDl = ds.getTotalDownloadSize();
        long curDl = ds.getCurrentDownloadSize();
        prgTotal.setString(ds.getPercent() + "%");
        prgTotal.setMaximum(totalDl);
        prgTotal.setValue(curDl);
    }

    private final class InfoUpdater extends Thread implements Runnable {

        @Override
        public void run() {
            while (isVisible()) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        updateInfos();
                    }

                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    interrupt();
                }
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            JMenuItem mi = new JMenuItem(T._.jd_plugins_optional_infobar_InfoDialog_hideWindow());
            mi.setIcon(NewTheme.I().getIcon("close", -1));
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
        if (enableDocking) {
            Point drag = e.getPoint();

            /*
             * Convert coordinate of the component to the whole screen and translate to the dragging-start-point.
             */
            Point point = new Point(this.point);
            System.out.println("1   " + point);
            SwingUtilities.convertPointToScreen(point, e.getComponent());
            SwingUtilities.convertPointFromScreen(point, this);
            System.out.println(point);
            SwingUtilities.convertPointToScreen(drag, e.getComponent());
            // drag.translate(-point.x, -point.y);

            int x = drag.x;
            int y = drag.y;
            int w = getWidth();
            int h = getHeight();

            /*
             * If distance to the upper and left screen border is less than DOCKING_DISTANCE, then dock the InfoDialog to the border.
             */
            if (x < DOCKING_DISTANCE) x = 0;
            if (y < DOCKING_DISTANCE) y = 0;

            DisplayMode dm = getGraphicsConfiguration().getDevice().getDisplayMode();
            int xMax = dm.getWidth() - w;
            int yMax = dm.getHeight() - h;

            /*
             * If distance to the lower and right screen border is less than DOCKING_DISTANCE, then dock the InfoDialog to the border.
             */
            if (x > xMax - DOCKING_DISTANCE) x = xMax;
            if (y > yMax - DOCKING_DISTANCE) y = yMax;

            /*
             * Finally set the new location.
             */
            this.setLocation(x - point.x, y - point.y);
        } else {
            Point window = this.getLocation();

            int x = window.x + e.getPoint().x - point.x;
            int y = window.y + e.getPoint().y - point.y;

            this.setLocation(x, y);
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void actionPerformed(ActionEvent e) {
        action.setSelected(false);
        action.actionPerformed(new ActionEvent(action, e.getID(), e.getActionCommand()));
    }

}