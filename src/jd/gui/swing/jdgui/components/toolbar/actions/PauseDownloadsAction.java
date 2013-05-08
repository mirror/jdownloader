package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;

import org.appwork.controlling.State;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.settings.GeneralSettings;

public class PauseDownloadsAction extends ToolBarAction implements DownloadWatchdogListener {

    public PauseDownloadsAction(SelectionInfo<?, ?> selection) {
        setIconKey("media-playback-pause");

        this.setEnabled(false);

        setTooltipText(_GUI._.gui_menu_action_break2_desc(JsonConfig.create(GeneralSettings.class).getPauseSpeed()));

        DownloadWatchDog.getInstance().getEventSender().addListener(this, true);

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

        }, true);
        State newState = DownloadWatchDog.getInstance().getStateMachine().getState();
        if (DownloadWatchDog.IDLE_STATE == newState || DownloadWatchDog.STOPPED_STATE == newState) {
            onDownloadWatchdogStateIsIdle();
        } else if (DownloadWatchDog.RUNNING_STATE == newState) {
            onDownloadWatchdogStateIsRunning();
        } else if (DownloadWatchDog.PAUSE_STATE == newState) {
            onDownloadWatchdogStateIsPause();
        }

    }

    public void actionPerformed(ActionEvent e) {
        boolean isPaused = DownloadWatchDog.getInstance().getStateMachine().getState() == DownloadWatchDog.PAUSE_STATE;
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(!isPaused);
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
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(false);
                setSelected(false);
            }
        };

    }

    @Override
    public void onDownloadWatchdogStateIsPause() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(true);
                setSelected(true);
            }
        };

    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(true);
                setSelected(false);
            }
        };

    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(false);
                setSelected(false);
            }
        };

    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

}
