package jd.plugins.components;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface SmoozedTranslation extends TranslateInterface {

    @Default(lngs = { "en", "de" }, values = { "Your trial period on SMOOZED.com has expired.\r\nGet a cheap SMOOZED PRO account for only 29,99 EUR per year to download again with full speed from all known hosters.", "Deine Testphase auf SMOOZED.com ist abgelaufen.\r\nHol dir einen günstigen SMOOZED PRO- Jahresaccount für nur 29.99 EUR, um wieder mit Fullspeed von allen bekannten Hostern herunterzuladen." })
    String free_trial_end();
}
