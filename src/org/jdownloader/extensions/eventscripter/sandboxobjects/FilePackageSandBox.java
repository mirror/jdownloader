package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.Application;
import org.jdownloader.extensions.eventscripter.ScriptAPI;
import org.jdownloader.plugins.FinalLinkState;

@ScriptAPI(description = "The context download list package")
public class FilePackageSandBox {

    private FilePackage filePackage;

    public FilePackageSandBox(FilePackage parentNode) {
        this.filePackage = parentNode;
    }

    public FilePackageSandBox() {

    }

    public long getBytesLoaded() {
        if (filePackage == null) {
            return 0;
        }
        final AtomicLong size = new AtomicLong(0);
        filePackage.getModifyLock().runReadLock(new Runnable() {

            @Override
            public void run() {
                for (DownloadLink link : filePackage.getChildren()) {
                    size.addAndGet(link.getView().getBytesLoaded());
                }

            }
        });
        return size.get();
    }

    public long getBytesTotal() {
        if (filePackage == null) {
            return 0;
        }

        final AtomicLong size = new AtomicLong(0);
        filePackage.getModifyLock().runReadLock(new Runnable() {

            @Override
            public void run() {
                for (DownloadLink link : filePackage.getChildren()) {
                    size.addAndGet(link.getView().getBytesTotal());
                }

            }
        });
        return size.get();
    }

    public boolean isFinished() {
        if (filePackage == null) {
            return false;
        }
        final AtomicBoolean finished = new AtomicBoolean(true);
        filePackage.getModifyLock().runReadLock(new Runnable() {

            @Override
            public void run() {
                for (DownloadLink link : filePackage.getChildren()) {
                    if (!FinalLinkState.CheckFinished(link.getFinalLinkState())) {
                        finished.set(false);
                        break;
                    }
                }

            }
        });

        return finished.get();
    }

    public String getDownloadFolder() {
        if (filePackage == null) {
            return Application.getResource("").getAbsolutePath();
        }
        return filePackage.getDownloadDirectory();
    }

    public String getName() {
        if (filePackage == null) {
            return "Example FilePackage Name";
        }
        return filePackage.getName();
    }

}
