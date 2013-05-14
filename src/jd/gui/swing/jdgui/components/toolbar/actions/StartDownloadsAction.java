package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.UserIF.Panels;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAction;
import org.jdownloader.translate._JDT;

public class StartDownloadsAction extends ToolBarAction implements DownloadWatchdogListener {

    /**
     * Create a new instance of StartDownloadsAction. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    public StartDownloadsAction(SelectionInfo<?, ?> selection) {
        setIconKey("media-playback-start");
        DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
        DownloadWatchDog.getInstance().notifyCurrentState(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (JDGui.getInstance().isCurrentPanel(Panels.LINKGRABBER)) {
            IOEQ.add(new Runnable() {
                public void run() {
                    java.util.List<AbstractNode> packages = new ArrayList<AbstractNode>(LinkGrabberTableModel.getInstance().getAllPackageNodes());
                    ConfirmAction ca = new ConfirmAction(new SelectionInfo<CrawledPackage, CrawledLink>(packages).setShiftDown(true));
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
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(true);
            }
        };

    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(false);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(true);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

}
