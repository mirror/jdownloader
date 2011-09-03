package org.jdownloader.remotecall;

import java.io.IOException;
import java.net.URLEncoder;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remotecall.client.RemoteCallClient;
import org.appwork.remotecall.client.RemoteCallCommunicationException;
import org.appwork.remotecall.server.ServerInvokationException;

public class RemoteClient extends RemoteCallClient {

    private String  host;
    private Browser br;

    public RemoteClient(String host) {
        this.host = host;
        br = new Browser();
    }

    @Override
    protected String send(String serviceName, String routine, String serialise) throws ServerInvokationException {
        try {
            String url = "http://" + this.host + "/" + serviceName + "/" + URLEncoder.encode(routine, "UTF-8");
            String red = br.postPageRaw(url, serialise);
            URLConnectionAdapter con = br.getHttpConnection();
            if (con.getResponseCode() == HTTPConstants.ResponseCode.SUCCESS_OK.getCode()) {
                return red;
            } else if (con.getResponseCode() == HTTPConstants.ResponseCode.SERVERERROR_INTERNAL.getCode()) {
                // Exception
                throw new ServerInvokationException(red, this.host);
            } else {
                throw new RemoteCallCommunicationException("Wrong ResponseCode " + con.getResponseCode());
            }
        } catch (final ServerInvokationException e) {
            throw e;
        } catch (final IOException e) {

            throw new RemoteCallCommunicationException(e);
        } catch (final Exception e) {
            if (e instanceof RuntimeException) { throw (RuntimeException) e; }
            throw new RuntimeException(e);
        }

    }

}
