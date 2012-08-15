package org.jdownloader.extensions.streaming.upnp.content;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.control.IncomingActionRequestMessage;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.model.types.csv.CSV;
import org.fourthline.cling.protocol.sync.ReceivingAction;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.jdownloader.extensions.streaming.upnp.ContentFactory;
import org.jdownloader.extensions.streaming.upnp.MediaServer;
import org.jdownloader.logging.LogController;

public class ContentDirectory extends AbstractContentDirectoryService {

    private ContentProvider defaultProvider;
    private LogSource       logger;
    private MediaServer     mediaServer;

    public ContentDirectory(MediaServer mediaServer) {
        super(getSearch(), getSort());
        defaultProvider = ContentFactory.create(this, mediaServer);
        this.mediaServer = mediaServer;
        logger = LogController.getInstance().getLogger("streaming");

    }

    private static List<String> getSort() {
        ArrayList<String> ret = new ArrayList<String>();
        for (String s : "dc:title,upnp:genre,upnp:album,dc:creator,res@size,res@duration,res@bitrate,dc:publisher,dc:language,upnp:originalTrackNumber,dc:date,upnp:producer,upnp:rating,upnp:actor,upnp:director,upnp:toc,dc:description,microsoft:year,microsoft:userRatingInStars,microsoft:userEffectiveRatingInStars,microsoft:userRating,microsoft:userEffectiveRating,microsoft:serviceProvider,microsoft:artistAlbumArtist,microsoft:artistPerformer,microsoft:artistConductor,microsoft:authorComposer,microsoft:authorOriginalLyricist,microsoft:authorWriter,microsoft:sourceUrl,upnp:userAnnotation,upnp:channelName,upnp:longDescription,upnp:programTitle".split(",")) {
            ret.add(s);
        }
        return ret;
    }

    private static List<String> getSearch() {
        ArrayList<String> ret = new ArrayList<String>();
        for (String s : "@id,@refID,dc:title,upnp:class,upnp:genre,upnp:artist,upnp:author,upnp:author@role,upnp:album,dc:creator,res@size,res@duration,res@protocolInfo,res@protection,dc:publisher,dc:language,upnp:originalTrackNumber,dc:date,upnp:producer,upnp:rating,upnp:actor,upnp:director,upnp:toc,dc:description,microsoft:userRatingInStars,microsoft:userEffectiveRatingInStars,microsoft:userRating,microsoft:userEffectiveRating,microsoft:serviceProvider,microsoft:artistAlbumArtist,microsoft:artistPerformer,microsoft:artistConductor,microsoft:authorComposer,microsoft:authorOriginalLyricist,microsoft:authorWriter,upnp:userAnnotation,upnp:channelName,upnp:longDescription,upnp:programTitle".split(",")) {
            ret.add(s);
        }
        return ret;
    }

    @Override
    public CSV<String> getSearchCapabilities() {
        return super.getSearchCapabilities();
    }

    @Override
    public CSV<String> getSortCapabilities() {
        return super.getSortCapabilities();
    }

    @Override
    public synchronized UnsignedIntegerFourBytes getSystemUpdateID() {
        return super.getSystemUpdateID();
    }

    @Override
    public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filterString, long firstResult, long maxResults, SortCriterion[] orderby) throws ContentDirectoryException {
        try {

            // This is just an example... you have to create the DIDL content
            // dynamically!
            System.out.println(objectID + " - browseFlag:" + browseFlag + " filter:" + filterString + " firstResult:" + firstResult + " maxResults:" + maxResults + " orderby:" + orderby);
            DIDLContent didl = new DIDLContent();
            ContentProvider contentProvider = getContentProvider(org.fourthline.cling.protocol.sync.ReceivingAction.getRequestMessage().getHeaders());
            UpnpHeaders headers = org.fourthline.cling.protocol.sync.ReceivingAction.getRequestMessage().getHeaders();
            IncomingActionRequestMessage rm = ReceivingAction.getRequestMessage();
            Filter filter = Filter.create(filterString);
            ContentNode node = contentProvider.getNode(objectID);
            if (node == null) {
                String didlStrng = new DIDLParser().generate(didl);

                System.out.println("-->" + didlStrng);
                return new BrowseResult(didlStrng, 0, 0);
            } else {
                if (browseFlag == BrowseFlag.METADATA) {

                    // ps3
                    if (node instanceof ContainerNode) {
                        didl.addContainer(((ContainerNode) node).getImpl());
                        String didlStrng = contentProvider.toDidlString(didl);
                        return new BrowseResult(didlStrng, 1, 1);
                    } else {
                        didl.addItem(((ItemNode) node).getImpl());
                        String didlStrng = contentProvider.toDidlString(didl);
                        return new BrowseResult(didlStrng, 1, 1);
                    }

                } else {
                    if (node instanceof ContainerNode) {
                        List<ContentNode> children = ((ContainerNode) node).getChildren();
                        for (ContentNode c : children) {
                            if (c instanceof ItemNode) {
                                didl.addItem(((ItemNode) c).getImpl());
                            } else {
                                didl.addContainer(((ContainerNode) c).getImpl());
                            }

                        }

                        //
                        String didlStrng = contentProvider.toDidlString(didl);

                        System.out.println(didlStrng);
                        return new BrowseResult(didlStrng, children.size(), children.size());
                    } else {
                        throw new WTFException();
                    }
                }
            }

        } catch (Throwable ex) {
            logger.log(ex);
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
        }
    }

    private ContentProvider getContentProvider(UpnpHeaders upnpHeaders) {
        logger.info("UPNP Headers: " + upnpHeaders);
        String host = upnpHeaders.getFirstHeader("Host");
        try {
            host = new URL("http://" + host).getHost();
            mediaServer.getProtocolInfo(InetAddress.getByName("192.168.2.53"));

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        // mediaServer
        return defaultProvider;
    }

    @Override
    public BrowseResult search(String containerId, String searchCriteriaString, String filterString, long firstResult, long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {
        try {
            logger.info(String.format("ContentDirectory receive search request with ContainerID:%s, SearchCriteria:%s, Filter:%s, FirstResult:%s, MaxResults:%s, SortCriterion:%s.", containerId, searchCriteriaString, filterString, firstResult, maxResults, orderBy));
            UpnpHeaders headers = org.fourthline.cling.protocol.sync.ReceivingAction.getRequestMessage().getHeaders();
            SearchCriteria searchCriterion = SearchCriteria.create(searchCriteriaString);

            Filter filter = Filter.create(filterString);
            ContentProvider contentProvider = getContentProvider(org.fourthline.cling.protocol.sync.ReceivingAction.getRequestMessage().getHeaders());

            // You can override this method to implement searching!
            System.out.println("SEARCH NOT SUPPORTED");

            DIDLContent didl = new DIDLContent();
            ContentNode node = contentProvider.getNode(containerId);
            if (node == null) {
                String didlStrng;

                didlStrng = new DIDLParser().generate(didl);

                System.out.println("-->" + didlStrng);
                return new BrowseResult(didlStrng, 0, 0);
            } else {
                ArrayList<ContentNode> files = getFiles(node);
                List<ContentNode> children = ((ContainerNode) node).getChildren();
                for (ContentNode c : files) {
                    switch (searchCriterion.getSearchType()) {
                    case SEARCH_IMAGE:
                        continue;
                    case SEARCH_PLAYLIST:
                        continue;
                    case SEARCH_UNKNOWN:
                    case SEARCH_VIDEO:

                    }
                    if (c instanceof ItemNode) {
                        didl.addItem(((ItemNode) c).getImpl());
                    } else {
                        didl.addContainer(((ContainerNode) c).getImpl());
                    }

                }
                String didlStrng = contentProvider.toDidlString(didl);
                System.out.println(didlStrng);
                return new BrowseResult(didlStrng, children.size(), children.size());

            }
        } catch (Throwable e) {
            logger.log(e);
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, e.toString());
        }
    }

    private ArrayList<ContentNode> getFiles(ContentNode node) {

        return getFiles(node, new ArrayList<ContentNode>());
    }

    private ArrayList<ContentNode> getFiles(ContentNode node, ArrayList<ContentNode> ret) {

        if (node instanceof ItemNode) {
            ret.add(node);
        } else {
            ret.add(node);
            for (ContentNode n : ((ContainerNode) node).getChildren()) {
                getFiles(n, ret);
            }
        }

        return ret;
    }

    public void onUpdate() {
        changeSystemUpdateID();
    }
}