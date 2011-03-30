package org.jdownloader.extensions.captchapush;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import jd.controlling.captcha.CaptchaController;
import jd.controlling.captcha.CaptchaSolver;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class CaptchaPushExtension extends AbstractExtension implements ActionListener, CaptchaSolver {

    private CaptchaPushConfig      config;
    private CaptchaPushConfigPanel configPanel;
    private MenuAction             activateAction;

    public CaptchaPushExtension() {
        super("JD Captcha Push");
    }

    // @Override
    // protected void initSettings(ConfigContainer config) {
    // final ConfigEntry ce;
    //
    // config.setGroup(new ConfigGroup(getConfigID(), getIconKey()));
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
    // getPluginConfig(), PROPERTY_HOST, "Host of CaptchaPushServer:"));
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER,
    // getPluginConfig(), PROPERTY_PORT, "Port of CaptchaPushServer:", 1000,
    // 65500, 1).setDefaultValue(19732));
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
    // config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD,
    // getPluginConfig(), "Topic for CaptchaPushServer:", PROPERTY_TOPIC));
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new
    // ActionListener() {
    //
    // public void actionPerformed(ActionEvent e) {
    // String randomTopic = getRandomString(16);
    //
    // getPluginConfig().setProperty(PROPERTY_TOPIC, randomTopic);
    // getPluginConfig().save();
    //
    // ce.getGuiListener().setData(randomTopic);
    // }
    //
    // }, "Generate New Topic", "Generate new topic for the CaptchaPushServer:",
    // null));
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER,
    // getPluginConfig(), PROPERTY_TIMEOUT,
    // "Timeout for CaptchaPushServer: [sec]", 30, 300,
    // 10).setDefaultValue(120));
    // }

    // private static String getRandomString(int len) {
    // String allowedChars = "0123456789abcdefghijklmnopqrstuvwxyz";
    // Random random = new Random();
    //
    // int max = allowedChars.length();
    // char[] charArray = new char[len];
    // for (int i = 0; i < len; i++) {
    // charArray[i] = allowedChars.charAt(random.nextInt(max));
    // }
    // return new String(charArray);
    // }

    @Override
    public String getIconKey() {
        return "gui.images.config.ocr";
    }

    public String solveCaptcha(String host, File captchaFile, String suggestion, String explain) {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
    }

    @Override
    protected void stop() throws StopException {
        CaptchaController.setCaptchaSolver(null);
    }

    @Override
    protected void start() throws StartException {
    }

    @Override
    protected void initExtension() throws StartException {
        activateAction = new MenuAction("captchapush", 0) {
            private static final long serialVersionUID = 3252473048646596851L;

            @Override
            public void onAction(ActionEvent e) {
                if (activateAction.isSelected()) {
                    CaptchaController.setCaptchaSolver(CaptchaPushExtension.this);
                } else {
                    CaptchaController.setCaptchaSolver(null);
                }
            }
        };
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(true);

        config = JsonConfig.create(CaptchaPushConfig.class);
        configPanel = new CaptchaPushConfigPanel(this, config);

        CaptchaController.setCaptchaSolver(this);

        logger.info("CaptchaPush: OK");
    }

    @Override
    public ExtensionConfigPanel<? extends AbstractExtension> getConfigPanel() {
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
        return null;
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

}