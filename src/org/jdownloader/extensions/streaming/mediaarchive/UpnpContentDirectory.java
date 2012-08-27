package org.jdownloader.extensions.streaming.mediaarchive;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLAttribute;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.DIDLObject.Property;
import org.fourthline.cling.support.model.DIDLObject.Property.DLNA.PROFILE_ID;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.VideoItem;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.upnp.content.Filter;
import org.jdownloader.extensions.streaming.upnp.content.SearchCriteria;
import org.jdownloader.logging.LogController;
import org.seamless.util.MimeType;

public class UpnpContentDirectory extends AbstractContentDirectoryService implements MediaListListener {

    private StreamingExtension     extension;
    private LogSource              logger;
    private MediaArchiveController archive;

    public UpnpContentDirectory(StreamingExtension extension) {
        super();
        logger = LogController.getInstance().getLogger(UpnpContentDirectory.class.getName());
        this.extension = extension;
        archive = extension.getMediaArchiveController();
        extension.getMediaArchiveController().getVideoController().getEventSender().addListener(this, true);
        extension.getMediaArchiveController().getAudioController().getEventSender().addListener(this, true);
        extension.getMediaArchiveController().getImageController().getEventSender().addListener(this, true);

    }

    @Override
    public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filterString, long firstResult, long maxResults, SortCriterion[] orderby) throws ContentDirectoryException {
        try {
            UpnpHeaders headers = org.fourthline.cling.protocol.sync.ReceivingAction.getRequestMessage().getHeaders();
            if (browseFlag == BrowseFlag.METADATA) {

                return browseMetaData(objectID, Filter.create(filterString), firstResult, maxResults, orderby, headers.get("User-agent") != null ? headers.get("User-agent").get(0) : null);

            } else {

                return browseContentDirectory(objectID, Filter.create(filterString), firstResult, maxResults, orderby, headers.get("User-agent") != null ? headers.get("User-agent").get(0) : null);

            }
        } catch (Exception e) {
            logger.log(e);

            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, e.toString());
        }

    }

    private BrowseResult browseContentDirectory(String objectID, Filter filter, long firstResult, long maxResults, SortCriterion[] orderby, String userAgent) throws Exception {

        DIDLContent didl = new DIDLContent();

        List<MediaNode> children = archive.getList();
        long to = children.size();
        if (userAgent.contains("Portable SDK for UPnP devices/1.6.16")) {
            // vlc/libupnp bug in
            // http://git.videolan.org/?p=vlc.git;a=commitdiff;h=794557eea63853456cf3120cdb1bdc88ca44ad9f.
            // should be fixed in libupnp 1.6.17
            maxResults = 5;
        }
        if (maxResults > 0) {
            to = Math.min(to, firstResult + maxResults);
        }

        int count = 0;
        for (long i = firstResult; i < to; i++) {
            MediaNode c = children.get((int) i);
            addMediaItemToDidl(userAgent, didl, c);
            count++;

        }

        String didlStrng = new DIDLParser().generate(didl);

        return new BrowseResult(didlStrng, count, children.size());

    }

    public void addMediaItemToDidl(String userAgent, DIDLContent didl, MediaNode c) {
        if (c instanceof VideoMediaItem) {
            didl.addItem(createVideoItem((VideoMediaItem) c, userAgent));
        } else if (c instanceof ImageMediaItem) {
            didl.addItem(createImageItem((ImageMediaItem) c, userAgent));
        } else if (c instanceof AudioMediaItem) {

            didl.addItem(createAudioItem((AudioMediaItem) c, userAgent));

        }
    }

    private Item createImageItem(ImageMediaItem c, String userAgent) {
        Res res = new Res(MimeType.valueOf(c.getMimeTypeString()), c.getSize(), extension.createStreamUrl(c.getUniqueID(), userAgent, null));
        //
        ImageItem ret = new ImageItem(c.getUniqueID(), "ROOT", c.getName(), null, res);
        // <upnp:albumArtURI dlna:profileID="JPEG_TN">http://192.168.2.122:8895/resource/4/COVER_IMAGE</upnp:albumArtURI>

        thumb(c, userAgent, ret);
        return ret;
    }

    private Item createVideoItem(VideoMediaItem c, String userAgent) {
        Res res = new Res(MimeType.valueOf(c.getMimeTypeString()), c.getSize(), formatDuration(c.getDuration()), (long) c.getBitrate(), extension.createStreamUrl(c.getUniqueID(), userAgent, null));
        //
        VideoItem ret = (new VideoItem(c.getUniqueID(), "ROOT", c.getName(), null, res));

        thumb(c, userAgent, ret);
        return ret;
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

    private Item createAudioItem(AudioMediaItem c, String userAgent) {
        PersonWithRole artist = new PersonWithRole(c.getArtist(), "Performer");

        Res res = new Res(MimeType.valueOf(c.getMimeTypeString()), c.getSize(), formatDuration(c.getStream().getDuration()), (long) c.getStream().getBitrate(), extension.createStreamUrl(c.getUniqueID(), userAgent, null));

        MusicTrack ret = new MusicTrack(c.getUniqueID(), "ROOT", c.getTitle(), c.getArtist(), c.getAlbum(), artist, res);
        thumb(c, userAgent, ret);

        return ret;
    }

    public void thumb(MediaItem c, String userAgent, Item ret) {
        try {
            List<Property<DIDLAttribute>> attributes = new ArrayList<Property<DIDLAttribute>>();
            attributes.add(new PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_TN")));
            ret.addProperty(new UPNP.ALBUM_ART_URI(new URI(extension.createStreamUrl(c.getUniqueID(), userAgent, ".albumart")), attributes));

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private BrowseResult browseMetaData(String objectID, Filter create, long firstResult, long maxResults, SortCriterion[] orderby, String userAgent) throws ContentDirectoryException {
        // ps3 and xbmc browse metaData
        try {
            MediaNode item = archive.getItemById(objectID);
            if (item != null) {
                DIDLContent didl = new DIDLContent();

                addMediaItemToDidl(userAgent, didl, item);
                String didlStrng;

                didlStrng = new DIDLParser().generate(didl);

                return new BrowseResult(didlStrng, 1, 1);
            } else {
                String didlStrng = new DIDLParser().generate(new DIDLContent());
                return new BrowseResult(didlStrng, 0, 0);
            }
        } catch (Exception e) {
            logger.log(e);

            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, e.toString());
        }
    }

    @Override
    public BrowseResult search(String containerId, String searchCriteriaString, String filterString, long firstResult, long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {
        try {
            logger.info(String.format("ContentDirectory receive search request with ContainerID:%s, SearchCriteria:%s, Filter:%s, FirstResult:%s, MaxResults:%s, SortCriterion:%s.", containerId, searchCriteriaString, filterString, firstResult, maxResults, orderBy));
            UpnpHeaders headers = org.fourthline.cling.protocol.sync.ReceivingAction.getRequestMessage().getHeaders();
            String userAgent = headers.get("User-agent") != null ? headers.get("User-agent").get(0) : null;
            SearchCriteria searchCriterion = SearchCriteria.create(searchCriteriaString);

            DIDLContent didl = new DIDLContent();
            Filter filter = Filter.create(filterString);

            List<MediaNode> children = archive.getList();
            long to = children.size();
            if (userAgent.contains("Portable SDK for UPnP devices/1.6.16")) {
                // vlc/libupnp bug in
                // http://git.videolan.org/?p=vlc.git;a=commitdiff;h=794557eea63853456cf3120cdb1bdc88ca44ad9f.
                // should be fixed in libupnp 1.6.17
                maxResults = 5;
            }
            if (maxResults > 0) {
                to = Math.min(to, firstResult + maxResults);
            }

            int count = 0;
            for (long i = firstResult; i < to; i++) {
                MediaNode c = children.get((int) i);

                switch (searchCriterion.getSearchType()) {
                case SEARCH_IMAGE:
                    if (c instanceof ImageMediaItem) {
                        addMediaItemToDidl(userAgent, didl, c);
                    }
                    break;
                case SEARCH_PLAYLIST:
                    break;
                case SEARCH_UNKNOWN:
                    break;
                case SEARCH_VIDEO:
                    if (c instanceof VideoMediaItem) {
                        addMediaItemToDidl(userAgent, didl, c);
                    }
                    break;
                case SEARCH_AUDIO:
                    if (c instanceof AudioMediaItem) {
                        addMediaItemToDidl(userAgent, didl, c);
                    }
                    break;
                }

            }
            String didlStrng = new DIDLParser().generate(didl);

            return new BrowseResult(didlStrng, count, children.size());

        } catch (Throwable e) {
            logger.log(e);
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, e.toString());
        }
    }

    @Override
    public void onContentChanged(MediaListController<?> caller) {

        changeSystemUpdateID();
    }

}
