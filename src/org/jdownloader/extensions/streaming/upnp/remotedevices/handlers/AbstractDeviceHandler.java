package org.jdownloader.extensions.streaming.upnp.remotedevices.handlers;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.HeaderCollection;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.meta.RemoteDevice;
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
import org.jdownloader.extensions.streaming.dlna.DLNATransferMode;
import org.jdownloader.extensions.streaming.dlna.DLNATransportConstants;
import org.jdownloader.extensions.streaming.dlna.Extensions;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.MP3Audio;
import org.jdownloader.extensions.streaming.dlna.profiles.image.JPEGImage;
import org.jdownloader.extensions.streaming.dlna.profiles.image.PNGImage;
import org.jdownloader.extensions.streaming.dlna.profiles.video.Mpeg2;
import org.jdownloader.extensions.streaming.mediaarchive.AudioMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.ExtDIDLParser;
import org.jdownloader.extensions.streaming.mediaarchive.ImageMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaFolder;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaNode;
import org.jdownloader.extensions.streaming.mediaarchive.SecDcmInfoProperty;
import org.jdownloader.extensions.streaming.mediaarchive.VideoMediaItem;
import org.jdownloader.extensions.streaming.upnp.DLNAOp;
import org.jdownloader.extensions.streaming.upnp.DLNAOrg;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;
import org.jdownloader.extensions.streaming.upnp.Filter;
import org.jdownloader.extensions.streaming.upnp.SearchCriteria;
import org.jdownloader.extensions.streaming.upnp.remotedevices.RendererInfo;
import org.seamless.util.MimeType;

public abstract class AbstractDeviceHandler {
    private static final String FFMPEG_AUDIO_SAMPLINGRATE = "-ar";
    private static final String FFMPEG_AUDIO_CHANNELS     = "-ac";
    private static final String FFMPEG_AUDIO_BITRATE      = "-b:a";
    private static final String FFMPEG_VIDEO_CODEC_MPEG2  = "mpeg2video";
    private static final String FFMPEG_AUDIO_CODEC_AC3    = "ac3";
    private static final String FFMPEG_AUDIO_CODEC        = "-acodec";
    private static final String FFMPEG_VIDEO_CODEC        = "-vcodec";

    private static final String FFMPEG_VIDEO_BITRATE      = "-b:v";
    private static final String FFMPEG_CONTAINER_FORMAT   = "-f";
    private static final String FFMPEG_RESOLUTION         = "-s";
    private static final String FFMPEG_KEEP_QUALITY       = "-sameq";
    private static final String FFMPEG_CONTAINER_MPEGTS   = "mpegts";
    private StreamingExtension  extension;

    public void addMediaItemToDidl(DIDLContent didl, MediaNode c) {
        Item item;
        if (c instanceof VideoMediaItem) {
            item = createVideoItem((VideoMediaItem) c);
            if (item != null) didl.addItem(item);
        } else if (c instanceof ImageMediaItem) {
            item = createImageItem((ImageMediaItem) c);
            if (item != null) didl.addItem(item);
        } else if (c instanceof AudioMediaItem) {
            item = createAudioItem((AudioMediaItem) c);
            if (item != null) didl.addItem(item);
        } else if (c instanceof MediaFolder) {
            Container container = createFolder((MediaFolder) c);
            if (container != null) didl.addContainer(container);
        }
    }

    protected String createStreamUrl(MediaNode c, String format, String subpath) {
        return extension.createStreamUrl(c.getUniqueID(), getID(), format, subpath);
    }

    public void addMetaInfos(MediaItem c, Item ret) {
        // samsung
        Res img = new Res(createProtocolInfo(JPEGImage.JPEG_TN, c), null, createStreamUrl(c, JPEGImage.JPEG_TN.getProfileID(), null));
        img.setResolution(JPEGImage.JPEG_TN.getWidth().getMax(), JPEGImage.JPEG_TN.getHeight().getMax());
        ret.addResource(img);
        try {
            List<Property<DIDLAttribute>> attributes = new ArrayList<Property<DIDLAttribute>>();
            attributes.add(new PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_TN")));
            ret.addProperty(new UPNP.ALBUM_ART_URI(new URI(createStreamUrl(c, JPEGImage.JPEG_TN.getProfileID(), null)), attributes));

            attributes = new ArrayList<Property<DIDLAttribute>>();
            attributes.add(new PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_SM")));
            ret.addProperty(new UPNP.ALBUM_ART_URI(new URI(createStreamUrl(c, JPEGImage.JPEG_SM.getProfileID(), null)), attributes));

            ret.addProperty(new UPNP.ICON(new URI(createStreamUrl(c, JPEGImage.JPEG_TN.getProfileID(), null))));
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

    public BrowseResult browseContentDirectory(RendererInfo rendererInfo, MediaFolder directory, Filter create, long firstResult, long maxResults, SortCriterion[] orderby) throws Exception {

        List<MediaNode> children = directory.getChildren();

        DIDLContent didl = new DIDLContent();
        // long to = children.size();
        // if (userAgent != null && userAgent.contains("Portable SDK for UPnP devices/1.6.16")) {
        // // vlc/libupnp bug in
        // // http://git.videolan.org/?p=vlc.git;a=commitdiff;h=794557eea63853456cf3120cdb1bdc88ca44ad9f.
        // // should be fixed in libupnp 1.6.17
        // maxResults = 5;
        // }
        // if (maxResults > 0) {
        // to = Math.min(to, firstResult + maxResults);
        // }

        int count = 0;
        for (long i = firstResult; i < Math.min(children.size(), firstResult + getMaxResultsPerCall(maxResults)); i++) {
            MediaNode c = children.get((int) i);
            addMediaItemToDidl(didl, c);
            count++;

        }

        String didlStrng = new ExtDIDLParser().generate(didl);
        System.out.println(didlStrng);
        return new BrowseResult(didlStrng, count, children.size());

    }

    public BrowseResult browseMetaData(RendererInfo rendererInfo, MediaNode item, Filter create, long firstResult, long maxResults, SortCriterion[] orderby) throws Exception {

        if (item != null) {
            DIDLContent didl = new DIDLContent();

            addMediaItemToDidl(didl, item);
            String didlStrng;

            didlStrng = new ExtDIDLParser().generate(didl);
            // didlStrng =
            // "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">"
            // + "<item id=\"1346331839294\" parentID=\"image\" restricted=\"0\">" + "<dc:title>hornoxe.com_userpicdump05_14.jpg</dc:title>"
            // +
            // // "<dc:creator>(directhttp) http://www.hornoxe.com/hornoxe-com-userpicdump-5/</dc:creator>" +
            // "<upnp:class>object.item.imageItem</upnp:class>" +
            // "<upnp:albumArtURI xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\" dlna:profileID=\"JPEG_TN\">http://192.168.2.122:3128/stream/xbmc/1346331839294/JPEG_TN</upnp:albumArtURI>"
            // +
            // "<upnp:albumArtURI xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\" dlna:profileID=\"JPEG_SM\">http://192.168.2.122:3128/stream/xbmc/1346331839294/JPEG_SM</upnp:albumArtURI>"
            // +
            // // "<upnp:icon>http://192.168.2.122:3128/stream/xbmc/1346331839294/JPEG_TN</upnp:icon>" +
            // // "<dc:publisher>directhttp</dc:publisher><dc:date>2012-09-03</dc:date>" +
            // "<res colorDepth=\"24\" protocolInfo=\"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_MED;DLNA.ORG_OP=01;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00d00000000000000000000000000000\" resolution=\"699x512\" size=\"63028\">http://192.168.2.122:3128/stream/xbmc/1346331839294/JPEG_MED</res>"
            // +
            // //
            // "<res protocolInfo=\"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_TN;DLNA.ORG_OP=01;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00d00000000000000000000000000000\" resolution=\"160x160\">http://192.168.2.122:3128/stream/xbmc/1346331839294/JPEG_TN</res>"
            // // +
            // // "<sec:dcminfo xmlns:sec=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">CREATIONDATE=0,BM=0</sec:dcminfo>" +
            // "</item>" + "</DIDL-Lite>";
            System.out.println(didlStrng);
            return new BrowseResult(didlStrng, 1, 1);
        } else {
            String didlStrng = new ExtDIDLParser().generate(new DIDLContent());
            return new BrowseResult(didlStrng, 0, 0);
        }

    }

    private Item createAudioItem(AudioMediaItem c) {
        PersonWithRole artist = new PersonWithRole(c.getArtist(), "Performer");

        MusicTrack ret = new MusicTrack(c.getUniqueID(), c.getParent().getUniqueID(), c.getTitle(), c.getArtist(), c.getAlbum(), artist);

        if (c.getDlnaProfiles() != null && c.getDlnaProfiles().length > 0) {
            main: for (String pString : c.getDlnaProfiles()) {
                ArrayList<Profile> profiles = Profile.ALL_PROFILES_MAP.get(pString);
                if (profiles != null) {
                    for (Profile p : profiles) {

                        Res res = new Res(createProtocolInfo(p, c), c.getSize(), formatDuration(c.getStream().getDuration()), (long) c.getStream().getBitrate(), createStreamUrl(c, p.getProfileID(), null));
                        ret.addResource(res);

                        break main;
                    }
                }

            }

        }
        if (ret.getResources().size() == 0) {
            Profile bestProfile = getBestProfileForTranscoding(c, null);
            Res res = new Res(createProtocolInfo(bestProfile, c), c.getSize(), formatDuration(c.getStream().getDuration()), (long) c.getStream().getBitrate(), createStreamUrl(c, bestProfile.getProfileID(), null));
            ret.addResource(res);
        }
        addMetaInfos(c, ret);

        return ret;
    }

    // public abstract String createContentType(Profile dlnaProfile, MediaItem mediaItem);

    protected String createDidlString(DIDLContent didl) throws Exception {
        return new ExtDIDLParser().generate(didl);
    }

    protected String createDlnaOrgCi(Profile profile, MediaItem mediaItem) {
        return "1";
    }

    public String createDlnaOrgFlags(Profile dlnaProfile, MediaItem mediaItem) {

        return DLNAOrg.create(DLNAOrg.BACKGROUND_TRANSFERT_MODE, DLNAOrg.DLNA_V15, DLNAOrg.INTERACTIVE_TRANSFERT_MODE);

    }

    /**
     * Supported Seek Modes
     * 
     * @param dlnaProfile
     * @param mediaItem
     * @return
     */
    public String createDlnaOrgOP(Profile dlnaProfile, MediaItem mediaItem) {
        return DLNAOp.create(DLNAOp.RANGE_SEEK_SUPPORTED);
    }

    /**
     * MediaProfile
     * 
     * @param dlnaProfile
     * @param mediaItem
     * @return
     */
    public String createDlnaOrgPN(Profile dlnaProfile, MediaItem mediaItem) {
        return dlnaProfile.getProfileID();

    }

    private Container createFolder(MediaFolder c) {

        Container con = new Container();
        con.setParentID(c.getParent() == null ? "-1" : c.getParent().getUniqueID());
        con.setId(c.getUniqueID());
        con.setChildCount(c.getChildren().size());
        con.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container"));

        con.setRestricted(true);
        con.setSearchable(true);
        con.setTitle(c.getName());

        try {
            if (c.getThumbnailPath() != null && Application.getRessourceURL(c.getThumbnailPath()) != null) {
                List<Property<DIDLAttribute>> attributes;

                attributes = new ArrayList<Property<DIDLAttribute>>();
                attributes.add(new PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", PNGImage.PNG_LRG_ICO.getProfileID())));
                con.addProperty(new UPNP.ALBUM_ART_URI(new URI(createStreamUrl(c, PNGImage.PNG_LRG_ICO.getProfileID(), null)), attributes));
                attributes = new ArrayList<Property<DIDLAttribute>>();
                attributes.add(new PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_SM")));
                con.addProperty(new UPNP.ALBUM_ART_URI(new URI(createStreamUrl(c, JPEGImage.JPEG_SM.getProfileID(), null)), attributes));

                attributes = new ArrayList<Property<DIDLAttribute>>();
                attributes.add(new PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", PNGImage.PNG_SM_ICO.getProfileID())));
                con.addProperty(new UPNP.ALBUM_ART_URI(new URI(createStreamUrl(c, PNGImage.PNG_SM_ICO.getProfileID(), null)), attributes));
                attributes = new ArrayList<Property<DIDLAttribute>>();
                attributes.add(new PROFILE_ID(new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, "dlna", "JPEG_TN")));
                con.addProperty(new UPNP.ALBUM_ART_URI(new URI(createStreamUrl(c, JPEGImage.JPEG_TN.getProfileID(), null)), attributes));

                con.addProperty(new UPNP.ICON(new URI(createStreamUrl(c, JPEGImage.JPEG_TN.getProfileID(), null))));
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return con;
    }

    public String createHeaderAcceptRanges(Profile dlnaProfile, MediaItem mediaItem, String transferModeHeader, String featuresHeader) {
        return "bytes";
    }

    private Item createImageItem(ImageMediaItem c) {

        ImageItem ret = new ImageItem(c.getUniqueID(), c.getParent().getUniqueID(), c.getName(), null);
        if (c.getDlnaProfiles() != null && c.getDlnaProfiles().length > 0) {
            main: for (String pString : c.getDlnaProfiles()) {
                ArrayList<Profile> profiles = Profile.ALL_PROFILES_MAP.get(pString);
                if (profiles != null) {
                    for (Profile p : profiles) {

                        Res res = new Res(createProtocolInfo(p, c), c.getSize(), createStreamUrl(c, p.getProfileID(), null));
                        res.setColorDepth(24l);
                        res.setResolution(c.getWidth(), c.getHeight());
                        ret.addResource(res);
                        break main;
                    }
                }

            }

        }
        if (ret.getResources().size() == 0) {

            Res res = new Res(createProtocolInfo(JPEGImage.JPEG_LRG, c), c.getSize(), createStreamUrl(c, JPEGImage.JPEG_LRG.getProfileID(), null));
            res.setResolution(c.getWidth(), c.getHeight());
            ret.addResource(res);
        }

        addMetaInfos(c, ret);
        return ret;
    }

    protected ProtocolInfo createProtocolInfo(Profile profile, MediaItem mediaItem) {
        // if (mediaItem instanceof VideoMediaItem) {
        ProtocolInfo ret = new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, profile.getMimeType().getLabel(), "DLNA.ORG_PN=" + profile.getProfileID() + ";DLNA.ORG_OP=" + createDlnaOrgOP(profile, mediaItem) + ";DLNA.ORG_CI=" + createDlnaOrgCi(profile, mediaItem) + ";DLNA.ORG_FLAGS=" + createDlnaOrgFlags(profile, mediaItem));
        return ret;
        // }
        //
        // ProtocolInfo ret = new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, profile.getMimeType().getLabel(),
        // ProtocolInfo.WILDCARD);
        // return ret;
    }

    private Item createVideoItem(VideoMediaItem c) {
        Res res = new Res(MimeType.valueOf(c.getMimeTypeString()), c.getSize(), formatDuration(c.getDuration()), (long) c.getSystemBitrate(), createStreamUrl(c, null, null));
        res.setResolution(c.getVideoStreams().get(0).getWidth(), c.getVideoStreams().get(0).getHeight());
        VideoItem ret = (new VideoItem(c.getUniqueID(), c.getParent().getUniqueID(), c.getName(), null, res));
        // <res
        // protocolInfo="http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_TN;DLNA.ORG_OP=00;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00D00000000000000000000000000000"
        // resolution="160x130">http://192.168.2.122:8895/resource/9/COVER_IMAGE</res>

        addMetaInfos(c, ret);
        return ret;
    }

    protected String formatDuration(long ms) {
        long days, hours, minutes, seconds;
        final StringBuilder string = new StringBuilder();
        days = ms / (24 * 60 * 60 * 1000);
        ms -= days * 24 * 60 * 60 * 1000;
        hours = ms / (60 * 60 * 1000);
        ms -= hours * 60 * 60 * 1000;
        minutes = ms / (60 * 1000);
        seconds = (ms - minutes * 60 * 1000) / 1000;

        return hours + ":" + minutes + ":" + seconds + ".000";
    }

    public Profile getBestProfileForTranscoding(MediaItem mediaItem, String formatID) {
        if (formatID != null) {
            ArrayList<Profile> ret = Profile.ALL_PROFILES_MAP.get(formatID);
            if (ret != null) return ret.get(0);
        }
        if (mediaItem instanceof VideoMediaItem) {
            //
            return Mpeg2.MPEG_TS_SD_EU;
        } else if (mediaItem instanceof ImageMediaItem) {
            //
            return JPEGImage.JPEG_MED;
        } else if (mediaItem instanceof AudioMediaItem) {
            //
            return MP3Audio.MP3;
        }
        return null;
    }

    public Profile getBestProfileWithoutTranscoding(MediaItem mediaItem, String formatID) {
        try {
            String profileString = mediaItem.getDlnaProfiles()[0];
            return Profile.ALL_PROFILES_MAP.get(profileString).get(0);
        } catch (Exception e) {

        }
        return null;
    }

    public String getID() {
        return getClass().getSimpleName();
    }

    /**
     * Some devices cannot handler many results. due to a libupnp error. vlc is a good example
     * 
     * @param maxResults
     * @return
     */
    protected long getMaxResultsPerCall(long maxResults) {
        return maxResults;
    }

    public abstract boolean matchesRemoteDevice(RemoteDevice d, DeviceCache cache);

    /**
     * Optional header check
     * 
     * @param headerCollection
     * @return
     */
    public boolean matchesStreamHeader(HeaderCollection headerCollection) {
        return true;
    }

    /**
     * Checks the useragent we see when the device requests a stream url
     * 
     * @param string
     * @return
     */
    public abstract boolean matchesStreamUserAgent(String string);

    public abstract boolean matchesUpnpHeader(UpnpHeaders headers);

    /**
     * Checks the userAgent we see when the device calls the browse Content Directory Command.
     * 
     * @param upnpUserAgent
     * @return
     */
    public abstract boolean matchesUpnpUserAgent(String upnpUserAgent);

    public BrowseResult searchContentDirectory(RendererInfo callerDevice, MediaArchiveController archive, MediaFolder directory, SearchCriteria searchCriterion, Filter filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws Exception {

        DIDLContent didl = new DIDLContent();

        List<MediaNode> children = MediaArchiveController.getAllChildren(directory.getChildren());

        int count = 0;
        for (long i = firstResult; i < Math.min(children.size(), firstResult + getMaxResultsPerCall(maxResults)); i++) {
            MediaNode c = children.get((int) i);

            switch (searchCriterion.getSearchType()) {
            case SEARCH_IMAGE:
                if (c instanceof ImageMediaItem) {
                    addMediaItemToDidl(didl, c);
                }
                break;
            case SEARCH_PLAYLIST:
                break;
            case SEARCH_UNKNOWN:
                break;
            case SEARCH_VIDEO:
                if (c instanceof VideoMediaItem) {
                    addMediaItemToDidl(didl, c);
                }
                break;
            case SEARCH_AUDIO:
                if (c instanceof AudioMediaItem) {
                    addMediaItemToDidl(didl, c);
                }
                break;
            }

        }
        String didlStrng = createDidlString(didl);

        return new BrowseResult(didlStrng, count, children.size());

    }

    public String getDlnaFeaturesString(Profile dlnaProfile, MediaItem mediaItem, String transferModeHeader, String featuresHeader) {
        // if (mediaItem instanceof VideoMediaItem) {
        return "DLNA.ORG_PN=" + createDlnaOrgPN(dlnaProfile, mediaItem) + ";DLNA.ORG_OP=" + createDlnaOrgOP(dlnaProfile, mediaItem) + ";DLNA.ORG_FLAGS=" + createDlnaOrgFlags(dlnaProfile, mediaItem);
        // } else {
        // return "DLNA.ORG_PN=" + createDlnaOrgPN(dlnaProfile, mediaItem);
        // }

    }

    public String createContentType(Profile dlnaProfile, MediaItem mediaItem) {
        if (dlnaProfile == null) {
            try {
                return Extensions.getType(Files.getExtension(mediaItem.getName()).toLowerCase(Locale.ENGLISH)) + "/" + Files.getExtension(mediaItem.getName());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return dlnaProfile.getMimeType().getLabel();
    }

    public void setExtension(StreamingExtension extension) {
        this.extension = extension;
    }

    public StreamingExtension getExtension() {
        return extension;
    }

    public String getTransferMode(GetRequest request, String defaultValue) {

        HTTPHeader head = request.getRequestHeaders().get(DLNATransportConstants.HEADER_TRANSFERMODE);
        String ret = null;
        if (head != null && StringUtils.isNotEmpty(head.getValue())) {
            ret = DLNATransferMode.valueOf(head.getValue());
        }
        return ret != null ? ret : defaultValue;

    }

    // D. = Demuxing supported
    // .E = Muxing supported
    // --
    // E 3g2 3GP2 (3GPP2 file format)
    // E 3gp 3GP (3GPP file format)
    // D 4xm 4X Technologies
    // E a64 a64 - video for Commodore 64
    // D aac raw ADTS AAC (Advanced Audio Coding)
    // DE ac3 raw AC-3
    // D act ACT Voice file format
    // D adf Artworx Data Format
    // E adts ADTS AAC (Advanced Audio Coding)
    // DE adx CRI ADX
    // D aea MD STUDIO audio
    // DE aiff Audio IFF
    // DE alaw PCM A-law
    // DE amr 3GPP AMR
    // D anm Deluxe Paint Animation
    // D apc CRYO APC
    // D ape Monkey's Audio
    // DE asf ASF (Advanced / Active Streaming Format)
    // E asf_stream ASF (Advanced / Active Streaming Format)
    // DE ass SSA (SubStation Alpha) subtitle
    // DE au Sun AU
    // DE avi AVI (Audio Video Interleaved)
    // E avm2 SWF (ShockWave Flash) (AVM2)
    // D avs AVISynth
    // D bethsoftvid Bethesda Softworks VID
    // D bfi Brute Force & Ignorance
    // D bin Binary text
    // D bink Bink
    // DE bit G.729 BIT file format
    // D bmv Discworld II BMV
    // D c93 Interplay C93
    // DE caf Apple Core Audio Format
    // DE cavsvideo raw Chinese AVS (Audio Video Standard) video
    // D cdg CD Graphics
    // D cdxl Commodore CDXL video
    // E crc CRC testing
    // DE daud D-Cinema audio
    // D dfa Chronomaster DFA
    // DE dirac raw Dirac
    // DE dnxhd raw DNxHD (SMPTE VC-3)
    // D dshow DirectShow capture
    // D dsicin Delphine Software International CIN
    // DE dts raw DTS
    // DE dv DV (Digital Video)
    // E dvd MPEG-2 PS (DVD VOB)
    // D dxa DXA
    // D ea Electronic Arts Multimedia
    // D ea_cdata Electronic Arts cdata
    // DE eac3 raw E-AC-3
    // DE f32be PCM 32-bit floating-point big-endian
    // DE f32le PCM 32-bit floating-point little-endian
    // DE f64be PCM 64-bit floating-point big-endian
    // DE f64le PCM 64-bit floating-point little-endian
    // DE ffm FFM (FFserver live feed)
    // DE ffmetadata FFmpeg metadata in text
    // D film_cpk Sega FILM / CPK
    // DE filmstrip Adobe Filmstrip
    // DE flac raw FLAC
    // D flic FLI/FLC/FLX animation
    // DE flv FLV (Flash Video)
    // E framecrc framecrc testing
    // E framemd5 Per-frame MD5 testing
    // DE g722 raw G.722
    // DE g723_1 raw G.723.1
    // D g729 G.729 raw format demuxer
    // E gif GIF Animation
    // D gsm raw GSM
    // DE gxf GXF (General eXchange Format)
    // DE h261 raw H.261
    // DE h263 raw H.263
    // DE h264 raw H.264 video
    // D hls,applehttp Apple HTTP Live Streaming
    // D ico Microsoft Windows ICO
    // D idcin id Cinematic
    // D idf iCE Draw File
    // D iff IFF (Interchange File Format)
    // DE ilbc iLBC storage
    // DE image2 image2 sequence
    // DE image2pipe piped image2 sequence
    // D ingenient raw Ingenient MJPEG
    // D ipmovie Interplay MVE
    // E ipod iPod H.264 MP4 (MPEG-4 Part 14)
    // E ismv ISMV/ISMA (Smooth Streaming)
    // D iss Funcom ISS
    // D iv8 IndigoVision 8000 video
    // DE ivf On2 IVF
    // DE jacosub JACOsub subtitle format
    // D jv Bitmap Brothers JV
    // DE latm LOAS/LATM
    // D lavfi Libavfilter virtual input device
    // DE libnut nut format
    // D lmlm4 raw lmlm4
    // D loas LOAS AudioSyncStream
    // D lxf VR native stream (LXF)
    // DE m4v raw MPEG-4 video
    // E matroska Matroska
    // D matroska,webm Matroska / WebM
    // E md5 MD5 testing
    // D mgsts Metal Gear Solid: The Twin Snakes
    // DE microdvd MicroDVD subtitle format
    // DE mjpeg raw MJPEG video
    // E mkvtimestamp_v2 extract pts as timecode v2 format, as defined by mkvtoolnix
    // DE mlp raw MLP
    // D mm American Laser Games MM
    // DE mmf Yamaha SMAF
    // E mov QuickTime / MOV
    // D mov,mp4,m4a,3gp,3g2,mj2 QuickTime / MOV
    // E mp2 MP2 (MPEG audio layer 2)
    // DE mp3 MP3 (MPEG audio layer 3)
    // E mp4 MP4 (MPEG-4 Part 14)
    // D mpc Musepack
    // D mpc8 Musepack SV8
    // DE mpeg MPEG-1 Systems / MPEG program stream
    // E mpeg1video raw MPEG-1 video
    // E mpeg2video raw MPEG-2 video
    // DE mpegts MPEG-TS (MPEG-2 Transport Stream)
    // D mpegtsraw raw MPEG-TS (MPEG-2 Transport Stream)
    // D mpegvideo raw MPEG video
    // E mpjpeg MIME multipart JPEG
    // D msnwctcp MSN TCP Webcam stream
    // D mtv MTV
    // DE mulaw PCM mu-law
    // D mvi Motion Pixels MVI
    // DE mxf MXF (Material eXchange Format)
    // E mxf_d10 MXF (Material eXchange Format) D-10 Mapping
    // D mxg MxPEG clip
    // D nc NC camera feed
    // D nsv Nullsoft Streaming Video
    // E null raw null video
    // DE nut NUT
    // D nuv NuppelVideo
    // DE ogg Ogg
    // DE oma Sony OpenMG audio
    // D paf Amazing Studio Packed Animation File
    // D pmp Playstation Portable PMP
    // E psp PSP MP4 (MPEG-4 Part 14)
    // D psxstr Sony Playstation STR
    // D pva TechnoTrend PVA
    // D qcp QCP
    // D r3d REDCODE R3D
    // DE rawvideo raw video
    // E rcv VC-1 test bitstream
    // D realtext RealText subtitle format
    // D rl2 RL2
    // DE rm RealMedia
    // DE roq raw id RoQ
    // D rpl RPL / ARMovie
    // DE rso Lego Mindstorms RSO
    // DE rtp RTP output
    // DE rtsp RTSP output
    // DE s16be PCM signed 16-bit big-endian
    // DE s16le PCM signed 16-bit little-endian
    // DE s24be PCM signed 24-bit big-endian
    // DE s24le PCM signed 24-bit little-endian
    // DE s32be PCM signed 32-bit big-endian
    // DE s32le PCM signed 32-bit little-endian
    // DE s8 PCM signed 8-bit
    // D sami SAMI subtitle format
    // DE sap SAP output
    // D sbg SBaGen binaural beats script
    // E sdl SDL output device
    // D sdp SDP
    // E segment segment
    // D shn raw Shorten
    // D siff Beam Software SIFF
    // DE smjpeg Loki SDL MJPEG
    // D smk Smacker
    // D smush LucasArts Smush
    // D sol Sierra SOL
    // DE sox SoX native
    // DE spdif IEC 61937 (used on S/PDIF - IEC958)
    // DE srt SubRip subtitle
    // E stream_segment,ssegment streaming segment muxer
    // D subviewer SubViewer subtitle format
    // E svcd MPEG-2 PS (SVCD)
    // DE swf SWF (ShockWave Flash)
    // D thp THP
    // D tiertexseq Tiertex Limited SEQ
    // D tmv 8088flex TMV
    // DE truehd raw TrueHD
    // D tta TTA (True Audio)
    // D tty Tele-typewriter
    // D txd Renderware TeXture Dictionary
    // DE u16be PCM unsigned 16-bit big-endian
    // DE u16le PCM unsigned 16-bit little-endian
    // DE u24be PCM unsigned 24-bit big-endian
    // DE u24le PCM unsigned 24-bit little-endian
    // DE u32be PCM unsigned 32-bit big-endian
    // DE u32le PCM unsigned 32-bit little-endian
    // DE u8 PCM unsigned 8-bit
    // D vc1 raw VC-1
    // D vc1test VC-1 test bitstream
    // E vcd MPEG-1 Systems / MPEG program stream (VCD)
    // D vfwcap VfW video capture
    // D vmd Sierra VMD
    // E vob MPEG-2 PS (VOB)
    // DE voc Creative Voice
    // D vqf Nippon Telegraph and Telephone Corporation (NTT) TwinVQ
    // D w64 Sony Wave64
    // DE wav WAV / WAVE (Waveform Audio)
    // D wc3movie Wing Commander III movie
    // E webm WebM
    // D wsaud Westwood Studios audio
    // D wsvqa Westwood Studios VQA
    // DE wtv Windows Television (WTV)
    // DE wv WavPack
    // D xa Maxis XA
    // D xbin eXtended BINary text (XBIN)
    // D xmv Microsoft XMV
    // D xwma Microsoft xWMA
    // D yop Psygnosis YOP
    // DE yuv4mpegpipe YUV4MPEG pipe
    public String[] getFFMpegTranscodeCommandline(MediaItem mediaItem, Profile profile, String dataUrl) {
        if (mediaItem instanceof VideoMediaItem) {
            //

            // should work for ps3
            return new String[] { "-i", dataUrl, "-y", FFMPEG_VIDEO_CODEC, FFMPEG_VIDEO_CODEC_MPEG2, FFMPEG_AUDIO_CODEC, FFMPEG_AUDIO_CODEC_AC3, FFMPEG_AUDIO_BITRATE, "384000", FFMPEG_AUDIO_CHANNELS, "2", FFMPEG_AUDIO_SAMPLINGRATE, "48000", FFMPEG_CONTAINER_FORMAT, "mpegts", dataUrl };

        } else if (mediaItem instanceof ImageMediaItem) {
            //
            throw new WTFException("Transcode Images with ffmpeg? no way");
        } else if (mediaItem instanceof AudioMediaItem) {
            //
            return new String[] { "-i", dataUrl, "-vn", FFMPEG_AUDIO_SAMPLINGRATE, "44100", FFMPEG_AUDIO_CHANNELS, "2", FFMPEG_AUDIO_BITRATE, "192000", FFMPEG_CONTAINER_FORMAT, "mp3", dataUrl };

        }
        throw new WTFException("Transcode Unknown items with ffmpeg? no way");
    }

    public long estimateTranscodedContentLength(MediaItem mediaItem, Profile dlnaProfile) {
        // if (mediaItem instanceof VideoMediaItem) {
        // //
        // return (((VideoMediaItem) mediaItem).getDuration() / 1000 * (2881700 + 128000)) / 8;
        //
        // } else if (mediaItem instanceof ImageMediaItem) {
        // return -1;
        // } else if (mediaItem instanceof AudioMediaItem) {
        // //
        // return (((AudioMediaItem) mediaItem).getStream().getDuration() / 1000 * 192000l) / 8;
        //
        // }
        return -1;
    }

}
