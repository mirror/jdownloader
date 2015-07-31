package org.jdownloader.container;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jd.controlling.linkcrawler.CrawledLink;
import jd.parser.Regex;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.PluginsC;

import org.jdownloader.controlling.UrlProtection;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class NZB extends PluginsC {

    public NZB() {
        super("NZB", "file:/.+\\.nzb", "$Revision: 13393 $");
    }

    public ContainerStatus callDecryption(final File nzbFile) {
        final ContainerStatus cs = new ContainerStatus(nzbFile);
        final ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(nzbFile);
            final DefaultHandler handler = new NZBSAXHandler(downloadLinks);
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            final SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new InputSource(fileInputStream), handler);
            final ArrayList<CrawledLink> crawledLinks = new ArrayList<CrawledLink>(downloadLinks.size());
            for (final DownloadLink downloadLink : downloadLinks) {
                crawledLinks.add(new CrawledLink(downloadLink));
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

    private class NZBFileSegment {
        private final int number;

        private int getNumber() {
            return number;
        }

        private long getBytes() {
            return bytes;
        }

        private String getMessageID() {
            return messageID;
        }

        private final long   bytes;
        private final String messageID;

        private NZBFileSegment(final int number, final long bytes, final String messageID) {
            this.number = number;
            this.bytes = bytes;
            this.messageID = messageID;
        }
    }

    public class NZBSAXHandler extends DefaultHandler {
        private CharArrayWriter                 text                = new CharArrayWriter();
        private Attributes                      attributes;
        private String                          path                = "";
        private DownloadLink                    currentDownloadLink = null;
        private final ArrayList<NZBFileSegment> segments            = new ArrayList<NZBFileSegment>();
        private int                             numberOfSegments    = -1;
        private final ArrayList<DownloadLink>   downloadLinks;
        private String                          segmentNumber       = null;
        private String                          segmentBytes        = null;
        private boolean                         isyEnc              = false;

        private NZBSAXHandler(ArrayList<DownloadLink> downloadLinks) {
            this.downloadLinks = downloadLinks;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
            return new InputSource(new StringReader(""));
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ("file".equalsIgnoreCase(qName)) {
                if (currentDownloadLink != null) {
                    long estimatedFileSize = 0;
                    final BitSet segmentSet = new BitSet();
                    for (final NZBFileSegment segment : segments) {
                        final int index = segment.getNumber();
                        if (!segmentSet.get(index)) {
                            segmentSet.set(index);
                            estimatedFileSize += segment.getBytes();
                        }
                    }
                    if (numberOfSegments > 0) {
                        for (int i = 1; i <= numberOfSegments; i++) {
                            if (!segmentSet.get(i)) {
                                currentDownloadLink.setProperty("incomplete", Boolean.TRUE);
                                currentDownloadLink.setAvailableStatus(AvailableStatus.FALSE);
                                break;
                            }
                        }
                    }
                    if (estimatedFileSize < currentDownloadLink.getVerifiedFileSize()) {
                        currentDownloadLink.setVerifiedFileSize(-1);
                    }
                    currentDownloadLink.setDownloadSize(estimatedFileSize);
                    downloadLinks.add(currentDownloadLink);
                }
                segments.clear();
                numberOfSegments = -1;
                final String date = attributes.getValue("date");
                final String subject = attributes.getValue("subject");
                isyEnc = subject.contains("yEnc");
                String name = new Regex(subject, "(^| )\"(.*?)\" ").getMatch(1);
                if (name == null) {
                    name = "Unsupported Subject:" + subject;
                }
                if (isyEnc) {
                    final String parts = new Regex(subject, "\"\\s*?yEnc\\s*?\\(1/(\\d+)\\)$").getMatch(0);
                    if (parts != null) {
                        numberOfSegments = Integer.parseInt(parts);
                    }
                    if (numberOfSegments != -1) {
                        final String fileSize = new Regex(subject, "\"\\s*yEnc\\s*\\(1/\\d+\\)\\s*?\\[(\\d+)").getMatch(0);
                        if (fileSize != null) {
                            currentDownloadLink.setVerifiedFileSize(Long.parseLong(fileSize));
                        }
                    } else {
                        String fileSize = new Regex(subject, "\"\\s*(\\d+)\\s*yEnc\\s*bytes").getMatch(0);
                        if (fileSize != null) {
                            currentDownloadLink.setVerifiedFileSize(Long.parseLong(fileSize));
                        }
                    }
                }
                final String usenetURL = "usenet://" + name + "|" + numberOfSegments + "|" + date;
                System.out.println(name + " " + date);
                currentDownloadLink = new DownloadLink(null, name, "usenet", usenetURL, true);
                currentDownloadLink.setUrlProtection(UrlProtection.PROTECTED_CONTAINER);
            }
            if ("segment".equalsIgnoreCase(qName)) {
                segmentNumber = attributes.getValue("number");
                segmentBytes = attributes.getValue("bytes");
            }
            path += "." + qName;
            this.attributes = attributes;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("segment".equalsIgnoreCase(qName)) {
                final String messageID = text.toString().trim();
                try {
                    segments.add(new NZBFileSegment(Integer.parseInt(segmentNumber), Long.parseLong(segmentBytes), messageID));
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            path = path.substring(0, path.length() - qName.length() - 1);
            text.reset();
        }

        public void characters(char[] ch, int start, int length) {
            text.write(ch, start, length);
        }
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }

    /*
     * we dont have to hide metalink container links
     */
    @Override
    public boolean hideLinks() {
        return false;
    }

}
