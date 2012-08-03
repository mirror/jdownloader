package org.jdownloader.extensions.vlcstreaming.upnp.content;

import org.fourthline.cling.support.model.DIDLContent;

public interface ContentProvider {

    ContentNode getNode(String objectID);

    String toDidlString(DIDLContent didl) throws Exception;

}
