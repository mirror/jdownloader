package jd.controlling.reconnect.plugins.liveheader;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultObjectValue;
import org.appwork.storage.config.annotations.PlainStorage;

@PlainStorage
public interface RouterSenderSettings extends ConfigInterface {

    @DefaultObjectValue("[]")
    ArrayList<String> getSentScriptIds();

    void setSentScriptIds(ArrayList<String> list);

}
