package org.jdownloader.extensions.streaming.mediaarchive;

import org.fourthline.cling.support.model.DIDLObject.Property;

public class SecDcmInfoProperty extends Property<String> implements SEC.NAMESPACE {
    public SecDcmInfoProperty(String string) {
        super(string, "dcminfo");
    }
}
