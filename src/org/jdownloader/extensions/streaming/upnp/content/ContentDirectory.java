package org.jdownloader.extensions.streaming.upnp.content;

import java.util.List;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.logging.Log;
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

public class ContentDirectory extends AbstractContentDirectoryService {

    private ContentProvider defaultProvider;

    public ContentDirectory() {

        defaultProvider = ContentFactory.create();

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
                    didl.addContainer(((ContainerNode) node).getImpl());
                    String didlStrng = contentProvider.toDidlString(didl);

                    return new BrowseResult(didlStrng, 1, 1);

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
        return defaultProvider;
    }

    @Override
    public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {
        // You can override this method to implement searching!
        return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
    }
}