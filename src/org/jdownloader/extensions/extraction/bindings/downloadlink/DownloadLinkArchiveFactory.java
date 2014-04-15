package org.jdownloader.extensions.extraction.bindings.downloadlink;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.settings.GeneralSettings;

public class DownloadLinkArchiveFactory extends DownloadLinkArchiveFile implements ArchiveFactory {

    public static final String DOWNLOADLINK_KEY_EXTRACTEDPATH = "EXTRACTEDPATH";

    private String             id;
    private static long        LAST_USED_TIMESTAMP;

    public DownloadLinkArchiveFactory(DownloadLink link) {
        super(link);
    }

    public String createExtractSubPath(String path, Archive archive) {
        DownloadLink link = getFirstLink(archive);
        try {
            FilePackage fp = link.getFilePackage();
            if (path.contains(PACKAGENAME)) {
                String packageName = null;
                if (FilePackage.isDefaultFilePackage(fp)) {
                    packageName = link.getStringProperty(DownloadLink.PROPERTY_LASTFPNAME, null);
                } else {
                    packageName = CrossSystem.alleviatePathParts(link.getFilePackage().getName());
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
                String dif = new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()).getAbsolutePath().replace(new File(link.getFileOutput(false, true)).getParent(), "");
                if (StringUtils.isEmpty(dif) || new File(dif).isAbsolute()) {
                    path = path.replace(SUBFOLDER, "");
                } else {
                    path = path.replace(SUBFOLDER, CrossSystem.alleviatePathParts(dif));
                }
            }
            return CrossSystem.fixPathSeperators(path);
        } catch (Exception e) {
            Log.exception(e);
        }
        return null;
    }

    public java.util.List<ArchiveFile> createPartFileList(final String file, String pattern) {
        final Pattern pat = Pattern.compile(pattern, CrossSystem.isWindows() ? Pattern.CASE_INSENSITIVE : 0);
        List<DownloadLink> links = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

            public boolean acceptNode(DownloadLink node) {
                String nodeFile = node.getFileOutput(false, true);
                return file.equals(nodeFile) || pat.matcher(nodeFile).matches();
            }

            public int returnMaxResults() {
                return 0;
            }
        });
        HashMap<String, DownloadLinkArchiveFile> map = new HashMap<String, DownloadLinkArchiveFile>();
        java.util.List<ArchiveFile> ret = new ArrayList<ArchiveFile>();
        for (DownloadLink l : links) {
            String linkName = l.getView().getDisplayName();
            DownloadLinkArchiveFile af = map.get(linkName);
            if (af == null) {
                af = new DownloadLinkArchiveFile(l);
                map.put(linkName, af);
                ret.add(af);
            } else {
                af.addMirror(l);
            }
        }
        java.util.List<ArchiveFile> filelist = new FileArchiveFactory(new File(getFilePath())).createPartFileList(file, pattern);
        for (ArchiveFile af : filelist) {
            DownloadLinkArchiveFile downloadLinkArchiveFile = map.get(af.getName());
            if (downloadLinkArchiveFile == null) {
                // There is a matching local file, without a downloadlink link. this can happen if the user removes finished downloads
                // immediatelly
                ret.add(af);
            }
        }
        return ret;
    }

    public File toFile(String path) {
        return new File(path);
    }

    public Collection<? extends String> getGuessedPasswordList(Archive archive) {
        HashSet<String> ret = new HashSet<String>();
        for (ArchiveFile af : archive.getArchiveFiles()) {
            if (af instanceof DownloadLinkArchiveFile) {
                for (DownloadLink link : ((DownloadLinkArchiveFile) af).getDownloadLinks()) {
                    String pw = link.getDownloadPassword();
                    if (StringUtils.isEmpty(pw) == false) ret.add(pw);
                }
            }
        }
        return ret;
    }

    public void fireArchiveAddedToQueue(Archive archive) {
        for (ArchiveFile af : archive.getArchiveFiles()) {
            if (af instanceof DownloadLinkArchiveFile) {
                for (DownloadLink link : ((DownloadLinkArchiveFile) af).getDownloadLinks()) {
                    link.setExtractionStatus(null);
                }
            }
        }
    }

    private DownloadLink getFirstLink(Archive archive) {
        if (archive.getFirstArchiveFile() instanceof DownloadLinkArchiveFile) { return ((DownloadLinkArchiveFile) archive.getFirstArchiveFile()).getDownloadLinks().get(0); }
        for (ArchiveFile af : archive.getArchiveFiles()) {
            if (af instanceof DownloadLinkArchiveFile) { return ((DownloadLinkArchiveFile) af).getDownloadLinks().get(0); }
        }
        throw new WTFException("Archive should always have at least one link");
    }

    public String createDefaultExtractToPath(Archive archive) {
        try {
            return new File(archive.getFirstArchiveFile().getFilePath()).getParent();
        } catch (final Throwable e) {
        }
        return new File(getFilePath()).getParent();
    }

    public Archive createArchive() {
        return new DownloadLinkArchive(this);
    }

    @Override
    public File getFolder() {
        return new File(getFilePath()).getParentFile();
    }

    @Override
    public String getID() {
        if (id != null) return id;
        synchronized (this) {
            if (id != null) return id;
            id = getIDFromFile(this);
        }
        return id;
    }

    private String getIDFromFile(DownloadLinkArchiveFile file) {
        for (DownloadLink link : file.getDownloadLinks()) {
            String id = link.getArchiveID();
            if (id != null) { return id; }
        }
        return null;
    }

    @Override
    public void onArchiveFinished(Archive archive) {
        String id = getID();
        if (id == null) {
            for (ArchiveFile af : archive.getArchiveFiles()) {
                if (af instanceof DownloadLinkArchiveFactory) {
                    id = getIDFromFile((DownloadLinkArchiveFactory) af);
                }
                if (id != null) break;
            }
        }
        if (id == null) {
            id = createUniqueAlltimeID();
        }
        for (ArchiveFile af : archive.getArchiveFiles()) {
            if (af instanceof DownloadLinkArchiveFile) {
                for (DownloadLink link : ((DownloadLinkArchiveFile) af).getDownloadLinks()) {
                    link.setArchiveID(id);
                }
            }
        }
    }

    public synchronized static String createUniqueAlltimeID() {
        long time = System.currentTimeMillis();
        if (time == LAST_USED_TIMESTAMP) time++;
        LAST_USED_TIMESTAMP = time;
        return time + "";
    }

    @Override
    public BooleanStatus getDefaultAutoExtract() {
        return BooleanStatus.UNSET;
    }

}
