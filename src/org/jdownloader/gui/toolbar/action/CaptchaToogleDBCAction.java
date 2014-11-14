package org.jdownloader.gui.toolbar.action;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolver;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class CaptchaToogleDBCAction extends AbstractToolbarToggleAction {

    public CaptchaToogleDBCAction() {
        super(DeathByCaptchaSolver.getInstance().getService().getConfig()._getStorageHandler().getKeyHandler("enabled", BooleanKeyHandler.class));

        setIconKey(IconKey.ICON_DBC);

    }

    @Override
    protected String createTooltip() {
        DeathByCaptchaSolverService service = DeathByCaptchaSolver.getInstance().getService();
        ;
        return _GUI._.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        DeathByCaptchaSolverService service = DeathByCaptchaSolver.getInstance().getService();
        ;
        return _GUI._.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        DeathByCaptchaSolverService service = DeathByCaptchaSolver.getInstance().getService();
        ;
        return _GUI._.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
