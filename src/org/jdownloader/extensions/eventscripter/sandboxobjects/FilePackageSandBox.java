package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.PackageController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;

import org.appwork.utils.Application;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.eventscripter.ScriptAPI;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;

@ScriptAPI(description = "The context download list package")
public class FilePackageSandBox {
    private final FilePackage filePackage;

    public FilePackageSandBox(FilePackage parentNode) {
        this.filePackage = parentNode;
    }

    public FilePackageSandBox() {
        this(null);
    }

    public long getBytesLoaded() {
        if (filePackage == null) {
            return 0;
        } else {
            return new FilePackageView(filePackage).aggregate().getDone();
        }
    }

    public ArchiveSandbox[] getArchives() {
        if (filePackage == null) {
            return null;
        } else {
            final ArrayList<ArchiveSandbox> list = new ArrayList<ArchiveSandbox>();
            filePackage.getModifyLock().runReadLock(new Runnable() {
                @Override
                public void run() {
                    final List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(filePackage.getChildren());
                    if (archives != null) {
                        for (final Archive archive : archives) {
                            list.add(new ArchiveSandbox(archive));
                        }
                    }
                }
            });
            return list.toArray(new ArchiveSandbox[] {});
        }
    }

    public DownloadLinkSandBox[] getDownloadLinks() {
        if (filePackage == null) {
            return null;
        } else {
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
    }

    public long getBytesTotal() {
        if (filePackage == null) {
            return 0;
        } else {
            return new FilePackageView(filePackage).aggregate().getSize();
        }
    }

    public String getComment() {
        if (filePackage == null) {
            return null;
        } else {
            return filePackage.getComment();
        }
    }

    public void setComment(String comment) {
        if (filePackage != null) {
            filePackage.setComment(comment);
        }
    }

    public void setName(String name) {
        if (filePackage != null) {
            filePackage.setName(name);
        }
    }

    @Override
    public int hashCode() {
        if (filePackage != null) {
            return filePackage.hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FilePackageSandBox) {
            return ((FilePackageSandBox) obj).filePackage == filePackage;
        } else {
            return super.equals(obj);
        }
    }

    public boolean isFinished() {
        if (filePackage == null) {
            return false;
        } else {
            return new FilePackageView(filePackage).aggregate().isFinished();
        }
    }

    public boolean remove() {
        if (filePackage != null && !FilePackage.isDefaultFilePackage(filePackage)) {
            final PackageController<FilePackage, DownloadLink> controller = filePackage.getControlledBy();
            if (controller != null) {
                controller.removePackage(filePackage);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String getUUID() {
        if (filePackage != null) {
            return filePackage.getUniqueID().toString();
        } else {
            return null;
        }
    }

    public long getAddedDate() {
        if (filePackage != null) {
            return filePackage.getCreated();
        } else {
            return -1;
        }
    }

    public long getFinishedDate() {
        if (filePackage != null) {
            return new FilePackageView(filePackage).aggregate().getFinishedDate();
        } else {
            return -1;
        }
    }

    public String getDownloadFolder() {
        if (filePackage == null) {
            return Application.getResource("").getAbsolutePath();
        } else {
            return filePackage.getDownloadDirectory();
        }
    }

    public void setDownloadFolder(String downloadFolder) {
        if (filePackage != null) {
            DownloadWatchDog.getInstance().setDownloadDirectory(filePackage, downloadFolder);
        }
    }

    @Override
    public String toString() {
        return "FilePackage Instance: " + getName();
    }

    public String getPriority() {
        if (filePackage != null) {
            return filePackage.getPriorityEnum().name();
        } else {
            return Priority.DEFAULT.name();
        }
    }

    public void setPriority(final String priority) {
        if (filePackage != null) {
            try {
                filePackage.setPriorityEnum(Priority.valueOf(priority));
            } catch (final Throwable e) {
                filePackage.setPriorityEnum(Priority.DEFAULT);
            }
        }
    }

    public String getName() {
        if (filePackage == null) {
            return "Example FilePackage Name";
        } else {
            return filePackage.getName();
        }
    }
}
