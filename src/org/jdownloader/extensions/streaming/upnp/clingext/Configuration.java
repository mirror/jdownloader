package org.jdownloader.extensions.streaming.upnp.clingext;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.binding.xml.DeviceDescriptorBinder;
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder;
import org.fourthline.cling.binding.xml.UDA10DeviceDescriptorBinderImpl;
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl;
import org.fourthline.cling.model.Namespace;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.DatagramIOConfigurationImpl;
import org.fourthline.cling.transport.impl.DatagramIOImpl;
import org.fourthline.cling.transport.impl.DatagramProcessorImpl;
import org.fourthline.cling.transport.impl.GENAEventProcessorImpl;
import org.fourthline.cling.transport.impl.MulticastReceiverConfigurationImpl;
import org.fourthline.cling.transport.impl.MulticastReceiverImpl;
import org.fourthline.cling.transport.impl.SOAPActionProcessorImpl;
import org.fourthline.cling.transport.impl.StreamClientConfigurationImpl;
import org.fourthline.cling.transport.impl.StreamClientImpl;
import org.fourthline.cling.transport.impl.StreamServerConfigurationImpl;
import org.fourthline.cling.transport.impl.StreamServerImpl;
import org.fourthline.cling.transport.spi.DatagramIO;
import org.fourthline.cling.transport.spi.DatagramProcessor;
import org.fourthline.cling.transport.spi.GENAEventProcessor;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.MulticastReceiver;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.fourthline.cling.transport.spi.SOAPActionProcessor;
import org.fourthline.cling.transport.spi.StreamClient;
import org.fourthline.cling.transport.spi.StreamServer;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.logging.LogController;
import org.seamless.util.Exceptions;

import com.sun.net.httpserver.HttpServer;

public class Configuration implements UpnpServiceConfiguration {

    private static Logger                 log    = Logger.getLogger(DefaultUpnpServiceConfiguration.class.getName());

    final private Executor                defaultExecutor;

    final private DatagramProcessor       datagramProcessor;
    final private SOAPActionProcessor     soapActionProcessor;
    final private GENAEventProcessor      genaEventProcessor;

    final private DeviceDescriptorBinder  deviceDescriptorBinderUDA10;
    final private ServiceDescriptorBinder serviceDescriptorBinderUDA10;

    final private Namespace               namespace;

    private StreamingExtension            extension;

    private static LogSource              LOGGER = LogController.getInstance().getLogger(Configuration.class.getName());

    @Override
    public StreamServer<StreamServerConfigurationImpl> createStreamServer(NetworkAddressFactory networkAddressFactory) {
        return new StreamServerImpl(new StreamServerConfigurationImpl(networkAddressFactory.getStreamListenPort())) {
            synchronized public void init(InetAddress bindAddress, Router router) throws InitializationException {
                try {
                    InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, configuration.getListenPort());
                    // configuration.
                    LOGGER.info("HTTPServer: " + bindAddress + ":" + configuration.getListenPort());
                    server = HttpServer.create(socketAddress, configuration.getTcpConnectionBacklog());
                    server.createContext("/", new ServerRequestHttpHandler(router, LOGGER));

                    LOGGER.info("Created server (for receiving TCP streams) on: " + server.getAddress());
                } catch (BindException ex) {
                    LOGGER.log(ex);
                } catch (Exception ex) {
                    throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
                }
            }
        };
    }

    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort) {
        return new FixedNetworkAddressFactoryImpl(streamListenPort);
    }

    public Configuration(StreamingExtension extension) {
        this.extension = extension;

        defaultExecutor = createDefaultExecutor();

        datagramProcessor = createDatagramProcessor();
        soapActionProcessor = createSOAPActionProcessor();
        genaEventProcessor = createGENAEventProcessor();

        deviceDescriptorBinderUDA10 = createDeviceDescriptorBinderUDA10();
        serviceDescriptorBinderUDA10 = createServiceDescriptorBinderUDA10();

        namespace = createNamespace();
    }

    public DatagramProcessor getDatagramProcessor() {
        return datagramProcessor;
    }

    public SOAPActionProcessor getSoapActionProcessor() {
        return soapActionProcessor;
    }

    public GENAEventProcessor getGenaEventProcessor() {
        return genaEventProcessor;
    }

    public StreamClient createStreamClient() {
        return new StreamClientImpl(new StreamClientConfigurationImpl());
    }

    public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
        return new MulticastReceiverImpl(new MulticastReceiverConfigurationImpl(networkAddressFactory.getMulticastGroup(), networkAddressFactory.getMulticastPort()));
    }

    public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
        return new DatagramIOImpl(new DatagramIOConfigurationImpl());
    }

    public Executor getMulticastReceiverExecutor() {
        return getDefaultExecutor();
    }

    public Executor getDatagramIOExecutor() {
        return getDefaultExecutor();
    }

    public Executor getStreamServerExecutor() {
        return getDefaultExecutor();
    }

    public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
        return deviceDescriptorBinderUDA10;
    }

    public ServiceDescriptorBinder getServiceDescriptorBinderUDA10() {
        return serviceDescriptorBinderUDA10;
    }

    public ServiceType[] getExclusiveServiceTypes() {
        return new ServiceType[0];
    }

    public int getRegistryMaintenanceIntervalMillis() {
        return 1000;
    }

    public Integer getRemoteDeviceMaxAgeSeconds() {
        return null;
    }

    public Executor getAsyncProtocolExecutor() {
        return getDefaultExecutor();
    }

    public Executor getSyncProtocolExecutor() {
        return getDefaultExecutor();
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public Executor getRegistryMaintainerExecutor() {
        return getDefaultExecutor();
    }

    public Executor getRegistryListenerExecutor() {
        return getDefaultExecutor();
    }

    public NetworkAddressFactory createNetworkAddressFactory() {
        return createNetworkAddressFactory(extension.getSettings().getUpnpHttpServerPort());
    }

    public void shutdown() {
        if (getDefaultExecutor() instanceof ThreadPoolExecutor) {
            log.fine("Shutting down thread pool");
            ((ThreadPoolExecutor) getDefaultExecutor()).shutdown();
        }
    }

    protected DatagramProcessor createDatagramProcessor() {
        return new DatagramProcessorImpl();
    }

    protected SOAPActionProcessor createSOAPActionProcessor() {
        return new SOAPActionProcessorImpl();
    }

    protected GENAEventProcessor createGENAEventProcessor() {
        return new GENAEventProcessorImpl();
    }

    protected DeviceDescriptorBinder createDeviceDescriptorBinderUDA10() {
        return new UDA10DeviceDescriptorBinderImpl();
    }

    protected ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
        return new UDA10ServiceDescriptorBinderImpl();
    }

    protected Namespace createNamespace() {
        return new Namespace();
    }

    protected Executor getDefaultExecutor() {
        return defaultExecutor;
    }

    protected Executor createDefaultExecutor() {
        return new ClingExecutor();
    }

    public static class ClingExecutor extends ThreadPoolExecutor {

        public ClingExecutor() {
            this(new ClingThreadFactory(), new ThreadPoolExecutor.DiscardPolicy() {
                // The pool is unbounded but rejections will happen during shutdown
                @Override
                public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
                    // Log and discard
                    log.info("Thread pool rejected execution of " + runnable.getClass());
                    super.rejectedExecution(runnable, threadPoolExecutor);
                }
            });
        }

        public ClingExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
            // This is the same as Executors.newCachedThreadPool
            super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), threadFactory, rejectedHandler);
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {
            super.afterExecute(runnable, throwable);
            if (throwable != null) {
                // Log only
                log.warning("Thread terminated " + runnable + " abruptly with exception: " + throwable);
                log.warning("Root cause: " + Exceptions.unwrap(throwable));
            }
        }
    }

    // Executors.DefaultThreadFactory is package visibility (...no touching, you unworthy JDK user!)
    public static class ClingThreadFactory implements ThreadFactory {

        protected final ThreadGroup   group;
        protected final AtomicInteger threadNumber = new AtomicInteger(1);
        protected final String        namePrefix   = "cling-";

        public ClingThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);

            return t;
        }
    }
}
