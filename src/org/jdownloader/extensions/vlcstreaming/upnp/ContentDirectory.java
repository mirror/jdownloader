package org.jdownloader.extensions.vlcstreaming.upnp;

import java.util.List;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.logging.Log;
import org.teleal.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.teleal.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.teleal.cling.support.contentdirectory.ContentDirectoryException;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.BrowseResult;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.SortCriterion;

public class ContentDirectory extends AbstractContentDirectoryService {

    private static final String ID_ROOT         = "0";
    private static final String ID_DOWNLOADLIST = "1";
    private static final String ID_LINKGRABBER  = "2";

    private ContentProvider     contentProvider;

    public ContentDirectory() {

        contentProvider = ContentFactory.create();
    }

    @Override
    public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult, long maxResults, SortCriterion[] orderby) throws ContentDirectoryException {
        try {

            // This is just an example... you have to create the DIDL content dynamically!
            System.out.println(objectID + " - browseFlag:" + browseFlag + " filter:" + filter + " firstResult:" + firstResult + " maxResults:" + maxResults + " orderby:" + orderby);
            DIDLContent didl = new DIDLContent();
            System.out.println(1);
            ContentNode node = contentProvider.getNode(objectID);
            if (node == null) {
                String didlStrng = new DIDLParser().generate(didl);
                System.out.println("-->" + didlStrng);
                return new BrowseResult(didlStrng, 0, 0);
            } else {
                if (browseFlag == BrowseFlag.METADATA) {

                    // ps3
                    didl.addContainer(((ContainerNode) node).getImpl());
                    String didlStrng = postEditDidl(new DIDLParser().generate(didl));

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
                        String didlStrng = postEditDidl(new DIDLParser().generate(didl));
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

    private String postEditDidl(String didlStrng) {
        return didlStrng.replace("\"true\"", "\"1\"").replace("\"false\"", "\"0\"");

    }

    @Override
    public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {
        // You can override this method to implement searching!
        return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
    }
}