package org.jdownloader.extensions.vlcstreaming;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.TranslateInterface;

public interface VLCStreamingTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Streaming" })
    String popup_streaming();

    @Default(lngs = { "en" }, values = { "Play" })
    String popup_streaming_playvlc();

    @Default(lngs = { "en" }, values = { "Streaming" })
    String gui_title();

    @Default(lngs = { "en" }, values = { "Manage all your Streaming Links" })
    String gui_tooltip();
}
