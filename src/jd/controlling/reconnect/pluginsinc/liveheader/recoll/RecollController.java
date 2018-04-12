package jd.controlling.reconnect.pluginsinc.liveheader.recoll;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.UIOManager;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class RecollController {
    private static final RecollController INSTANCE  = new RecollController();
    protected static String               HTTP_BASE = "https://reconnect.jdownloader.org/";

    // static {
    // if (!Application.isJared(null)) {
    // HTTP_BASE = "https://reconnect.jdownloader.org/test/";
    // }
    // }
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

    /**
     * Create a new instance of RecollController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private RecollController() {
        logger = LogController.getInstance().getLogger(getClass().getName());
        queue = new Queue("RecollQueue") {
        };
    }

    protected <Typo> Typo call(String command, TypeRef<Typo> type, Object... objects) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                sb.append(i == 0 ? "" : "&");
                sb.append(Encoding.urlEncode(JSonStorage.serializeToJson(objects[i])));
            }
        }
        Browser br = new Browser();
        br.setAllowedResponseCodes(200, 503, 500, 400);
        String ret = br.postPageRaw(HTTP_BASE + command, sb.toString());
        if (br.getRequest().getHttpConnection().getResponseCode() == 503) {
            throw new RetryIOException(ret);
        }
        if (br.getRequest().getHttpConnection().getResponseCode() == 400) {
            throw new BadQueryException(ret);
        }
        if (type == null) {
            return null;
        }
        return JSonStorage.restoreFromString(ret, type);
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

    public List<RouterData> findRouter(RouterData rd) throws InterruptedException {
        try {
            return call("findRouter", new TypeRef<ArrayList<RouterData>>() {
            }, rd);
        } catch (RetryIOException e) {
            Thread.sleep(2000);
            return findRouter(rd);
        } catch (IOException e) {
            logger.log(e);
            return null;
        }
    }

    public String getManufactor(String mac) {
        try {
            return call("getManufactor", TypeRef.STRING, mac);
        } catch (IOException e) {
            logger.log(e);
            return null;
        }
    }

    public AddRouterResponse addRouter(RouterData rd) {
        try {
            return call("addRouter", AddRouterResponse.TYPE_REF, rd);
        } catch (IOException e) {
            logger.log(e);
            ;
            return null;
        }
    }

    public String getIsp() {
        try {
            return call("getIsp", TypeRef.STRING);
        } catch (IOException e) {
            logger.log(e);
            ;
            return null;
        }
    }

    public List<RouterData> query(String name, String manufactor, String isp) throws InterruptedException, BadQueryException {
        try {
            return call("query", new TypeRef<ArrayList<RouterData>>() {
            }, name, manufactor, isp);
        } catch (RetryIOException e) {
            Thread.sleep(2000);
            return query(name, manufactor, isp);
        } catch (BadQueryException e) {
            throw e;
        } catch (IOException e) {
            logger.log(e);
            UIOManager.I().showErrorMessage(T.T.LiveHeaderDetectionWizard_runOnlineScan_notavailable_mm());
            return null;
        }
    }
}
