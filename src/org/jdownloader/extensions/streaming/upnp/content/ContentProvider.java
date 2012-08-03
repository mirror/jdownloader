package org.jdownloader.extensions.streaming.upnp.content;

import org.fourthline.cling.support.model.DIDLContent;

public interface ContentProvider {

    ContentNode getNode(String objectID);

    String toDidlString(DIDLContent didl) throws Exception;

}
