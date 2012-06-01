package jd.controlling.reconnect.pluginsinc.liveheader;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.PlainStorage;

@PlainStorage
public interface RouterSenderSettings extends ConfigInterface {

    @DefaultJsonObject("[]")
    ArrayList<String> getSentScriptIds();

    void setSentScriptIds(ArrayList<String> list);

}
