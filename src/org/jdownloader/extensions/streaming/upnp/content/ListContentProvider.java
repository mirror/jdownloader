package org.jdownloader.extensions.streaming.upnp.content;

import java.net.InetAddress;
import java.util.HashSet;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Files;
import org.appwork.utils.logging.Log;
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
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.streaming.IDFactory;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediainfo.MediaInfo;
import org.jdownloader.extensions.streaming.upnp.MediaServer;
import org.seamless.util.MimeType;

public class ListContentProvider implements ContentProvider {
    private RootContainer       root;
    private DIDLParser          didlParser;
    private ExtractionExtension extractionExtension;
    private StreamingExtension  streamingExtension;
    private InetAddress         address;

    public ListContentProvider(final ContentDirectory contentDirectory, MediaServer mediaServer) {
        didlParser = new DIDLParser();
        extractionExtension = (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();
        streamingExtension = (StreamingExtension) ExtensionController.getInstance().getExtension(StreamingExtension.class)._getExtension();
        address = mediaServer.getRouter().getNetworkAddressFactory().getBindAddresses()[0];
        root = refresh();
        new Thread("Refresher Contentprovider") {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5 * 60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    root = refresh();
                    contentDirectory.onUpdate();
                }

            }
        }.start();
        // for (DownloadLink dl : ) {
        // String ext = Files.getExtension(dl.getName());
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
        // Res res = new Res(mimeType, 123456l, "00:03:25", 8192l,
        // "http://"+address.getHost()+":3128/vlcstreaming/video?mp3");
        // dl.get
        // return new MusicTrack(getID() + "", parent.getID() + "",
        // "MyTitle.mp3", artist.getName(), "MyAlbum", artist, res);
        // }
        //
        // });
        // }

    }

    public RootContainer refresh() {
        RootContainer root = new RootContainer();
        FolderContainer downloadlist;

        addChildren(root, downloadlist = new FolderContainer("1", "DownloadList"));
        //
        HashSet<String> archives = new HashSet<String>();

        for (final DownloadLink dl : DownloadController.getInstance().getAllDownloadLinks()) {
            DownloadLinkArchiveFactory fac = new DownloadLinkArchiveFactory(dl);
            final Archive archive = extractionExtension.getArchiveByFactory(fac);
            final String id = IDFactory.create(dl, null);

            if (archive != null) {

                if (archive.getFirstArchiveFile().getName().endsWith(".rar")) {
                    if (archives.add(extractionExtension.createArchiveID(fac))) {
                        addChildren(downloadlist, new ArchiveContainer(root, id, this, extractionExtension, streamingExtension, archive));
                    }
                }
            } else {

                String ext = Files.getExtension(dl.getName());
                if ("mp3".equals(ext)) {
                    try {
                        final MediaInfo mi = new MediaInfo(null) {

                            @Override
                            public String getArtist() {
                                return "Unknown Artist";
                            }

                            @Override
                            public MimeType getMimeType() {
                                return new MimeType("audio", "mpeg");
                            }

                            @Override
                            public String getTitle() {
                                return dl.getName();
                            }

                        };

                        addChildren(downloadlist, new ContentItem(id) {

                            @Override
                            public Item getImpl(String deviceID) {

                                PersonWithRole artist = new PersonWithRole(mi.getArtist(), "Performer");
                                MimeType mimeType = mi.getMimeType();
                                Res res;

                                res = new Res(mimeType, mi.getContentLength(), formatDuration(mi.getDuration()), mi.getBitrate(), streamingExtension.createStreamUrl(id, deviceID));

                                return new MusicTrack(getID(), parent.getID(), mi.getTitle(), mi.getArtist(), mi.getAlbum(), artist, res);
                            }

                        });
                    } catch (Throwable e) {
                        Log.exception(e);
                        continue;
                    }

                } else if ("mkv".equals(ext) || "mp4".equals(ext) || "avi".equals(ext) || "flv".equals(ext)) {
                    try {

                        final MediaInfo mi = new MediaInfo(null) {

                            @Override
                            public MimeType getMimeType() {
                                return new MimeType("video", "mp4");
                            }

                            @Override
                            public String getTitle() {
                                return dl.getName();
                            }

                        };
                        addChildren(downloadlist, new ContentItem(id) {

                            @Override
                            public Item getImpl(String deviceID) {

                                Res res = new Res(mi.getMimeType(), mi.getContentLength(), formatDuration(mi.getDuration()), mi.getBitrate(), streamingExtension.createStreamUrl(id, deviceID));
                                //
                                return (new VideoItem(getID(), getParent().getID(), mi.getTitle(), null, res));
                            }

                        });
                    } catch (Throwable e) {
                        Log.exception(e);
                        continue;
                    }
                }

            }
        }
        return root;
    }

    String formatDuration(long ms) {
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

    public void addChildren(ContainerNode parent, ContentNode child) {
        parent.addChildren(child);
        child.setRoot(parent.getRoot());
        if (parent.getRoot().getMap().put(child.getID() + "", child) != null)

        //
            throw new WTFException("ID DUPES");
    }

    @Override
    public ContentNode getNode(String objectID) {
        return root.getMap().get(objectID);
    }

    @Override
    public String toDidlString(DIDLContent didl) throws Exception {
        synchronized (didlParser) {
            // didlParser is not thread safe
            // ps3 seems to prefer 1/0 instead of true/false
            // .replace("\"true\"", "\"1\"").replace("\"false\"", "\"0\"")
            return didlParser.generate(didl);

        }

    }

    public void removeChildren(ContainerNode parent, FolderContainer child) {
        parent.removeChildren(child);
        parent.getRoot().getMap().remove(child.getID());
    }

    public String getHost() {
        return address.getHostAddress();
    }

}
