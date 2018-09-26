package org.jdownloader.extensions.extraction.bindings.file;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.multi.ArchiveType;
import org.jdownloader.extensions.extraction.split.SplitType;
import org.jdownloader.settings.GeneralSettings;

public class FileArchiveFactory extends FileArchiveFile implements ArchiveFactory {
    private final Archive origin;

    public FileArchiveFactory(File archiveStartFile) {
        this(archiveStartFile, null);
    }

    public FileArchiveFactory(File archiveStartFile, Archive origin) {
        super(archiveStartFile);
        this.origin = origin;
    }

    public boolean isDeepExtraction() {
        return origin != null;
    }

    protected List<File> findFiles(Pattern pattern, File directory) {
        if (Application.getJavaVersion() >= Application.JAVA17) {
            return new FileArchiveFactoryNIO().findFiles(pattern, directory);
        } else {
            final ArrayList<File> ret = new ArrayList<File>();
            if (pattern != null && directory != null && directory.exists()) {
                final String[] directoryFiles = directory.list();
                if (directoryFiles != null) {
                    final String absoluteDirectoryPath = directory.getAbsolutePath();
                    for (final String directoryFile : directoryFiles) {
                        final String directoryFilePath = absoluteDirectoryPath + File.separator + directoryFile;
                        if (pattern.matcher(directoryFilePath).matches()) {
                            final File dFile = new File(directory, directoryFile);
                            if (dFile.isFile()) {
                                ret.add(dFile);
                            }
                        }
                    }
                }
            }
            return ret;
        }
    }

    public java.util.List<ArchiveFile> createPartFileList(String file, String patternString) {
        final Pattern pattern = Pattern.compile(patternString, CrossSystem.isWindows() ? Pattern.CASE_INSENSITIVE : 0);
        final List<ArchiveFile> ret = new ArrayList<ArchiveFile>();
        for (final File foundFile : findFiles(pattern, getFile().getParentFile())) {
            ret.add(new FileArchiveFile(foundFile));
        }
        return ret;
    }

    public Archive createArchive(SplitType splitType) {
        if (origin == null) {
            return new Archive(this, splitType);
        }
        return new Archive(this, splitType) {
            @Override
            public Archive getParentArchive() {
                return origin;
            }
        };
    }

    public Archive createArchive(ArchiveType archiveType) {
        if (origin == null) {
            return new Archive(this, archiveType);
        }
        return new Archive(this, archiveType) {
            @Override
            public Archive getParentArchive() {
                return origin;
            }
        };
    }

    public Collection<? extends String> getGuessedPasswordList(Archive archive) {
        final HashSet<String> ret = new HashSet<String>();
        ret.add(new File(archive.getArchiveFiles().get(0).getFilePath()).getName());
        return ret;
    }

    public void fireArchiveAddedToQueue(Archive archive) {
    }

    public String createDefaultExtractToPath(Archive archive) {
        return getFile().getParent();
    }

    public String createExtractSubPath(String path, Archive archiv) {
        try {
            final DownloadLink downloadLink = DownloadLinkArchiveFactory.getFirstDownloadLinkPart(archiv);
            if (downloadLink != null) {
                final String ret = new DownloadLinkArchiveFactory(downloadLink).createExtractSubPath(path, archiv);
                if (ret != null) {
                    return ret;
                }
            }
        } catch (final Throwable e) {
        }
        final ArchiveFile firstArchiveFile = archiv.getArchiveFiles().get(0);
        try {
            if (path.contains(PACKAGENAME)) {
                path = path.replace(PACKAGENAME, "");
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Could not set packagename for " + firstArchiveFile.getFilePath());
            }
            if (path.contains(ARCHIVENAME)) {
                if (!StringUtils.isEmpty(archiv.getName())) {
                    path = path.replace(ARCHIVENAME, archiv.getName());
                } else {
                    path = path.replace(ARCHIVENAME, "");
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Could not set archivename for " + firstArchiveFile.getFilePath());
                }
            }
            if (path.contains(HOSTER)) {
                path = path.replace(HOSTER, "");
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Could not set hoster for " + firstArchiveFile.getFilePath());
            }
            if (path.contains("$DATE:")) {
                int start = path.indexOf("$DATE:");
                int end = start + 6;
                while (end < path.length() && path.charAt(end) != '$') {
                    end++;
                }
                try {
                    SimpleDateFormat format = new SimpleDateFormat(path.substring(start + 6, end));
                    path = path.replace(path.substring(start, end + 1), format.format(new Date()));
                } catch (Exception e) {
                    path = path.replace(path.substring(start, end + 1), "");
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Could not set extraction date. Maybe pattern is wrong. For " + firstArchiveFile.getFilePath());
                }
            }
            String dif = new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()).getAbsolutePath().replace(getFile().getParent(), "");
            if (new File(dif).isAbsolute()) {
                dif = "";
            }
            path = path.replace(SUBFOLDER, dif);
            return CrossSystem.fixPathSeparators(path);
        } catch (Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        return null;
    }

    public File toFile(String path) {
        return new File(path);
    }

    @Override
    public File getFolder() {
        return getFile().getParentFile();
    }

    @Override
    public BooleanStatus getDefaultAutoExtract() {
        return BooleanStatus.UNSET;
    }
}
