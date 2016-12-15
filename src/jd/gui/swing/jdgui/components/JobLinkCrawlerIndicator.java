package jd.gui.swing.jdgui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.Timer;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinkOrigin;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class JobLinkCrawlerIndicator extends IconedProcessIndicator implements ActionListener {

    /**
     *
     */
    private static final long    serialVersionUID = -7267364376253248300L;
    private final JobLinkCrawler jobLinkCrawler;
    private final StatusBarImpl  statusBar;
    private final Timer          timer;

    private static String getIconKey(LinkCollectingJob job) {
        final LinkOrigin origin = job.getOrigin().getOrigin();
        if (origin != null) {
            switch (origin) {
            case CLIPBOARD:
                return IconKey.ICON_CLIPBOARD;
            case ADD_CONTAINER_ACTION:
            case DOWNLOADED_CONTAINER:
                return IconKey.ICON_ADDCONTAINER;
            case CNL:
                return IconKey.ICON_LOGO_CNL;
            case MYJD:
                return IconKey.ICON_LOGO_MYJDOWNLOADER;
            case ADD_LINKS_DIALOG:
            case DRAG_DROP_ACTION:
                return IconKey.ICON_ADD;
            default:
                return IconKey.ICON_LINKGRABBER;
            }
        } else {
            return IconKey.ICON_LINKGRABBER;
        }
    }

    public JobLinkCrawlerIndicator(final StatusBarImpl statusBar, final JobLinkCrawler jobLinkCrawler) {
        super(new AbstractIcon(getIconKey(jobLinkCrawler.getJob()), 16));
        this.statusBar = statusBar;
        this.jobLinkCrawler = jobLinkCrawler;
        setTitle(_GUI.T.StatusBarImpl_initGUI_linkgrabber());
        setDescription(_GUI.T.StatusBarImpl_initGUI_linkgrabber_desc_inactive());
        setEnabled(true);
        timer = new Timer(1000, this);
        if (jobLinkCrawler.isRunning()) {
            statusBar.addProcessIndicator(this);
            timer.start();
        }
    }

    private final int countDownMax = 5;

    private int       countDown    = countDownMax;

    private void update() {
        if (jobLinkCrawler.isRunning()) {
            countDown = countDownMax;
            if (!isIndeterminate()) {
                setIndeterminate(true);
            }
            setDescription(_GUI.T.LinkCrawlerBubbleContent_update_runnning());
        } else if (jobLinkCrawler.getLinkChecker().isRunning()) {
            countDown = countDownMax;
            if (!isIndeterminate()) {
                setIndeterminate(true);
            }
            setDescription(_GUI.T.LinkCrawlerBubbleContent_update_online());
        } else if (jobLinkCrawler.hasWaitingInQueue()) {
            countDown = countDownMax;
            if (!isIndeterminate()) {
                setIndeterminate(true);
            }
            setDescription(_GUI.T.LinkCrawlerBubbleContent_update_processing());
        } else {
            if (isIndeterminate()) {
                setIndeterminate(false);
            }
            setDescription(_GUI.T.StatusBarImpl_initGUI_linkgrabber_desc_inactive());
            if (countDown-- == 0) {
                timer.stop();
                statusBar.removeProcessIndicator(this);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            final JPopupMenu popup = new JPopupMenu();
            popup.add(new AppAction() {

                private static final long serialVersionUID = -968768342263254431L;
                {
                    this.setIconKey(IconKey.ICON_CANCEL);
                    this.setName(_GUI.T.StatusBarImpl_initGUI_abort_linkgrabber());
                    this.setEnabled(jobLinkCrawler.isCollecting());
                }

                public void actionPerformed(ActionEvent e) {
                    jobLinkCrawler.abort();
                }

            });
            popup.addSeparator();
            popup.add(new AppAction() {
                private static final long serialVersionUID = -968768342263254431L;
                {
                    this.setIconKey(IconKey.ICON_CANCEL);
                    this.setName(_GUI.T.StatusBarImpl_initGUI_abort_linkgrabber_all());
                    this.setEnabled(LinkCollector.getInstance().isCollecting());
                }

                public void actionPerformed(ActionEvent e) {
                    LinkCollector.getInstance().abort();
                }

            });
            popup.show(JobLinkCrawlerIndicator.this, e.getPoint().x, 0 - popup.getPreferredSize().height);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
    }

}
