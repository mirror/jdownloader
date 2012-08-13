package org.jdownloader.extensions.streaming;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.TranslateInterface;

public interface StreamingTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Streaming" })
    String popup_streaming();

    @Default(lngs = { "en" }, values = { "Play" })
    String popup_streaming_playvlc();

    @Default(lngs = { "en" }, values = { "Streaming" })
    String gui_title();

    @Default(lngs = { "en" }, values = { "Manage all your Streaming Links" })
    String gui_tooltip();

    @Default(lngs = { "en" }, values = { "Play Archive" })
    String unraraction();

    @Default(lngs = { "en" }, values = { "Open Rar Archive" })
    String open_rar();

    @Default(lngs = { "en" }, values = { "Please wait while JDownloader is preparing the Stream" })
    String open_rar_msg();

    @Default(lngs = { "en" }, values = { "Enter Password to open %s1" })
    String enter_password(String name);

    @Default(lngs = { "en" }, values = { "Please enter Passwords. Try one of these:\r\n%s1" })
    String enter_passwordfor(String list);

    @Default(lngs = { "en" }, values = { "Please enter Passwords." })
    String enter_passwordfor2();

    @Default(lngs = { "en" }, values = { "Wrong Password!" })
    String wrong_password();

    @Default(lngs = { "en" }, values = { "Play to %s1" })
    String playto(String displayName);
}
