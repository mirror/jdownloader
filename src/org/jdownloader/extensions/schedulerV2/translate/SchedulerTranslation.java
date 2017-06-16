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

    @Default(lngs = { "en" }, values = { "Edit Schedule Plan" })
    String addScheduleEntryDialog_title_edit();

    @Default(lngs = { "en" }, values = { "Only once" })
    String time_option_only_once();

    @Default(lngs = { "en" }, values = { "Hourly" })
    String time_option_hourly();

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

    @Default(lngs = { "en" }, values = { "Action & Parameters" })
    String addScheduleEntryDialog_actionParameters();

    @Default(lngs = { "en" }, values = { "Schedule" })
    String addScheduleEntryDialog_defaultScheduleName();

    @Default(lngs = { "en" }, values = { "Set simultaneous Downloads" })
    String action_setConnections();

    @Default(lngs = { "en" }, values = { "Enable Reconnect" })
    String action_enableReconnect();

    @Default(lngs = { "en" }, values = { "Disable Reconnect" })
    String action_disableReconnect();

    @Default(lngs = { "en" }, values = { "Pause Downloads" })
    String action_pauseDownloads();

    @Default(lngs = { "en" }, values = { "Reconnect" })
    String action_Reconnect();

    @Default(lngs = { "en" }, values = { "Restart JDownloader" })
    String action_Restart_JDownloader();

    @Default(lngs = { "en" }, values = { "Set Downloadspeed" })
    String action_setDownloadspeed();

    @Default(lngs = { "en" }, values = { "Start Downloads" })
    String action_startDownload();

    @Default(lngs = { "en" }, values = { "Add all Downloads" })
    String action_addAllDownloads();

    @Default(lngs = { "en" }, values = { "Set Chunks per File" })
    String action_setChunks();

    @Default(lngs = { "en" }, values = { "Stop Downloads" })
    String action_stopDownload();

    @Default(lngs = { "en" }, values = { "Unpause Downloads" })
    String action_unpauseDownload();

    @Default(lngs = { "en" }, values = { "Disable Speed Limit" })
    String action_disableSpeedLimit();

    @Default(lngs = { "en" }, values = { "Set Stop Mark" })
    String action_setSopMark();

    @Default(lngs = { "en" }, values = { "Set active Captcha Service" })
    String action_setCaptchaService();

    @Default(lngs = { "en" }, values = { "None" })
    String action_captcha_none();

    @Default(lngs = { "en" }, values = { "Specific Days" })
    String time_option_specificDays();

    @Default(lngs = { "en" }, values = { "Days: %s1 at %s2" })
    String timeformat_repeats_specificDays(String days, String time);

    @Default(lngs = { "en" }, values = { "Never" })
    String lit_never();

    @Default(lngs = { "en" }, values = { "Mon" })
    String weekday_short_monday();

    @Default(lngs = { "en" }, values = { "Tue" })
    String weekday_short_tuesday();

    @Default(lngs = { "en" }, values = { "Wed" })
    String weekday_short_wednesday();

    @Default(lngs = { "en" }, values = { "Thu" })
    String weekday_short_thursday();

    @Default(lngs = { "en" }, values = { "Fri" })
    String weekday_short_friday();

    @Default(lngs = { "en" }, values = { "Sat" })
    String weekday_short_saturday();

    @Default(lngs = { "en" }, values = { "Sun" })
    String weekday_short_sunday();

    @Default(lngs = { "en" }, values = { "Service" })
    String action_captcha_service();

    @Default(lngs = { "en" }, values = { "Copy" })
    String lit_copy();

    @Default(lngs = { "en" }, values = { "Days" })
    String addScheduleEntryDialog_days();

    @Default(lngs = { "en" }, values = { "Parameter" })
    String addScheduleEntryDialog_no_parameter_caption();

    @Default(lngs = { "en" }, values = { "Chunks" })
    String addScheduleEntryDialog_chunks();

    @Default(lngs = { "en" }, values = { "Downloads" })
    String addScheduleEntryDialog_downloads();

    @Default(lngs = { "en" }, values = { "Enable Account" })
    String action_enableAccount();

    @Default(lngs = { "en" }, values = { "Account" })
    String addScheduleEntryDialog_account();

    @Default(lngs = { "en" }, values = { "Disable Account" })
    String action_disableAccount();

    @Default(lngs = { "en" }, values = { "No Accounts available" })
    String addScheduleEntryDialog_noAccount();
}