package org.jdownloader.extensions.streaming;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterfaceFactory;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.Application;
import org.appwork.utils.io.streamingio.Streaming;
import org.appwork.utils.io.streamingio.StreamingInputStream;
import org.appwork.utils.io.streamingio.StreamingOutputStream;
import org.appwork.utils.io.streamingio.StreamingOverlapWrite;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public class StreamingProvider {

    private StreamingExtension extension;

    public StreamingProvider(StreamingExtension extension) {
        this.extension = extension;
    }

    protected static HashMap<DownloadLink, Streaming> streaming = new HashMap<DownloadLink, Streaming>();

    public Streaming getStreamingProvider(final DownloadLink remoteLink) throws IOException {
        synchronized (streaming) {
            Streaming stream = streaming.get(remoteLink);
            if (stream == null || stream.isClosed()) {
                File tmp = Application.getResource("/tmp/streaming/" + remoteLink.getUniqueID());
                FileCreationManager.getInstance().mkdir(tmp.getParentFile());
                stream = new Streaming(tmp.getAbsolutePath()) {
                    private long fileSize = -1;

                    @Override
                    public boolean connectStreamingOutputStream(final StreamingOutputStream streamingOutputStream, final long startPosition, long endPosition) throws IOException {
                        /* this method should be called within our VLCStreamingThread */
                        ClassLoader oldClassLoader = null;
                        try {
                            List<Account> accounts = AccountController.getInstance().getValidAccounts(remoteLink.getHost());
                            oldClassLoader = Thread.currentThread().getContextClassLoader();
                            final DownloadLink mirror = new DownloadLink(remoteLink.getDefaultPlugin(), remoteLink.getName(), remoteLink.getHost(), remoteLink.getDownloadURL(), true);
                            mirror.setProperties(remoteLink.getProperties());
                            PluginClassLoaderChild cl;
                            Thread.currentThread().setContextClassLoader(cl = PluginClassLoader.getInstance().getChild());
                            final PluginForHost plugin = remoteLink.getDefaultPlugin().getLazyP().newInstance(cl);
                            plugin.setBrowser(new Browser());
                            plugin.setCustomizedDownloadFactory(new DownloadInterfaceFactory() {

                                @Override
                                public DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request) throws Exception {
                                    return new StreamingDownloadInterface(downloadLink.getLivePlugin(), downloadLink, request);
                                }

                                @Override
                                public DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request, boolean resume, int chunks) throws Exception {
                                    return new StreamingDownloadInterface(downloadLink.getLivePlugin(), downloadLink, request);
                                }
                            });
                            mirror.setLivePlugin(plugin);
                            plugin.init();
                            /* forward requested range for DownloadInterface */
                            if (endPosition > 0) {
                                mirror.setProperty("streamingRange", "bytes=" + startPosition + "-" + endPosition);
                            } else {
                                mirror.setProperty("streamingRange", "bytes=" + startPosition + "-");
                            }
                            if (accounts != null && accounts.size() > 0) {
                                plugin.handlePremium(mirror, accounts.get(0));
                            } else {
                                plugin.handleFree(mirror);
                            }
                            /* TODO: needs to be rewritten */
                            final URLConnectionAdapter con = null;
                            // getDownloadInstance().getConnection();
                            if (con.getResponseCode() == 200 || con.getResponseCode() == 206) {
                                if (remoteLink.getVerifiedFileSize() < 0) {
                                    /* we don't have a verified filesize yet, let's check if we have it now! */
                                    if (con.getRange() != null) {
                                        if (con.getRange()[2] > 0) {
                                            remoteLink.setVerifiedFileSize(con.getRange()[2]);
                                        }
                                    } else if (con.getRequestProperty("Range") == null && con.getLongContentLength() > 0 && con.isOK()) {
                                        remoteLink.setVerifiedFileSize(con.getLongContentLength());
                                    }
                                }

                                if (fileSize == -1) fileSize = con.getCompleteContentLength();
                                new Thread("HTTP Reader Stream") {
                                    public void run() {
                                        long read = 0;
                                        try {
                                            byte[] buffer = new byte[10240];
                                            InputStream is = con.getInputStream();
                                            while (true) {
                                                int ret = is.read(buffer);
                                                if (ret == -1) break;
                                                if (ret == 0) continue;
                                                read += ret;
                                                streamingOutputStream.write(buffer, 0, ret);
                                            }
                                            System.out.println("Chunk Done -> Chunk Started at: " + startPosition + " read: " + read);
                                        } catch (StreamingOverlapWrite e) {
                                            System.out.println("Chunk overlapping");
                                        } catch (final Throwable e) {
                                            e.printStackTrace();
                                            System.out.println("Chunk Error -> Chunk Started at: " + startPosition + " read: " + read);
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

                            throw new IOException(e);
                        } finally {
                            Thread.currentThread().setContextClassLoader(oldClassLoader);
                        }
                    }

                    @Override
                    protected synchronized void closeInputStream(StreamingInputStream streamingInputStream) {
                        final StreamingOutputStream outputStream = findLastStreamingOutputStreamFor(streamingInputStream);

                        super.closeInputStream(streamingInputStream);
                        if (outputStream != null) {
                            DelayedRunnable delayedOutputStreamCloser = new DelayedRunnable(10000) {
                                @Override
                                public String getID() {
                                    return "CloseInputStream";
                                }

                                @Override
                                public void delayedrun() {
                                    java.util.List<StreamingInputStream> all = findAllStreamingInputStreamsFor(outputStream);
                                    if (all.size() == 0) {
                                        System.out.println("Last input closed, close output");
                                        outputStream.close();
                                    }
                                }
                            };
                            delayedOutputStreamCloser.resetAndStart();
                        }

                    }

                    @Override
                    protected synchronized void closeOutputStream(StreamingOutputStream streamingOutputStream) {
                        System.out.println("outputStream closed");
                        super.closeOutputStream(streamingOutputStream);
                    }

                    @Override
                    public long getFinalFileSize() {
                        long ret = remoteLink.getVerifiedFileSize();
                        if (ret != -1) return ret;
                        return fileSize;
                    }

                };
                streaming.put(remoteLink, stream);
            }
            return stream;
        }
    }
}
