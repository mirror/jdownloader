package org.jdownloader.auth;

import java.util.List;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.annotations.CustomStorageName;
import org.appwork.storage.config.annotations.DefaultJsonObject;

@CustomStorageName("jd.controlling.authentication.AuthenticationControllerSettings")
public interface AuthenticationControllerSettings extends ConfigInterface {
    @DefaultJsonObject("[]")
    @CryptedStorage(key = { 2, 4, 4, 5, 2, 7, 4, 3, 12, 61, 14, 75, -2, -7, -44, 33 })
    List<AuthenticationInfo> getList();

    void setList(List<AuthenticationInfo> list);
}
