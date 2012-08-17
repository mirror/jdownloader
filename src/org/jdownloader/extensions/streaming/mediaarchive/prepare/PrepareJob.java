package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.util.HashMap;
import java.util.HashSet;

import jd.controlling.AccountController;
import jd.plugins.DownloadLink;

import org.appwork.utils.Files;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;

public class PrepareJob extends QueueAction<Void, RuntimeException> {

    private PrepareEntry                             jobEntry;
    private MediaArchiveController                   controller;
    private StreamingExtension                       extension;
    private ExtractionExtension                      extractor;
    private static HashMap<String, ExtensionHandler> HANDLER;

    static {
        HANDLER = new HashMap<String, ExtensionHandler>();
        ExtensionHandler defaultAudioHandler = new AudioHandler();
        ExtensionHandler defaultVideoHandler = new VideoHandler();
        ExtensionHandler defaultImageHandler = new ImageHandler();
        HANDLER.put("mp3", defaultAudioHandler);
        // video
        HANDLER.put("mp4", defaultVideoHandler);
        HANDLER.put("avi", defaultVideoHandler);
        HANDLER.put("mkv", defaultVideoHandler);
        HANDLER.put("flv", defaultVideoHandler);
        HANDLER.put("webm", defaultVideoHandler);
        HANDLER.put("mov", defaultVideoHandler);

        // image
        HANDLER.put("jpg", defaultImageHandler);
        HANDLER.put("gif", defaultImageHandler);
        HANDLER.put("png", defaultImageHandler);
        HANDLER.put("jpeg", defaultImageHandler);
    }

    public PrepareJob(MediaArchiveController mediaArchiveController, PrepareEntry pe) {
        this.jobEntry = pe;
        controller = mediaArchiveController;
        extension = controller.getExtension();
        extractor = extension.getExtractingExtension();

    }

    @Override
    protected Void run() throws RuntimeException {
        HashSet<String> archives = new HashSet<String>();

        for (DownloadLink dl : jobEntry.getLinks()) {
            Boolean streamingWithoutAccount = (Boolean) dl.getBooleanProperty("STREAMING", false);
            if (streamingWithoutAccount || AccountController.getInstance().hasAccounts(dl.getHost())) {
                String url = dl.getDownloadURL();
                String name = dl.getName();
                if ("rar".equals(Files.getExtension(name))) {
                    DownloadLinkArchiveFactory lfa = new DownloadLinkArchiveFactory(dl);

                    if (extractor.isLinkSupported(lfa)) {
                        String archiveID = extractor.createArchiveID(lfa);
                        if (archives.contains(archiveID)) continue;
                        Archive archive = extractor.getArchiveByFactory(lfa);
                        handleArchive(archive);
                        archives.add(archiveID);

                    }
                } else {
                    ExtensionHandler handler = HANDLER.get(Files.getExtension(name));
                    MediaItem node = handler.handle(extension, dl);
                    if (node != null) {
                        controller.addMedia(node);
                    }
                }
            }
        }

        return null;
    }

    private void handleArchive(Archive archive) {
    }
}
