package jd.plugins.optional.captchapush;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.captcha.CaptchaController;
import jd.controlling.captcha.CaptchaSolver;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision: 42$", id = "captchapush", interfaceversion = PluginOptional.ADDON_INTERFACE_VERSION)
public class CaptchaPush extends PluginOptional implements CaptchaSolver {

    private static final String JDL_PREFIX       = "jd.plugins.optional.captchapush.CaptchaPush.";

    private static final String PROPERTY_ENABLE  = "PROPERTY_ENABLE";
    private static final String PROPERTY_HOST    = "PROPERTY_HOST";
    private static final String PROPERTY_PORT    = "PROPERTY_PORT";
    private static final String PROPERTY_TOPIC   = "PROPERTY_TOPIC";
    private static final String PROPERTY_TIMEOUT = "PROPERTY_TIMEOUT";

    private MenuAction          activateAction;

    public CaptchaPush(PluginWrapper wrapper) {
        super(wrapper);

        initConfigEntries();
    }

    private void initConfigEntries() {
        final ConfigEntry ce;

        config.setGroup(new ConfigGroup(getHost(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_HOST, JDL.L(JDL_PREFIX + "host", "Host of CaptchaPushServer:")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), PROPERTY_PORT, JDL.L(JDL_PREFIX + "port", "Port of CaptchaPushServer:"), 1000, 65500, 1).setDefaultValue(19732));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), JDL.L(JDL_PREFIX + "topic", "Topic for CaptchaPushServer:"), PROPERTY_TOPIC));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String randomTopic = getRandomString(16);

                getPluginConfig().setProperty(PROPERTY_TOPIC, randomTopic);
                getPluginConfig().save();

                ce.getGuiListener().setData(randomTopic);
            }

        }, JDL.L(JDL_PREFIX + "generateTopic.description", "Generate New Topic"), JDL.L(JDL_PREFIX + "generateTopic.label", "Generate new topic for the CaptchaPushServer:"), null));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), PROPERTY_TIMEOUT, JDL.L(JDL_PREFIX + "timeout", "Timeout for CaptchaPushServer: [sec]"), 30, 300, 10).setDefaultValue(120));
    }

    private static String getRandomString(int len) {
        String allowedChars = "0123456789abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();

        int max = allowedChars.length();
        char[] charArray = new char[len];
        for (int i = 0; i < len; i++) {
            charArray[i] = allowedChars.charAt(random.nextInt(max));
        }
        return new String(charArray);
    }

    @Override
    public boolean initAddon() {
        activateAction = new MenuAction("captchapush", 0) {
            private static final long serialVersionUID = 3252473048646596851L;

            @Override
            public void onAction(ActionEvent e) {
                getPluginConfig().setProperty(PROPERTY_ENABLE, activateAction.isSelected());
                getPluginConfig().save();

                if (activateAction.isSelected()) {
                    CaptchaController.setCaptchaSolver(CaptchaPush.this);
                } else {
                    CaptchaController.setCaptchaSolver(null);
                }
            }
        };
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(getPluginConfig().getBooleanProperty(PROPERTY_ENABLE, false));

        if (getPluginConfig().getBooleanProperty(PROPERTY_ENABLE, false)) {
            CaptchaController.setCaptchaSolver(this);
        }

        logger.info("CaptchaPush: OK");
        return true;
    }

    @Override
    public void onExit() {
        CaptchaController.setCaptchaSolver(null);
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    @Override
    public String getIconKey() {
        return "gui.images.config.ocr";
    }

    public String solveCaptcha(String host, File captchaFile, String suggestion, String explain) {
        return null;
    }

}
