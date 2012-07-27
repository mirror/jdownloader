package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GeneralSettings;

public class PauseDownloadsAction extends AbstractToolbarAction {
    private static final PauseDownloadsAction INSTANCE = new PauseDownloadsAction();

    /**
     * get the only existing instance of PauseDownloadsAction. This is a singleton
     * 
     * @return
     */
    public static PauseDownloadsAction getInstance() {
        return PauseDownloadsAction.INSTANCE;
    }

    /**
     * Create a new instance of PauseDownloadsAction. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private PauseDownloadsAction() {

    }

    public void actionPerformed(ActionEvent e) {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(isSelected());
    }

    @Override
    public String createIconKey() {
        return "media-playback-pause";
    }

    @Override
    protected String createAccelerator() {
        return ShortcutController._.getPauseDownloadsToggleAction();
    }

    @Override
    public String createTooltip() {
        return _GUI._.action_pause_tooltip();
    }

    @Override
    protected void doInit() {

        this.setEnabled(false);
        setSelected(false);
        setTooltipText(_GUI._.gui_menu_action_break2_desc(JsonConfig.create(GeneralSettings.class).getPauseSpeed()));

        DownloadWatchDog.getInstance().getStateMachine().addListener(new StateEventListener() {

            public void onStateUpdate(StateEvent event) {
            }

            public void onStateChange(StateEvent event) {
                if (DownloadWatchDog.IDLE_STATE == event.getNewState() || DownloadWatchDog.STOPPED_STATE == event.getNewState()) {
                    setEnabled(false);
                    setSelected(false);
                } else if (DownloadWatchDog.RUNNING_STATE == event.getNewState()) {
                    setEnabled(true);
                    setSelected(false);
                } else if (DownloadWatchDog.PAUSE_STATE == event.getNewState()) {
                    setEnabled(true);
                    setSelected(true);
                }
            }
        });

        org.jdownloader.settings.staticreferences.CFG_GENERAL.PAUSE_SPEED.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        setTooltipText(_GUI._.gui_menu_action_break2_desc(org.jdownloader.settings.staticreferences.CFG_GENERAL.PAUSE_SPEED.getValue()));

                    }
                };
            }

        });
    }

}
