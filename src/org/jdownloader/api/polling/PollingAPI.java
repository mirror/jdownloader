package org.jdownloader.api.polling;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("polling")
public interface PollingAPI extends RemoteAPIInterface {
    List<PollingResultAPIStorable> poll(APIQuery queryParams);
}
