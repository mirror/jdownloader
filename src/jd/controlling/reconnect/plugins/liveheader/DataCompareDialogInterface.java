package jd.controlling.reconnect.plugins.liveheader;

import org.jdownloader.gui.uiserio.UserIODefinition;

public interface DataCompareDialogInterface extends UserIODefinition {

    String getUsername();

    String getPassword();

    String getManufactor();

    String getRouterName();

    String getFirmware();

    String getHostName();

}
