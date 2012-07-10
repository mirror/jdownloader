package org.jdownloader.extensions.vlcstreaming;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.TranslateInterface;

public interface VLCStreamingTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Streaming" })
    String popup_streaming();

    @Default(lngs = { "en" }, values = { "Play with VLC" })
    String popup_streaming_playvlc();
}
