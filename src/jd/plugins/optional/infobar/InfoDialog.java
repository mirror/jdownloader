package jd.plugins.optional.infobar;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import jd.controlling.DownloadController;
import jd.controlling.DownloadInformations;
import jd.gui.swing.components.SpeedMeterPanel;
import jd.gui.swing.jdgui.components.JDProgressBar;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.userio.DummyFrame;
import jd.nutils.Formatter;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class InfoDialog extends JDialog {

    private static InfoDialog INSTANCE = null;

    public static InfoDialog getInstance(final MenuAction action) {
        if (INSTANCE == null) INSTANCE = new InfoDialog(action);
        return INSTANCE;
    }

    private static final long serialVersionUID = 4715904261105562064L;

    private final DownloadInformations ds;

    private InfoUpdater updater = null;

    private JDProgressBar prgTotal;
    private JLabel lblProgress;
    private JLabel lblETA;

    private InfoDialog(final MenuAction action) {
        super(DummyFrame.getDialogParent());

        ds = new DownloadInformations();

        this.setTitle(JDL.L("jd.plugins.optional.infobar.InfoDialog.title", "QuickInfo"));
        this.setAlwaysOnTop(true);
        this.setModal(false);
        this.setResizable(false);

        if (action != null) {
            addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    action.setSelected(false);
                }

            });
        }

        initGui();
    }

    private void initGui() {
        prgTotal = new JDProgressBar();
        prgTotal.setStringPainted(true);

        this.setLayout(new MigLayout("ins 5, wrap 2", "[][grow,fill]"));
        this.add(new SpeedMeterPanel(false, true), "h 30!, w 300!, spanx");
        this.add(new JLabel(JDL.L("plugins.optional.trayIcon.progress", "Progress:")));
        this.add(lblProgress = new JLabel(""));
        this.add(prgTotal, "spanx, growx");
        this.add(new JLabel(JDL.L("plugins.optional.trayIcon.eta", "ETA:")));
        this.add(lblETA = new JLabel(""));
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

        dispose();
    }

    private void updateInfos() {
        DownloadController.getInstance().getDownloadStatus(ds);

        lblProgress.setText(Formatter.formatFilesize(ds.getCurrentDownloadSize(), 0) + " / " + Formatter.formatFilesize(ds.getTotalDownloadSize(), 0));

        long totalDl = ds.getTotalDownloadSize();
        long curDl = ds.getCurrentDownloadSize();
        prgTotal.setString(Math.round((curDl * 10000.0) / totalDl) / 100.0 + "%");
        prgTotal.setMaximum(totalDl);
        prgTotal.setValue(curDl);

        lblETA.setText(Formatter.formatSeconds(ds.getETA()));
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
                    interrupt();
                }
            }
        }

    }

}
