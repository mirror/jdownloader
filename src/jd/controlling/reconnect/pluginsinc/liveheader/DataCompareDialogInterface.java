package jd.controlling.reconnect.pluginsinc.liveheader;

import org.appwork.uio.Out;
import org.appwork.uio.UserIODefinition;

public interface DataCompareDialogInterface extends UserIODefinition {
    @Out
    String getUsername();

    @Out
    String getPassword();

    @Out
    String getManufactor();

    @Out
    String getRouterName();

    @Out
    String getFirmware();

    @Out
    String getHostName();

}
