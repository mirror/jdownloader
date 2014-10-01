package org.jdownloader.extensions.schedulerV2.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface SchedulerTranslation extends TranslateInterface {
    @Default(lngs = { "en" }, values = { "Define time schedules to execute actions, start downloads,..." })
    String description();

    @Default(lngs = { "en" }, values = { "Scheduler" })
    String title();

    @Default(lngs = { "en" }, values = { "Enable" })
    String scheduleTable_column_enable();

    @Default(lngs = { "en" }, values = { "Name" })
    String scheduleTable_column_name();

    @Default(lngs = { "en" }, values = { "Action" })
    String scheduleTable_column_action();

    @Default(lngs = { "en" }, values = { "Next Execution" })
    String scheduleTable_column_next_execution();

    @Default(lngs = { "en" }, values = { "Repeats" })
    String scheduleTable_column_repeats();

    @Default(lngs = { "en" }, values = { "Really remove %s1 schedule plan(s)?" })
    String entry_remove_action_title(int size);

    @Default(lngs = { "en" }, values = { "Really remove %s1" })
    String entry_remove_action_msg(String string);

    @Default(lngs = { "en" }, values = { "Create new Schedule Plan" })
    String addScheduleEntryDialog_title();

    @Default(lngs = { "en" }, values = { "Only once" })
    String time_option_only_once();

    @Default(lngs = { "en" }, values = { "Hourly" })
    String time_option_hourly();

    @Default(lngs = { "en" }, values = { "Daily" })
    String time_option_daily();

    @Default(lngs = { "en" }, values = { "Weekly" })
    String time_option_weekly();

    @Default(lngs = { "en" }, values = { "Choose interval" })
    String time_option_choose_interval();

    @Default(lngs = { "en" }, values = { "Parameter" })
    String scheduleTable_column_actionparameter();

    @Default(lngs = { "en" }, values = { "Every %s1 hours and %s2 minutes" })
    String timeformat_repeats_interval(String hour, String minute);

    @Default(lngs = { "en" }, values = { "Weekly, on %s1 at %s2" })
    String timeformat_repeats_weekly(String day, String time);

    @Default(lngs = { "en" }, values = { "Daily, at %s1" })
    String timeformat_repeats_daily(String time);

    @Default(lngs = { "en" }, values = { "Hourly, at minute %s1" })
    String timeformat_repeats_hourly(String minute);

    @Default(lngs = { "en" }, values = { "Time / Interval" })
    String addScheduleEntryDialog_header_time();

    @Default(lngs = { "en" }, values = { "Repeat" })
    String addScheduleEntryDialog_repeat();

    @Default(lngs = { "en" }, values = { "No parameters to set." })
    String addScheduleEntryDialog_no_parameter();

    @Default(lngs = { "en" }, values = { "Speed" })
    String addScheduleEntryDialog_speed();

    @Default(lngs = { "en" }, values = { "Date" })
    String addScheduleEntryDialog_date();

    @Default(lngs = { "en" }, values = { "Time" })
    String addScheduleEntryDialog_time();

    @Default(lngs = { "en" }, values = { "Hours" })
    String addScheduleEntryDialog_hours();

    @Default(lngs = { "en" }, values = { "Minutes" })
    String addScheduleEntryDialog_minutes();

    @Default(lngs = { "en" }, values = { "Minute" })
    String addScheduleEntryDialog_minute();

    @Default(lngs = { "en" }, values = { "Number" })
    String addScheduleEntryDialog_number();

    @Default(lngs = { "en" }, values = { "Action & Parameters" })
    String addScheduleEntryDialog_actionParameters();

    @Default(lngs = { "en" }, values = { "Schedule" })
    String addScheduleEntryDialog_defaultScheduleName();

    @Default(lngs = { "en" }, values = { "Set concurrent connections" })
    String action_setConnections();

    @Default(lngs = { "en" }, values = { "Enable reconnect" })
    String action_enableReconnect();

    @Default(lngs = { "en" }, values = { "Disable reconnect" })
    String action_disableReconnect();

    @Default(lngs = { "en" }, values = { "Pause downloads" })
    String action_pauseDownloads();

    @Default(lngs = { "en" }, values = { "Reconnect" })
    String action_Reconnect();

    @Default(lngs = { "en" }, values = { "Set downloadspeed" })
    String action_setDownloadspeed();

    @Default(lngs = { "en" }, values = { "Start download" })
    String action_startDownload();

    @Default(lngs = { "en" }, values = { "Set chunks per file" })
    String action_setChunks();

    @Default(lngs = { "en" }, values = { "Stop download" })
    String action_stopDownload();

    @Default(lngs = { "en" }, values = { "Unpause downloads" })
    String action_unpauseDownload();

    @Default(lngs = { "en" }, values = { "Disable speed limit" })
    String action_disableSpeedLimit();

}