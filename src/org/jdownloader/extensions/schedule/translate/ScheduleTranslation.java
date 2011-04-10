package org.jdownloader.extensions.schedule.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface ScheduleTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Minute:" })
    String plugin_optional_scheduler_add_minute();

    @Default(lngs = { "en" }, values = { "Next Execution" })
    String jd_plugins_optional_schedule_SchedulerTableModel_nextexecution();

    @Default(lngs = { "en" }, values = { "Interval: %s1h %s2m" })
    String jd_plugins_optional_schedule_MainGui_MyTableModel_add_interval(Object s1, Object s2);

    @Default(lngs = { "en" }, values = { "Pause Downloads" })
    String jd_plugins_optional_schedule_modules_pauseDownloads();

    @Default(lngs = { "en" }, values = { "Enable Clipboard Monitoring" })
    String jd_plugins_optional_schedule_modules_enableClipboard();

    @Default(lngs = { "en" }, values = { "Stop Downloads" })
    String jd_plugins_optional_schedule_modules_stopDownloads();

    @Default(lngs = { "en" }, values = { "Save" })
    String plugin_optional_scheduler_add_save();

    @Default(lngs = { "en" }, values = { "Set StopMark" })
    String jd_plugins_optional_schedule_modules_setstopmark();

    @Default(lngs = { "en" }, values = { "Unpause Downloads" })
    String jd_plugins_optional_schedule_modules_unpauseDownloads();

    @Default(lngs = { "en" }, values = { "Weekly" })
    String plugin_optional_scheduler_add_weekly();

    @Default(lngs = { "en" }, values = { "Only once" })
    String plugin_optional_scheduler_add_once();

    @Default(lngs = { "en" }, values = { "Enable Premium" })
    String jd_plugins_optional_schedule_modules_enablePremium();

    @Default(lngs = { "en" }, values = { "On/Off" })
    String jd_plugins_optional_schedule_SchedulerTableModel_onoff();

    @Default(lngs = { "en" }, values = { "Daily" })
    String jd_plugins_optional_schedule_MainGui_MyTableModel_add_daily();

    @Default(lngs = { "en" }, values = { "Parameter" })
    String plugin_optional_scheduler_add_column_executions_parameter();

    @Default(lngs = { "en" }, values = { "Edit" })
    String jd_plugins_optional_schedule_MainGui_edit();

    @Default(lngs = { "en" }, values = { "disabled" })
    String jd_plugins_optional_schedule_disabled();

    @Default(lngs = { "en" }, values = { "Repeats" })
    String plugin_optional_scheduler_add_repeats();

    @Default(lngs = { "en" }, values = { "Repeats" })
    String jd_plugins_optional_schedule_SchedulerTableModel_repeats();

    @Default(lngs = { "en" }, values = { "No correct Parameter" })
    String plugin_optional_scheduler_add_problem_badparameter();

    @Default(lngs = { "en" }, values = { "Set max Downloads" })
    String jd_plugins_optional_schedule_modules_setMaxDownloads();

    @Default(lngs = { "en" }, values = { "No Parameter needed" })
    String plugin_optional_scheduler_add_noparameter();

    @Default(lngs = { "en" }, values = { "Disable Reconnect" })
    String jd_plugins_optional_schedule_modules_disableReconnect();

    @Default(lngs = { "en" }, values = { "Execution time is in the past" })
    String plugin_optional_scheduler_add_problem_pastdate();

    @Default(lngs = { "en" }, values = { "Scheduler" })
    String jd_plugins_optional_schedule_schedule();

    @Default(lngs = { "en" }, values = { "Schedule your downloads" })
    String jd_plugins_optional_schedule_SchedulerView_tooltip();

    @Default(lngs = { "en" }, values = { "Name" })
    String plugin_optional_scheduler_add_column_executions_name();

    @Default(lngs = { "en" }, values = { "Disable Clipboard Monitoring" })
    String jd_plugins_optional_schedule_modules_disableClipboard();

    @Default(lngs = { "en" }, values = { "Cancel" })
    String plugin_optional_scheduler_add_cancel();

    @Default(lngs = { "en" }, values = { "Daily" })
    String plugin_optional_scheduler_add_daily();

    @Default(lngs = { "en" }, values = { "Scheduler" })
    String jd_plugins_optional_schedule_SchedulerView_title();

    @Default(lngs = { "en" }, values = { "Do Shutdown" })
    String jd_plugins_optional_schedule_modules_doShutdown();

    @Default(lngs = { "en" }, values = { "Do Reconnect" })
    String jd_plugins_optional_schedule_modules_doReconnect();

    @Default(lngs = { "en" }, values = { "Enable Premium for specific Host" })
    String jd_plugins_optional_schedule_modules_enablePremiumForHost();

    @Default(lngs = { "en" }, values = { "Do Sleep" })
    String jd_plugins_optional_schedule_modules_doSleep();

    @Default(lngs = { "en" }, values = { "Enable Reconnect" })
    String jd_plugins_optional_schedule_modules_enableReconnect();

    @Default(lngs = { "en" }, values = { "Do Backup" })
    String jd_plugins_optional_schedule_modules_doBackup();

    @Default(lngs = { "en" }, values = { "wait a moment" })
    String jd_plugins_optional_schedule_wait();

    @Default(lngs = { "en" }, values = { "Disable a specific Host" })
    String jd_plugins_optional_schedule_modules_disableHost();

    @Default(lngs = { "en" }, values = { "Disable Premium" })
    String jd_plugins_optional_schedule_modules_disablePremium();

    @Default(lngs = { "en" }, values = { "Set Downloadspeed" })
    String jd_plugins_optional_schedule_modules_setDownloadSpeed();

    @Default(lngs = { "en" }, values = { "Start Downloads" })
    String jd_plugins_optional_schedule_modules_startDownloads();

    @Default(lngs = { "en" }, values = { "No changes made" })
    String plugin_optional_scheduler_add_problem_nochanges();

    @Default(lngs = { "en" }, values = { "Hourly" })
    String plugin_optional_scheduler_add_hourly();

    @Default(lngs = { "en" }, values = { "Date" })
    String jd_plugins_optional_schedule_SchedulerTableModel_date();

    @Default(lngs = { "en" }, values = { "Name" })
    String plugin_optional_scheduler_add_name();

    @Default(lngs = { "en" }, values = { "Hourly" })
    String jd_plugins_optional_schedule_MainGui_MyTableModel_add_hourly();

    @Default(lngs = { "en" }, values = { "Choose interval" })
    String plugin_optional_scheduler_add_specific();

    @Default(lngs = { "en" }, values = { "Weekly" })
    String jd_plugins_optional_schedule_MainGui_MyTableModel_add_weekly();

    @Default(lngs = { "en" }, values = { "Repeattime equals Zero" })
    String plugin_optional_scheduler_add_problem_zerorepeat();

    @Default(lngs = { "en" }, values = { "Month:" })
    String plugin_optional_scheduler_add_month();

    @Default(lngs = { "en" }, values = { "Set Chunks" })
    String jd_plugins_optional_schedule_modules_setChunks();

    @Default(lngs = { "en" }, values = { "Only once" })
    String jd_plugins_optional_schedule_MainGui_MyTableModel_add_once();

    @Default(lngs = { "en" }, values = { "Disable Premium for specific Host" })
    String jd_plugins_optional_schedule_modules_disablePremiumForHost();

    @Default(lngs = { "en" }, values = { "Name" })
    String jd_plugins_optional_schedule_SchedulerTableModel_name();

    @Default(lngs = { "en" }, values = { "Year:" })
    String plugin_optional_scheduler_add_year();

    @Default(lngs = { "en" }, values = { "Unset StopMark" })
    String jd_plugins_optional_schedule_modules_unsetstopmark();

    @Default(lngs = { "en" }, values = { "Do Hibernate" })
    String jd_plugins_optional_schedule_modules_doHibernate();

    @Default(lngs = { "en" }, values = { "expired" })
    String jd_plugins_optional_schedule_expired();

    @Default(lngs = { "en" }, values = { "Date/Time" })
    String plugin_optional_scheduler_add_date2();

    @Default(lngs = { "en" }, values = { "Name is empty" })
    String plugin_optional_scheduler_add_problem_emptyname();

    @Default(lngs = { "en" }, values = { "Easily schedule almost any action you can think of for JDownloader to execute at any moment in the future, also supporting repetition." })
    String jd_plugins_optional_schedule_schedule_description();

    @Default(lngs = { "en" }, values = { "Day:" })
    String plugin_optional_scheduler_add_day();

    @Default(lngs = { "en" }, values = { "Enable a specific Host" })
    String jd_plugins_optional_schedule_modules_enableHost();

    @Default(lngs = { "en" }, values = { "Action" })
    String jd_plugins_optional_schedule_SchedulerTableModel_action();

    @Default(lngs = { "en" }, values = { "Time" })
    String jd_plugins_optional_schedule_SchedulerTableModel_time();

    @Default(lngs = { "en" }, values = { "Hour:" })
    String plugin_optional_scheduler_add_hour();
}