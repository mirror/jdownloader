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
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAction;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.StartButtonAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.translate._JDT;

public class StartDownloadsAction extends ToolBarAction implements DownloadWatchdogListener, GUIListener, GenericConfigEventListener<Enum> {

    /**
     * Create a new instance of StartDownloadsAction. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    public StartDownloadsAction(SelectionInfo<?, ?> selection) {
        setIconKey("media-playback-start");
        setName(_JDT._.StartDownloadsAction_createTooltip_());
        DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
        DownloadWatchDog.getInstance().notifyCurrentState(this);
        CFG_GUI.START_BUTTON_ACTION_IN_LINKGRABBER_CONTEXT.getEventSender().addListener(this, true);
        GUIEventSender.getInstance().addListener(this, true);
        onGuiMainTabSwitch(null, MainTabbedPane.getInstance().getSelectedView());
    }

    public void actionPerformed(ActionEvent e) {
        if (JDGui.getInstance().isCurrentPanel(Panels.LINKGRABBER)) {
            IOEQ.add(new Runnable() {
                public void run() {
                    switch (CFG_GUI.CFG.getStartButtonActionInLinkgrabberContext()) {
                    case ADD_ALL_LINKS_AND_START_DOWNLOADS:
                        java.util.List<AbstractNode> packages = new ArrayList<AbstractNode>(LinkGrabberTableModel.getInstance().getAllPackageNodes());
                        ConfirmAction ca = new ConfirmAction(new SelectionInfo<CrawledPackage, CrawledLink>(packages).setShiftDown(true));
                        ca.setAutostart(true);
                        ca.actionPerformed(null);
                        break;

                    case START_DOWNLOADS_ONLY:
                        DownloadWatchDog.getInstance().startDownloads();
                        break;

                    case DISABLED:
                        return;
                    }

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

    @Override
    public void onGuiMainTabSwitch(View oldView, final View newView) {
        if (newView instanceof LinkGrabberView && CFG_GUI.CFG.getStartButtonActionInLinkgrabberContext() == StartButtonAction.DISABLED) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    setEnabled(false);
                }
            };
        } else {
            DownloadWatchDog.getInstance().notifyCurrentState(this);
        }
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
        onGuiMainTabSwitch(null, MainTabbedPane.getInstance().getSelectedView());
    }
}
