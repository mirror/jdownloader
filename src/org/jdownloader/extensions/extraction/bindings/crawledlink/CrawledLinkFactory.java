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
import jd.controlling.packagecontroller.ModifyLock;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.settings.GeneralSettings;

public class CrawledLinkFactory extends CrawledLinkArchiveFile implements ArchiveFactory {

    public CrawledLinkFactory(CrawledLink l) {
        super(l);

    }

    private CrawledLink getFirstLink() {
        return getLinks().get(0);
    }

    private CrawledLink getFirstLink(Archive archive) {
        if (archive.getFirstArchiveFile() instanceof CrawledLinkArchiveFile) { return ((CrawledLinkArchiveFile) archive.getFirstArchiveFile()).getLinks().get(0); }
        for (ArchiveFile af : archive.getArchiveFiles()) {
            if (af instanceof CrawledLinkArchiveFile) { return ((CrawledLinkArchiveFile) af).getLinks().get(0); }
        }
        throw new WTFException("Archive should always have at least one link");
    }

    public java.util.List<ArchiveFile> createPartFileList(String file, String pattern) {
        final Pattern pat = Pattern.compile(pattern, CrossSystem.isWindows() ? Pattern.CASE_INSENSITIVE : 0);
        java.util.List<ArchiveFile> ret = new ArrayList<ArchiveFile>();
        if (getFirstLink().getParentNode() == null) {
            // not yet packagized
            ret.add(this);
        } else {
            ModifyLock modifyLock = getFirstLink().getParentNode().getModifyLock();
            boolean readL = modifyLock.readLock();
            try {
                List<CrawledLink> links = getFirstLink().getParentNode().getChildren();
                HashMap<String, CrawledLinkArchiveFile> map = new HashMap<String, CrawledLinkArchiveFile>();
                for (CrawledLink l : links) {
                    String linkName = l.getName();
                    if (linkName.equals(file) || pat.matcher(linkName).matches()) {
                        CrawledLinkArchiveFile af = map.get(linkName);
                        if (af == null) {
                            af = new CrawledLinkArchiveFile(l);
                            map.put(linkName, af);
                            ret.add(af);
                        } else {
                            af.addMirror(l);
                        }
                    }
                }
            } finally {
                modifyLock.readUnlock(readL);
            }
        }
        return ret;
    }

    public Collection<? extends String> getGuessedPasswordList(Archive archive) {
        return new HashSet<String>();
    }

    public void fireArchiveAddedToQueue(Archive archive) {
    }

    public String createDefaultExtractToPath(Archive archive) {
        try {
            CrawledLink firstLink = getFirstLink(archive);
            return LinkTreeUtils.getDownloadDirectory(firstLink).getAbsolutePath();
        } catch (final Throwable e) {
        }
        return null;
    }

    public String createExtractSubPath(String path, Archive archive) {
        CrawledLink link = getFirstLink(archive);
        try {
            CrawledPackage fp = link.getParentNode();
            if (path.contains(PACKAGENAME)) {
                String packageName = CrossSystem.alleviatePathParts(fp.getName());
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
            return CrossSystem.fixPathSeperators(path);
        } catch (Exception e) {
            Log.exception(e);
        }
        return null;
    }

    public Archive createArchive() {
        return new Archive(this);
    }

    public File toFile(String path) {
        return new File(path);
    }

    @Override
    public File getFolder() {
        try {
            return LinkTreeUtils.getDownloadDirectory(getFirstLink());
        } catch (Throwable e) {
            return new File(JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());
        }
    }

    private String id;

    @Override
    public String getID() {
        if (id != null) return id;
        synchronized (this) {
            if (id != null) return id;
            id = getIDFromFile(this);
        }
        return id;
    }

    private String getIDFromFile(CrawledLinkArchiveFile file) {
        for (CrawledLink link : file.getLinks()) {
            String id = link.getDownloadLink().getArchiveID();
            if (id != null) { return id; }
        }
        return null;
    }

    @Override
    public void onArchiveFinished(Archive archive) {
        String id = getID();
        if (id == null) {
            for (ArchiveFile af : archive.getArchiveFiles()) {
                if (af instanceof CrawledLinkArchiveFile) {
                    id = getIDFromFile((CrawledLinkArchiveFile) af);
                }
                if (id != null) break;
            }
        }
        if (id == null) {
            id = DownloadLinkArchiveFactory.createUniqueAlltimeID();
        }
        HashSet<String> pws = new HashSet<String>();
        // link
        for (ArchiveFile af : archive.getArchiveFiles()) {
            if (af instanceof CrawledLinkArchiveFile) {
                for (CrawledLink link : ((CrawledLinkArchiveFile) af).getLinks()) {
                    link.getDownloadLink().setArchiveID(id);
                    pws.addAll(link.getArchiveInfo().getExtractionPasswords());
                }
            }
        }
        if (BooleanStatus.UNSET != getDefaultAutoExtract()) {
            /* make sure the autoextractValue is written to archiveSettings */
            archive.getSettings();
        }
        if (pws.size() > 0) {
            HashSet<String> storedPws = archive.getSettings().getPasswords();
            if (storedPws != null) {
                pws.addAll(storedPws);

            }
            archive.getSettings().setPasswords(pws);
        }
    }

    @Override
    public BooleanStatus getDefaultAutoExtract() {
        return getLinks().get(0).getArchiveInfo().getAutoExtract();
    }

}
