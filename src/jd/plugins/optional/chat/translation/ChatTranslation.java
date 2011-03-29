package jd.plugins.optional.chat.translation;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en", "de" })
public interface ChatTranslation extends TranslateInterface {
    @Default(lngs = { "en", "de" }, values = { "Your name", "Dein Name" })
    String settings_nick();

    @Default(lngs = { "en", "de" }, values = { "Contact JDownloader Developers and Supporters", "Kontaktiere JDownloader Entwickler oder Supporter." })
    String description();

    @Default(lngs = { "en", "de" }, values = { "Enable userlist colors", "Benutzerfarben aktivieren" })
    String settings_enabled_userlist_colors();

    @Default(lngs = { "en", "de" }, values = { "Userlist position", "Position der Benutzerliste" })
    String settings_userlist_position();

    @Default(lngs = { "en", "de" }, values = { "Right side", "Rechte Seite" })
    String settings_userlist_position_right();

    @Default(lngs = { "en", "de" }, values = { "Left side", "Linke Seite" })
    String settings_userlist_position_left();

    @Default(lngs = { "en", "de" }, values = { "Perform this actions after entering chat:", "Nach dem Beitreten diese Aktionen ausführen:" })
    String settings_perform();

    @Default(lngs = { "en", "de" }, values = { "%s1 Channel", "%s1 Kanal" })
    String gui_tab_title(String currentChannel);

}
