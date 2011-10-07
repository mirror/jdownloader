package jd.controlling.reconnect.plugins.liveheader;

import org.appwork.utils.swing.dialog.UserIODefinition;

public interface GetLoginsInterface extends UserIODefinition {

    String getUsername();

    String getPassword();

}
