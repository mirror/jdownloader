package org.jdownloader.container;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import jd.controlling.linkcrawler.ArchiveInfo;
import jd.controlling.linkcrawler.CrawledLink;
import jd.parser.Regex;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginsC;
import jd.plugins.components.NZBSAXHandler;

public class NZB extends PluginsC {
    public NZB() {
        super("NZB", "file:/.+\\.nzb$", "$Revision: 13393 $");
    }

    public NZB newPluginInstance() {
        return new NZB();
    }

    /* Default .nzb filename scheme containing a title and file-password. */
    public static final String PATTERN_COMMON_FILENAME_SCHEME = "^([^\\{]+)\\{\\{(.*?)\\}\\}\\.nzb$";

    public ContainerStatus callDecryption(final File nzbFile) {
        final ContainerStatus cs = new ContainerStatus(nzbFile);
        final ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(nzbFile);
            final Regex nzbCommonFilenameScheme = new Regex(nzbFile.getName(), PATTERN_COMMON_FILENAME_SCHEME);
            downloadLinks.addAll(NZBSAXHandler.parseNZB(fileInputStream));
            final ArrayList<CrawledLink> crawledLinks = new ArrayList<CrawledLink>(downloadLinks.size());
            final ArchiveInfo archiveInfo;
            final FilePackage fp;
            if (nzbCommonFilenameScheme.matches()) {
                fp = FilePackage.getInstance();
                fp.setName(nzbCommonFilenameScheme.getMatch(0));
                archiveInfo = new ArchiveInfo();
                archiveInfo.addExtractionPassword(nzbCommonFilenameScheme.getMatch(1));
            } else {
                fp = null;
                archiveInfo = null;
            }
            for (final DownloadLink downloadLink : downloadLinks) {
                if (fp != null) {
                    downloadLink._setFilePackage(fp);
                }
                final CrawledLink crawledLink = new CrawledLink(downloadLink);
                crawledLink.setArchiveInfo(archiveInfo);
                crawledLinks.add(crawledLink);
            }
            cls = crawledLinks;
            cs.setStatus(ContainerStatus.STATUS_FINISHED);
            return cs;
        } catch (final Exception e) {
            logger.log(e);
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            return cs;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (final Throwable igrnoe) {
            }
        }
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }

    @Override
    public boolean hideLinks() {
        return true;
    }
}
