package org.jdownloader.api.jdanywhere.api;

import java.util.Map;

import org.appwork.storage.config.ConfigInterface;

public interface JDAnywhereConfig extends ConfigInterface {

    Map<String, CaptchaPushRegistration> getList();

    void setList(Map<String, CaptchaPushRegistration> captchaPushList);
}
