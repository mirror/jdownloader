package jd.plugins.components.gopro;

import org.appwork.storage.flexijson.FlexiJSonNode;

public class FlexiJSonNodeResponse {
    public final FlexiJSonNode jsonNode;
    public final String        jsonString;

    public FlexiJSonNodeResponse(FlexiJSonNode jsonNode, String jsonString) {
        this.jsonNode = jsonNode;
        this.jsonString = jsonString;
    }
}
