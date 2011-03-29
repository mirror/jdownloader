package org.jdownloader.extensions.chat;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;

public interface ChatConfig extends ConfigInterface {
    @DefaultStringValue("#jDownloader")
    String getChannel();

    @DefaultBooleanValue(true)
    boolean isUserColorEnabled();

    String getNick();

    void setNick(String nick);

    String getChannelLanguage();

    void setChannelLanguage(String lng);

    @DefaultIntValue(0)
    int getUserListPosition();

    @DefaultStringValue("irc.freenode.net")
    String getIrcServer();

    @DefaultIntValue(6667)
    int getIrcPort();

    String getPerformOnLoginCommands();

    void setUserColorEnabled(boolean selected);

    void setUserListPosition(int selectedIndex);

    void setPerformOnLoginCommands(String text);

}
