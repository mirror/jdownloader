package org.jdownloader.extensions.schedulerV2.helpers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.extensions.schedulerV2.CFG_SCHEDULER;
import org.jdownloader.extensions.schedulerV2.actions.AbstractScheduleAction;
import org.jdownloader.extensions.schedulerV2.actions.AddAllDownloadsAction;
import org.jdownloader.extensions.schedulerV2.actions.CaptchaServiceAction;
import org.jdownloader.extensions.schedulerV2.actions.DebugAction;
import org.jdownloader.extensions.schedulerV2.actions.DisableAccountAction;
import org.jdownloader.extensions.schedulerV2.actions.DisableReconnectAction;
import org.jdownloader.extensions.schedulerV2.actions.DisableSpeedLimitAction;
import org.jdownloader.extensions.schedulerV2.actions.EnableAccountAction;
import org.jdownloader.extensions.schedulerV2.actions.EnableReconnectAction;
import org.jdownloader.extensions.schedulerV2.actions.PauseDownloadAction;
import org.jdownloader.extensions.schedulerV2.actions.ReconnectAction;
import org.jdownloader.extensions.schedulerV2.actions.RestartJDownloaderAction;
import org.jdownloader.extensions.schedulerV2.actions.SetChunksAction;
import org.jdownloader.extensions.schedulerV2.actions.SetConnectionsAction;
import org.jdownloader.extensions.schedulerV2.actions.SetDownloadspeedAction;
import org.jdownloader.extensions.schedulerV2.actions.SetStopMarkAction;
import org.jdownloader.extensions.schedulerV2.actions.StartDownloadAction;
import org.jdownloader.extensions.schedulerV2.actions.StopDownloadAction;
import org.jdownloader.extensions.schedulerV2.actions.UnpauseDownloadAction;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;
import org.jdownloader.extensions.schedulerV2.translate.T;

public class ActionHelper {
    public static final List<AbstractScheduleAction> ACTIONS = Collections.unmodifiableList(new ArrayList<AbstractScheduleAction>() {
        {
            if (CFG_SCHEDULER.CFG.isDebugMode()) {
                add(new DebugAction(null));
            }
            add(new StartDownloadAction(null));
            add(new AddAllDownloadsAction(null));
            add(new SetStopMarkAction(null));
            add(new StopDownloadAction(null));
            add(new SetDownloadspeedAction(null));
            add(new DisableSpeedLimitAction(null));
            add(new SetConnectionsAction(null));
            add(new SetChunksAction(null));
            add(new PauseDownloadAction(null));
            add(new UnpauseDownloadAction(null));
            add(new ReconnectAction(null));
            add(new EnableReconnectAction(null));
            add(new DisableReconnectAction(null));
            add(new CaptchaServiceAction(null));
            add(new EnableAccountAction(null));
            add(new DisableAccountAction(null));
            add(new RestartJDownloaderAction(null));
        }
    });

    public static AbstractScheduleAction newActionInstance(ScheduleEntryStorable actionStorable) throws Exception {
        for (AbstractScheduleAction action : ACTIONS) {
            Class<? extends AbstractScheduleAction> actionClass = action.getClass();
            String actionID = action.getActionID();
            if (StringUtils.equals(actionID, actionStorable.getActionID())) {
                return actionClass.getConstructor(String.class).newInstance(actionStorable.getActionConfig());
            }
        }
        return null;
    }

    public static enum TIME_OPTIONS {
        ONLYONCE(T.T.time_option_only_once()),
        HOURLY(T.T.time_option_hourly()),
        SPECIFICDAYS(T.T.time_option_specificDays()),
        CHOOSEINTERVAL(T.T.time_option_choose_interval());
        private final String readableName;

        public final String getReadableName() {
            return readableName;
        }

        private TIME_OPTIONS(String readableName) {
            this.readableName = readableName;
        }
    }

    public static enum WEEKDAY {
        MONDAY(T.T.weekday_short_monday()),
        TUESDAY(T.T.weekday_short_tuesday()),
        WEDNESDAY(T.T.weekday_short_wednesday()),
        THURSDAY(T.T.weekday_short_thursday()),
        FRIDAY(T.T.weekday_short_friday()),
        SATURDAY(T.T.weekday_short_saturday()),
        SUNDAY(T.T.weekday_short_sunday());
        private final String readableName;

        public final String getReadableName() {
            return readableName;
        }

        private WEEKDAY(String readableName) {
            this.readableName = readableName;
        }
    }

    public static final HashMap<Integer, WEEKDAY> dayMap = new HashMap<Integer, ActionHelper.WEEKDAY>() {
        {
            put(Calendar.MONDAY, WEEKDAY.MONDAY);
            put(Calendar.TUESDAY, WEEKDAY.TUESDAY);
            put(Calendar.WEDNESDAY, WEEKDAY.WEDNESDAY);
            put(Calendar.THURSDAY, WEEKDAY.THURSDAY);
            put(Calendar.FRIDAY, WEEKDAY.FRIDAY);
            put(Calendar.SATURDAY, WEEKDAY.SATURDAY);
            put(Calendar.SUNDAY, WEEKDAY.SUNDAY);
        }
    };
}
