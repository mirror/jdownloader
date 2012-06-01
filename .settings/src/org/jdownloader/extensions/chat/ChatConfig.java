package org.jdownloader.extensions.chat;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface ChatConfig extends ExtensionConfigInterface {
    @DefaultStringValue("#jDownloader")
    @AboutConfig
    String getChannel();

    void setChannel(String channel);

    @AboutConfig
    String getChannelLanguage();

    @DefaultIntValue(6667)
    @AboutConfig
    int getIrcPort();

    void setIrcPort(int port);

    @DefaultStringValue("irc.freenode.net")
    @AboutConfig
    String getIrcServer();

    void setIrcServer(String server);

    @AboutConfig
    String getNick();

    @AboutConfig
    String getPerformOnLoginCommands();

    @DefaultIntValue(0)
    @AboutConfig
    @SpinnerValidator(min = 0, max = 1)
    int getUserListPosition();

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isUserColorEnabled();

    void setChannelLanguage(String lng);

    void setNick(String nick);

    void setPerformOnLoginCommands(String text);

    void setUserColorEnabled(boolean selected);

    void setUserListPosition(int selectedIndex);

}
