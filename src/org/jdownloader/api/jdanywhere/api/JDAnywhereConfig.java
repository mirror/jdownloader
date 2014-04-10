package org.jdownloader.api.jdanywhere.api;

import java.util.Map;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.CryptedStorage;

@CryptedStorage(key = { 0x00, 0x02, 0x11, 0x01, 0x01, 0x54, 0x02, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 })
public interface JDAnywhereConfig extends ConfigInterface {

    Map<String, CaptchaPushRegistration> getList();

    void setList(Map<String, CaptchaPushRegistration> captchaPushList);
}
