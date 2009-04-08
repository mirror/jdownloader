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
import net.miginfocom.swing.MigLayout;

public class DownloadTaskPane extends TaskPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -9134449913836967453L;
    public static final int ACTION_SHOW_PANEL = 1;
    public static final int ACTION_STARTSTOP = 2;
    private static final String LEFTGAP = "gapleft 20";

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
        this.setLayout(new MigLayout("ins 5, wrap 1", "[fill,grow,null:null:150]", "[]0[]0[]0[]0[]"));
        initGUI();

        Timer fadeTimer = new Timer(1000, this);
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

        add(downloadlist = new JLabel(JDLocale.L("gui.taskpanes.download.downloadlist", "Downloadlist")),"gapbottom 5");
        downloadlist.setIcon(JDTheme.II("gui.splash.dllist", 16, 16));
        add(packages = new JLabel(JDLocale.LF("gui.taskpanes.download.downloadlist.packages", "%s Package(s)", 0)), LEFTGAP+",gapbottom 2");
        add(downloadlinks = new JLabel(JDLocale.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Link(s)", 0)), LEFTGAP+",gapbottom 2");
        add(totalsize = new JLabel(JDLocale.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", 0)), LEFTGAP+",gapbottom 7");
        add(progresslabel = new JLabel(JDLocale.L("gui.taskpanes.download.progress", "Total progress")),"gapbottom 5");
        progresslabel.setIcon(JDTheme.II("gui.images.progress", 16, 16));
        add(progress = new JDProgressBar(), LEFTGAP + ", height 12!,gapbottom 2");
        progress.setStringPainted(false);
        add(speed = new JLabel(JDLocale.LF("gui.taskpanes.download.progress.speed", "Speed: %s", 0)), LEFTGAP+",gapbottom 2");

        add(eta = new JLabel(JDLocale.LF("gui.taskpanes.download.progress.eta", "ETA: %s", 0)), LEFTGAP+",gapbottom 2");

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
