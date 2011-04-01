package org.jdownloader.extensions.captchapush;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.captcha.CaptchaController;
import jd.controlling.captcha.CaptchaEventListener;
import jd.controlling.captcha.CaptchaEventSender;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.extensions.AbstractConfigPanel;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class CaptchaPushExtension extends AbstractExtension implements CaptchaEventListener {

    private CaptchaPushConfig      config;
    private CaptchaPushConfigPanel configPanel;
    private MenuAction             activateAction;

    public CaptchaPushExtension() {
        super("Captcha Push");
    }

    @Override
    public String getIconKey() {
        return "gui.images.config.ocr";
    }

    @Override
    protected void stop() throws StopException {
        CaptchaEventSender.getInstance().removeListener(this);
    }

    @Override
    protected void start() throws StartException {
        CaptchaEventSender.getInstance().addListener(this);
    }

    @Override
    protected void initExtension() throws StartException {
        activateAction = new MenuAction("captchapush", 0) {
            private static final long serialVersionUID = 3252473048646596851L;

            @Override
            public void onAction(ActionEvent e) {
                if (activateAction.isSelected()) {
                    CaptchaEventSender.getInstance().addListener(CaptchaPushExtension.this);
                } else {
                    CaptchaEventSender.getInstance().removeListener(CaptchaPushExtension.this);
                }
            }
        };
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(true);

        config = JsonConfig.create(CaptchaPushConfig.class);
        configPanel = new CaptchaPushConfigPanel(this, config);

        logger.info("CaptchaPush: OK");
    }

    @Override
    public AbstractConfigPanel getConfigPanel() {
        return configPanel;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getConfigID() {
        return "captchapush";
    }

    @Override
    public String getAuthor() {
        return "Greeny";
    }

    @Override
    public String getDescription() {
        return "This plugin can push any Captcha request to your Android or WebOS Smartphone";
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    public void captchaTodo(CaptchaController controller) {
        /* TODO: Push Captcha to Broker! */
    }

    public void captchaFinish(CaptchaController controller) {
        /* TODO: Push Finish to Broker! */
    }

}