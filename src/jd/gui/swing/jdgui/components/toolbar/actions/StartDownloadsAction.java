package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogJob;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkcollector.LinkCollector.MoveLinksMode;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.JDGui.Panels;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.StartButtonAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.translate._JDT;

public class StartDownloadsAction extends AbstractToolBarAction implements DownloadWatchdogListener, GUIListener, GenericConfigEventListener<Enum>, ActionContext {

    private static final String ICON_KEY = "media-playback-start";

    /**
     * Create a new instance of StartDownloadsAction. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    public StartDownloadsAction() {

        setName(_JDT._.StartDownloadsAction_createTooltip_());
        DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
        DownloadWatchDog.getInstance().notifyCurrentState(this);
        CFG_GUI.START_BUTTON_ACTION_IN_LINKGRABBER_CONTEXT.getEventSender().addListener(this, true);
        GUIEventSender.getInstance().addListener(this, true);
        onGuiMainTabSwitch(null, MainTabbedPane.getInstance().getSelectedView());

        setAccelerator(KeyEvent.VK_S);
        updateEnableState();

    }

    @Override
    public void initContextDefaults() {
        setHideIfDownloadsAreRunning(false);

    }

    @Override
    public void onKeyModifier(int parameter) {
    }

    public void actionPerformed(final ActionEvent e) {
        if (JDGui.getInstance().isCurrentPanel(Panels.LINKGRABBER)) {
            final SelectionInfo<CrawledPackage, CrawledLink> selection = LinkGrabberTable.getInstance().getSelectionInfo(false, true);
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    switch (CFG_GUI.CFG.getStartButtonActionInLinkgrabberContext()) {
                    case ADD_ALL_LINKS_AND_START_DOWNLOADS:
                        ConfirmLinksContextAction.confirmSelection(MoveLinksMode.MANUAL, selection, true, false, true, null, BooleanStatus.FALSE, CFG_LINKGRABBER.CFG.getDefaultOnAddedOfflineLinksAction(), CFG_LINKGRABBER.CFG.getDefaultOnAddedDupesLinksAction());
                        break;
                    case START_DOWNLOADS_ONLY:
                        DownloadWatchDog.getInstance().startDownloads();
                        break;
                    }
                    return null;
                }
            });
        } else {
            DownloadWatchDog.getInstance().startDownloads();

        }
        if (badge != null) {
            DownloadWatchDog.getInstance().enqueueJob(new DownloadWatchDogJob() {

                @Override
                public void interrupt() {
                }

                @Override
                public void execute(DownloadSession currentSession) {
                    currentSession.setForcedOnlyModeEnabled(false);
                    updateEnableState();
                }
            });
        }
    }

    @Override
    public String createTooltip() {
        if (badge != null) {
            return _JDT._.StartDownloadsAction_forced_createTooltip_();
        }
        return _JDT._.StartDownloadsAction_createTooltip_();
    }

    private boolean            hideIfDownloadsAreRunning     = false;

    public static final String HIDE_IF_DOWNLOADS_ARE_RUNNING = "HideIfDownloadsAreRunning";

    public static String getHideIfDownloadsAreRunningTranslation() {
        return _JDT._.StartDownloadsAction_getHideIfDownloadsAreRunningTranslation_();
    }

    @Customizer(link = "#getHideIfDownloadsAreRunningTranslation")
    public boolean isHideIfDownloadsAreRunning() {
        return hideIfDownloadsAreRunning;
    }

    public void setHideIfDownloadsAreRunning(boolean showIfDownloadsAreRunning) {
        this.hideIfDownloadsAreRunning = showIfDownloadsAreRunning;

        updateEnableState();

    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
        updateEnableState();

    }

    @Override
    public String getIconKey() {
        return null;
    }

    private String        badge        = null;
    private static String BADGE_FORCED = "prio_3_clear";

    private void updateEnableState() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                DownloadSession session = DownloadWatchDog.getInstance().getSession();
                boolean enable = (!DownloadWatchDog.getInstance().isRunning());
                // putValue(Action.SMALL_ICON, null);
                // putValue(Action.LARGE_ICON_KEY, null);
                String newBadge = null;

                if (session != null && DownloadWatchDog.getInstance().isRunning()) {
                    enable |= session.isForcedOnlyModeEnabled();
                    if (session.isForcedOnlyModeEnabled()) {

                        newBadge = BADGE_FORCED;
                    }
                }
                if (!StringUtils.equals(newBadge, badge)) {
                    // fire Icon changed
                    badge = newBadge;
                    firePropertyChange(Action.SMALL_ICON, new Object(), new Object());
                    firePropertyChange(Action.LARGE_ICON_KEY, new Object(), new Object());
                }
                if (badge != null) {
                    setTooltipText(_JDT._.StartDownloadsAction_forced_createTooltip_());

                } else {
                    setTooltipText(_JDT._.StartDownloadsAction_createTooltip_());
                }

                View view = JDGui.getInstance().getMainTabbedPane().getSelectedView();

                if (enable && view instanceof LinkGrabberView) {
                    if (CFG_GUI.CFG.getStartButtonActionInLinkgrabberContext() == StartButtonAction.DISABLED) {
                        enable = false;
                    }
                }
                setEnabled(enable);
                if (isHideIfDownloadsAreRunning()) {
                    setVisible(enable);

                }
            }
        };
    }

    /**
     * @return
     */
    protected Icon getSmallIconForToolbar() {

        if (badge == null) {
            return new AbstractIcon(ICON_KEY, 18);
        } else {
            return new BadgeIcon(ICON_KEY, badge, 18, 10, 0, 0);
        }

    }

    /**
     * @return
     */
    protected Icon getLargeIconForToolbar() {
        if (badge == null) {
            return new AbstractIcon(ICON_KEY, 24);
        } else {
            return new BadgeIcon(ICON_KEY, badge, 24, 12, 0, 0);
        }
    }

    @Override
    public Object getValue(String key) {

        return super.getValue(key);
    }

    @Override
    public AbstractButton createButton() {
        ExtButton bt = new ExtButton(this);

        bt.setHideActionText(true);
        return bt;
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {

    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        updateEnableState();
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        updateEnableState();
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, final View newView) {

        DownloadWatchDog.getInstance().notifyCurrentState(this);

    }

    @Override
    public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
        onGuiMainTabSwitch(null, MainTabbedPane.getInstance().getSelectedView());
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
    }
}
