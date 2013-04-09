package org.jdownloader.extensions.myjdownloader;

import jd.gui.swing.jdgui.views.settings.components.PasswordInput;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.extensions.ExtensionConfigPanel;

public class MyJDownloaderConfigPanel extends ExtensionConfigPanel<MyJDownloaderExtension> {

    /**
     * 
     */
    private static final long serialVersionUID = -7040189853650586391L;
    private TextInput         userName;
    private PasswordInput     passWord;

    public MyJDownloaderConfigPanel(MyJDownloaderExtension plg, MyDownloaderExtensionConfig config) {
        super(plg);
        userName = new TextInput(config.getStorageHandler().getKeyHandler("Email", StringKeyHandler.class));
        passWord = new PasswordInput(config.getStorageHandler().getKeyHandler("Password", StringKeyHandler.class));
        addPair("Email:", null, userName);
        addPair("Password:", null, passWord);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }

}
