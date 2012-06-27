package org.jdownloader.extensions.extraction.bindings.crawledlink;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
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

    public ArrayList<ArchiveFile> createPartFileList(String file, String pattern) {
        final Pattern pat = Pattern.compile(pattern, CrossSystem.isWindows() ? Pattern.CASE_INSENSITIVE : 0);
        ArrayList<ArchiveFile> ret = new ArrayList<ArchiveFile>();
        if (getFirstLink().getParentNode() == null) {
            // not yet packagized
            ret.add(this);
        } else {
            List<CrawledLink> links = getFirstLink().getParentNode().getView().getItems();
            // for (CrawledLink l : ) {
            // if (l.getName().equals(file) || pat.matcher(l.getName()).matches()) {
            // CrawledLinkArchiveFile claf = new CrawledLinkArchiveFile(l);
            // // if(claf.isComplete()&&claf.isValid()){
            // boolean contains = false;
            // for (Iterator<ArchiveFile> it = ret.iterator(); it.hasNext();) {
            // ArchiveFile af = it.next();
            // if (af.equals(claf)) {
            // if (!af.isComplete() || !af.isValid()) {
            // it.remove();
            // } else {
            // contains = true;
            // }
            // }
            // }
            //
            // if (!contains) {
            // ret.add(claf);
            // }
            // // }
            // }
            // }

            HashMap<String, CrawledLinkArchiveFile> map = new HashMap<String, CrawledLinkArchiveFile>();

            for (CrawledLink l : links) {
                if (l.getName().equals(file) || pat.matcher(l.getName()).matches()) {
                    CrawledLinkArchiveFile af = map.get(l.getName());
                    if (af == null) {
                        af = new CrawledLinkArchiveFile(l);

                        map.put(l.getName(), af);
                        ret.add(af);
                    } else {
                        af.addMirror(l);
                    }
                }

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
        return null;
    }

    public String createExtractSubPath(String path, Archive archiv) {
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
        } catch (NullPointerException e) {
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
            String id = link.getDownloadLink().getStringProperty(DownloadLinkArchiveFactory.ID);
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
                    link.getDownloadLink().setProperty(DownloadLinkArchiveFactory.ID, id);
                    pws.addAll(link.getArchiveInfo().getExtractionPasswords());
                }
            }
        }
        if (pws.size() > 0) {
            HashSet<String> storedPws = archive.getSettings().getPasswords();
            if (storedPws != null) {
                pws.addAll(storedPws);

            }
            archive.getSettings().setPasswords(pws);

        }

    }

}
