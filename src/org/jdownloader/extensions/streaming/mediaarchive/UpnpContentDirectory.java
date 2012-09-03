package org.jdownloader.extensions.streaming.mediaarchive;

import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.binding.annotations.UpnpAction;
import org.fourthline.cling.binding.annotations.UpnpOutputArgument;
import org.fourthline.cling.binding.annotations.UpnpStateVariable;
import org.fourthline.cling.binding.annotations.UpnpStateVariables;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.SortCriterion;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.upnp.Filter;
import org.jdownloader.extensions.streaming.upnp.SearchCriteria;
import org.jdownloader.extensions.streaming.upnp.remotedevices.RendererInfo;
import org.jdownloader.logging.LogController;

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
            // Special IDS:
            // xbmc: video/folder.jpg
            System.out.println("Browse: " + objectID + " " + browseFlag);
            UpnpHeaders headers = org.fourthline.cling.protocol.sync.ReceivingAction.getRequestMessage().getHeaders();

            RendererInfo callerDevice = extension.getMediaServer().getDeviceManager().findDeviceByUpnpHeaders(headers);
            if (browseFlag == BrowseFlag.METADATA) {

                return callerDevice.getHandler().browseMetaData(callerDevice, archive.getItemById(objectID), Filter.create(filterString), firstResult, maxResults, orderby);

            } else {

                return callerDevice.getHandler().browseContentDirectory(callerDevice, archive.getDirectory(objectID), Filter.create(filterString), firstResult, maxResults, orderby);

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

            RendererInfo callerDevice = extension.getMediaServer().getDeviceManager().findDeviceByUpnpHeaders(headers);

            SearchCriteria searchCriterion = SearchCriteria.create(searchCriteriaString);
            return callerDevice.getHandler().searchContentDirectory(callerDevice, archive, archive.getDirectory(containerId), searchCriterion, Filter.create(filterString), firstResult, maxResults, orderBy);

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
