package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.IO;
import org.jdownloader.extensions.eventscripter.ScriptThread;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.multi.ArchiveType;

public class ArchiveSandbox {

    private final Archive archive;

    public ArchiveSandbox(Archive archive) {
        this.archive = archive;
    }

    public ArchiveSandbox() {
        this(null);
    }

    public String getExtractionLog() {
        if (archive != null) {
            final File file = archive.getExtractLogFile();
            if (file != null && file.exists()) {
                try {
                    return IO.readFileToString(file);
                } catch (IOException e) {
                    throw new WTFException(e);
                }
            }
        }
        return null;
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

    public DownloadLinkSandBox[] getDownloadLinks() {
        if (archive != null) {
            final ArrayList<DownloadLinkSandBox> ret = new ArrayList<DownloadLinkSandBox>();
            for (final ArchiveFile file : archive.getArchiveFiles()) {
                if (file instanceof DownloadLinkArchiveFile) {
                    for (final DownloadLink dl : ((DownloadLinkArchiveFile) file).getDownloadLinks()) {
                        ret.add(new DownloadLinkSandBox(dl));
                    }
                }
            }
            if (ret.size() > 0) {
                return ret.toArray(new DownloadLinkSandBox[] {});
            }
        }
        return null;
    }

    public Object getInfo() {
        if (archive != null) {
            return ((ScriptThread) Thread.currentThread()).toNative(archive.getSettings());
        }
        return null;
    }

    public String getArchiveType() {
        if (archive != null) {
            return archive.getSplitType() == null ? (String.valueOf(archive.getArchiveType())) : (String.valueOf(archive.getSplitType()));
        }
        return ArchiveType.RAR_MULTI.name();
    }

    public String getExtractToFolder() {
        return ExtractionExtension.getInstance().getFinalExtractToFolder(archive, false).toString();
    }

    public String[] getExtractedFiles() {
        if (archive != null && archive.getExtractedFiles() != null && archive.getExtractedFiles().size() > 0) {
            final ArrayList<String> lst = new ArrayList<String>();
            for (final File s : archive.getExtractedFiles()) {
                lst.add(s.getAbsolutePath());
            }
            if (lst.size() > 0) {
                return lst.toArray(new String[] {});
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
