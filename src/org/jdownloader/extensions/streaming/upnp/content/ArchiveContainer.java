package org.jdownloader.extensions.streaming.upnp.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.utils.Files;
import org.appwork.utils.logging.Log;
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

    public ArchiveContainer(String id, ListContentProvider listContentProvider, ExtractionExtension archiver, StreamingExtension streamingExtension, Archive archive) {
        super(id, "[ARCHIVE] " + archive.getName());
        this.extractionExtension = archiver;
        this.streamingExtension = streamingExtension;
        this.archive = archive;
        this.listContentProvider = listContentProvider;
        listContentProvider.addChildren(this, empty = new FolderContainer(id + ".empty", "[Opening rar...]"));
    }

    public List<ContentNode> getChildren() {
        synchronized (ArchiveContainer.this) {
            if (!triedToOpen) {
                triedToOpen = true;
                Thread th = new Thread("RarOpener " + archive.getName()) {
                    public void run() {
                        readRarContents();
                    }
                };
                th.start();
                try {
                    th.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return super.getChildren();
    }

    protected void readRarContents() {

        List<ContentNode> children = new ArrayList<ContentNode>();
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
            this.unknownPassword = true;
        } catch (Throwable e) {
            exception = e;
        }

        setChildren(children);
        listContentProvider.removeChildren(this, empty);
        if (unknownPassword) {

            listContentProvider.addChildren(this, new ErrorEntry("Password unknown. Add password to passwordlist!"));

        } else if (exception != null) {
            listContentProvider.addChildren(this, new ErrorEntry(exception.getClass().getSimpleName() + ": " + exception.getMessage()));
        } else {

            ContentView cv = archive.getContentView();
            mountFolder(this, cv);
        }

    }

    private void mountFile(FolderContainer parent, final PackedFile archiveFile) {

        String id = parent.getID() + "/" + archiveFile.getName();
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

    private void mountFolder(FolderContainer dir, PackedFile file) {
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
