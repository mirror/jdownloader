package org.jdownloader.extensions.vlcstreaming.upnp;

import java.util.HashMap;

import org.appwork.exceptions.WTFException;
import org.teleal.cling.support.model.PersonWithRole;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.common.util.MimeType;

public class BasicContentProvider implements ContentProvider {
    private RootContainer root;

    public BasicContentProvider() {

        createRoot();
        FolderContainer downloadlist;
        addChildren(root, downloadlist = new FolderContainer(1, "DownloadList"));

        addChildren(downloadlist, new ContentItem(10) {

            @Override
            public Item getImpl() {

                PersonWithRole artist = new PersonWithRole("MyArtist", "Performer");
                MimeType mimeType = new MimeType("audio", "mpeg");
                Res res = new Res(mimeType, 123456l, "00:03:25", 8192l, "http://192.168.2.122:3128/vlcstreaming/video?mp3");

                return new MusicTrack(getID() + "", parent.getID() + "", "MyTitle.mp3", artist.getName(), "MyAlbum", artist, res);
            }

        });

        addChildren(downloadlist, new ContentItem(11) {

            @Override
            public Item getImpl() {

                PersonWithRole artist = new PersonWithRole("MyArtist", "Performer");
                MimeType mimeType = new MimeType("audio", "mpeg");
                Res res = new Res(mimeType, 123456l, "00:03:25", 8192l, "http://192.168.2.122:3128/vlcstreaming/video?mp3");

                return new MusicTrack(getID() + "", parent.getID() + "", "MyTitle2.mp3", artist.getName(), "MyAlbum", artist, res);
            }

        });
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
        // // audio.setClazz(new org.teleal.cling.support.model.DIDLObject.Class("object.container"));
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
        // audio.setClazz(new org.teleal.cling.support.model.DIDLObject.Class("object.container"));
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
        // movie.setClazz(new org.teleal.cling.support.model.DIDLObject.Class("object.container"));
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
        // audio.setClazz(new org.teleal.cling.support.model.DIDLObject.Class("object.container"));
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
        // movie.setClazz(new org.teleal.cling.support.model.DIDLObject.Class("object.container"));
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

}
