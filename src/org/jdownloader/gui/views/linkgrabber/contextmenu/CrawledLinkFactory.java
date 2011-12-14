package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class CrawledLinkFactory extends CrawledLinkArchiveFile implements ArchiveFactory {

    public CrawledLinkFactory(CrawledLink l) {
        super(l);
    }

    public Collection<? extends ArchiveFile> createPartFileList(String pattern) {
        final Pattern pat = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        ArrayList<CrawledLinkArchiveFile> ret = new ArrayList<CrawledLinkArchiveFile>();
        for (CrawledLink l : LinkGrabberTableModel.getInstance().getAllChildrenNodes()) {
            if (pat.matcher(l.getName()).matches()) {
                ret.add(new CrawledLinkArchiveFile(l));
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

}
