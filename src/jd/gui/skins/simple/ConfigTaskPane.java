package jd.gui.skins.simple;

import javax.swing.ImageIcon;

import jd.gui.skins.simple.config.ConfigPanelAddons;
import jd.gui.skins.simple.config.ConfigPanelCaptcha;
import jd.gui.skins.simple.config.ConfigPanelDownload;
import jd.gui.skins.simple.config.ConfigPanelEventmanager;
import jd.gui.skins.simple.config.ConfigPanelGUI;
import jd.gui.skins.simple.config.ConfigPanelGeneral;
import jd.gui.skins.simple.config.ConfigPanelPluginForContainer;
import jd.gui.skins.simple.config.ConfigPanelPluginForDecrypt;
import jd.gui.skins.simple.config.ConfigPanelPluginForHost;
import jd.gui.skins.simple.config.ConfigPanelReconnect;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class ConfigTaskPane extends TreeTaskPane {

    public ConfigTaskPane(String string, ImageIcon ii) {
        super(string, ii);
        initGUI();
    }

    private void initGUI() {

        Object[] configConstructorObjects = new Object[] { JDUtilities.getConfiguration() };
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelGeneral.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.general", "General settings"), JDTheme.II("gui.images.config.home")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelDownload.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.download", "Download/Network settings"), JDTheme.II("gui.images.config.network_local")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelGUI.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.gui", "Benutzeroberfläche"), JDTheme.II("gui.images.config.gui")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelReconnect.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.reconnect", "Reconnect settings"), JDTheme.II("gui.images.config.reconnect")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelCaptcha.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.jac", "OCR Captcha settings"), JDTheme.II("gui.images.config.ocr")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelPluginForHost.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.hostPlugin", "Host Plugin settings"), JDTheme.II("gui.images.config.host")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelPluginForDecrypt.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.decryptPlugin", "Decrypter Plugin settings"), JDTheme.II("gui.images.config.decrypt")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelAddons.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.addons", "Addon manager"), JDTheme.II("gui.images.config.packagemanager")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelPluginForContainer.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.containerPlugin", "Link-Container settings"), JDTheme.II("gui.images.config.container")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelEventmanager.class, configConstructorObjects, JDLocale.L("gui.config.tabLables.eventManager", "Eventmanager"), JDTheme.II("gui.images.config.eventmanager")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelAddons.class, configConstructorObjects, "Save", JDTheme.II("gui.images.save")));
        
    }

    /**
     * 
     */
    private static final long serialVersionUID = -7720749076951577192L;

}
