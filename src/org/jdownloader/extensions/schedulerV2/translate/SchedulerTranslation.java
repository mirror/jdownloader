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

}