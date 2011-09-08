package jd.controlling.reconnect.plugins.liveheader;

import org.jdownloader.gui.uiserio.UserIODefinition;

public interface GetLoginsInterface extends UserIODefinition {

    String getUsername();

    String getPassword();

}
