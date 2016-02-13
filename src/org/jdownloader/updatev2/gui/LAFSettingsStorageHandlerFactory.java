package org.jdownloader.updatev2.gui;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JsonKeyValueStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.StorageHandlerFactory;
import org.appwork.storage.config.handler.DefaultFactoryInterface;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.swing.synthetica.SyntheticaSettings;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.extmanager.LoggerFactory;

public class LAFSettingsStorageHandlerFactory implements StorageHandlerFactory<LAFSettings> {

    @Override
    public StorageHandler<LAFSettings> create(File path, Class<LAFSettings> configInterface) {
        StorageHandler<LAFSettings> ret = new StorageHandler<LAFSettings>(path, configInterface) {

            @Override
            protected void preInit(File path, Class<LAFSettings> configInterfac) {
                setDefaultFactory(new DefaultFactoryInterface() {

                    @Override
                    public Object getDefaultValue(KeyHandler<?> handler, Object o) {
                        Object def = o;
                        try {
                            def = handler.getGetMethod().invoke(LAFOptions.getLookAndFeelExtension(), new Object[] {});
                        } catch (Throwable e) {
                            LoggerFactory.getDefaultLogger().log(e);
                        }
                        return def;

                    }
                });
            }
        };
        for (Entry<Method, KeyHandler<?>> e : ret.getMap().entrySet()) {
            e.getValue().setAllowWriteDefaultObjects(false);
        }
        // restore old storage
        try {
            File oldLafSettingsFile = Application.getResource("cfg/org.appwork.swing.synthetica.SyntheticaSettings.json");
            if (oldLafSettingsFile.exists()) {
                JsonKeyValueStorage prim = (JsonKeyValueStorage) JsonConfig.create(SyntheticaSettings.class)._getStorageHandler().getPrimitiveStorage();
                for (String s : prim.getKeys()) {
                    KeyHandler<Object> keyH = ret.getKeyHandler(s.toLowerCase(Locale.ENGLISH));
                    Object oldValue = prim.get(s, null);
                    if (keyH != null && !Objects.equals(oldValue, keyH.getDefaultValue())) {
                        keyH.setValue(oldValue);
                    }
                }
                JsonConfig.create(SyntheticaSettings.class)._getStorageHandler().setSaveInShutdownHookEnabled(false);
                oldLafSettingsFile.delete();
            }
        } catch (Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }
        return ret;
    }

    @Override
    public StorageHandler<LAFSettings> create(String urlPath, Class<LAFSettings> configInterface) {
        throw new WTFException("Not Implemented");
    }

}
