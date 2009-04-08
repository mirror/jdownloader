package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.Timer;

import jd.controlling.JDDownloadController;
import jd.gui.skins.simple.components.DownloadView.JDProgressBar;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class DownloadTaskPane extends TaskPanel implements ActionListener {

    private static final long serialVersionUID = -9134449913836967453L;
    public static final int ACTION_SHOW_PANEL = 1;
    public static final int ACTION_STARTSTOP = 2;

    private JButton startStop;
    private JLabel packages;
    private JLabel downloadlinks;
    private JLabel totalsize;
    private JDProgressBar progress;
    private JLabel speed;
    private JLabel eta;
    private JLabel downloadlist;
    private JLabel progresslabel;

    public DownloadTaskPane(String string, ImageIcon ii) {
        super(string, ii, "downloadtask");
        initGUI();

        Timer fadeTimer = new Timer(2000, this);
        fadeTimer.setInitialDelay(0);
        fadeTimer.start();
    }

    /**
     * TODO
     */
    private void update() {
        JDDownloadController dlc = JDUtilities.getDownloadController();
        packages.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.packages", "%s Packages", dlc.getPackages().size()));
        downloadlinks.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Links", dlc.getAllDownloadLinks().size()));
        long tot = 0;
        long loaded = 0;
        for (DownloadLink l : dlc.getAllDownloadLinks()) {
            tot += l.getDownloadSize();
            loaded += l.getDownloadCurrent();
        }

        totalsize.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", JDUtilities.formatKbReadable(tot / 1024)));
        progress.setMaximum(tot);
        progress.setValue(loaded);

        if (JDUtilities.getController().getSpeedMeter() > 1024) {
            speed.setText(JDLocale.LF("gui.taskpanes.download.progress.speed", "Speed: %s", JDUtilities.formatBytesToMB(JDUtilities.getController().getSpeedMeter()) + "/s"));

            long etanum = (tot - loaded) / JDUtilities.getController().getSpeedMeter();

            eta.setText(JDLocale.LF("gui.taskpanes.download.progress.eta", "ETA: %s", JDUtilities.formatSeconds(etanum)));
        } else {
            eta.setText("");
            speed.setText("");
        }
    }

    private void initGUI() {

        downloadlist = (new JLabel(JDLocale.L("gui.taskpanes.download.downloadlist", "Downloadlist")));
        downloadlist.setIcon(JDTheme.II("gui.splash.dllist", 16, 16));
        packages = (new JLabel(JDLocale.LF("gui.taskpanes.download.downloadlist.packages", "%s Package(s)", 0)));
        downloadlinks = (new JLabel(JDLocale.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Link(s)", 0)));
        totalsize = (new JLabel(JDLocale.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", 0)));
        progresslabel = (new JLabel(JDLocale.L("gui.taskpanes.download.progress", "Total progress")));
        progresslabel.setIcon(JDTheme.II("gui.images.progress", 16, 16));
        progress = (new JDProgressBar());
        progress.setStringPainted(false);
        speed = (new JLabel(JDLocale.LF("gui.taskpanes.download.progress.speed", "Speed: %s", 0)));

        eta = (new JLabel(JDLocale.LF("gui.taskpanes.download.progress.eta", "ETA: %s", 0)));

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

        if (e.getSource() == startStop) {
            this.broadcastEvent(new ActionEvent(this, ACTION_STARTSTOP, ((JButton) e.getSource()).getName()));
            return;
        } else {
            update();
        }

    }

}
