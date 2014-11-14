package org.jdownloader.gui.toolbar.action;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.captcha.v2.solver.myjd.CaptchaMyJDSolver;
import org.jdownloader.captcha.v2.solver.myjd.CaptchaMyJDSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class CaptchaToogleMyJDAutoAction extends AbstractToolbarToggleAction {

    public CaptchaToogleMyJDAutoAction() {
        super(CaptchaMyJDSolver.getInstance().getService().getConfig()._getStorageHandler().getKeyHandler("enabled", BooleanKeyHandler.class));

        setIconKey(IconKey.ICON_MYJDOWNLOADER);

    }

    @Override
    protected String createTooltip() {
        CaptchaMyJDSolverService service = CaptchaMyJDSolver.getInstance().getService();
        return _GUI._.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        CaptchaMyJDSolverService service = CaptchaMyJDSolver.getInstance().getService();
        return _GUI._.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        CaptchaMyJDSolverService service = CaptchaMyJDSolver.getInstance().getService();
        return _GUI._.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
