package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.ArrayList;

public class FeatureList extends ArrayList<ContentDirectoryFeature> {

    public FeatureList() {
        super();
    }

    public String getFeatureList() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Features xmlns=\"urn:schemas-upnp-org:av:avs\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\" urn:schemas-upnp-org:av:avs http://www.upnp.org/schemas/av/avs.xsd\">");
        for (int i = 0; i < size(); i++) {
            sb.append(get(i).toString());
        }
        sb.append("</Features>");
        return sb.toString();
    }
}
