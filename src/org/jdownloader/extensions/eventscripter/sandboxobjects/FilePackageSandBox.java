package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.Application;
import org.jdownloader.extensions.eventscripter.ScriptAPI;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
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

    public ArchiveSandbox[] getArchives() {
        if (filePackage == null) {
            return null;
        }

        final HashSet<String> ret = new HashSet<String>();
        final ArrayList<ArchiveSandbox> list = new ArrayList<ArchiveSandbox>();

        filePackage.getModifyLock().runReadLock(new Runnable() {

            @Override
            public void run() {
                for (DownloadLink link : filePackage.getChildren()) {
                    Archive archive = ArchiveValidator.EXTENSION.getArchiveByFactory(new DownloadLinkArchiveFactory(link));
                    if (archive != null) {
                        if (ret.add(archive.getArchiveID())) {
                            list.add(new ArchiveSandbox(archive));
                        }
                        continue;
                    }
                    ArrayList<Object> list = new ArrayList<Object>();
                    list.add(link);
                    List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(list);

                    archive = (archives == null || archives.size() == 0) ? null : archives.get(0);
                    if (archive != null) {
                        if (ret.add(archive.getArchiveID())) {
                            list.add(new ArchiveSandbox(archive));
                        }

                    }
                }

            }
        });

        return list.toArray(new ArchiveSandbox[] {});

    }

    public DownloadLinkSandBox[] getDownloadLinks() {
        if (filePackage == null) {
            return null;
        }
        final ArrayList<DownloadLinkSandBox> ret = new ArrayList<DownloadLinkSandBox>();
        filePackage.getModifyLock().runReadLock(new Runnable() {

            @Override
            public void run() {
                for (DownloadLink link : filePackage.getChildren()) {
                    ret.add(new DownloadLinkSandBox(link));
                }

            }
        });
        return ret.toArray(new DownloadLinkSandBox[] {});

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

    public String getComment() {
        if (filePackage == null) {
            return null;
        }

        return filePackage.getComment();
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
                    // only enabled links count. this is the same in the jd gui, so let's use the same logic here
                    if (link.isEnabled() && !FinalLinkState.CheckFinished(link.getFinalLinkState())) {
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
