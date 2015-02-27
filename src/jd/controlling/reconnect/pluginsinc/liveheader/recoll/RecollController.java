package jd.controlling.reconnect.pluginsinc.liveheader.recoll;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class RecollController {
    private static final RecollController INSTANCE  = new RecollController();
    protected static final String         HTTP_BASE = "https://payments.appwork.org/test/recoll/";

    /**
     * get the only existing instance of RecollController. This is a singleton
     * 
     * @return
     */
    public static RecollController getInstance() {
        return RecollController.INSTANCE;
    }

    private LogSource logger;
    private Queue     queue;
    private Browser   br;

    /**
     * Create a new instance of RecollController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private RecollController() {

        logger = LogController.getInstance().getLogger(getClass().getName());
        br = new Browser();
        queue = new Queue("RecollQueue") {

        };

    }

    public void trackValidateStartAsynch(final String scriptID) {

        queue.addAsynch(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                try {
                    call("incTestStart", null, scriptID);

                } catch (Exception e) {
                    logger.log(e);
                }
                return null;
            }
        });
    }

    protected <Typo> Typo call(String command, TypeRef<Typo> type, Object... objects) throws IOException {
        StringBuilder sb = new StringBuilder();

        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                sb.append(i == 0 ? "" : "&");

                sb.append(Encoding.urlEncode(JSonStorage.serializeToJson(objects[i])));
            }
        }
        String ret = br.postPageRaw(HTTP_BASE + command, sb.toString());
        if (type == null) {
            return null;
        }
        return JSonStorage.restoreFromString(ret, type);
    }

    public void trackValidateEnd(final String scriptID) {

        queue.addAsynch(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                try {
                    call("incTestEnd", null, scriptID);

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
                    call("setWorking", null, scriptID, successDuration, offlineDuration);

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
                    call("setNotWorking", null, scriptID);

                } catch (Exception e) {
                    logger.log(e);
                }
                return null;
            }
        });
    }

    public boolean isAlive() {

        try {
            return call("isAlive", TypeRef.BOOLEAN);
        } catch (IOException e) {
            logger.log(e);
            ;
            return false;
        }

    }

    public List<RouterData> findRouter(RouterData rd) {
        try {
            return call("findRouter", new TypeRef<ArrayList<RouterData>>() {
            }, rd);
        } catch (IOException e) {
            logger.log(e);
            return null;
        }

    }

    public String getManufactor(String mac) {
        try {
            return call("getManufactor", TypeRef.STRING);
        } catch (IOException e) {
            logger.log(e);
            ;
            return null;
        }

    }

    public boolean addRouter(RouterData rd) {
        try {
            return call("addRouter", TypeRef.BOOLEAN, rd);
        } catch (IOException e) {
            logger.log(e);
            ;
            return false;
        }

    }

}
