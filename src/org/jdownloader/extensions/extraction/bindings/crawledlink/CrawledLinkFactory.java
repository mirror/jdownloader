package org.jdownloader.extensions.extraction.bindings.crawledlink;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.ModifyLock;
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
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.settings.GeneralSettings;

public class CrawledLinkFactory extends CrawledLinkArchiveFile implements ArchiveFactory {
    public CrawledLinkFactory(CrawledLink l) {
        super(l);
    }

    private CrawledLink getFirstPart() {
        return getLinks().get(0);
    }

    public boolean isDeepExtraction() {
        return false;
    }

    private static CrawledLink getFirstCrawledLink(Archive archive) {
        for (final ArchiveFile af : archive.getArchiveFiles()) {
            if (af instanceof CrawledLinkArchiveFile) {
                return ((CrawledLinkArchiveFile) af).getLinks().get(0);
            }
        }
        throw new WTFException("Archive should always have at least one link");
    }

    protected String modifyPartFilePattern(String pattern) {
        final HashSet<Character> unsafeChars = new HashSet<Character>();
        for (final CrawledLink crawledLink : getLinks()) {
            final PluginForHost defaultPlugin = crawledLink.gethPlugin();
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
        final CrawledPackage parentNode = getFirstPart().getParentNode();
        if (parentNode == null) {
            final List<ArchiveFile> ret = new ArrayList<ArchiveFile>();
            // not yet packagized
            ret.add(this);
            return ret;
        } else {
            final HashMap<String, ArchiveFile> map = new HashMap<String, ArchiveFile>();
            final ModifyLock modifyLock = parentNode.getModifyLock();
            boolean readL = modifyLock.readLock();
            try {
                for (CrawledLink link : parentNode.getChildren()) {
                    final String linkName = link.getName();
                    if (pat.matcher(linkName).matches()) {
                        CrawledLinkArchiveFile af = (CrawledLinkArchiveFile) map.get(linkName);
                        if (af == null) {
                            af = new CrawledLinkArchiveFile(link);
                            map.put(linkName, af);
                        } else {
                            af.addMirror(link);
                        }
                    }
                }
            } finally {
                modifyLock.readUnlock(readL);
            }
            final File directory = LinkTreeUtils.getDownloadDirectory(parentNode);
            if (directory != null) {
                final List<ArchiveFile> localFiles = new FileArchiveFactory(directory).createPartFileList(file, pattern);
                for (ArchiveFile localFile : localFiles) {
                    final ArchiveFile archiveFile = map.get(localFile.getName());
                    if (archiveFile == null) {
                        // There is a matching local file, without a downloadlink link. this can happen if the user removes finished
                        // downloads
                        // immediatelly
                        map.put(localFile.getName(), localFile);
                    } else if (archiveFile instanceof CrawledLinkArchiveFile) {
                        final CrawledLinkArchiveFile af = (CrawledLinkArchiveFile) archiveFile;
                        af.setFileArchiveFileExists(true);
                    }
                }
            }
            return new ArrayList<ArchiveFile>(map.values());
        }
    }

    public Collection<? extends String> getGuessedPasswordList(Archive archive) {
        return new HashSet<String>();
    }

    public void fireArchiveAddedToQueue(Archive archive) {
    }

    public String createDefaultExtractToPath(Archive archive) {
        try {
            final CrawledLink firstLink = getFirstCrawledLink(archive);
            return LinkTreeUtils.getDownloadDirectory(firstLink).getAbsolutePath();
        } catch (final Throwable e) {
        }
        return null;
    }

    public String createExtractSubPath(String path, Archive archive) {
        final CrawledLink link = getFirstCrawledLink(archive);
        try {
            if (path.contains(PACKAGENAME)) {
                CrawledPackage fp = link.getParentNode();
                String packageName = null;
                if (fp != null) {
                    packageName = CrossSystem.alleviatePathParts(fp.getName());
                }
                if (!StringUtils.isEmpty(packageName)) {
                    path = path.replace(PACKAGENAME, packageName);
                } else {
                    path = path.replace(PACKAGENAME, "");
                }
            }
            if (path.contains(ARCHIVENAME)) {
                String archiveName = CrossSystem.alleviatePathParts(archive.getName());
                if (!StringUtils.isEmpty(archiveName)) {
                    path = path.replace(ARCHIVENAME, archiveName);
                } else {
                    path = path.replace(ARCHIVENAME, "");
                }
            }
            if (path.contains(HOSTER)) {
                String hostName = CrossSystem.alleviatePathParts(link.getHost());
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
                String defaultDest = createDefaultExtractToPath(archive);
                if (!StringUtils.isEmpty(defaultDest)) {
                    String dif = new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()).getAbsolutePath().replace(defaultDest, "");
                    if (StringUtils.isEmpty(dif) || new File(dif).isAbsolute()) {
                        path = path.replace(SUBFOLDER, "");
                    } else {
                        path = path.replace(SUBFOLDER, CrossSystem.alleviatePathParts(dif));
                    }
                } else {
                    path = path.replace(SUBFOLDER, "");
                }
            }
            return CrossSystem.fixPathSeparators(path);
        } catch (Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        return null;
    }

    public Archive createArchive(ArchiveType archiveType) {
        return new Archive(this, archiveType);
    }

    public Archive createArchive(SplitType splitType) {
        return new Archive(this, splitType);
    }

    public File toFile(String path) {
        return new File(path);
    }

    @Override
    public File getFolder() {
        try {
            return LinkTreeUtils.getDownloadDirectory(getFirstPart());
        } catch (Throwable e) {
            return new File(JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());
        }
    }

    @Override
    public void setArchive(final Archive archive) {
        super.setArchive(archive);
        if (BooleanStatus.UNSET != getDefaultAutoExtract()) {
            /* make sure the autoextractValue is written to archiveSettings */
            archive.getSettings();
        }
    }

    @Override
    public BooleanStatus getDefaultAutoExtract() {
        final CrawledLink first = getLinks().get(0);
        if (first.hasArchiveInfo()) {
            return first.getArchiveInfo().getAutoExtract();
        } else {
            return BooleanStatus.UNSET;
        }
    }
}
