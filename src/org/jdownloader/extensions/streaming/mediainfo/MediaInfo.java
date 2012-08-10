package org.jdownloader.extensions.streaming.mediainfo;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.seamless.util.MimeType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MediaInfo extends MediaInfoTrack {
    public File                  file;
    private List<MediaInfoTrack> tracks;

    public MediaInfo(File file) {
        super("general");
        this.file = file;
        tracks = new ArrayList<MediaInfoTrack>();
    }

    public List<MediaInfoTrack> getTracks() {
        return tracks;
    }

    public String getArtist() {
        return get("Performer");
    }

    public static void main(String[] args) {
        Application.setApplication(".jd_home");
        try {
            MediaInfo mi = new MediaInfo(new File("g:\\test.mkv")).load();
            System.out.println(mi.getTracks());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public MediaInfo load() throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        String exec = null;
        if (CrossSystem.isWindows()) {
            if (CrossSystem.is64BitOperatingSystem()) {
                exec = Application.getResource("tools/Windows/mediainfo/x64/MediaInfo.exe").getAbsolutePath();
            } else {
                exec = Application.getResource("tools/Windows/mediainfo/i386/MediaInfo.exe").getAbsolutePath();
            }
        } else if (CrossSystem.isMac()) {
            exec = Application.getResource("tools/mac/MediaInfo/MediaInfo.dmg").getAbsolutePath();
        } else {
            exec = "MediaInfo";
        }

        final ProcessBuilder pb = ProcessBuilderFactory.create(exec, "--Output=XML", file.getAbsolutePath());

        Process process = pb.start();
        String result = IO.readInputStreamToString(process.getInputStream());

        process.waitFor();

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setValidating(false);
        Document doc = f.newDocumentBuilder().parse(new InputSource(new StringReader(result)));
        NodeList nodes = doc.getFirstChild().getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ("File".equals(node.getNodeName())) {
                NodeList trackTypes = node.getChildNodes();
                for (int ii = 0; ii < trackTypes.getLength(); ii++) {
                    Node node2 = trackTypes.item(ii);
                    if ("track".equals(node2.getNodeName())) {
                        String trackType = node2.getAttributes().getNamedItem("type").getTextContent().toLowerCase(Locale.ENGLISH);
                        MediaInfoTrack track;
                        if ("video".equals(trackType)) {
                            track = new VideoInfoTrack();
                            tracks.add(track);
                        } else if ("audio".equals(trackType)) {
                            track = new AudioInfoTrack();
                            tracks.add(track);
                        } else if ("general".equals(trackType)) {

                            track = this;
                        } else {
                            track = new MediaInfoTrack(trackType);
                            tracks.add(track);
                        }

                        NodeList entries = node2.getChildNodes();
                        for (int iii = 0; iii < entries.getLength(); iii++) {
                            Node entry = entries.item(iii);
                            String key = entry.getNodeName();
                            if ("#text".equals(key)) continue;

                            track.put(key, entry.getFirstChild().getTextContent());
                        }
                    }

                }
            }
        }

        return this;
    }

    public MimeType getMimeType() {
        if ("MPEG Audio".equals(getFormat())) {
            return new MimeType("audio", "mpeg");
        } else {
            return new MimeType("video", "mp4");
        }
    }

    public String getTitle() {
        String title = get("Track_name");
        if (title == null) {
            if (file != null) title = file.getName();
        }
        return title;
    }

    public String getAlbum() {
        return get("Album");
    }

}
