package org.jdownloader.remotecall;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remotecall.RemoteCallInterface;
import org.appwork.remotecall.Utils;
import org.appwork.remotecall.client.RemoteCallClient;
import org.appwork.remotecall.client.RemoteCallCommunicationException;
import org.appwork.remotecall.client.SerialiseException;
import org.appwork.remotecall.server.ParsingException;
import org.appwork.remotecall.server.Requestor;
import org.appwork.remotecall.server.ServerInvokationException;
import org.appwork.utils.logging.Log;
import org.appwork.utils.reflection.Clazz;

public class RemoteClient extends RemoteCallClient {

    private String    host;
    protected Browser br;

    public RemoteClient(String host) {
        this.host = host;
        br = new Browser();
        br.setConnectTimeout(125000);
        br.setReadTimeout(125000);
        br.setAllowedResponseCodes(new int[] { 500 });
        br.setLoadLimit(10 * 1024 * 1024);
    }

    public String getHost() {
        return host;
    }

    public Object call(String serviceName, Method method, Object[] args) throws ServerInvokationException {
        try {
            if (method.getAnnotation(MultiForm.class) != null) {
                return sendMultiForm(serviceName, method, args);
            } else {
                return send(serviceName, method, Utils.serialise(args));
            }
        } catch (final SerialiseException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    private Object sendMultiForm(String serviceName, Method routine, Object... args) throws ServerInvokationException {
        try {
            String url = "http://" + this.host + "/" + serviceName + "/" + URLEncoder.encode(routine.getName(), "UTF-8");
            System.out.println(url);
            PostFormDataRequest req = (PostFormDataRequest) br.createPostFormDataRequest(url);
            for (int i = 0; i < args.length; i++) {

                Object o = args[i];
                if (o != null && Clazz.isByteArray(o.getClass())) {
                    req.addFormData(new FormData(i + "", "P" + i, (byte[]) o));
                } else {
                    req.addFormData(new FormData(i + "", Utils.serialiseSingleObject(o)));
                }

            }

            URLConnectionAdapter con = br.openRequestConnection(req);
            String red = br.loadConnection(con).getHtmlCode();
            if (con.getResponseCode() == HTTPConstants.ResponseCode.SUCCESS_OK.getCode()) {
                return red;
            } else if (con.getResponseCode() == HTTPConstants.ResponseCode.SERVERERROR_INTERNAL.getCode()) {
                // Exception
                throw new ServerInvokationException(red, new Requestor(serviceName, routine.getName(), "MULTIFORM"));
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

    @Override
    protected Object send(String serviceName, Method routine, String serialise) throws ServerInvokationException {
        try {
            String url = "http://" + this.host + "/" + serviceName + "/" + URLEncoder.encode(routine.getName(), "UTF-8");
            Log.L.finer(url + "?" + serialise);

            String red = br.postPageRaw(url, serialise);

            URLConnectionAdapter con = br.getHttpConnection();
            if (con.getResponseCode() == HTTPConstants.ResponseCode.SUCCESS_OK.getCode()) {
                return red;
            } else if (con.getResponseCode() == HTTPConstants.ResponseCode.SERVERERROR_INTERNAL.getCode()) {
                // Exception
                throw new ServerInvokationException(red, new Requestor(serviceName, routine.getName(), serialise));
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

    public <T extends RemoteCallInterface> T create(Class<T> class1) {
        try {
            return getFactory().newInstance(class1);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
    }
}
