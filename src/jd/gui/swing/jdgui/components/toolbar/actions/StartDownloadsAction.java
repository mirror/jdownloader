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
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.StartButtonAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.translate._JDT;

public class StartDownloadsAction extends AbstractToolBarAction implements DownloadWatchdogListener, GUIListener, GenericConfigEventListener<Enum>, ActionContext {

    /**
     * Create a new instance of StartDownloadsAction. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    public StartDownloadsAction() {
        setName(_JDT.T.StartDownloadsAction_createTooltip_());
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
        DownloadSession session = DownloadWatchDog.getInstance().getSession();

        if (session != null && session.isForcedOnlyModeEnabled()) {
            DownloadWatchDog.getInstance().enqueueJob(new DownloadWatchDogJob() {

                @Override
                public void interrupt() {
                }

                @Override
                public void execute(DownloadSession currentSession) {
                    currentSession.setForcedOnlyModeEnabled(false);
                    updateEnableState();
                }

                @Override
                public boolean isHighPriority() {
                    return true;
                }
            });
        }
    }

    @Override
    public String createTooltip() {
        final DownloadSession session = DownloadWatchDog.getInstance().getSession();
        if (session != null && session.isForcedOnlyModeEnabled()) {
            return _JDT.T.StartDownloadsAction_forced_createTooltip_();
        }
        return _JDT.T.StartDownloadsAction_createTooltip_();
    }

    private boolean            hideIfDownloadsAreRunning     = false;

    public static final String HIDE_IF_DOWNLOADS_ARE_RUNNING = "HideIfDownloadsAreRunning";

    public static String getHideIfDownloadsAreRunningTranslation() {
        return _JDT.T.StartDownloadsAction_getHideIfDownloadsAreRunningTranslation_();
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

    private void updateEnableState() {
        DownloadWatchDog.getInstance().enqueueJob(new DownloadWatchDogJob() {

            @Override
            public boolean isHighPriority() {
                return false;
            }

            @Override
            public void interrupt() {
            }

            @Override
            public void execute(final DownloadSession session) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        final boolean isRunning = DownloadWatchDog.getInstance().isRunning();
                        boolean enable = (!isRunning);
                        Icon newSmall = normalSmall;
                        if (session != null && isRunning) {
                            enable |= session.isForcedOnlyModeEnabled();
                            if (session.isForcedOnlyModeEnabled()) {
                                newSmall = forcedSmall;
                            }
                        }
                        if (newSmall != smallIcon) {
                            if (newSmall == forcedSmall) {
                                smallIcon = forcedSmall;
                                largeIcon = forcedLarge;
                            } else {
                                smallIcon = normalSmall;
                                largeIcon = normalLarge;
                            }
                            firePropertyChange(Action.SMALL_ICON, new Object(), new Object());
                            firePropertyChange(Action.LARGE_ICON_KEY, new Object(), new Object());
                        }
                        if (smallIcon == forcedSmall) {
                            setTooltipText(_JDT.T.StartDownloadsAction_forced_createTooltip_());

                        } else {
                            setTooltipText(_JDT.T.StartDownloadsAction_createTooltip_());
                        }
                        final View view = JDGui.getInstance().getMainTabbedPane().getSelectedView();
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
        });

    }

    private final Icon normalSmall = new AbstractIcon(IconKey.ICON_MEDIA_PLAYBACK_START, 18);
    private final Icon forcedSmall = new AbstractIcon(IconKey.ICON_PLAY_BREAKUP_FORCED_ONLY, 18); ;
    private final Icon normalLarge = new AbstractIcon(IconKey.ICON_MEDIA_PLAYBACK_START, 24);
    private final Icon forcedLarge = new AbstractIcon(IconKey.ICON_PLAY_BREAKUP_FORCED_ONLY, 24); ;
    private Icon       smallIcon   = normalSmall;
    private Icon       largeIcon   = normalLarge;

    /**
     * @return
     */
    protected Icon getSmallIconForToolbar() {
        return smallIcon;
    }

    /**
     * @return
     */
    protected Icon getLargeIconForToolbar() {
        return largeIcon;
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
