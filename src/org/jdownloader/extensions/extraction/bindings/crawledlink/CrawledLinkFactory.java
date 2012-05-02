package org.jdownloader.extensions.extraction.bindings.crawledlink;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.settings.GeneralSettings;

public class CrawledLinkFactory extends CrawledLinkArchiveFile implements ArchiveFactory {

    public CrawledLinkFactory(CrawledLink l) {
        super(l);

    }

    public ArrayList<ArchiveFile> createPartFileList(String pattern) {
        final Pattern pat = Pattern.compile(pattern, CrossSystem.isWindows() ? Pattern.CASE_INSENSITIVE : 0);
        ArrayList<ArchiveFile> ret = new ArrayList<ArchiveFile>();
        if (getLink().getParentNode() == null) {
            // not yet packagized
            ret.add(this);
        } else {
            for (CrawledLink l : getLink().getParentNode().getView().getItems()) {
                if (pat.matcher(l.getName()).matches()) {
                    CrawledLinkArchiveFile claf = new CrawledLinkArchiveFile(l);
                    // if(claf.isComplete()&&claf.isValid()){
                    boolean contains = false;
                    for (Iterator<ArchiveFile> it = ret.iterator(); it.hasNext();) {
                        ArchiveFile af = it.next();
                        if (af.equals(claf)) {
                            if (!af.isComplete() || !af.isValid()) {
                                it.remove();
                            } else {
                                contains = true;
                            }
                        }
                    }

                    if (!contains) {
                        ret.add(claf);
                    }
                    // }
                }
            }
        }

        return ret;
    }

    public void fireExtractToChange(Archive archive) {
    }

    public Collection<? extends String> getPasswordList(Archive archive) {
        return new HashSet<String>();
    }

    public void fireArchiveAddedToQueue(Archive archive) {
    }

    public String getExtractPath(Archive archive) {
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
            return new File(this.getLink().getParentNode().getDownloadFolder());
        } catch (NullPointerException e) {
            return new File(JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());
        }
    }

}
