package jd.controlling.reconnect.pluginsinc.easybox804;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.CryptedStorage;

@CryptedStorage(key = { 1, 6, 4, 5, 2, 7, 4, 3, 12, 61, 14, 75, -2, -7, -44, 33 })
public interface EasyBox804ReconnectConfig extends ConfigInterface {

    public String getPassword();

    public void setPassword(String pw);

    public String getRouterIP();

    public void setRouterIP(String pw);
}
