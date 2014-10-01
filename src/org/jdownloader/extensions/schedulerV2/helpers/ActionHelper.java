package org.jdownloader.extensions.schedulerV2.helpers;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.extensions.schedulerV2.actions.DisableSpeedLimitAction;
import org.jdownloader.extensions.schedulerV2.actions.DisableReconnectAction;
import org.jdownloader.extensions.schedulerV2.actions.EnableReconnectAction;
import org.jdownloader.extensions.schedulerV2.actions.IScheduleAction;
import org.jdownloader.extensions.schedulerV2.actions.PauseDownloadAction;
import org.jdownloader.extensions.schedulerV2.actions.ReconnectAction;
import org.jdownloader.extensions.schedulerV2.actions.SetChunksAction;
import org.jdownloader.extensions.schedulerV2.actions.SetConnectionsAction;
import org.jdownloader.extensions.schedulerV2.actions.SetDownloadspeedAction;
import org.jdownloader.extensions.schedulerV2.actions.StartDownloadAction;
import org.jdownloader.extensions.schedulerV2.actions.StopDownloadAction;
import org.jdownloader.extensions.schedulerV2.actions.UnpauseDownloadAction;
import org.jdownloader.extensions.schedulerV2.translate.T;

public class ActionHelper {

    public static final List<IScheduleAction> ACTIONS = new ArrayList<IScheduleAction>() {
                                                          {
                                                              add(new StartDownloadAction());
                                                              add(new StopDownloadAction());
                                                              add(new SetDownloadspeedAction());
                                                              add(new DisableSpeedLimitAction());
                                                              add(new SetConnectionsAction());
                                                              add(new SetChunksAction());
                                                              add(new PauseDownloadAction());
                                                              add(new UnpauseDownloadAction());
                                                              add(new ReconnectAction());
                                                              add(new EnableReconnectAction());
                                                              add(new DisableReconnectAction());
                                                          }
                                                      };

    public static IScheduleAction getAction(String actionStorableID) {
        for (IScheduleAction action : ACTIONS) {
            if (action.getStorableID().equals(actionStorableID)) {
                return action;
            }
        }
        return null;
    }

    public static final String[] TIME_OPTIONS = { "ONLYONCE", "HOURLY", "DAILY", "WEEKLY", "CHOOSEINTERVAL" };

    public static String getPrettyTimeOption(String option) {
        if (option.equals("ONLYONCE")) {
            return T._.time_option_only_once();
        } else if (option.equals("HOURLY")) {
            return T._.time_option_hourly();
        } else if (option.equals("DAILY")) {
            return T._.time_option_daily();
        } else if (option.equals("WEEKLY")) {
            return T._.time_option_weekly();
        } else if (option.equals("CHOOSEINTERVAL")) {
            return T._.time_option_choose_interval();
        }

        return "ERROR: MISSING";
    }
}
