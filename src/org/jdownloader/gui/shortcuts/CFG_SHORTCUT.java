package org.jdownloader.gui.shortcuts;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;

public class CFG_SHORTCUT {
    public static void main(String[] args) {
        System.out.println(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

        ConfigUtils.printStaticMappings(ShortcutSettings.class);

    }

    // Static Mappings for interface org.jdownloader.gui.shortcuts.ShortcutSettings
    public static final ShortcutSettings                 CFG               = JsonConfig.create(ShortcutSettings.class);
    public static final StorageHandler<ShortcutSettings> SH                = (StorageHandler<ShortcutSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // cmd pressed A
    public static final StringKeyHandler                 TEXT_FIELD_SELECT = SH.getKeyHandler("TextFieldSelect", StringKeyHandler.class);
    // cmd pressed C
    public static final StringKeyHandler                 TEXT_FIELD_COPY   = SH.getKeyHandler("TextFieldCopy", StringKeyHandler.class);
    // cmd pressed X
    public static final StringKeyHandler                 TEXT_FIELD_CUT    = SH.getKeyHandler("TextFieldCut", StringKeyHandler.class);
    // cmd pressed V
    public static final StringKeyHandler                 TEXT_FIELD_PASTE  = SH.getKeyHandler("TextFieldPaste", StringKeyHandler.class);
    // pressed DELETE
    public static final StringKeyHandler                 TEXT_FIELD_DELETE = SH.getKeyHandler("TextFieldDelete", StringKeyHandler.class);
}