package org.jdownloader.extensions.schedulerV2.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.swing.jdgui.views.settings.components.ComboBox;

import org.appwork.swing.MigPanel;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("SET_CAPTCHASERVICE")
public class CaptchaServiceAction extends AbstractScheduleAction<CaptchaServiceActionConfig> {

    public CaptchaServiceAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_setCaptchaService();
    }

    @Override
    public void execute() {

        switch (getConfig()._getService()) {
        case NINEKWEU:
            org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA.CFG.setEnabled(true);
            org.jdownloader.settings.staticreferences.CFG_CAPTCHABROTHERHOOD.CFG.setEnabled(false);
            org.jdownloader.settings.staticreferences.CFG_DBC.CFG.setEnabled(false);
            break;
        case CAPTCHABROTHERHOOD:
            org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA.CFG.setEnabled(false);
            org.jdownloader.settings.staticreferences.CFG_CAPTCHABROTHERHOOD.CFG.setEnabled(true);
            org.jdownloader.settings.staticreferences.CFG_DBC.CFG.setEnabled(false);
            break;
        case DEATHBYCAPTCHA:
            org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA.CFG.setEnabled(false);
            org.jdownloader.settings.staticreferences.CFG_CAPTCHABROTHERHOOD.CFG.setEnabled(false);
            org.jdownloader.settings.staticreferences.CFG_DBC.CFG.setEnabled(true);
            break;
        case NONE:
        default:
            org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA.CFG.setEnabled(false);
            org.jdownloader.settings.staticreferences.CFG_CAPTCHABROTHERHOOD.CFG.setEnabled(false);
            org.jdownloader.settings.staticreferences.CFG_DBC.CFG.setEnabled(false);
            break;
        }
    }

    public static enum CAPTCHA_SERVICE {
        NONE(T._.action_captcha_none()),
        NINEKWEU("9kw.eu"),
        DEATHBYCAPTCHA("deathbycaptcha.eu"),
        CAPTCHABROTHERHOOD("captchabrotherhood.com");

        private final String readableName;

        public final String getReadableName() {
            return readableName;
        }

        private CAPTCHA_SERVICE(String readableName) {
            this.readableName = readableName;
        }
    }

    @Override
    public JPanel getConfigPanel() {
        MigPanel actionParameterPanelInt = new MigPanel("ins 0,wrap 2", "[sg 1][sg 2,grow,fill]", "");
        actionParameterPanelInt.add(new JLabel(T._.action_captcha_service() + ":"));// TODO

        final ComboBox<CAPTCHA_SERVICE> cbService = new ComboBox<CAPTCHA_SERVICE>(CAPTCHA_SERVICE.values()) {
            @Override
            protected String getLabel(int index, CAPTCHA_SERVICE value) {
                return value.getReadableName();
            }
        };
        cbService.setSelectedItem(getConfig()._getService());
        cbService.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getConfig()._setService(cbService.getSelectedItem());
            }
        });

        actionParameterPanelInt.add(cbService);
        return actionParameterPanelInt;
    }

    @Override
    public String getReadableParameter() {
        return getConfig()._getService().getReadableName();
    }
}
