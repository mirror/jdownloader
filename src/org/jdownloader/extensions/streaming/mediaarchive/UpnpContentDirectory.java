package org.jdownloader.extensions.streaming.mediaarchive;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.binding.annotations.UpnpAction;
import org.fourthline.cling.binding.annotations.UpnpOutputArgument;
import org.fourthline.cling.binding.annotations.UpnpStateVariable;
import org.fourthline.cling.binding.annotations.UpnpStateVariables;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.control.IncomingActionRequestMessage;
import org.fourthline.cling.protocol.sync.ReceivingAction;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLAttribute;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.DIDLObject.Property;
import org.fourthline.cling.support.model.DIDLObject.Property.DC;
import org.fourthline.cling.support.model.DIDLObject.Property.DLNA.PROFILE_ID;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP;
import org.fourthline.cling.support.model.Person;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.VideoItem;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.image.JPEGImage;
import org.jdownloader.extensions.streaming.upnp.DLNAOrg;
import org.jdownloader.extensions.streaming.upnp.Filter;
import org.jdownloader.extensions.streaming.upnp.SearchCriteria;
import org.jdownloader.logging.LogController;
import org.seamless.util.MimeType;

@UpnpStateVariables({ @UpnpStateVariable(name = "A_ARG_TYPE_ObjectID", sendEvents = false, datatype = "string"), @UpnpStateVariable(name = "A_ARG_TYPE_Result", sendEvents = false, datatype = "string"), @UpnpStateVariable(name = "A_ARG_TYPE_BrowseFlag", sendEvents = false, datatype = "string", allowedValuesEnum = BrowseFlag.class), @UpnpStateVariable(name = "A_ARG_TYPE_Filter", sendEvents = false, datatype = "string"), @UpnpStateVariable(name = "A_ARG_TYPE_SortCriteria", sendEvents = false, datatype = "string"), @UpnpStateVariable(name = "A_ARG_TYPE_Index", sendEvents = false, datatype = "ui4"), @UpnpStateVariable(name = "A_ARG_TYPE_Count", sendEvents = false, datatype = "ui4"), @UpnpStateVariable(name = "A_ARG_TYPE_UpdateID", sendEvents = false, datatype = "ui4"), @UpnpStateVariable(name = "A_ARG_TYPE_URI", sendEvents = false, datatype = "uri"),
        @UpnpStateVariable(name = "A_ARG_TYPE_SearchCriteria", sendEvents = false, datatype = "string"), @UpnpStateVariable(name = "A_ARG_TYPE_Featurelist", sendEvents = false, datatype = "string") })
public class UpnpContentDirectory extends AbstractContentDirectoryService implements MediaListListener {

    private static final String TRAILING_ZEROS = "000000000000000000000000";

    @UpnpAction(out = { @UpnpOutputArgument(name = "FeatureList", stateVariable = "A_ARG_TYPE_Result", getterName = "getFeatureList") })
    public FeatureList x_GetFeatureList() {
        FeatureList ret = new FeatureList();
        ret.add(new SamsungBasicFeature());
        return ret;
    }

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
            IncomingActionRequestMessage msg = ReceivingAction.getRequestMessage();
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

        List<MediaNode> children = archive.getDirectory(objectID).getChildren();

        DIDLContent didl = new DIDLContent();
        long to = children.size();
        if (userAgent != null && userAgent.contains("Portable SDK for UPnP devices/1.6.16")) {
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

        String didlStrng = new ExtDIDLParser().generate(didl);
        System.out.println(didlStrng);
        return new BrowseResult(didlStrng, count, children.size());

    }

    public void addMediaItemToDidl(String userAgent, DIDLContent didl, MediaNode c) {
        if (c instanceof VideoMediaItem) {

            didl.addItem(createVideoItem((VideoMediaItem) c, userAgent));
        } else if (c instanceof ImageMediaItem) {
            didl.addItem(createImageItem((ImageMediaItem) c, userAgent));
        } else if (c instanceof AudioMediaItem) {
            didl.addItem(createAudioItem((AudioMediaItem) c, userAgent));
        } else if (c instanceof MediaFolder) {
            didl.addContainer(createFolder((MediaFolder) c, userAgent));
        }
    }

    private Container createFolder(MediaFolder c, String userAgent) {

        Container con = new Container();
        con.setParentID(c.getParent().getUniqueID());
        con.setId(c.getUniqueID());
        con.setChildCount(c.getChildren().size());
        con.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container"));
        con.setRestricted(true);
        con.setSearchable(true);
        con.setTitle(c.getName());

        return con;
    }

    private Item createImageItem(ImageMediaItem c, String userAgent) {
        Res res = new Res(MimeType.valueOf(c.getMimeTypeString()), c.getSize(), extension.createStreamUrl(c.getUniqueID(), userAgent, null));
        ImageItem ret = new ImageItem(c.getUniqueID(), c.getParent().getUniqueID(), c.getName(), null, res);

        thumb(c, userAgent, ret);
        return ret;
    }

    private Item createVideoItem(VideoMediaItem c, String userAgent) {
        Res res = new Res(MimeType.valueOf(c.getMimeTypeString()), c.getSize(), formatDuration(c.getDuration()), (long) c.getSystemBitrate(), extension.createStreamUrl(c.getUniqueID(), userAgent, null));
        res.setResolution(c.getVideoStreams().get(0).getWidth(), c.getVideoStreams().get(0).getHeight());
        VideoItem ret = (new VideoItem(c.getUniqueID(), c.getParent().getUniqueID(), c.getName(), null, res));
        // <res
        // protocolInfo="http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_TN;DLNA.ORG_OP=00;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00D00000000000000000000000000000"
        // resolution="160x130">http://192.168.2.122:8895/resource/9/COVER_IMAGE</res>

        thumb(c, userAgent, ret);
        return ret;
    }

    private ProtocolInfo createProtocolInfo(Profile profile) {

        ProtocolInfo ret = new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, profile.getMimeType().getLabel(), "DLNA.ORG_PN=" + profile.getProfileID() + ";DLNA.ORG_OP=" + getOP(profile) + ";DLNA.ORG_CI=" + getCI(profile) + ";DLNA.ORG_FLAGS=" + getFlags(profile));
        return ret;
    }

    private String getFlags(Profile profile) {
        return DLNAOrg.create(DLNAOrg.BACKGROUND_TRANSFERT_MODE, DLNAOrg.DLNA_V15, DLNAOrg.INTERACTIVE_TRANSFERT_MODE);
    }

    private String getCI(Profile profile) {
        return "1";
    }

    private String getOP(Profile profile) {
        return "00";
    }

    String formatDuration(long ms) {
        long days, hours, minutes, seconds, milliseconds;
        final StringBuilder string = new StringBuilder();
        milliseconds = ms % 1000;
        days = ms / (24 * 60 * 60 * 1000);
        ms -= days * 24 * 60 * 60 * 1000;
        hours = ms / (60 * 60 * 1000);
        ms -= hours * 60 * 60 * 1000;
        minutes = ms / (60 * 1000);
        seconds = (ms - minutes * 60 * 1000) / 1000;

        return hours + ":" + minutes + ":" + seconds + ".000";
    }

    private Item createAudioItem(AudioMediaItem c, String userAgent) {
        PersonWithRole artist = new PersonWithRole(c.getArtist(), "Performer");

        Res res = new Res(MimeType.valueOf(c.getMimeTypeString()), c.getSize(), formatDuration(c.getStream().getDuration()), (long) c.getStream().getBitrate(), extension.createStreamUrl(c.getUniqueID(), userAgent, null));

        MusicTrack ret = new MusicTrack(c.getUniqueID(), c.getParent().getUniqueID(), c.getTitle(), c.getArtist(), c.getAlbum(), artist, res);
        thumb(c, userAgent, ret);

        return ret;
    }

    public void thumb(MediaItem c, String userAgent, Item ret) {
        Res img = new Res(createProtocolInfo(JPEGImage.JPEG_TN), null, extension.createStreamUrl(c.getUniqueID(), userAgent, ".albumart.jpeg_tn"));
        img.setResolution(JPEGImage.JPEG_TN.getWidth().getMax(), JPEGImage.JPEG_TN.getHeight().getMax());
        ret.addResource(img);
        try {
            List<Property<DIDLAttribute>> attributes = new ArrayList<Property<DIDLAttribute>>();
            attributes.add(new PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_TN")));
            ret.addProperty(new UPNP.ALBUM_ART_URI(new URI(extension.createStreamUrl(c.getUniqueID(), userAgent, ".albumart.jpeg_tn")), attributes));

            attributes = new ArrayList<Property<DIDLAttribute>>();
            attributes.add(new PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_SM")));
            ret.addProperty(new UPNP.ALBUM_ART_URI(new URI(extension.createStreamUrl(c.getUniqueID(), userAgent, ".albumart.jpeg_sm")), attributes));

            ret.addProperty(new UPNP.ICON(new URI(extension.createStreamUrl(c.getUniqueID(), userAgent, ".albumart.jpeg_tn"))));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (c.getCreator() != null) {
            ret.setCreator(c.getCreator());
        }
        if (c.getGenres() != null) {

            ret.removeProperties(UPNP.GENRE.class);
            for (String genre : c.getGenres()) {
                ret.addProperty(new UPNP.GENRE(genre));
            }
        }
        if (c.getArtist() != null) {
            ret.addProperty(new UPNP.ARTIST(new PersonWithRole(c.getArtist(), "Performer")));
        }

        ret.addProperty(new DC.PUBLISHER(new Person(c.getDownloadLink().getHost())));

        if (c.getAlbum() != null) {
            ret.replaceFirstProperty(new UPNP.ALBUM(c.getAlbum()));
        }
        if (c.getDate() > 0) ret.replaceFirstProperty(new DC.DATE(new SimpleDateFormat("YYYY-MM-dd").format(new Date(c.getDate()))));
        if (c.getActors() != null) {

            ret.removeProperties(UPNP.ACTOR.class);

            for (int i = 0; i < c.getActors().length; i++) {
                ret.addProperty(new UPNP.ACTOR(new PersonWithRole(c.getActors()[i], "Actor")));
            }

        }
        // Samsung Bookmarks
        // see X_SetBookmark
        ret.addProperty(new SecDcmInfoProperty("CREATIONDATE=0,BM=0"));

    }

    private BrowseResult browseMetaData(String objectID, Filter create, long firstResult, long maxResults, SortCriterion[] orderby, String userAgent) throws ContentDirectoryException {
        // ps3 and xbmc browse metaData
        try {
            MediaNode item = archive.getItemById(objectID);
            if (item != null) {
                DIDLContent didl = new DIDLContent();

                addMediaItemToDidl(userAgent, didl, item);
                String didlStrng;

                didlStrng = new ExtDIDLParser().generate(didl);

                return new BrowseResult(didlStrng, 1, 1);
            } else {
                String didlStrng = new ExtDIDLParser().generate(new DIDLContent());
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

            List<MediaNode> children = getAllChildren(archive.getDirectory(containerId).getChildren());
            long to = children.size();
            if (userAgent != null && userAgent.contains("Portable SDK for UPnP devices/1.6.16")) {
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
            String didlStrng = new ExtDIDLParser().generate(didl);

            return new BrowseResult(didlStrng, count, children.size());

        } catch (Throwable e) {
            logger.log(e);
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, e.toString());
        }
    }

    private List<MediaNode> getAllChildren(List<MediaNode> children) {
        ArrayList<MediaNode> ret = new ArrayList<MediaNode>();
        for (MediaNode mn : children) {
            ret.add(mn);
            if (mn instanceof MediaFolder) {
                ret.addAll(getAllChildren(((MediaFolder) mn).getChildren()));
            }
        }
        return ret;
    }

    @Override
    public void onContentChanged(MediaListController<?> caller) {

        changeSystemUpdateID();
    }

}
