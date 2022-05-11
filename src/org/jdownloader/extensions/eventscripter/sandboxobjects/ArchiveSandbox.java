package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.jdownloader.extensions.eventscripter.EnvironmentException;
import org.jdownloader.extensions.eventscripter.ScriptThread;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFile;
import org.jdownloader.extensions.extraction.multi.ArchiveType;

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
        final ArchiveLinkSandbox[] links = getArchiveLinks();
        if (links != null) {
            final ArrayList<DownloadLinkSandBox> ret = new ArrayList<DownloadLinkSandBox>();
            for (ArchiveLinkSandbox link : links) {
                final DownloadLinkSandBox dlsb = link.getDownloadLinkSandBox();
                if (dlsb != null) {
                    ret.add(dlsb);
                }
            }
            if (ret.size() > 0) {
                return ret.toArray(new DownloadLinkSandBox[] {});
            }
        }
        return null;
    }

    public ArchiveLinkSandbox[] getArchiveLinks() {
        if (archive != null) {
            final ArrayList<ArchiveLinkSandbox> ret = new ArrayList<ArchiveLinkSandbox>();
            for (final ArchiveFile file : archive.getArchiveFiles()) {
                if (file instanceof DownloadLinkArchiveFile) {
                    for (final DownloadLink dl : ((DownloadLinkArchiveFile) file).getDownloadLinks()) {
                        ret.add(new ArchiveLinkSandbox(new DownloadLinkSandBox(dl)));
                    }
                } else if (file instanceof CrawledLinkArchiveFile) {
                    for (final CrawledLink dl : ((CrawledLinkArchiveFile) file).getLinks()) {
                        ret.add(new ArchiveLinkSandbox(new CrawledLinkSandbox(dl)));
                    }
                } else if (file instanceof MissingArchiveFile) {
                    ret.add(new ArchiveLinkSandbox(((MissingArchiveFile) file).getFilePath()));
                } else if (file instanceof FileArchiveFile) {
                    ret.add(new ArchiveLinkSandbox(ScriptEnvironment.getPath(((FileArchiveFile) file).getFilePath())));
                }
            }
            if (ret.size() > 0) {
                return ret.toArray(new ArchiveLinkSandbox[] {});
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
            return ArchiveType.RAR_MULTI.name();
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
