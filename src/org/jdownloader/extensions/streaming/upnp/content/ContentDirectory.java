package org.jdownloader.extensions.streaming.upnp.content;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.model.message.UpnpHeaders;
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

        defaultProvider = ContentFactory.create();
        this.mediaServer = mediaServer;
        logger = LogController.getInstance().getLogger("streaming");
    }

    @Override
    public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult, long maxResults, SortCriterion[] orderby) throws ContentDirectoryException {
        try {

            // This is just an example... you have to create the DIDL content dynamically!
            System.out.println(objectID + " - browseFlag:" + browseFlag + " filter:" + filter + " firstResult:" + firstResult + " maxResults:" + maxResults + " orderby:" + orderby);
            DIDLContent didl = new DIDLContent();
            ContentProvider contentProvider = getContentProvider(org.fourthline.cling.protocol.sync.ReceivingAction.getRequestMessage().getHeaders());

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
                        String didlStrng = contentProvider.toDidlString(didl);
                        System.out.println(didlStrng);
                        return new BrowseResult(didlStrng, children.size(), children.size());
                    } else {
                        throw new WTFException();
                    }
                }
            }

        } catch (Exception ex) {
            Log.exception(ex);
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
    public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {
        // You can override this method to implement searching!
        System.out.println("SEARCH NOT SUPPORTED");
        return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
    }
}