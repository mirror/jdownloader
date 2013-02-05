package jd.controlling.reconnect.pluginsinc.liveheader.recoll;

import java.util.List;

import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RecollInterface;
import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.remotecall.RemoteClient;

public class RecollController {
    private static final RecollController INSTANCE = new RecollController();

    /**
     * get the only existing instance of RecollController. This is a singleton
     * 
     * @return
     */
    public static RecollController getInstance() {
        return RecollController.INSTANCE;
    }

    private RecollInterface serverConnector;
    private LogSource       logger;
    private Queue           queue;

    /**
     * Create a new instance of RecollController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private RecollController() {

        try {
            logger = LogController.getInstance().getLogger(getClass().getName());
            // static String UPDATE3_JDOWNLOADER_ORG_RECOLL = ; // "update3.jdownloader.org/recoll";
            serverConnector = new RemoteClient("update3.jdownloader.org/recoll").getFactory().newInstance(RecollInterface.class);

        } catch (Throwable e) {
            logger.log(e);
        }
        queue = new Queue("RecollQueue") {

        };

    }

    public void trackValidateStartAsynch(final String scriptID) {

        queue.addAsynch(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                try {
                    serverConnector.incTestStart(scriptID, null);

                } catch (Exception e) {
                    logger.log(e);
                }
                return null;
            }
        });
    }

    public void trackValidateEnd(final String scriptID) {

        queue.addAsynch(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                try {
                    serverConnector.incTestEnd(scriptID, null);

                } catch (Exception e) {
                    logger.log(e);
                }
                return null;
            }
        });
    }

    public void trackWorking(final String scriptID, final long successDuration, final long offlineDuration) {
        queue.addAsynch(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                try {
                    serverConnector.setWorking(scriptID, null, successDuration, offlineDuration);

                } catch (Exception e) {
                    logger.log(e);
                }
                return null;
            }
        });
    }

    public void trackNotWorking(final String scriptID) {
        queue.addAsynch(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                try {
                    serverConnector.setNotWorking(scriptID, null);

                } catch (Exception e) {
                    logger.log(e);
                }
                return null;
            }
        });
    }

    public boolean isAlive() {
        return serverConnector.isAlive();
    }

    public List<RouterData> findRouter(RouterData rd) {
        return serverConnector.findRouter(rd, null);
    }

    public String getManufactor(String mac) {
        return serverConnector.getManufactor(mac);
    }

    public boolean addRouter(RouterData rd) {

        return serverConnector.addRouter(rd, null);
    }

}
