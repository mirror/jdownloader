package org.jdownloader.extensions.myjdownloader;

import javax.swing.JTextArea;

import jd.gui.swing.jdgui.views.settings.components.PasswordInput;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.ExtensionConfigPanel;

public class MyJDownloaderConfigPanel extends ExtensionConfigPanel<MyJDownloaderExtension> {

    /**
     * 
     */
    private static final long serialVersionUID = -7040189853650586391L;
    private TextInput         userName;
    private PasswordInput     passWord;
    private JTextArea         currentIP        = null;

    public MyJDownloaderConfigPanel(MyJDownloaderExtension plg, MyDownloaderExtensionConfig config) {
        super(plg);
        userName = new TextInput(config.getStorageHandler().getKeyHandler("Username", StringKeyHandler.class));
        passWord = new PasswordInput(config.getStorageHandler().getKeyHandler("Password", StringKeyHandler.class));
        currentIP = addDescription("CurrentIP: not connected");
        addPair("Username:", null, userName);
        addPair("Password:", null, passWord);
    }

    public JTextArea getCurrentIP() {
        return currentIP;
    }

    public void setCurrentIP(final String ip) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (StringUtils.isEmpty(ip)) {
                    currentIP.setText("CurrentIP: not connected");
                } else {
                    currentIP.setText("CurrentIP: " + ip);
                }
            }
        };
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }

}
