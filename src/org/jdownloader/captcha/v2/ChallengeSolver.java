package org.jdownloader.captcha.v2;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import jd.controlling.captcha.SkipException;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptcha2FallbackChallenge;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public abstract class ChallengeSolver<T> {
    public static final ChallengeSolver EXTERN = new ChallengeSolver<Object>() {
        @Override
        public void solve(SolverJob<Object> solverJob) throws InterruptedException, SolverException, SkipException {
            throw new WTFException("Not Implemented");
        }
    };

    private ChallengeSolver() {
    }

    public boolean setInvalid(AbstractResponse<?> response) {
        return false;
    }

    public boolean setUnused(AbstractResponse<?> response) {
        return false;
    }

    public boolean setValid(AbstractResponse<?> response) {
        return false;
    }

    protected ThreadPoolExecutor threadPool;
    private Class<T>             resultType;
    private SolverService        service;

    /**
     *
     * @param i
     *            size of the threadpool. if i<=0 there will be no threadpool. each challenge will get a new thread in this case
     */
    @SuppressWarnings("unchecked")
    public ChallengeSolver(SolverService service, int i) {
        this.service = service;
        if (service == null) {
            this.service = (SolverService) this;
        }
        service.addSolver(this);
        initThreadPool(i);
        Class<?> cls = this.getClass();
        while (true) {
            Type superClass = cls.getGenericSuperclass();
            if (superClass == null) {
                throw new IllegalArgumentException("Wrong Construct");
            }
            if (superClass instanceof Class) {
                cls = (Class<?>) superClass;
            } else if (superClass instanceof ParameterizedType) {
                resultType = (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
                break;
            } else {
                throw new IllegalArgumentException("Wrong Construct");
            }
        }
    }

    public ChallengeSolver(int i) {
        this(null, i);
    }

    protected HashMap<SolverJob<T>, JobRunnable<T>> map = new HashMap<SolverJob<T>, JobRunnable<T>>();

    public SolverService getService() {
        return service;
    }

    public boolean isEnabled() {
        return getService().getConfig().isEnabled();
    }

    // public void setEnabled(boolean b) {
    // getService().getConfig().setEnabled(b);
    // }
    public List<SolverJob<T>> listJobs() {
        synchronized (map) {
            return new ArrayList<SolverJob<T>>(map.keySet());
        }
    }

    public boolean isJobDone(SolverJob<?> job) {
        synchronized (map) {
            return !map.containsKey(job);
        }
    }

    public void enqueue(SolverJob<T> job) {
        final JobRunnable<T> jr = new JobRunnable<T>(this, job);
        synchronized (map) {
            map.put(job, jr);
            if (threadPool == null) {
                new Thread(jr, "ChallengeSolverThread").start();
            } else {
                threadPool.execute(jr);
            }
        }
    }

    protected static void checkInterruption() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    public void kill(SolverJob<T> job) {
        if (job != null) {
            synchronized (map) {
                final JobRunnable<T> jr = map.remove(job);
                if (jr != null) {
                    job.getLogger().info("Cancel " + jr);
                    jr.cancel();
                } else {
                    job.getLogger().info("Could not kill " + job + " in " + this);
                }
            }
        }
    }

    public String toString() {
        return getClass().getSimpleName();
    }

    private void initThreadPool(int i) {
        if (i <= 0) {
            return;
        }
        threadPool = new ThreadPoolExecutor(i, i, 5000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), new ThreadFactory() {
            public Thread newThread(final Runnable r) {
                return new Thread(r, "SolverThread:" + ChallengeSolver.this.toString());
            }
        }, new ThreadPoolExecutor.AbortPolicy());
        threadPool.allowCoreThreadTimeOut(true);
    }

    public abstract void solve(SolverJob<T> solverJob) throws InterruptedException, SolverException, SkipException;

    public Class<T> getResultType() {
        return resultType;
    }

    public boolean canHandle(Challenge<?> c) {
        if (c instanceof AbstractBrowserChallenge) {
            return false;
        }
        if (c instanceof AbstractRecaptcha2FallbackChallenge) {
            return false;
        }
        if (!getResultType().isAssignableFrom(c.getResultType())) {
            return false;
        }
        if (!validateBlackWhite(c)) {
            return false;
        }
        return true;
    }

    public boolean validateBlackWhite(Challenge<?> c) {
        if (getService().getConfig().isBlackWhiteListingEnabled()) {
            final String host = c.getHost();
            final ArrayList<String> whitelist = getService().getConfig().getWhitelistEntries();
            if (whitelist != null) {
                for (final String s : whitelist) {
                    try {
                        final Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
                        if (!StringUtils.equalsIgnoreCase(host, c.getTypeID())) {
                            if (pattern.matcher(host + "-" + c.getTypeID()).matches()) {
                                return true;
                            }
                            if (pattern.matcher(host).matches()) {
                                return true;
                            }
                        }
                        if (pattern.matcher(c.getTypeID()).matches()) {
                            return true;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
            final ArrayList<String> blacklist = getService().getConfig().getBlacklistEntries();
            if (blacklist != null) {
                for (final String s : blacklist) {
                    try {
                        final Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
                        if (!StringUtils.equalsIgnoreCase(host, c.getTypeID())) {
                            if (pattern.matcher(host + "-" + c.getTypeID()).matches()) {
                                return false;
                            }
                            if (pattern.matcher(host).matches()) {
                                return false;
                            }
                        }
                        if (pattern.matcher(c.getTypeID()).matches()) {
                            return false;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    public long getTimeout() {
        return -1;
    }

    public int getWaitForByID(String solverID) {
        Integer obj = getWaitForMap().get(solverID);
        return obj == null ? 0 : obj.intValue();
    }

    private Map<String, Integer> waitForMap = null;

    public synchronized Map<String, Integer> getWaitForMap() {
        if (waitForMap != null) {
            return waitForMap;
        }
        Map<String, Integer> map = getService().getConfig().getWaitForMap();
        if (map == null || map.size() == 0) {
            map = getService().getWaitForOthersDefaultMap();
            getService().getConfig().setWaitForMap(map);
        }
        waitForMap = Collections.synchronizedMap(map);
        return waitForMap;
    }
}
