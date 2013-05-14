package org.jdownloader.extensions.chat;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.utils.Application;

public class CFG_CHAT {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(ChatConfig.class, "Application.getResource(\"cfg/\" + " + ChatExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.chat.ChatConfig
    public static final ChatConfig                 CFG                       = JsonConfig.create(Application.getResource("cfg/" + ChatExtension.class.getName()), ChatConfig.class);
    public static final StorageHandler<ChatConfig> SH                        = (StorageHandler<ChatConfig>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    public static final BooleanKeyHandler          FRESH_INSTALL             = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          GUI_ENABLED               = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler          USER_COLOR_ENABLED        = SH.getKeyHandler("UserColorEnabled", BooleanKeyHandler.class);
    // 6667
    public static final IntegerKeyHandler          IRC_PORT                  = SH.getKeyHandler("IrcPort", IntegerKeyHandler.class);
    // irc.freenode.net
    public static final StringKeyHandler           IRC_SERVER                = SH.getKeyHandler("IrcServer", StringKeyHandler.class);
    // null
    public static final StringKeyHandler           PERFORM_ON_LOGIN_COMMANDS = SH.getKeyHandler("PerformOnLoginCommands", StringKeyHandler.class);
    // false
    public static final BooleanKeyHandler          ENABLED                   = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
    // #jDownloader
    public static final StringKeyHandler           CHANNEL                   = SH.getKeyHandler("Channel", StringKeyHandler.class);
    // null
    public static final StringKeyHandler           NICK                      = SH.getKeyHandler("Nick", StringKeyHandler.class);
    // null
    public static final StringKeyHandler           CHANNEL_LANGUAGE          = SH.getKeyHandler("ChannelLanguage", StringKeyHandler.class);
    // 0
    public static final IntegerKeyHandler          USER_LIST_POSITION        = SH.getKeyHandler("UserListPosition", IntegerKeyHandler.class);
}