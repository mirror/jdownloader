package org.jdownloader.api.polling;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("polling")
public interface PollingAPI extends RemoteAPIInterface {
    List<PollingResultAPIStorable> poll(APIQuery queryParams);
}
