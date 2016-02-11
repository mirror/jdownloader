package org.jdownloader.updatev2.gui;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map.Entry;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.StorageHandlerFactory;
import org.appwork.storage.config.handler.DefaultFactoryInterface;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
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
        return ret;
    }

    @Override
    public StorageHandler<LAFSettings> create(String urlPath, Class<LAFSettings> configInterface) {
        throw new WTFException("Not Implemented");
    }

}
