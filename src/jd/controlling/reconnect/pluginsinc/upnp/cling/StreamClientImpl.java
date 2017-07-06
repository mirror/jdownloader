package jd.controlling.reconnect.pluginsinc.upnp.cling;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.appwork.utils.net.httpconnection.HTTPConnection;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.appwork.utils.net.httpconnection.HTTPConnectionImpl;
import org.fourthline.cling.model.message.StreamRequestMessage;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.UpnpMessage;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.UpnpHeader;
import org.fourthline.cling.transport.impl.StreamClientConfigurationImpl;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.StreamClient;
import org.seamless.util.URIUtil;
import org.seamless.util.io.IO;

/**
 * customized StreamClient implementation for HTTPConnection from Appwork GmbH
 *
 * https://github.com/4thline/cling/blob/master/core/src/main/java/org/fourthline/cling/transport/impl/StreamClientImpl.java
 *
 * @author daniel
 *
 */
public class StreamClientImpl implements StreamClient<StreamClientConfigurationImpl> {
    final protected StreamClientConfigurationImpl configuration;

    public StreamClientImpl(StreamClientConfigurationImpl configuration) throws InitializationException {
        this.configuration = configuration;
    }

    @Override
    public StreamClientConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public StreamResponseMessage sendRequest(final StreamRequestMessage requestMessage) throws InterruptedException {
        final UpnpRequest requestOperation = requestMessage.getOperation();
        final URL url = URIUtil.toURL(requestOperation.getURI());
        HTTPConnection urlConnection = null;
        InputStream inputStream;
        try {
            final byte[] bodyBytes;
            if (requestMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
                if (requestMessage.getBodyString() != null) {
                    bodyBytes = requestMessage.getBodyString().getBytes("UTF-8");
                } else {
                    bodyBytes = null;
                }
            } else if (requestMessage.getBodyType().equals(UpnpMessage.BodyType.BYTES)) {
                bodyBytes = requestMessage.getBodyBytes();
            } else {
                bodyBytes = null;
            }
            urlConnection = new HTTPConnectionImpl(url) {
                @Override
                protected boolean isRequiresOutputStream() {
                    return super.isRequiresOutputStream() || bodyBytes != null;
                }
            };
            urlConnection.setRequestMethod(RequestMethod.valueOf(requestOperation.getHttpMethodName()));
            urlConnection.setReadTimeout(configuration.getTimeoutSeconds() * 1000);
            urlConnection.setConnectTimeout(configuration.getTimeoutSeconds() * 1000);
            if (!requestMessage.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
                urlConnection.setRequestProperty(UpnpHeader.Type.USER_AGENT.getHttpName(), getConfiguration().getUserAgentValue(requestMessage.getUdaMajorVersion(), requestMessage.getUdaMinorVersion()));
            }
            for (final Map.Entry<String, List<String>> entry : requestMessage.getHeaders().entrySet()) {
                for (final String v : entry.getValue()) {
                    final String headerName = entry.getKey();
                    urlConnection.setRequestProperty(headerName, v);
                }
            }
            if (bodyBytes != null) {
                urlConnection.setRequestProperty("Content-Length", Integer.toString(bodyBytes.length));
                urlConnection.connect();
                urlConnection.getOutputStream().write(bodyBytes);
                urlConnection.finalizeConnect();
            }
            urlConnection.setAllowedResponseCodes(new int[] { urlConnection.getResponseCode() });
            inputStream = urlConnection.getInputStream();
            return createResponse(urlConnection, inputStream);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    protected StreamResponseMessage createResponse(HTTPConnection urlConnection, InputStream inputStream) throws Exception {
        if (urlConnection.getResponseCode() == -1) {
            return null;
        } else {
            final UpnpResponse responseOperation = new UpnpResponse(urlConnection.getResponseCode(), urlConnection.getResponseMessage());
            final StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);
            responseMessage.setHeaders(new UpnpHeaders(urlConnection.getHeaderFields()));
            byte[] bodyBytes = null;
            if (inputStream != null) {
                bodyBytes = IO.readBytes(inputStream);
            }
            if (bodyBytes != null && bodyBytes.length > 0 && responseMessage.isContentTypeMissingOrText()) {
                responseMessage.setBodyCharacters(bodyBytes);
            } else if (bodyBytes != null && bodyBytes.length > 0) {
                responseMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);
            }
            return responseMessage;
        }
    }

    @Override
    public void stop() {
    }
}
