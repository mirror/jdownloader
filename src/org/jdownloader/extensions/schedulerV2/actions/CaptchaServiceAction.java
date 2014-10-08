package org.jdownloader.extensions.schedulerV2.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;

import jd.gui.swing.jdgui.views.settings.components.ComboBox;

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
    protected void createPanel() {
        panel.put(new JLabel(T._.action_captcha_service() + ":"), "gapleft 10,");

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

        panel.put(cbService, "");
    };

    @Override
    public String getReadableParameter() {
        return getConfig()._getService().getReadableName();
    }
}
