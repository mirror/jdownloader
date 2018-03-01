package org.jdownloader.captcha.v2;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;

import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.captcha.SkipRequest;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.statistics.StatsManager.CollectionName;

public abstract class Challenge<T> {
    private static final int      REDUCER      = 100;
    private final UniqueAlltimeID id           = new UniqueAlltimeID();
    private final Class<T>        resultType;
    private final long            created      = System.currentTimeMillis();
    private volatile long         lastActivity = created;
    private int                   timeout      = -1;
    private volatile boolean      accountLogin = false;
    private final boolean         createdInsideAccountChecker;
    private int                   round        = -1;

    public int getRound() {
        return round;
    }

    public Object getAPIStorable(String format) throws Exception {
        return null;
    }

    public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
        final Plugin plg = getPlugin();
        if (plg != null) {
            if (plg instanceof PluginForHost) {
                return canBeSkippedByPluginforHost(skipRequest, solver, challenge, plg);
            } else if (plg instanceof PluginForDecrypt) {
                return canBeSkippedByPluginforDecrypt(skipRequest, solver, challenge, plg);
            }
        }
        return false;
    }

    private boolean canBeSkippedByPluginforDecrypt(final SkipRequest skipRequest, final ChallengeSolver<?> solver, final Challenge<?> challenge, final Plugin plugin) {
        final Plugin challengePlugin = challenge.getPlugin();
        if (challengePlugin == null || !(challengePlugin instanceof PluginForDecrypt)) {
            /* we only want block PluginForDecrypt captcha here */
            return false;
        }
        final PluginForDecrypt currentPlugin = (PluginForDecrypt) plugin;
        final LinkCrawler currentCrawler = currentPlugin.getCrawler();
        final CrawledLink currentOrigin = currentPlugin.getCurrentLink().getOriginLink();
        final PluginForDecrypt decrypt = (PluginForDecrypt) challengePlugin;
        if (currentCrawler != decrypt.getCrawler()) {
            /* we have a different crawler source */
            return false;
        }
        switch (skipRequest) {
        case STOP_CURRENT_ACTION:
            /* user wants to stop current action (eg crawling) */
            return true;
        case BLOCK_ALL_CAPTCHAS:
            /* user wants to block all captchas (current session) */
            return true;
        case BLOCK_HOSTER:
            /* user wants to block captchas from specific hoster */
            return StringUtils.equals(currentPlugin.getHost(), challengePlugin.getHost());
        case BLOCK_PACKAGE:
            final CrawledLink crawledLink = decrypt.getCurrentLink();
            return crawledLink != null && crawledLink.getOriginLink() == currentOrigin;
        default:
            return false;
        }
    }

    private boolean canBeSkippedByPluginforHost(final SkipRequest skipRequest, final ChallengeSolver<?> solver, final Challenge<?> challenge, final Plugin plugin) {
        if (isCreatedInsideAccountChecker() || isAccountLogin()) {
            /* we don't want to skip login captcha inside fetchAccountInfo(Thread is AccountCheckerThread) */
            return false;
        }
        final DownloadLink challengeLink = challenge.getDownloadLink();
        if (challengeLink == null) {
            return false;
        }
        final Plugin challengePlugin = challenge.getPlugin();
        if (challengePlugin != null && !(challengePlugin instanceof PluginForHost)) {
            /* we only want block PluginForHost captcha here */
            return false;
        }
        final PluginForHost currentPlugin = (PluginForHost) plugin;
        final DownloadLink currentLink = currentPlugin.getDownloadLink();
        switch (skipRequest) {
        case BLOCK_ALL_CAPTCHAS:
            /* user wants to block all captchas (current session) */
            return true;
        case BLOCK_HOSTER:
            /* user wants to block captchas from specific hoster */
            return StringUtils.equals(currentPlugin.getHost(), challengePlugin.getHost());
        case BLOCK_PACKAGE:
            /* user wants to block captchas from current FilePackage */
            if (challengeLink.getDefaultPlugin() == null) {
                return false;
            }
            final FilePackage currentFilePackage = currentLink.getFilePackage();
            return !FilePackage.isDefaultFilePackage(currentFilePackage) && challengeLink.getFilePackage() == currentLink.getFilePackage();
        default:
            return false;
        }
    }

    public boolean isAccountLogin() {
        return accountLogin;
    }

    // can be overridden to validate a response before adding it to the job
    public boolean validateResponse(AbstractResponse<T> response) {
        if (response.getPriority() <= 0) {
            return false;
        }
        return true;
    }

    public void setAccountLogin(boolean accountLogin) {
        this.accountLogin = accountLogin;
    }

    public UniqueAlltimeID getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public Challenge(String method, String explain2) {
        typeID = method;
        explain = explain2;
        createdInsideAccountChecker = Thread.currentThread() instanceof AccountCheckerThread;
        Type superClass = this.getClass().getGenericSuperclass();
        while (superClass instanceof Class) {
            superClass = ((Class<?>) superClass).getGenericSuperclass();
            if (superClass == null) {
                throw new IllegalArgumentException("Wrong Construct");
            }
        }
        resultType = (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public boolean isCreatedInsideAccountChecker() {
        return createdInsideAccountChecker;
    }

    public int getTimeout() {
        return Math.max(-1, timeout);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Class<T> getResultType() {
        return resultType;
    }

    abstract public boolean isSolved();

    private String typeID;
    private String explain;

    public String getTypeID() {
        return typeID;
    }

    public long getCreated() {
        return created;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void keepAlive() {
        final Plugin plugin = getPlugin();
        if (plugin != null && plugin.keepAlive(this)) {
            lastActivity = System.currentTimeMillis();
        }
    }

    public long getValidUntil() {
        final int timeout = getTimeout();
        if (timeout > 0) {
            return getLastActivity() + timeout;
        } else {
            return -1;
        }
    }

    public void setTypeID(String typeID) {
        this.typeID = typeID;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }

    public ResponseList<T> getResult() {
        return result;
    }

    public void setResult(ResponseList<T> result) {
        this.result = result;
    }

    private ResponseList<T> result;
    private SolverJob<T>    job;

    public String getHost() {
        final Plugin plg = getPlugin();
        return plg == null ? null : plg.getHost();
    }

    public DownloadLink getDownloadLink() {
        final Plugin plugin = getPlugin();
        if (plugin != null && plugin instanceof PluginForHost) {
            return ((PluginForHost) plugin).getDownloadLink();
        }
        return null;
    }

    public DomainInfo getDomainInfo() {
        final Plugin plugin = getPlugin();
        if (plugin == null) {
            throw new WTFException("no plugin for this challenge!?");
        }
        DomainInfo ret = null;
        if (plugin instanceof PluginForHost) {
            final DownloadLink dl = ((PluginForHost) plugin).getDownloadLink();
            if (dl != null) {
                ret = dl.getDomainInfo();
            }
        }
        if (ret == null) {
            ret = DomainInfo.getInstance(plugin.getHost());
        }
        if (ret != null) {
            return ret;
        }
        throw new WTFException("no domaininfo for this challenge!?");
    }

    public Plugin getPlugin() {
        return null;
    }

    // Workaround until we implemented proper Refresh SUpport in the plugins
    public T getRefreshTrigger() {
        if (getResultType() == String.class) {
            return (T) "";
        }
        return null;
    }

    public AbstractResponse<T> parseAPIAnswer(String result, String resultFormat, ChallengeSolver<?> solver) {
        return null;
    }

    public boolean isRefreshTrigger(String result) {
        return result == getRefreshTrigger();
    }

    public void initController(SolverJob<T> job) {
        this.job = job;
        Plugin plg = getPlugin();
        if (plg != null) {
            round = plg.addChallenge(this);
        }
    }

    public SolverJob<T> getJob() {
        return job;
    }

    /**
     * is called from plugins after all it's challenges have been handled. NOTE: Not all plugins call this method.
     */
    public void cleanup() {
    }

    /**
     * called when the controller handled this challenge
     */
    public void onHandled() {
    }

    public void sendStatsError(ChallengeSolver solver, Throwable e) {
        if (e == null || e instanceof InterruptedException) {
            return;
        }
        if (solver == null || !(solver instanceof CESChallengeSolver)) {
            return;
        }
        //
        HashMap<String, String> info = createStatsInfoMap(solver);
        info.put("errorclass", e.getClass().getSimpleName());
        info.put("errormessage", e.getMessage());
        StatsManager.I().track(REDUCER, "captchaCES", "error", info, CollectionName.CAPTCHA);
    }

    public void sendStatsSolving(ChallengeSolver solver) {
        if (solver == null || !(solver instanceof CESChallengeSolver)) {
            return;
        }
        HashMap<String, String> info = createStatsInfoMap(solver);
        StatsManager.I().track(REDUCER, "captchaCES", "solving", info, CollectionName.CAPTCHA);
    }

    public void sendStatsValidation(ChallengeSolver solver, String status) {
        if (solver == null || !(solver instanceof CESChallengeSolver)) {
            return;
        }
        HashMap<String, String> info = createStatsInfoMap(solver);
        info.put("status", status);
        StatsManager.I().track(REDUCER, "captchaCES", "validation", info, CollectionName.CAPTCHA);
    }

    private HashMap<String, String> createStatsInfoMap(ChallengeSolver solver) {
        HashMap<String, String> info;
        info = new HashMap<String, String>();
        info.put("service", solver.getService().getID());
        info.put("solver", solver.getClass().getSimpleName());
        info.put("type", getTypeID());
        try {
            info.put("host", getHost());
        } catch (Throwable e) {
            info.put("host", "unknown");
        }
        return info;
    }

    // is called in a 1000ms interval while solvers are active. can be used to check for external success (like oauth
    public void poll(SolverJob<T> job2) {
    }
}
