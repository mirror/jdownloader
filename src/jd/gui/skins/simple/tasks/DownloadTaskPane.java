package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.Timer;

import jd.controlling.DownloadController;
import jd.gui.skins.simple.components.DownloadView.JDProgressBar;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class DownloadTaskPane extends TaskPanel implements ActionListener {

    private static final long serialVersionUID = -9134449913836967453L;
    public static final int ACTION_SHOW_PANEL = 1;
    public static final int ACTION_STARTSTOP = 2;

    private JLabel packages;
    private JLabel downloadlinks;
    private JLabel totalsize;
    private JDProgressBar progress;
    private JLabel speed;
    private JLabel eta;
    private JLabel downloadlist;
    private JLabel progresslabel;
    private Timer fadeTimer;

    public DownloadTaskPane(String string, ImageIcon ii) {
        super(string, ii, "downloadtask");
        initGUI();

        fadeTimer = new Timer(2000, this);
        fadeTimer.setInitialDelay(0);
        fadeTimer.start();
    }

    /**
     * TODO: soll mal über events aktuallisiert werden
     */
    private void update() {
        DownloadController dlc = JDUtilities.getDownloadController();
        packages.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.packages", "%s Packages", dlc.getPackages().size()));
        downloadlinks.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Links", dlc.getAllDownloadLinks().size()));
        long tot = 0;
        long loaded = 0;
        for (DownloadLink l : dlc.getAllDownloadLinks()) {
            if (!l.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS) && l.isEnabled()) {
                tot += l.getDownloadSize();
                loaded += l.getDownloadCurrent();
            }
        }

        totalsize.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", Formatter.formatReadable(tot)));
        progress.setMaximum(tot);
        progress.setValue(loaded);
        progress.setToolTipText(Math.round((loaded * 10000.0) / tot) / 100.0 + "%");
        long speedm = JDUtilities.getController().getSpeedMeter();
        if (speedm > 1024) {
            speed.setText(JDLocale.LF("gui.taskpanes.download.progress.speed", "Speed: %s", Formatter.formatReadable(speedm) + "/s"));
            long etanum = (tot - loaded) / speedm;
            eta.setText(JDLocale.LF("gui.taskpanes.download.progress.eta", "ETA: %s", Formatter.formatSeconds(etanum)));
        } else {
            eta.setText("");
            speed.setText("");
        }
    }

    private void initGUI() {

        downloadlist = new JLabel(JDLocale.L("gui.taskpanes.download.downloadlist", "Downloadlist"));
        downloadlist.setIcon(JDTheme.II("gui.splash.dllist", 16, 16));
        packages = new JLabel(JDLocale.LF("gui.taskpanes.download.downloadlist.packages", "%s Package(s)", 0));
        downloadlinks = new JLabel(JDLocale.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Link(s)", 0));
        totalsize = new JLabel(JDLocale.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", 0));
        progresslabel = new JLabel(JDLocale.L("gui.taskpanes.download.progress", "Total progress"));
        progresslabel.setIcon(JDTheme.II("gui.images.progress", 16, 16));
        progress = new JDProgressBar();
        progress.setStringPainted(false);
        speed = new JLabel(JDLocale.LF("gui.taskpanes.download.progress.speed", "Speed: %s", 0));
        eta = new JLabel(JDLocale.LF("gui.taskpanes.download.progress.eta", "ETA: %s", 0));

        add(downloadlist, D1_LABEL_ICON);
        add(packages, D2_LABEL);
        add(downloadlinks, D2_LABEL);
        add(totalsize, D2_LABEL);
        add(progresslabel, D1_LABEL_ICON);
        add(progress, D2_PROGRESSBAR);
        add(speed, D2_LABEL);
        add(eta, D2_LABEL);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == fadeTimer) {
            if (!this.isCollapsed()) update();
        }
    }

}
