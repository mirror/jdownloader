package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.actions.CaptchaServiceAction.CAPTCHA_SERVICE;

public class CaptchaServiceActionConfig implements IScheduleActionConfig {
    public CaptchaServiceActionConfig(/* Storable */) {
    }

    public CAPTCHA_SERVICE _getService() {
        try {
            return CAPTCHA_SERVICE.valueOf(service);
        } catch (final Exception e) {
            return CAPTCHA_SERVICE.NONE;
        }
    }

    public String getService() {
        return service;
    }

    public void _setService(CAPTCHA_SERVICE service) {
        if (service == null) {
            service = CAPTCHA_SERVICE.NONE;
        }
        this.service = service.name();
    }

    public void setService(String service) {
        this.service = service;
    }

    private String service = CAPTCHA_SERVICE.NONE.name();
}
