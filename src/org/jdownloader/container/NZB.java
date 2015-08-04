package org.jdownloader.container;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jd.controlling.linkcrawler.CrawledLink;
import jd.parser.Regex;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.PluginsC;
import jd.plugins.components.UsenetFile;
import jd.plugins.components.UsenetFileSegment;

import org.appwork.utils.formatter.SizeFormatter;
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

    public class NZBSAXHandler extends DefaultHandler {
        private final CharArrayWriter               text              = new CharArrayWriter();
        private String                              path              = "";
        private UsenetFile                          currentFile       = null;
        private UsenetFileSegment                   currentSegment    = null;
        private final ArrayList<DownloadLink>       downloadLinks;
        private String                              date;
        private boolean                             isyEnc            = false;
        private final Comparator<UsenetFileSegment> segmentComparator = new Comparator<UsenetFileSegment>() {

                                                                          public int compare(int x, int y) {
                                                                              return (x < y) ? -1 : ((x == y) ? 0 : 1);
                                                                          }

                                                                          @Override
                                                                          public int compare(UsenetFileSegment o1, UsenetFileSegment o2) {
                                                                              return compare(o1.getIndex(), o2.getIndex());
                                                                          }
                                                                      };

        private NZBSAXHandler(ArrayList<DownloadLink> downloadLinks) {
            this.downloadLinks = downloadLinks;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
            // avoid parser to read external entities
            return new InputSource(new StringReader(""));
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ("file".equalsIgnoreCase(qName)) {
                if (currentFile != null) {
                    final ArrayList<UsenetFileSegment> segments = currentFile.getSegments();
                    Collections.sort(segments, segmentComparator);
                    final BitSet segmentSet = new BitSet();
                    long estimatedFileSize = 0;
                    for (final UsenetFileSegment segment : segments) {
                        final int index = segment.getIndex();
                        if (!segmentSet.get(index)) {
                            segmentSet.set(index);
                            estimatedFileSize += Math.max(0, segment.getSize());
                        }
                    }
                    if (isyEnc) {
                        segmentSet.clear();
                        final int numOfSegments = segmentSet.length();
                        final long estimatedFileSizeTemp = estimatedFileSize;
                        estimatedFileSize = 0;
                        long processedSize = 0;
                        for (final UsenetFileSegment segment : segments) {
                            final int index = segment.getIndex();
                            if (!segmentSet.get(index)) {
                                segmentSet.set(index);
                                long estimatedSegmentSize = segment.getSize();
                                estimatedSegmentSize -= ("=ybegin part=" + index + " total=" + numOfSegments + " line=... size=" + estimatedFileSizeTemp + " name=" + currentFile.getName() + "\r\n").length();
                                if (index == numOfSegments) {
                                    estimatedSegmentSize -= ("=yend size=" + estimatedSegmentSize + " part=" + index + " pcrc32=........ crc32=........\r\n").length();
                                } else {
                                    estimatedSegmentSize -= ("=yend size=" + estimatedSegmentSize + " part=" + index + " pcrc32=........\r\n").length();
                                }
                                estimatedSegmentSize -= ("=ypart begin=" + processedSize + " end=" + processedSize + estimatedSegmentSize + "\r\n").length();
                                estimatedSegmentSize -= (estimatedSegmentSize / 128) * 2;// CRLF for each line
                                estimatedSegmentSize = (long) (estimatedSegmentSize / 1.015f); // ~ 1.5% overhead because of yEnc
                                processedSize += estimatedSegmentSize;
                            }
                        }
                        estimatedFileSize = processedSize;
                    }
                    if (currentFile.getSize() != -1) {
                        estimatedFileSize = Math.min(estimatedFileSize, currentFile.getSize());
                        if (estimatedFileSize != currentFile.getSize()) {
                            currentFile.setSize(-1);
                        }
                    }
                    final String usenetURL = "usenet://" + currentFile.getName() + "|" + currentFile.getNumSegments() + "|" + date;
                    final DownloadLink downloadLink = new DownloadLink(null, currentFile.getName(), "usenet", usenetURL, true);
                    downloadLink.setUrlProtection(UrlProtection.PROTECTED_CONTAINER);
                    if (estimatedFileSize > 0) {
                        downloadLink.setDownloadSize(estimatedFileSize);
                    }
                    // check for missing segments
                    final int maxSegments = Math.max(currentFile.getNumSegments(), segmentSet.length() - 1);
                    for (int i = 1; i <= maxSegments; i++) {
                        if (!segmentSet.get(i)) {
                            logger.info(currentFile.getName() + " is missing segment " + i);
                            downloadLink.setProperty("incomplete", Boolean.TRUE);
                            downloadLink.setAvailableStatus(AvailableStatus.FALSE);
                            break;
                        }
                    }
                    try {
                        // compress the jsonString with gzip and encode as base64
                        currentFile._write(downloadLink);
                    } catch (final IOException e) {
                        logger.info("Exception for " + currentFile.getName());
                        logger.log(e);
                        downloadLink.setProperty("incomplete", Boolean.TRUE);
                        downloadLink.setAvailableStatus(AvailableStatus.FALSE);
                    }
                    downloadLinks.add(downloadLink);
                }
                currentFile = new UsenetFile();
                date = attributes.getValue("date");
                final String subject = attributes.getValue("subject");
                // XXXXX - "Filename.jpg" [XX/XX] 52,44 MB yEnc (1/41)
                String nameBySubject = new Regex(subject, "( |^)\"(.*?)\" ").getMatch(1);
                if (nameBySubject == null) {
                    // XXX - NNNNNNNN - XXX XXX - NNNNNN - [0001 of 0100] - XX - 100.52 Kb - Filename.jpg (1/1)
                    nameBySubject = new Regex(subject, "(.+ |^)(.*?) \\(1/\\d+").getMatch(1);
                }
                if (nameBySubject == null) {
                    nameBySubject = "Unsupported Subject:" + subject;
                }
                currentFile.setName(nameBySubject);
                if (subject.contains(" yEnc ")) {
                    isyEnc = true;
                    final String parts = new Regex(subject, "\\s*?yEnc\\s*?\\(1/(\\d+)\\)$").getMatch(0);
                    if (parts != null) {
                        currentFile.setNumSegments(Integer.parseInt(parts));
                    }
                    String fileSize = new Regex(subject, "\\s*yEnc\\s*\\(1/\\d+\\)\\s*?\\[([0-9\\.,]+\\s*?(kb|mb|gb|b))").getMatch(0);
                    if (fileSize != null) {
                        currentFile.setSize(SizeFormatter.getSize(fileSize));
                    } else {
                        fileSize = new Regex(subject, "\\s*([0-9\\.,]+\\s*?(kb|mb|gb|b))\\s*yEnc").getMatch(0);
                        if (fileSize != null) {
                            currentFile.setSize(SizeFormatter.getSize(fileSize));
                        }
                    }
                } else {
                    isyEnc = false;
                    String fileSize = new Regex(subject, "([0-9\\.,]+\\s*?(kb|mb|gb|b))").getMatch(0);
                    if (fileSize != null) {
                        currentFile.setSize(SizeFormatter.getSize(fileSize));
                    }
                }
            }
            if ("segment".equalsIgnoreCase(qName)) {
                currentSegment = new UsenetFileSegment();
                final String number = attributes.getValue("number");
                if (number != null) {
                    currentSegment.setIndex(Integer.parseInt(number));
                }
                final String bytes = attributes.getValue("bytes");
                if (bytes != null) {
                    currentSegment.setSize(Long.parseLong(bytes));
                }
            }
            path += "." + qName;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("segment".equalsIgnoreCase(qName)) {
                final String messageID = text.toString().trim();
                currentSegment.setMessageID(messageID);
                currentFile.getSegments().add(currentSegment);
                currentSegment = null;
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

    @Override
    public boolean hideLinks() {
        return false;
    }

}
