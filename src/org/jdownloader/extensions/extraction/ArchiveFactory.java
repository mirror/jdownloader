package org.jdownloader.extensions.extraction;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public interface ArchiveFactory extends ArchiveFile {
    public static final String SUBFOLDER   = "%SUBFOLDER%";
    public static final String ARCHIVENAME = "%ARCHIVENAME%";
    public static final String HOSTER      = "%HOSTER%";
    public static final String PACKAGENAME = "%PACKAGENAME%";

    java.util.List<ArchiveFile> createPartFileList(String file, String pattern);

    // for (DownloadLink link1 : archive.getDownloadLinks()) {
    // link1.setProperty(ExtractionExtension.DOWNLOADLINK_KEY_EXTRACTEDPATH,
    // dl.getAbsolutePath());
    // }
    public File getFolder();

    // void fireExtractToChange(Archive archive);

    Collection<? extends String> getGuessedPasswordList(Archive archive);

    // archive.getFirstDownloadLink().getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
    // archive.getFirstDownloadLink().getLinkStatus().setErrorMessage(null);
    void fireArchiveAddedToQueue(Archive archive);

    /*
     * if (archive.getFactory().getProperty(DOWNLOADLINK_KEY_EXTRACTTOPATH) != null){ return (File)
     * archive.getFactory().getProperty(DOWNLOADLINK_KEY_EXTRACTTOPATH); } if (archive.getFactory() instanceof DummyDownloadLink) return new
     * File(archive.getFactory().getFileOutput()).getParentFile();
     */
    String createDefaultExtractToPath(Archive archive);

    String createExtractSubPath(String path, Archive archiv);

    Archive createArchive();

    File toFile(String path);

    String getID();

    void onArchiveFinished(Archive archive);

}
