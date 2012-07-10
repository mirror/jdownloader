package org.jdownloader.extensions.vlcstreaming;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.http.Browser;
import jd.http.BrowserSettings;
import jd.http.Request;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterfaceFactory;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPI404Exception;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.Input2OutputStreamForwarder;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.jdownloader.logging.LogController;

public class VLCStreamingAPIImpl implements VLCStreamingAPI {

    private class StreamingThread extends Thread implements BrowserSettings, DownloadInterfaceFactory {

        private DownloadLink      link;
        private RemoteAPIResponse response;
        private RemoteAPIRequest  request;
        private HTTPProxy         proxy;
        private LogSource         logger;

        public StreamingThread(DownloadLink link, RemoteAPIResponse response, RemoteAPIRequest request) {
            this.link = link;
            this.response = response;
            this.request = request;
            proxy = Browser._getGlobalProxy();
            logger = LogController.getInstance().getLogger("VLCStreaming");
            logger.setAllowTimeoutFlush(false);
        }

        @Override
        public void run() {
            try {
                PluginForHost plugin = link.getDefaultPlugin().getLazyP().newInstance();
                plugin.setBrowser(new Browser());
                link.setLivePlugin(plugin);
                this.setContextClassLoader(plugin.getLazyP().getClassLoader());
                plugin.init();
                plugin.handle(link, null);
            } catch (final Throwable e) {
                logger.log(e);
                logger.flush();
            } finally {
                logger.close();
                response.closeConnection();
            }
        }

        @Override
        public HTTPProxy getCurrentProxy() {
            return proxy;
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public boolean isDebug() {
            return true;
        }

        @Override
        public boolean isVerbose() {
            return true;
        }

        @Override
        public void setCurrentProxy(HTTPProxy proxy) {
        }

        @Override
        public void setDebug(boolean b) {
        }

        @Override
        public void setLogger(Logger logger) {
        }

        @Override
        public void setVerbose(boolean b) {
        }

        @Override
        public DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request) throws Exception {
            return new VLCStreamingDownloadInterface(this, downloadLink.getLivePlugin(), downloadLink, request);
        }

        @Override
        public DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request, boolean resume, int chunks) throws Exception {
            return new VLCStreamingDownloadInterface(this, downloadLink.getLivePlugin(), downloadLink, request);
        }

    }

    private class VLCStreamingDownloadInterface extends DownloadInterface {

        private StreamingThread vlcStreamingThread;

        public VLCStreamingDownloadInterface(StreamingThread vlcStreamingThread, PluginForHost plugin, DownloadLink downloadLink, Request request) throws Exception {
            super(plugin, downloadLink, request);
            this.vlcStreamingThread = vlcStreamingThread;
        }

        @Override
        protected boolean checkResumabled() {
            return true;
        }

        @Override
        protected boolean connectResumable() throws IOException {
            HTTPHeader rangeRequest = vlcStreamingThread.request.getRequestHeaders().get("Range");
            if (rangeRequest != null) {
                request.getHeaders().put("Range", rangeRequest.getValue());
            } else {
                request.getHeaders().remove("Range");
            }
            browser.connect(request);
            return true;
        }

        @Override
        public boolean isRangeRequestSupported() {
            return true;
        }

        @Override
        public boolean isResumable() {
            return true;
        }

        @Override
        public boolean startDownload() throws Exception {
            try {
                if (connection.getResponseCode() == 200) {
                    vlcStreamingThread.response.setResponseCode(ResponseCode.SUCCESS_OK);
                } else if (connection.getResponseCode() == 206) {
                    vlcStreamingThread.response.setResponseCode(ResponseCode.SUCCESS_PARTIAL_CONTENT);
                } else {
                    throw new Exception("Unhandled ResponseCode " + connection.getResponseCode());
                }
                Map<String, List<String>> responseHeaders = connection.getHeaderFields();
                Iterator<Entry<String, List<String>>> it = responseHeaders.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, List<String>> next = it.next();
                    vlcStreamingThread.response.getResponseHeaders().add(new HTTPHeader(next.getKey(), next.getValue().get(0)));
                }
                Input2OutputStreamForwarder forwarder = new Input2OutputStreamForwarder(connection.getInputStream(), vlcStreamingThread.response.getOutputStream());
                forwarder.forward();
            } finally {
                try {
                    connection.disconnect();
                } catch (final Throwable e) {
                }
            }
            return true;
        }

        @Override
        public synchronized void stopDownload() {
            super.stopDownload();
        }

        @Override
        protected void onChunksReady() {
        }

        @Override
        protected void setupChunks() throws Exception {
        }

        @Override
        public void cleanupDownladInterface() {
        }

        @Override
        protected boolean writeChunkBytes(Chunk chunk) {
            return false;
        }

    }

    @Override
    public void play(RemoteAPIResponse response, RemoteAPIRequest request) {
        DownloadLink remoteLink = null;
        try {
            String id = HttpRequest.getParameterbyKey(request, "id");
            if (id != null) {
                final long longID = Long.parseLong(id);
                List<DownloadLink> ret = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

                    @Override
                    public int returnMaxResults() {
                        return 1;
                    }

                    @Override
                    public boolean isChildrenNodeFiltered(DownloadLink node) {
                        return node.getUniqueID().getID() == longID;
                    }
                });
                if (ret != null && ret.size() == 1) remoteLink = ret.get(0);
            }
            if (remoteLink == null) throw new RemoteAPI404Exception(id);
            DownloadLink mirror = new DownloadLink(remoteLink.getDefaultPlugin(), remoteLink.getName(), remoteLink.getHost(), remoteLink.getDownloadURL(), true);
            remoteLink.copyTo(mirror);
            response.setResponseAsync(true);
            new StreamingThread(mirror, response, request).start();
        } catch (final Throwable e) {
            if (e instanceof RemoteAPIException) throw (RemoteAPIException) e;
            throw new RemoteAPIException(e);
        }
    }
}
