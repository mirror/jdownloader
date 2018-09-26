package org.jdownloader.extensions.extraction.bindings.downloadlink;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNodeVisitor;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.extensions.extraction.multi.ArchiveType;
import org.jdownloader.extensions.extraction.split.SplitType;
import org.jdownloader.settings.GeneralSettings;

public class DownloadLinkArchiveFactory extends DownloadLinkArchiveFile implements ArchiveFactory {
    public static final String DOWNLOADLINK_KEY_EXTRACTEDPATH = "EXTRACTEDPATH";

    public DownloadLinkArchiveFactory(DownloadLink link) {
        super(link);
    }

    public boolean isDeepExtraction() {
        return false;
    }

    public String createExtractSubPath(String path, Archive archive) {
        final DownloadLink link = getFirstDownloadLinkPart(archive);
        try {
            if (path.contains(PACKAGENAME)) {
                final String packageName = CrossSystem.alleviatePathParts(link.getLastValidFilePackage().getName());
                if (!StringUtils.isEmpty(packageName)) {
                    path = path.replace(PACKAGENAME, packageName);
                } else {
                    path = path.replace(PACKAGENAME, "");
                }
            }
            if (path.contains(ARCHIVENAME)) {
                final String archiveName = CrossSystem.alleviatePathParts(archive.getName());
                if (!StringUtils.isEmpty(archiveName)) {
                    path = path.replace(ARCHIVENAME, archiveName);
                } else {
                    path = path.replace(ARCHIVENAME, "");
                }
            }
            if (path.contains(HOSTER)) {
                final String hostName = CrossSystem.alleviatePathParts(link.getHost());
                if (!StringUtils.isEmpty(hostName)) {
                    path = path.replace(HOSTER, hostName);
                } else {
                    path = path.replace(HOSTER, "");
                }
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
                } catch (Throwable e) {
                    path = path.replace(path.substring(start, end + 1), "");
                }
            }
            if (path.contains(SUBFOLDER)) {
                final String dif = new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()).getAbsolutePath().replace(new File(link.getFileOutput(false, true)).getParent(), "");
                if (StringUtils.isEmpty(dif) || new File(dif).isAbsolute()) {
                    path = path.replace(SUBFOLDER, "");
                } else {
                    path = path.replace(SUBFOLDER, CrossSystem.alleviatePathParts(dif));
                }
            }
            return CrossSystem.fixPathSeparators(path);
        } catch (Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        return null;
    }

    protected String modifyPartFilePattern(String pattern) {
        final HashSet<Character> unsafeChars = new HashSet<Character>();
        for (final DownloadLink downloadLink : getDownloadLinks()) {
            final PluginForHost defaultPlugin = downloadLink.getDefaultPlugin();
            if (defaultPlugin != null) {
                final char[] fileNameReplaceMap = defaultPlugin.getFilenameReplaceMap();
                if (fileNameReplaceMap != null) {
                    for (final char replace : fileNameReplaceMap) {
                        unsafeChars.add(Character.valueOf(replace));
                    }
                }
            }
        }
        if (unsafeChars.size() > 0) {
            final String filePathPattern = new Regex(pattern, "\\^\\\\Q(.*?)\\\\E").getMatch(0);
            final File filePath = new File(filePathPattern);
            final String fileNamePattern = filePath.getName();
            String modifiedFilaNamePattern = fileNamePattern;
            for (final Character unsafeChar : unsafeChars) {
                modifiedFilaNamePattern = modifiedFilaNamePattern.replace(unsafeChar.toString(), "\\\\E.\\\\Q");
            }
            // we need to escape $ because it has special meaning in String.replaceFirst
            modifiedFilaNamePattern = modifiedFilaNamePattern.replace("$", "\\$");
            final String modifiedFilePathPattern = filePathPattern.replaceFirst(Pattern.quote(fileNamePattern) + "$", modifiedFilaNamePattern);
            final String ret = pattern.replace("^\\Q" + filePathPattern + "\\E", "^\\Q" + modifiedFilePathPattern + "\\E");
            return ret;
        } else {
            return pattern;
        }
    }

    public List<ArchiveFile> createPartFileList(final String file, final String archivePartFilePattern) {
        final String pattern = modifyPartFilePattern(archivePartFilePattern);
        final Pattern pat = Pattern.compile(pattern, CrossSystem.isWindows() ? Pattern.CASE_INSENSITIVE : 0);
        final String fileParent = new File(file).getParent();
        final HashMap<String, ArchiveFile> map = new HashMap<String, ArchiveFile>();
        DownloadController.getInstance().visitNodes(new AbstractNodeVisitor<DownloadLink, FilePackage>() {
            @Override
            public Boolean visitPackageNode(FilePackage pkg) {
                if (CrossSystem.isWindows()) {
                    final String downloadDirectory = CrossSystem.fixPathSeparators(pkg.getDownloadDirectory());
                    return StringUtils.equalsIgnoreCase(fileParent, downloadDirectory);
                } else {
                    final String downloadDirectory = CrossSystem.fixPathSeparators(pkg.getDownloadDirectory());
                    if (downloadDirectory.endsWith("/")) {
                        return StringUtils.equals(fileParent.concat("/"), downloadDirectory);
                    } else {
                        return StringUtils.equals(fileParent, downloadDirectory);
                    }
                }
            }

            @Override
            public Boolean visitChildrenNode(DownloadLink node) {
                final String nodeFile = node.getFileOutput(false, true);
                if (nodeFile == null) {
                    // http://board.jdownloader.org/showthread.php?t=59031
                    return false;
                }
                if (pat.matcher(nodeFile).matches()) {
                    final String nodeName = node.getView().getDisplayName();
                    DownloadLinkArchiveFile af = (DownloadLinkArchiveFile) map.get(nodeName);
                    if (af == null) {
                        af = new DownloadLinkArchiveFile(node);
                        map.put(nodeName, af);
                    } else {
                        af.addMirror(node);
                    }
                }
                return true;
            }
        }, true);
        final List<ArchiveFile> localFiles = new FileArchiveFactory(new File(getFilePath())).createPartFileList(file, pattern);
        for (ArchiveFile localFile : localFiles) {
            final ArchiveFile archiveFile = map.get(localFile.getName());
            if (archiveFile == null) {
                // There is a matching local file, without a downloadlink link. this can happen if the user removes finished downloads
                // immediatelly
                map.put(localFile.getName(), localFile);
            } else if (archiveFile instanceof DownloadLinkArchiveFile) {
                final DownloadLinkArchiveFile af = (DownloadLinkArchiveFile) archiveFile;
                af.setFileArchiveFileExists(localFile.exists());
            }
        }
        return new ArrayList<ArchiveFile>(map.values());
    }

    public File toFile(String path) {
        return new File(path);
    }

    public Collection<? extends String> getGuessedPasswordList(Archive archive) {
        final LinkedHashSet<String> ret = new LinkedHashSet<String>();
        for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
            if (archiveFile instanceof DownloadLinkArchiveFile) {
                for (final DownloadLink downloadLink : ((DownloadLinkArchiveFile) archiveFile).getDownloadLinks()) {
                    final String pw = downloadLink.getDownloadPassword();
                    if (StringUtils.isEmpty(pw) == false) {
                        ret.add(pw);
                    }
                }
            }
        }
        return ret;
    }

    public void fireArchiveAddedToQueue(Archive archive) {
        for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
            if (archiveFile instanceof DownloadLinkArchiveFile) {
                for (final DownloadLink downloadLink : ((DownloadLinkArchiveFile) archiveFile).getDownloadLinks()) {
                    downloadLink.setExtractionStatus(null);
                }
            }
        }
    }

    public static DownloadLink getFirstDownloadLinkPart(Archive archive) {
        for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
            if (archiveFile instanceof DownloadLinkArchiveFile) {
                return ((DownloadLinkArchiveFile) archiveFile).getDownloadLinks().get(0);
            }
        }
        throw new WTFException("Archive should always have at least one link");
    }

    public String createDefaultExtractToPath(Archive archive) {
        try {
            return new File(archive.getArchiveFiles().get(0).getFilePath()).getParent();
        } catch (final Throwable e) {
        }
        return new File(getFilePath()).getParent();
    }

    public Archive createArchive(ArchiveType archiveType) {
        return new DownloadLinkArchive(this, archiveType);
    }

    public Archive createArchive(SplitType splitType) {
        return new DownloadLinkArchive(this, splitType);
    }

    @Override
    public File getFolder() {
        return new File(getFilePath()).getParentFile();
    }

    @Override
    public BooleanStatus getDefaultAutoExtract() {
        return BooleanStatus.UNSET;
    }
}
