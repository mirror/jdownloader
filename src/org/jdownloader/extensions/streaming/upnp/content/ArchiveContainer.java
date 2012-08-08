package org.jdownloader.extensions.streaming.upnp.content;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.utils.Files;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.VideoItem;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediainfo.MediaInfo;
import org.jdownloader.extensions.streaming.rarstream.RarStreamer;
import org.jdownloader.logging.LogController;
import org.seamless.util.MimeType;

public class ArchiveContainer extends FolderContainer {

    private ExtractionExtension extractionExtension;
    private Archive             archive;
    private StreamingExtension  streamingExtension;
    private boolean             triedToOpen     = false;
    private boolean             unknownPassword = false;
    private Throwable           exception       = null;
    private ListContentProvider listContentProvider;
    private FolderContainer     empty;
    private LogSource           logger;

    public ArchiveContainer(RootContainer root, String id, ListContentProvider listContentProvider, ExtractionExtension archiver, StreamingExtension streamingExtension, Archive archive) {
        super(id, "[ARCHIVE] " + archive.getName());
        this.setRoot(root);
        this.extractionExtension = archiver;
        this.streamingExtension = streamingExtension;
        this.archive = archive;
        logger = LogController.getInstance().getLogger("streaming");
        this.listContentProvider = listContentProvider;
        listContentProvider.addChildren(this, empty = new FolderContainer(id + ".empty", "[Opening rar...]"));
        getChildren();
    }

    public List<ContentNode> getChildren() {
        synchronized (ArchiveContainer.this) {
            if (!triedToOpen) {
                triedToOpen = true;

                readRarContents();

            }
        }
        return super.getChildren();
    }

    protected void readRarContents() {

        List<ContentNode> children = new ArrayList<ContentNode>();

        buildContentView();
        setChildren(children);
        listContentProvider.removeChildren(this, empty);
        if (unknownPassword) {

            listContentProvider.addChildren(this, new ErrorEntry("Password unknown. Add password to passwordlist!"));

        } else if (exception != null) {
            listContentProvider.addChildren(this, new ErrorEntry(exception.getClass().getSimpleName() + ": " + exception.getMessage()));
        } else {

            ContentView cv = archive.getContentView();
            try {
                mountFolder(this, cv);
            } catch (Throwable e) {
                exception = e;
                listContentProvider.addChildren(this, new ErrorEntry(exception.getClass().getSimpleName() + ": " + exception.getMessage()));
                logger.log(e);
            }
        }

    }

    private void buildContentView() {

        if (archive.getContentView() == null || (archive.getContentView().getTotalFileCount() + archive.getContentView().getTotalFolderCount() == 0)) {
            try {
                RarStreamer rartream = new RarStreamer(archive, streamingExtension, extractionExtension) {
                    protected String askPassword() throws DialogClosedException, DialogCanceledException {

                        // if password is not in list, we cannot open the archive.
                        throw new DialogClosedException(0);
                    }

                    protected void openArchiveInDialog() throws DialogClosedException, DialogCanceledException, ExtractionException {
                        try {
                            openArchive();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            throw new ExtractionException(e, streamProvider != null ? rarStreamProvider.getLatestAccessedStream().getArchiveFile() : null);
                        }

                    }

                };

                rartream.openArchive();
                rartream.updateContentView();
                triedToOpen = true;
            } catch (DialogClosedException e) {
                // password unknown
                logger.log(e);
                this.unknownPassword = true;
            } catch (Throwable e) {
                logger.log(e);
                exception = e;
            }

        }
    }

    private void mountFile(FolderContainer parent, final PackedFile archiveFile) throws UnsupportedEncodingException {

        String id;

        id = parent.getID() + "/" + URLEncoder.encode(archiveFile.getName(), "UTF-8");

        final String url = "http://" + listContentProvider.getHost() + ":3128/vlcstreaming/stream?" + id;
        if (archiveFile.isDirectory()) {
            FolderContainer dir = new FolderContainer(id, archiveFile.getName());
            listContentProvider.addChildren(parent, dir);
            mountFolder(dir, archiveFile);

        } else {

            String ext = Files.getExtension(archiveFile.getName());
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
                            return archiveFile.getName();
                        }

                    };

                    listContentProvider.addChildren(parent, new ContentItem(id) {

                        @Override
                        public Item getImpl() {

                            PersonWithRole artist = new PersonWithRole(mi.getArtist(), "Performer");
                            MimeType mimeType = mi.getMimeType();
                            Res res;

                            res = new Res(mimeType, mi.getContentLength(), listContentProvider.formatDuration(mi.getDuration()), mi.getBitrate(), url);

                            return new MusicTrack(getID(), parent.getID(), mi.getTitle(), mi.getArtist(), mi.getAlbum(), artist, res);
                        }

                    });
                } catch (Throwable e1) {
                    Log.exception(e1);
                    return;
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
                            return archiveFile.getName();
                        }

                    };
                    listContentProvider.addChildren(parent, new ContentItem(id) {

                        @Override
                        public Item getImpl() {

                            Res res = new Res(mi.getMimeType(), mi.getContentLength(), listContentProvider.formatDuration(mi.getDuration()), mi.getBitrate(), url);
                            //
                            return (new VideoItem(getID(), getParent().getID(), mi.getTitle(), null, res));
                        }

                    });
                } catch (Throwable e1) {
                    Log.exception(e1);
                    return;
                }
            }
        }

    }

    private void mountFolder(FolderContainer dir, PackedFile file) throws UnsupportedEncodingException {
        for (Entry<String, PackedFile> e : file.getChildren().entrySet()) {
            mountFile(dir, e.getValue());
        }
    }

    @Override
    public Container getImpl() {
        Container con = new Container();
        con.setParentID(getParent().getID());
        con.setId(getID());
        con.setChildCount(getChildren().size() == 0 ? 1 : getChildren().size());
        con.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container.storageFolder"));
        con.setRestricted(true);
        con.setSearchable(false);
        con.setTitle(getTitle());
        return con;
    }
}
