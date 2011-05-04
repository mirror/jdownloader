package jd.controlling.authentication;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultObjectValue;

public interface AuthenticationControllerSettings extends ConfigInterface {
    @DefaultObjectValue("[]")
    ArrayList<AuthenticationInfo> getList();

    void setList(ArrayList<AuthenticationInfo> list);
}
