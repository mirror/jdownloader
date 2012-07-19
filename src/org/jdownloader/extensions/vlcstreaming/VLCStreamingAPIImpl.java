package org.jdownloader.extensions.vlcstreaming;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jd.controlling.AccountController;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.remoteapi.RemoteAPI404Exception;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.Application;
import org.appwork.utils.ReusableByteArrayOutputStreamPool;
import org.appwork.utils.ReusableByteArrayOutputStreamPool.ReusableByteArrayOutputStream;
import org.appwork.utils.io.streamingio.Streaming;
import org.appwork.utils.io.streamingio.StreamingInputStream;
import org.appwork.utils.io.streamingio.StreamingOutputStream;
import org.appwork.utils.io.streamingio.StreamingOverlapWrite;
import org.appwork.utils.net.httpserver.requests.HttpRequest;

public class VLCStreamingAPIImpl implements VLCStreamingAPI {

    private static HashMap<DownloadLink, Streaming> streaming = new HashMap<DownloadLink, Streaming>();

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
            final DownloadLink finalRemoteLink = remoteLink;
            response.setResponseAsync(true);
            synchronized (streaming) {
                Streaming stream = streaming.get(remoteLink);
                if (stream == null) {
                    File tmp = Application.getResource("/tmp/streaming/" + remoteLink.getUniqueID());
                    tmp.getParentFile().mkdirs();
                    stream = new Streaming(tmp.getAbsolutePath()) {
                        private long       fileSize = -1;
                        final DownloadLink source   = finalRemoteLink;

                        @Override
                        public boolean connectStreamingOutputStream(final StreamingOutputStream streamingOutputStream, long startPosition, long endPosition) throws IOException {
                            /* this method should be called within our VLCStreamingThread */
                            ClassLoader oldClassLoader = null;
                            try {
                                LinkedList<Account> accounts = AccountController.getInstance().getValidAccounts(source.getHost());
                                oldClassLoader = Thread.currentThread().getContextClassLoader();
                                final DownloadLink mirror = new DownloadLink(source.getDefaultPlugin(), source.getName(), source.getHost(), source.getDownloadURL(), true);
                                source.copyTo(mirror);
                                final PluginForHost plugin = source.getDefaultPlugin().getLazyP().newInstance();
                                plugin.setBrowser(new Browser());
                                mirror.setLivePlugin(plugin);
                                Thread.currentThread().setContextClassLoader(plugin.getLazyP().getClassLoader());
                                plugin.init();
                                /* forward requested range for DownloadInterface */
                                if (endPosition > 0) {
                                    mirror.setProperty("streamingRange", "bytes=" + startPosition + "-" + endPosition);
                                } else {
                                    mirror.setProperty("streamingRange", "bytes=" + startPosition + "-");
                                }
                                if (accounts.size() > 0) {
                                    plugin.handlePremium(mirror, accounts.get(0));
                                } else {
                                    plugin.handleFree(mirror);
                                }
                                final URLConnectionAdapter con = mirror.getDownloadInstance().getConnection();
                                if (con.getResponseCode() == 200 || con.getResponseCode() == 206) {
                                    if (fileSize == -1) fileSize = con.getCompleteContentLength();
                                    new Thread() {
                                        public void run() {
                                            ReusableByteArrayOutputStream buffer = null;
                                            try {
                                                buffer = ReusableByteArrayOutputStreamPool.getReusableByteArrayOutputStream(10240, false);
                                                InputStream is = con.getInputStream();
                                                while (true) {
                                                    int ret = is.read(buffer.getInternalBuffer());
                                                    if (ret == -1) break;
                                                    if (ret == 0) continue;
                                                    streamingOutputStream.write(buffer.getInternalBuffer(), 0, ret);
                                                }
                                            } catch (StreamingOverlapWrite e) {
                                                System.out.println("Chunk overlapping");
                                            } catch (final Throwable e) {
                                                e.printStackTrace();
                                            } finally {
                                                try {
                                                    con.disconnect();
                                                } catch (final Throwable e) {
                                                }
                                                try {
                                                    plugin.clean();
                                                } catch (final Throwable e) {
                                                    e.printStackTrace();
                                                }
                                                ReusableByteArrayOutputStreamPool.reuseReusableByteArrayOutputStream(buffer);
                                                streamingOutputStream.close();
                                            }
                                        }
                                    }.start();
                                    return true;
                                } else {
                                    plugin.clean();
                                    return false;
                                }
                            } catch (final Throwable e) {
                                e.printStackTrace();
                                throw new IOException(e);
                            } finally {
                                Thread.currentThread().setContextClassLoader(oldClassLoader);
                            }
                        }

                        @Override
                        protected synchronized void closeInputStream(StreamingInputStream streamingInputStream) {
                            StreamingOutputStream outputStream = findLastStreamingOutputStreamFor(streamingInputStream);
                            super.closeInputStream(streamingInputStream);
                            if (outputStream != null) {
                                outputStream.close();
                            }
                        }

                        @Override
                        protected synchronized void closeOutputStream(StreamingOutputStream streamingOutputStream) {
                            System.out.println("outputStream closed");
                            super.closeOutputStream(streamingOutputStream);
                        }

                        @Override
                        public long getFinalFileSize() {
                            long ret = source.getVerifiedFileSize();
                            if (ret != -1) return ret;
                            return fileSize;
                        }

                    };
                    streaming.put(remoteLink, stream);
                }
                new VLCStreamingThread(response, request, stream).start();
            }
        } catch (final Throwable e) {
            if (e instanceof RemoteAPIException) throw (RemoteAPIException) e;
            throw new RemoteAPIException(e);
        }
    }
}
