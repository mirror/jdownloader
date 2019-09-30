package jd.plugins.components;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.UrlProtection;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.components.usenet.UsenetFile;
import org.jdownloader.plugins.components.usenet.UsenetFileSegment;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class NZBSAXHandler extends DefaultHandler {
    public static ArrayList<DownloadLink> parseNZB(final String string) throws Exception {
        return parseNZB(new InputStream() {
            final StringReader stringReader = new StringReader(string);

            @Override
            public int read() throws IOException {
                return stringReader.read();
            }
        });
    }

    public static ArrayList<DownloadLink> parseNZB(InputStream is) throws Exception {
        final ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
        final NZBSAXHandler handler = new NZBSAXHandler(downloadLinks);
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        // www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setValidating(false);
        final SAXParser saxParser = factory.newSAXParser();
        try {
            saxParser.parse(new InputSource(is), handler);
        } catch (final Throwable e) {
            // parser can throw exceptions (eg trailing chars)
            LogController.CL().log(e);
        } finally {
            try {
                handler.finishCurrentFile();
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
        return downloadLinks;
    }

    private final CharArrayWriter               text              = new CharArrayWriter();
    private String                              path              = "";
    private final HashSet<String>               passwords         = new HashSet<String>();
    private String                              password          = null;
    private UsenetFile                          currentFile       = null;
    private UsenetFileSegment                   currentSegment    = null;
    private final ArrayList<DownloadLink>       downloadLinks;
    private String                              date              = null;
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

    public NZBSAXHandler(ArrayList<DownloadLink> downloadLinks) {
        this.downloadLinks = downloadLinks;
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
        // avoid parser to read external entities
        return new InputSource(new StringReader(""));
    }

    public void finishCurrentFile() {
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
                    LogController.CL().info(currentFile.getName() + " is missing segment " + i);
                    downloadLink.setProperty("incomplete", Boolean.TRUE);
                    downloadLink.setAvailableStatus(AvailableStatus.FALSE);
                    break;
                }
            }
            try {
                // compress the jsonString with gzip and encode as base64
                currentFile._write(downloadLink);
            } catch (final IOException e) {
                LogController.CL().info("Exception for " + currentFile.getName());
                LogController.CL().log(e);
                downloadLink.setProperty("incomplete", Boolean.TRUE);
                downloadLink.setAvailableStatus(AvailableStatus.FALSE);
            }
            downloadLinks.add(downloadLink);
            currentFile = null;
        }
        if (passwords.size() > 0) {
            final ArrayList<String> sourcePluginPasswords = new ArrayList<String>(passwords);
            for (final DownloadLink downloadLink : downloadLinks) {
                downloadLink.setSourcePluginPasswordList(sourcePluginPasswords);
            }
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (StringUtils.equalsIgnoreCase("meta", qName)) {
            for (int index = 0; index < attributes.getLength(); index++) {
                if (StringUtils.equalsIgnoreCase("password", attributes.getValue(index))) {
                    password = "";
                    break;
                }
            }
        } else if (StringUtils.equalsIgnoreCase("file", qName)) {
            finishCurrentFile();
            currentFile = new UsenetFile();
            date = attributes.getValue("date");
            final String subject = attributes.getValue("subject");
            // XXXXX - "Filename.jpg" [XX/XX] 52,44 MB yEnc (1/41)
            String nameBySubject = new Regex(subject, "( |^)\"([^\"]*?\\.[a-z0-9]{2,4})\"").getMatch(1);
            if (nameBySubject == null) {
                nameBySubject = new Regex(subject, "( \"|^\"?)([^\"]*?\\.[a-z0-9]{2,4})\"").getMatch(1);
                if (nameBySubject == null) {
                    // XXX - NNNNNNNN - XXX XXX - NNNNNN - [0001 of 0100] - XX - 100.52 Kb - Filename.jpg (1/1)
                    nameBySubject = new Regex(subject, "(.+ |^)(.*?) \\(1/\\d+").getMatch(1);
                }
            }
            if (nameBySubject == null) {
                final String fileNameExtension = Files.getExtension(subject);
                final ExtensionsFilterInterface compiled = CompiledFiletypeFilter.getExtensionsFilterInterface(fileNameExtension);
                if (compiled != null) {
                    nameBySubject = subject;
                } else {
                    nameBySubject = "Unsupported Subject:" + subject;
                }
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
                String fileSize = new Regex(subject, "([0-9\\.,]+\\s*?(kb|mb|gb))").getMatch(0);
                if (fileSize != null) {
                    currentFile.setSize(SizeFormatter.getSize(fileSize));
                }
            }
        } else if (StringUtils.equalsIgnoreCase("segment", qName)) {
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
        if (StringUtils.equalsIgnoreCase("meta", qName) && "".equals(password)) {
            password = text.toString().trim();
            if (StringUtils.isNotEmpty(password)) {
                passwords.add(password);
            }
            password = null;
        } else if ("segment".equalsIgnoreCase(qName)) {
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
