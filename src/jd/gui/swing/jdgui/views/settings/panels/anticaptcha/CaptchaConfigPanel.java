package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;

import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_SOUND;

public class CaptchaConfigPanel extends AbstractConfigPanel {
    private static final long serialVersionUID = 1L;
    private CESSettingsPanel  psp;

    public String getTitle() {
        return _GUI._.AntiCaptchaConfigPanel_getTitle();
    }

    public CaptchaConfigPanel() {
        super();
        this.addHeader(getTitle(), NewTheme.I().getIcon("ocr", 32));
        this.addDescriptionPlain(_GUI._.AntiCaptchaConfigPanel_onShow_description());
        addPair(_GUI._.AntiCaptchaConfigPanel_AntiCaptchaConfigPanel_sounds(), null, new Checkbox(CFG_SOUND.CAPTCHA_SOUND_ENABLED));
        addPair(_GUI._.AntiCaptchaConfigPanel_AntiCaptchaConfigPanel_countdown_download(), null, new Checkbox(CFG_CAPTCHA.DIALOG_COUNTDOWN_FOR_DOWNLOADS_ENABLED));
        this.addHeader(_GUI._.AntiCaptchaConfigPanel_AntiCaptchaConfigPanel_solver(), NewTheme.I().getIcon("share", 32));
        this.addDescriptionPlain(_GUI._.AntiCaptchaConfigPanel_onShow_description_solver());
        add(psp = new CESSettingsPanel());
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("ocr", 32);
    }

    @Override
    protected void onShow() {

        super.onShow();
    }

    @Override
    public void save() {

    }

    @Override
    public void updateContents() {

    }
}