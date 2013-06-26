package jd.controlling.reconnect.pluginsinc.liveheader;

import org.appwork.uio.In;
import org.appwork.uio.Out;
import org.appwork.uio.UserIODefinition;

public interface GetLoginsInterface extends UserIODefinition {
    @Out
    @In
    String getUsername();

    @Out
    @In
    String getPassword();

}
