package org.jdownloader.extensions.streaming.upnp.remotedevices.handlers;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import org.jdownloader.extensions.streaming.dlna.profiles.image.JPEGImage;
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
    private StreamingExtension extension;

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

    protected String createStreamUrl(MediaItem c, String format, String subpath) {
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
            // "<DIDL-Lite xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\">    <item id=\"{472F254D-1B25-49F5-B904-21B208A75C88}.0.8853685C\"  restricted=\"1\" parentID=\"8853685C\">        <dc:title>ahggbagh</dc:title>        <res size=\"1308243\" resolution=\"928x714\" protocolInfo=\"http-get:*:image/png:DLNA.ORG_PN=PNG_LRG;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=00d00000000000000000000000000000\" colorDepth=\"24\">http://192.168.2.122:10243/WMPNSSv4/102445194/0_ezQ3MkYyNTRELTFCMjUtNDlGNS1COTA0LTIxQjIwOEE3NUM4OH0uMC44ODUzNjg1Qw.png</res>        <res resolution=\"160x123\" protocolInfo=\"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_TN;DLNA.ORG_OP=01;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00d00000000000000000000000000000\" colorDepth=\"24\">http://192.168.2.122:10243/WMPNSSv4/102445194/ezQ3MkYyNTRELTFCMjUtNDlGNS1COTA0LTIxQjIwOEE3NUM4OH0uMC44ODUzNjg1Qw.jpg?thumbnail=true,formatID=13,width=160,height=123</res>        <res resolution=\"623x480\" protocolInfo=\"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_SM;DLNA.ORG_OP=01;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00d00000000000000000000000000000\" colorDepth=\"24\">http://192.168.2.122:10243/WMPNSSv4/102445194/ezQ3MkYyNTRELTFCMjUtNDlGNS1COTA0LTIxQjIwOEE3NUM4OH0uMC44ODUzNjg1Qw.jpg?formatID=16,width=623,height=480</res>        <res resolution=\"928x714\" protocolInfo=\"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_MED;DLNA.ORG_OP=01;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00d00000000000000000000000000000\" colorDepth=\"24\">http://192.168.2.122:10243/WMPNSSv4/102445194/ezQ3MkYyNTRELTFCMjUtNDlGNS1COTA0LTIxQjIwOEE3NUM4OH0uMC44ODUzNjg1Qw.jpg?formatID=17,width=928,height=714</res>        <res resolution=\"136x90\" protocolInfo=\"http-get:*:image/x-ycbcr-yuv420:DLNA.ORG_OP=01;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00d00000000000000000000000000000\" colorDepth=\"24\">http://192.168.2.122:10243/WMPNSSv4/102445194/ezQ3MkYyNTRELTFCMjUtNDlGNS1COTA0LTIxQjIwOEE3NUM4OH0uMC44ODUzNjg1Qw?formatID=82,width=136,height=90,thumbnail=false,aspectRatio=9:8,rFill=20,gFill=20,bFill=20</res>        <res resolution=\"684x456\" protocolInfo=\"http-get:*:image/x-ycbcr-yuv420:DLNA.ORG_OP=01;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00d00000000000000000000000000000\" colorDepth=\"24\">http://192.168.2.122:10243/WMPNSSv4/102445194/ezQ3MkYyNTRELTFCMjUtNDlGNS1COTA0LTIxQjIwOEE3NUM4OH0uMC44ODUzNjg1Qw?formatID=83,width=684,height=456,thumbnail=false,aspectRatio=9:8,rFill=20,gFill=20,bFill=20</res>        <res resolution=\"928x714\" protocolInfo=\"http-get:*:image/x-ycbcr-yuv420:DLNA.ORG_OP=01;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=00d00000000000000000000000000000\" colorDepth=\"24\">http://192.168.2.122:10243/WMPNSSv4/102445194/ezQ3MkYyNTRELTFCMjUtNDlGNS1COTA0LTIxQjIwOEE3NUM4OH0uMC44ODUzNjg1Qw?formatID=84,width=928,height=714,thumbnail=false,aspectRatio=1:1,rFill=20,gFill=20,bFill=20</res>        <upnp:class>object.item.imageItem.photo</upnp:class>        <upnp:album>[Keine Schl..sselw..rter]</upnp:album>        <dc:date>2012-03-28</dc:date>        <upnp:scheduledStartTime>2012-03-28T13:23:00</upnp:scheduledStartTime>        <upnp:albumArtURI xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\" dlna:profileID=\"JPEG_SM\">http://192.168.2.122:10243/WMPNSSv4/102445194/0_ezQ3MkYyNTRELTFCMjUtNDlGNS1COTA0LTIxQjIwOEE3NUM4OH0uMC44ODUzNjg1Qw.jpg?albumArt=true</upnp:albumArtURI>        <upnp:albumArtURI xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\" dlna:profileID=\"JPEG_TN\">http://192.168.2.122:10243/WMPNSSv4/102445194/ezQ3MkYyNTRELTFCMjUtNDlGNS1COTA0LTIxQjIwOEE3NUM4OH0uMC44ODUzNjg1Qw.jpg?albumArt=true,formatID=13</upnp:albumArtURI>        <desc xmlns:microsoft=\"urn:schemas-microsoft-com:WMPNSS-1-0/\" id=\"folderPath\" nameSpace=\"urn:schemas-microsoft-com:WMPNSS-1-0/\">        <microsoft:folderPath>Pictures</microsoft:folderPath>    </desc></item></DIDL-Lite>";
            return new BrowseResult(didlStrng, 1, 1);
        } else {
            String didlStrng = new ExtDIDLParser().generate(new DIDLContent());
            return new BrowseResult(didlStrng, 0, 0);
        }

    }

    private Item createAudioItem(AudioMediaItem c) {
        PersonWithRole artist = new PersonWithRole(c.getArtist(), "Performer");

        Res res = new Res(MimeType.valueOf(c.getMimeTypeString()), c.getSize(), formatDuration(c.getStream().getDuration()), (long) c.getStream().getBitrate(), createStreamUrl(c, null, null));

        MusicTrack ret = new MusicTrack(c.getUniqueID(), c.getParent().getUniqueID(), c.getTitle(), c.getArtist(), c.getAlbum(), artist, res);
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

    public abstract Profile getBestProfileForTranscoding(MediaItem mediaItem);

    public Profile getBestProfileWithoutTranscoding(MediaItem mediaItem) {
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

}
