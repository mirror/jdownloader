package jd.plugins.optional.remoteserv.remotecall.client;

import java.net.URL;

import jd.plugins.optional.remoteserv.remotecall.HttpClient;

import org.appwork.storage.JSonStorage;

public class RemoteCallClient {

    private final RemoteCallClientFactory factory;
    private final String                  host;
    private final HttpClientImpl          httpClient;

    public RemoteCallClient(final String host) {

        this.host = host;
        this.factory = new RemoteCallClientFactory(this);
        this.httpClient = new HttpClientImpl();
    }

    public Object call(final String serviceName, final String name, final Object[] args) throws Throwable {

        return this.getHTTPClient().post(new URL("http://" + this.host + "/" + serviceName + "/" + name), this.serialise(args));
    }

    public RemoteCallClientFactory getFactory() {
        return this.factory;
    }

    public HttpClient getHTTPClient() {
        return this.httpClient;
    }

    private Object serialise(final Object o) throws SerialiseException {
        try {
            return JSonStorage.toString(o);

        } catch (final Exception e) {
            throw new SerialiseException(e);

        }
    }

    private String serialise(final Object[] args) throws SerialiseException {
        final StringBuilder sb = new StringBuilder();
        for (final Object o : args) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(this.serialise(o));
        }
        return sb.toString();
    }

}
