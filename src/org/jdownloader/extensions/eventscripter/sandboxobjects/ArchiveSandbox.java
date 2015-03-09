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
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.multi.ArchiveType;

public class ArchiveSandbox {

    private Archive archive;

    public ArchiveSandbox(Archive archive) {
        this.archive = archive;

        // archive.getName();

    }

    public ArchiveSandbox() {
        // {"variant":null,"host":null,"name":null,"comment":null,"availability":null,"variants":false,"priority":"DEFAULT","packageUUID":-1,"bytesTotal":-1,"uuid":-1,"url":null,"enabled":false}

    }

    public String getExtractionLog() {
        File file = archive.getExtractLogFile();
        if (file != null && file.exists()) {
            try {
                return IO.readFileToString(file);
            } catch (IOException e) {
                throw new WTFException(e);
            }
        }
        return null;
    }

    public String getUsedPassword() {
        if (archive == null) {
            return null;
        }
        return archive.getFinalPassword();
    }

    public DownloadLinkSandBox[] getDownloadLinks() {
        if (archive == null) {
            return null;
        }
        ArrayList<DownloadLinkSandBox> ret = new ArrayList<DownloadLinkSandBox>();

        for (ArchiveFile file : archive.getArchiveFiles()) {
            if (file instanceof DownloadLinkArchiveFile) {
                for (DownloadLink dl : ((DownloadLinkArchiveFile) file).getDownloadLinks()) {
                    ret.add(new DownloadLinkSandBox(dl));
                }

            }
        }
        return ret.toArray(new DownloadLinkSandBox[] {});

    }

    public Object getInfo() {
        if (archive == null) {
            return null;
        }

        return ((ScriptThread) Thread.currentThread()).toNative(archive.getSettings());

    }

    public String getArchiveType() {
        if (archive == null) {
            return ArchiveType.RAR_MULTI.name();
        }

        return archive.getSplitType() == null ? (archive.getArchiveType() + "") : (archive.getSplitType() + "");
    }

    public String[] getExtractedFiles() {
        if (archive == null || archive.getExtractedFiles() == null || archive.getExtractedFiles().size() == 0) {
            return null;
        }
        ArrayList<String> lst = new ArrayList<String>();
        for (File s : archive.getExtractedFiles()) {
            lst.add(s.getAbsolutePath());
        }

        return lst.toArray(new String[] {});
    }

    public String getName() {
        if (archive == null) {
            return null;
        }
        return archive.getName();
    }

    public String getFolder() {
        if (archive == null) {
            return null;
        }
        return archive.getFolder().getAbsolutePath();
    }
}
