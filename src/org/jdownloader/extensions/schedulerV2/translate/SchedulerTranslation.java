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

}