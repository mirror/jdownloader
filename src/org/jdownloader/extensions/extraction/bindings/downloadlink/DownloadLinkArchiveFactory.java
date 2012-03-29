package org.jdownloader.extensions.extraction.bindings.downloadlink;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.settings.GeneralSettings;

public class DownloadLinkArchiveFactory extends DownloadLinkArchiveFile implements ArchiveFactory {

    public static final String DOWNLOADLINK_KEY_EXTRACTTOPATH = "EXTRAXT_TO_PATH";
    public static final String DOWNLOADLINK_KEY_EXTRACTEDPATH = "EXTRACTEDPATH";

    public DownloadLinkArchiveFactory(DownloadLink link) {
        super(link);
    }

    public String createExtractSubPath(String path, Archive archiv) {

        DownloadLink link = ((DownloadLinkArchiveFile) archiv.getFirstArchiveFile()).getDownloadLink();
        try {
            if (link.getFilePackage().getName() != null) {
                // if (link instanceof DummyDownloadLink &&
                // archiv.getExtractor().getArchiveName(archiv.getFirstArchiveFile())
                // != null) {
                // path = path.replace("%PACKAGENAME%",
                // archiv.getExtractor().getArchiveName(archiv.getFirstArchiveFile()));
                // } else {
                path = path.replace("%PACKAGENAME%", link.getFilePackage().getName());
                // }
            } else {
                path = path.replace("%PACKAGENAME%", "");
                Log.L.severe("Could not set packagename for " + archiv.getFirstArchiveFile().getFilePath());
            }

            if (archiv.getName() != null) {
                path = path.replace("%ARCHIVENAME%", archiv.getName());
            } else {
                path = path.replace("%ARCHIVENAME%", "");
                Log.L.severe("Could not set archivename for " + archiv.getFirstArchiveFile().getFilePath());
            }

            if (link.getHost() != null) {
                path = path.replace("%HOSTER%", link.getHost());
            } else {
                path = path.replace("%HOSTER%", "");
                Log.L.severe("Could not set hoster for " + archiv.getFirstArchiveFile().getFilePath());
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
                    Log.L.severe("Could not set extraction date. Maybe pattern is wrong. For " + archiv.getFirstArchiveFile().getFilePath());
                }
            }

            String dif = new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()).getAbsolutePath().replace(new File(link.getFileOutput()).getParent(), "");
            if (new File(dif).isAbsolute()) {
                dif = "";
            }
            path = path.replace("%SUBFOLDER%", dif);

            path = path.replaceAll("[/]+", "\\\\");
            path = path.replaceAll("[\\\\]+", "\\\\");
            return path;
        } catch (Exception e) {
            Log.exception(e);
        }

        return null;
    }

    public ArrayList<ArchiveFile> createPartFileList(String pattern) {

        final Pattern pat = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        List<DownloadLink> links = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

            public boolean isChildrenNodeFiltered(DownloadLink node) {
                return pat.matcher(node.getFileOutput()).matches();
            }

            public int returnMaxResults() {
                return 0;
            }
        });

        ArrayList<ArchiveFile> ret = new ArrayList<ArchiveFile>();
        for (DownloadLink l : links) {
            ret.add(new DownloadLinkArchiveFile(l));
        }

        return ret;
    }

    public File toFile(String path) {
        return new File(path);
    }

    public void fireExtractToChange(Archive archive) {
        for (ArchiveFile link1 : archive.getArchiveFiles()) {
            ((DownloadLinkArchiveFile) link1).getDownloadLink().setProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTEDPATH, archive.getExtractTo().getAbsolutePath());
        }

    }

    public Collection<? extends String> getPasswordList(Archive archive) {

        Set<String> ret = FilePackage.getPasswordAuto(((DownloadLinkArchiveFile) archive.getFirstArchiveFile()).getDownloadLink().getFilePackage());
        // VM crashes if password is filename
        // ret.add(new
        // File(archive.getFirstArchiveFile().getFilePath()).getName());
        return ret;
    }

    public void fireArchiveAddedToQueue(Archive archive) {
        DownloadLink link = ((DownloadLinkArchiveFile) archive.getFirstArchiveFile()).getDownloadLink();
        link.getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
        link.getLinkStatus().setErrorMessage(null);
    }

    public String getExtractPath(Archive archive) {
        try {
            DownloadLink link = ((DownloadLinkArchiveFile) archive.getFirstArchiveFile()).getDownloadLink();
            if (link.getProperty(DOWNLOADLINK_KEY_EXTRACTTOPATH) != null) { return ((File) link.getProperty(DOWNLOADLINK_KEY_EXTRACTTOPATH)).getAbsolutePath(); }

            return new File(archive.getFirstArchiveFile().getFilePath()).getParent();
        } catch (Throwable e) {
            DownloadLink link = this.getDownloadLink();
            if (link.getProperty(DOWNLOADLINK_KEY_EXTRACTTOPATH) != null) { return ((File) link.getProperty(DOWNLOADLINK_KEY_EXTRACTTOPATH)).getAbsolutePath(); }

            return new File(getFilePath()).getParent();

        }
    }

    public Archive createArchive() {
        return new DownloadLinkArchive(this);
    }

}
