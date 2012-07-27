package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.UserIF.Panels;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAction;
import org.jdownloader.translate._JDT;

public class StartDownloadsAction extends AbstractToolbarAction {
    private static final StartDownloadsAction INSTANCE = new StartDownloadsAction();

    /**
     * get the only existing instance of StartDownloadsAction. This is a singleton
     * 
     * @return
     */
    public static StartDownloadsAction getInstance() {
        return StartDownloadsAction.INSTANCE;
    }

    /**
     * Create a new instance of StartDownloadsAction. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private StartDownloadsAction() {

    }

    public void actionPerformed(ActionEvent e) {
        if (JDGui.getInstance().isCurrentPanel(Panels.LINKGRABBER)) {
            IOEQ.add(new Runnable() {
                public void run() {
                    ArrayList<AbstractNode> packages = new ArrayList<AbstractNode>(LinkGrabberTableModel.getInstance().getAllPackageNodes());
                    ConfirmAction ca = new ConfirmAction(true, new SelectionInfo<CrawledPackage, CrawledLink>(packages));
                    ca.setAutostart(true);
                    ca.actionPerformed(null);
                }
            }, true);
        } else {
            DownloadWatchDog.getInstance().startDownloads();
        }
    }

    @Override
    public String createTooltip() {
        return _JDT._.StartDownloadsAction_createTooltip_();
    }

    @Override
    protected String createAccelerator() {
        return ShortcutController._.getStartDownloadsAction();
    }

    @Override
    public String createIconKey() {
        return "media-playback-start";
    }

    @Override
    protected void doInit() {
        DownloadWatchDog.getInstance().getStateMachine().addListener(new StateEventListener() {

            public void onStateUpdate(StateEvent event) {
            }

            public void onStateChange(StateEvent event) {
                if (DownloadWatchDog.IDLE_STATE == event.getNewState() || DownloadWatchDog.STOPPED_STATE == event.getNewState()) {
                    setEnabled(true);
                } else if (DownloadWatchDog.RUNNING_STATE == event.getNewState()) {
                    setEnabled(false);
                }
            }
        });
    }

}
