package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdownloader.extensions.eventscripter.EnvironmentException;
import org.jdownloader.extensions.eventscripter.ScriptThread;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionExtension;

public class ArchiveSandbox {
    private final Archive archive;

    public ArchiveSandbox(Archive archive) {
        this.archive = archive;
    }

    public ArchiveSandbox() {
        this(null);
    }

    @Override
    public int hashCode() {
        if (archive != null) {
            return archive.hashCode();
        } else {
            return super.hashCode();
        }
    }

    public ArchiveSandbox getParentArchive() {
        if (archive != null) {
            final Archive parent = archive.getParentArchive();
            if (parent != null) {
                return new ArchiveSandbox(parent);
            }
        }
        return null;
    }

    public ArchiveFileSandbox getLastArchiveFile() {
        if (archive != null) {
            final ArchiveFile lastArchiveFile = archive.getLastArchiveFile();
            if (lastArchiveFile != null) {
                return new ArchiveFileSandbox(lastArchiveFile);
            }
        }
        return null;
    }

    public ArchiveSandbox getRootArchive() {
        if (archive != null) {
            final Archive root = archive.getRootArchive();
            if (root != null) {
                return new ArchiveSandbox(root);
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArchiveSandbox) {
            return ((ArchiveSandbox) obj).archive == archive;
        } else {
            return super.equals(obj);
        }
    }

    @Deprecated
    public String getExtractionLog() {
        return "DEPRECATED, this method may be removed in future version";
    }

    public boolean isPasswordProtected() {
        if (archive != null) {
            return archive.isProtected() || archive.isPasswordRequiredToOpen();
        } else {
            return false;
        }
    }

    public String getUsedPassword() {
        if (archive != null) {
            return archive.getFinalPassword();
        } else {
            return null;
        }
    }

    public String getArchiveID() {
        if (archive != null) {
            return archive.getSettings()._getArchiveID();
        } else {
            return null;
        }
    }

    public String getSettingsID() {
        if (archive != null) {
            return archive.getSettings()._getSettingsID();
        } else {
            return null;
        }
    }

    public List<String> getPasswords() {
        if (archive != null) {
            return archive.getSettings().getPasswords();
        } else {
            return null;
        }
    }

    public void setPasswords(List<String> passwords) {
        if (archive != null) {
            archive.setPasswords(passwords);
        }
    }

    @Deprecated
    public DownloadLinkSandBox[] getDownloadLinks() {
        final ArchiveFileSandbox[] links = getArchiveFiles();
        if (links != null) {
            final ArrayList<DownloadLinkSandBox> ret = new ArrayList<DownloadLinkSandBox>();
            for (ArchiveFileSandbox link : links) {
                final DownloadLinkSandBox[] downloadLinks = link.getDownloadLinks();
                if (downloadLinks != null) {
                    ret.addAll(Arrays.asList(downloadLinks));
                }
            }
            if (ret.size() > 0) {
                return ret.toArray(new DownloadLinkSandBox[] {});
            }
        }
        return null;
    }

    public ArchiveFileSandbox[] getArchiveFiles() {
        if (archive != null) {
            final ArrayList<ArchiveFileSandbox> ret = new ArrayList<ArchiveFileSandbox>();
            for (final ArchiveFile file : archive.getArchiveFiles()) {
                ret.add(new ArchiveFileSandbox(file));
            }
            if (ret.size() > 0) {
                return ret.toArray(new ArchiveFileSandbox[] {});
            }
        }
        return null;
    }

    public Object getInfo() {
        if (archive != null) {
            return ((ScriptThread) Thread.currentThread()).toNative(archive.getSettings());
        } else {
            return null;
        }
    }

    public String getArchiveType() {
        if (archive != null) {
            return archive.getSplitType() == null ? (String.valueOf(archive.getArchiveType())) : (String.valueOf(archive.getSplitType()));
        } else {
            return null;
        }
    }

    public String getExtractToFolder() {
        return ExtractionExtension.getInstance().getFinalExtractToFolder(archive, false).toString();
    }

    public String[] getExtractedFiles() {
        if (archive != null && archive.getExtractedFiles() != null && archive.getExtractedFiles().size() > 0) {
            final ArrayList<String> ret = new ArrayList<String>(archive.getExtractedFiles().size());
            for (final File file : archive.getExtractedFiles()) {
                ret.add(file.getAbsolutePath());
            }
            if (ret.size() > 0) {
                return ret.toArray(new String[] {});
            }
        }
        return null;
    }

    public FilePathSandbox[] getExtractedFilePaths() throws EnvironmentException {
        final String[] files = getExtractedFiles();
        if (files != null && files.length > 0) {
            final ArrayList<FilePathSandbox> ret = new ArrayList<FilePathSandbox>(files.length);
            for (final String file : files) {
                ret.add(ScriptEnvironment.getPath(file));
            }
            if (ret.size() > 0) {
                return ret.toArray(new FilePathSandbox[] {});
            }
        }
        return null;
    }

    public String getName() {
        if (archive != null) {
            return archive.getName();
        } else {
            return null;
        }
    }

    public String getFolder() {
        if (archive != null) {
            return archive.getFolder().getAbsolutePath();
        } else {
            return null;
        }
    }
}
