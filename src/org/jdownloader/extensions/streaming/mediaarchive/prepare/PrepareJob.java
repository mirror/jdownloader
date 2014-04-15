package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.controlling.AccountController;
import jd.plugins.DownloadLink;

import org.appwork.utils.Files;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.dlna.Extensions;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.AbstractAudioProfile;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.image.AbstractImageProfile;
import org.jdownloader.extensions.streaming.dlna.profiles.video.AbstractAudioVideoProfile;
import org.jdownloader.extensions.streaming.mediaarchive.AudioMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.ImageMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.ProfileMatch;
import org.jdownloader.extensions.streaming.mediaarchive.VideoMediaItem;
import org.jdownloader.logging.LogController;

public class PrepareJob extends QueueAction<Void, RuntimeException> {

    private PrepareEntry                                      jobEntry;
    private MediaArchiveController                            controller;
    private StreamingExtension                                extension;
    private ExtractionExtension                               extractor;
    private String                                            status;
    private static HashMap<String, HashSet<ExtensionHandler>> HANDLER;
    private static LogSource                                  LOGGER;

    static {
        LOGGER = LogController.getInstance().getLogger(PrepareJob.class.getName());
        HANDLER = new HashMap<String, HashSet<ExtensionHandler>>();
        ExtensionHandler defaultAudioHandler = new AudioHandler();
        ExtensionHandler defaultVideoHandler = new VideoHandler();
        ExtensionHandler defaultImageHandler = new ImageHandler();
        Profile.init();
        for (Profile p : Profile.ALL_PROFILES) {
            AbstractMediaContainer[] container = p.getContainer();
            if (container == null) {
                LOGGER.warning("Container List is null: " + p);
            } else {
                for (AbstractMediaContainer c : container) {
                    if (p instanceof AbstractImageProfile) {
                        for (Extensions e : c.getExtensions()) {
                            put(e.getExtension(), defaultImageHandler);
                        }
                    } else if (p instanceof AbstractAudioProfile) {
                        for (Extensions e : c.getExtensions()) {
                            put(e.getExtension(), defaultAudioHandler);
                        }
                    } else if (p instanceof AbstractAudioVideoProfile) {
                        for (Extensions e : c.getExtensions()) {
                            put(e.getExtension(), defaultVideoHandler);
                        }

                    }
                }
            }
        }
        for (Extensions e : Extensions.values()) {
            if (e.name().startsWith("AUDIO_VIDEO_")) {
                put(e.getExtension(), defaultVideoHandler);
            } else if (e.name().startsWith("AUDIO_")) {
                put(e.getExtension(), defaultAudioHandler);
            } else if (e.name().startsWith("IMAGE_")) {
                put(e.getExtension(), defaultImageHandler);
            }

        }

    }

    public PrepareJob(MediaArchiveController mediaArchiveController, PrepareEntry pe) {
        this.jobEntry = pe;
        controller = mediaArchiveController;
        extension = controller.getExtension();
        extractor = extension.getExtractingExtension();

    }

    private static void put(String extension, ExtensionHandler defaultImageHandler) {
        HashSet<ExtensionHandler> lst = HANDLER.get(extension);
        if (lst == null) {
            lst = new HashSet<ExtensionHandler>();
            HANDLER.put(extension, lst);
        }
        LOGGER.info("Add ExtensionHandler " + extension + " " + defaultImageHandler);
        lst.add(defaultImageHandler);
    }

    @Override
    protected Void run() throws RuntimeException {
        HashSet<String> archives = new HashSet<String>();

        for (DownloadLink dl : jobEntry.getLinks()) {
            Boolean streamingWithoutAccount = (Boolean) dl.getBooleanProperty("STREAMING", false);

            // instanceof may fail due to dynamic plugin loader
            boolean isDirect = "DirectHTTP".equalsIgnoreCase(dl.getHost()) || "http links".equalsIgnoreCase(dl.getHost());
            if (streamingWithoutAccount || AccountController.getInstance().hasAccounts(dl.getHost()) || isDirect) {
                String url = dl.getDownloadURL();
                String name = dl.getView().getDisplayName();
                if ("rar".equals(Files.getExtension(name))) {
                    DownloadLinkArchiveFactory lfa = new DownloadLinkArchiveFactory(dl);
                    status = T._.open_rar(dl.getView().getDisplayName());
                    if (extractor.isLinkSupported(lfa)) {
                        String archiveID = extractor.createArchiveID(lfa);
                        if (archives.contains(archiveID)) continue;
                        Archive archive = extractor.getArchiveByFactory(lfa);
                        handleArchive(archive);
                        archives.add(archiveID);

                    }
                } else {
                    status = T._.prepare(dl.getView().getDisplayName());
                    HashSet<ExtensionHandler> handlers = HANDLER.get(Files.getExtension(name));
                    if (handlers == null) {
                        LOGGER.warning("No Handler for " + name);
                        return null;
                    }
                    for (ExtensionHandler handler : handlers) {
                        MediaItem node = handler.handle(extension, dl);
                        if (node != null) {

                            List<ProfileMatch> profiles = findProfile(node);
                            HashSet<String> profileNames = new HashSet<String>();

                            for (ProfileMatch pm : profiles) {
                                profileNames.add(pm.getProfile().getProfileID());
                                node.setContainerFormat(pm.getProfile().getMimeType().getLabel());
                            }
                            node.setDlnaProfiles(profileNames.toArray(new String[] {}));
                            onMedia(node);

                        }
                    }
                }
            }
        }

        return null;
    }

    protected void onMedia(MediaItem node) {
        controller.addMedia(node);

    }

    private List<ProfileMatch> findProfile(MediaItem mediaItem) {
        if (mediaItem == null) return null;
        ArrayList<ProfileMatch> ret = new ArrayList<ProfileMatch>();
        LOGGER.info("find DLNA Profile: " + mediaItem.getDownloadLink());
        for (Profile p : Profile.ALL_PROFILES) {
            if (mediaItem instanceof VideoMediaItem) {
                VideoMediaItem video = (VideoMediaItem) mediaItem;
                if (p instanceof AbstractAudioVideoProfile) {
                    ProfileMatch match = video.matches((AbstractAudioVideoProfile) p);

                    if (match != null) {
                        LOGGER.info(match.toString());
                        ret.add(match);
                    }

                }
            } else if (mediaItem instanceof ImageMediaItem) {
                ImageMediaItem item = (ImageMediaItem) mediaItem;
                if (p instanceof AbstractImageProfile) {
                    ProfileMatch match = item.matches((AbstractImageProfile) p);

                    if (match != null) {
                        LOGGER.info(match.toString());
                        ret.add(match);
                    }

                }

            } else if (mediaItem instanceof AudioMediaItem) {
                AudioMediaItem item = (AudioMediaItem) mediaItem;
                if (p instanceof AbstractAudioProfile) {
                    ProfileMatch match = item.matches((AbstractAudioProfile) p);

                    if (match != null) {
                        LOGGER.info(match.toString());
                        ret.add(match);
                    }

                }

            }

        }
        return ret;
    }

    private void handleArchive(Archive archive) {
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        return jobEntry.getName();
    }
}
