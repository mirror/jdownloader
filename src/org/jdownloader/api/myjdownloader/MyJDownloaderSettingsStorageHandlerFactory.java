package org.jdownloader.api.myjdownloader;

import java.io.File;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.StorageHandlerFactory;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.DebugMode;
import org.appwork.utils.swing.dialog.Dialog;

public class MyJDownloaderSettingsStorageHandlerFactory implements StorageHandlerFactory<MyJDownloaderSettings> {
    @Override
    public StorageHandler<MyJDownloaderSettings> create(File path, Class<MyJDownloaderSettings> configInterface) {
        return new StorageHandler<MyJDownloaderSettings>(path, configInterface) {
            @Override
            protected void error(final Throwable e) {
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    new Thread("ERROR THROWER") {
                        @Override
                        public void run() {
                            Dialog.getInstance().showExceptionDialog(e.getClass().getSimpleName(), e.getMessage(), e);
                        }
                    }.start();
                }
            }

            protected void preInit(File file, java.lang.Class<MyJDownloaderSettings> configInterfac) {
                File jsonFile = new File(file.getAbsolutePath() + ".json");
                if (jsonFile.exists()) {
                    return;
                }
                File oldFile = new File(jsonFile.getParentFile(), "org.jdownloader.extensions.myjdownloader.MyJDownloaderExtension.json");
                if (oldFile.exists()) {
                    oldFile.renameTo(jsonFile);
                }
            };
        };
    }

    @Override
    public StorageHandler<MyJDownloaderSettings> create(String urlPath, Class<MyJDownloaderSettings> configInterface) {
        throw new WTFException("Not Implemented");
    }
}
