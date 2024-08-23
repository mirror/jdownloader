package org.jdownloader.plugins.components.youtube.variants.generics;

import java.util.Locale;

import org.appwork.storage.Storable;
import org.appwork.txtresource.TranslationFactory;

public class GenericAudioInfo extends AbstractGenericVariantInfo implements Storable {
    public GenericAudioInfo(/* storable */) {
    }

    private int    aBitrate = -1;
    private Locale _locale;

    public Locale _getLocale() {
        return _locale;
    }

    public int getaBitrate() {
        return aBitrate;
    }

    public void setaBitrate(int bitrate) {
        this.aBitrate = bitrate;
    }

    public String getaId() {
        return aId;
    }

    public void setaId(String aId) {
        if (aId != null) {
            this.aId = aId;
            _locale = TranslationFactory.stringToLocale(aId.replaceAll("[^a-zA-Z\\-]*", ""));
        } else {
            this.aId = null;
            this._locale = null;
        }
    }

    private String aId = null;
}
