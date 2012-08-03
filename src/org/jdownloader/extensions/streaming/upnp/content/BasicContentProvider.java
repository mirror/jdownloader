package org.jdownloader.extensions.streaming.upnp.content;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Files;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.VideoItem;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.extensions.streaming.upnp.content.mediainfo.AudioMediaInfo;
import org.jdownloader.extensions.streaming.upnp.content.mediainfo.VideoMediaInfo;
import org.jdownloader.settings.GeneralSettings;
import org.seamless.util.MimeType;

public class BasicContentProvider implements ContentProvider {
    private RootContainer root;
    private DIDLParser    didlParser;

    public BasicContentProvider() {
        didlParser = new DIDLParser();
        createRoot();
        FolderContainer downloadlist;

        addChildren(root, downloadlist = new FolderContainer("1", "DownloadList"));
        //
        try {
            mountHDFolder(new File(JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()), root);
        } catch (UnsupportedEncodingException e) {
            throw new WTFException(e);
        }
        // for (DownloadLink dl : ) {
        // String ext = Files.getExtension(dl.getFinalFileName());
        //
        // if("jpg".equals(ext)){
        //
        //
        // }else if("mp4".equals(ext)){
        //
        //
        // }else if ("mp3".equals(ext)){
        //
        // addChildren(downloadlist, new ContentItem(10) {
        //
        // @Override
        // public Item getImpl() {
        //
        // PersonWithRole artist = new PersonWithRole("MyArtist", "Performer");
        // MimeType mimeType = new MimeType("audio", "mpeg");
        // Res res = new Res(mimeType, 123456l, "00:03:25", 8192l, "http://192.168.2.122:3128/vlcstreaming/video?mp3");
        // dl.get
        // return new MusicTrack(getID() + "", parent.getID() + "", "MyTitle.mp3", artist.getName(), "MyAlbum", artist, res);
        // }
        //
        // });
        // }

    }

    // if (node instanceof ContainerNode) {
    //
    // // Container audio = new Container();
    // // audio.setId("0");
    // // audio.setParentID("-1");
    // // audio.setTitle("Wurzel");
    // // // audio.setCreator("JDownloader");
    // // audio.setRestricted(true);
    // // audio.setSearchable(false);
    // //
    // // audio.setChildCount(2);
    // // audio.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container"));
    //
    // } else if (node instanceof ItemNode) {
    // didl.addContainer(((ItemNode) node).getImpl());
    // String didlStrng = new DIDLParser().generate(didl);
    // didlStrng = didlStrng.replace("\"true\"", "\"1\"").replace("\"false\"", "\"0\"");
    // System.out.println("-->" + didlStrng);
    // return new BrowseResult(didlStrng, 1, 1);
    // Container audio = new Container();
    // audio.setId(ID_DOWNLOADLIST);
    // audio.setParentID("0");
    // audio.setTitle("DownloadList");
    // audio.setCreator("JDownloader");
    // audio.setRestricted(true);
    // audio.setSearchable(true);
    // audio.setWriteStatus(WriteStatus.NOT_WRITABLE);
    // audio.setChildCount(3);
    // audio.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container"));
    // didl.addContainer(audio);
    // String didlStrng = new DIDLParser().generate(didl);
    // System.out.println("-->" + didlStrng);
    //
    // } else if (objectID.equals(ID_LINKGRABBER)) {
    // Container movie = new Container();
    // movie.setId(ID_LINKGRABBER);
    // movie.setParentID("0");
    // movie.setTitle("Linkgrabber");
    // movie.setCreator("JDownloader");
    // movie.setRestricted(true);
    // movie.setSearchable(true);
    // movie.setWriteStatus(WriteStatus.NOT_WRITABLE);
    // movie.setChildCount(0);
    // movie.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container"));
    // didl.addContainer(movie);
    // String didlStrng = new DIDLParser().generate(didl);
    // System.out.println("-->" + didlStrng);
    // return new BrowseResult(didlStrng, 0, 0);
    // }
    // } else {
    //
    // if (objectID.equals(ID_ROOT)) {
    // // root
    //
    // Container audio = new Container();
    // audio.setId(ID_DOWNLOADLIST);
    // audio.setParentID("0");
    // audio.setTitle("DownloadList");
    // audio.setCreator("JDownloader");
    // audio.setRestricted(true);
    // audio.setSearchable(true);
    // audio.setWriteStatus(WriteStatus.NOT_WRITABLE);
    // audio.setChildCount(3);
    // audio.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container"));
    // didl.addContainer(audio);
    //
    // Container movie = new Container();
    // movie.setId(ID_LINKGRABBER);
    // movie.setParentID("0");
    // movie.setTitle("Linkgrabber");
    // movie.setCreator("JDownloader");
    // movie.setRestricted(true);
    // movie.setSearchable(true);
    // movie.setWriteStatus(WriteStatus.NOT_WRITABLE);
    // movie.setChildCount(0);
    // movie.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container"));
    // didl.addContainer(movie);
    //
    // String didlStrng = new DIDLParser().generate(didl);
    // System.out.println("-->" + didlStrng);
    // return new BrowseResult(didlStrng, 2, 2);
    // } else if (objectID.equals(ID_DOWNLOADLIST)) {
    //
    // PersonWithRole artist = new PersonWithRole("MYArtist", "Performer");
    // MimeType mimeType = new MimeType("audio", "mpeg");
    // Res res = new Res(mimeType, 123456l, "00:03:25", 8192l, "http://192.168.2.122:3128/vlcstreaming/video?mp3");
    // didl.addItem(new MusicTrack("101", ID_DOWNLOADLIST, "MyTitle.mp3", artist.getName(), "MyAlbum", artist, res));
    //
    // res = new Res(new MimeType("video", "mp4"), 123456l, "00:03:25,000", 8192l,
    // "http://192.168.2.122:3128/vlcstreaming/video?mp4");
    //
    // didl.addItem(new VideoItem("102", ID_DOWNLOADLIST, "MyMovie.mp4", "My Creator", res));
    //
    // res = new Res(new MimeType("video", "mkv"), 123456l, "00:00:01", 8192l, "http://192.168.2.122:3128/vlcstreaming/video?mkv");
    //
    // didl.addItem(new VideoItem("103", ID_DOWNLOADLIST, "MyMovie.mkv", "My Creator", res));
    // String didlStrng = new DIDLParser().generate(didl);
    // System.out.println("-->" + didlStrng);
    // return new BrowseResult(didlStrng, 3, 3);
    // } else if (objectID.equals(ID_LINKGRABBER)) {
    //
    // }
    //
    // //
    // }

    private void mountHDFolder(File file, FolderContainer root2) throws UnsupportedEncodingException {
        ExtractionExtension archiver = (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();
        HashSet<String> archives = new HashSet<String>();
        for (final File f : file.listFiles()) {

            if (f.isFile()) {
                FileArchiveFactory fac = new FileArchiveFactory(f);
                Archive archive = archiver.getArchiveByFactory(fac);
                System.out.println(archive);
                if (archive != null) {
                    String id = archiver.createArchiveID(fac);

                    if (archive.getFirstArchiveFile().getName().endsWith(".rar")) {
                        if (archives.add(id)) {
                            addChildren(root2, new ArchiveContainer(id, archive));
                        }
                    }
                } else {

                    String pp = f.getAbsolutePath().replace("\\", "\\\\");
                    final String url = "http://192.168.2.122:3128/vlcstreaming/video?\"" + URLEncoder.encode(pp, "UTF-8") + "\"";
                    String ext = Files.getExtension(f.getName());
                    if ("mp3".equals(ext)) {
                        addChildren(root2, new ContentItem(f.getAbsolutePath()) {

                            @Override
                            public Item getImpl() {

                                AudioMediaInfo mi = new AudioMediaInfo(f);
                                PersonWithRole artist = new PersonWithRole(mi.getArtist(), "Performer");
                                MimeType mimeType = mi.getMimeType();
                                Res res;

                                res = new Res(mimeType, mi.getContentLength(), formatDuration(mi.getDuration()), mi.getBitrate(), url);

                                return new MusicTrack(getID(), parent.getID(), mi.getTitle(), mi.getArtist(), mi.getAlbum(), artist, res);
                            }

                        });

                    } else if ("mkv".equals(ext) || "mp4".equals(ext) || "avi".equals(ext)) {
                        addChildren(root2, new ContentItem(f.getAbsolutePath()) {

                            @Override
                            public Item getImpl() {

                                VideoMediaInfo mi = new VideoMediaInfo(f);
                                Res res = new Res(mi.getMimeType(), mi.getContentLength(), formatDuration(mi.getDuration()), mi.getBitrate(), url);
                                //
                                return (new VideoItem(getID(), getParent().getID(), mi.getTitle(), null, res));
                            }

                        });

                    }
                    System.out.println(ext);
                }
            } else {

                // FolderContainer dir = new FolderContainer(f.getAbsolutePath(), f.getName());
                // addChildren(root2, dir);
                // mountHDFolder(f, dir);
            }

        }

    }

    private String formatDuration(int ms) {
        long days, hours, minutes, seconds, milliseconds;
        final StringBuilder string = new StringBuilder();
        milliseconds = ms % 1000;
        days = ms / (24 * 60 * 60 * 1000);
        ms -= days * 24 * 60 * 60 * 1000;
        hours = ms / (60 * 60 * 1000);
        ms -= hours * 60 * 60 * 1000;
        minutes = ms / 60;
        seconds = ms - minutes * 60;

        return hours + ":" + minutes + ":" + seconds;
    }

    private HashMap<String, ContentNode> map = new HashMap<String, ContentNode>();

    private void createRoot() {
        root = new RootContainer();
        map.put(root.getID() + "", root);
    }

    private void addChildren(ContainerNode parent, ContentNode child) {
        parent.addChildren(child);

        if (map.put(child.getID() + "", child) != null) throw new WTFException("ID DUPES");
    }

    @Override
    public ContentNode getNode(String objectID) {
        return map.get(objectID);
    }

    @Override
    public String toDidlString(DIDLContent didl) throws Exception {
        synchronized (didlParser) {
            // didlParser is not thread safe
            // ps3 seems to prefer 1/0 instead of true/false
            return didlParser.generate(didl).replace("\"true\"", "\"1\"").replace("\"false\"", "\"0\"");

        }

    }

}
