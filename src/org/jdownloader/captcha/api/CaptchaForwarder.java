package org.jdownloader.captcha.api;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.remoteapi.exceptions.FileNotFound404Exception;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs.Recaptcha2FallbackChallengeViaPhantomJS;

import jd.http.Browser;

public class CaptchaForwarder implements CaptchaForwarderAPIInterface {
    private static final CaptchaForwarder INSTANCE = new CaptchaForwarder();
    private static final AtomicLong       ID       = new AtomicLong(System.currentTimeMillis());

    public static CaptchaForwarder getInstance() {
        return INSTANCE;
    }

    private ThreadPoolExecutor runner;

    private CaptchaForwarder() {
    }

    public void runEntry(Job job) {
        try {
            job.result = job.run();
        } catch (Exception e) {
            LoggerFactory.getDefaultLogger().log(e);
            job.exception = e;
        } finally {
            done.put(job.id, job);
        }
    }

    abstract class Job {
        public Exception exception;
        private long     id;
        public String    result;

        public abstract String run() throws Exception;

        public Job(long id) {
            this.id = id;
        }
    }

    @Override
    public long createJobRecaptchaV2(final String lsiteKey, final String stoken, final String domain, final String reason) {
        return enqueue(new Job(ID.incrementAndGet()) {
            @Override
            public String run() throws Exception {
                return new CaptchaHelperCrawlerPluginRecaptchaV2(null, new Browser()) {
                    public String getSiteDomain() {
                        return domain;
                    };

                    public String getSiteKey() {
                        return lsiteKey;
                    };

                    @Override
                    protected org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge createChallenge(jd.plugins.PluginForDecrypt plugin) {
                        return new RecaptchaV2Challenge(lsiteKey, stoken, false, null, this.br, this.getSiteDomain()) {
                            @Override
                            public org.jdownloader.DomainInfo getDomainInfo() {
                                return DomainInfo.getInstance(domain);
                            };

                            public int getTimeout() {
                                return 5 * 60 * 1000;
                            };

                            public String getHost() {
                                return domain;
                            };

                            @Override
                            protected org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs.Recaptcha2FallbackChallengeViaPhantomJS createPhantomJSChallenge() {
                                Recaptcha2FallbackChallengeViaPhantomJS ret = new Recaptcha2FallbackChallengeViaPhantomJS(this) {
                                    @Override
                                    public DomainInfo getDomainInfo() {
                                        return DomainInfo.getInstance(domain);
                                    };

                                    @Override
                                    public String getExplain() {
                                        return reason;
                                    };

                                    @Override
                                    public synchronized File getImageFile() {
                                        if (this.imageFile == null) {
                                            this.imageFile = Application.getResource("tmp/" + System.currentTimeMillis() + ".png");
                                        }
                                        return this.imageFile;
                                    };

                                    @Override
                                    public int getTimeout() {
                                        return 6 * 60 * 1000;
                                    };

                                    @Override
                                    public String getHost() {
                                        return domain;
                                    };
                                };
                                return ret;
                            };
                        };
                    };
                }.getToken();
            }
        });
    }

    private ConcurrentHashMap<Long, Job> done = new ConcurrentHashMap<Long, Job>();

    private long enqueue(Job job) {
        ensureRunner().execute(createRunnable(job));
        return job.id;
    }

    private Runnable createRunnable(final Job job) {
        return new Runnable() {
            @Override
            public void run() {
                runEntry(job);
            }
        };
    }

    private synchronized ThreadPoolExecutor ensureRunner() {
        if (runner == null) {
            runner = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1024));
        }
        return runner;
    }

    @Override
    public String getResult(long jobID) throws FileNotFound404Exception, InternalApiException {
        Job job = done.remove(jobID);
        if (job == null) {
            throw new FileNotFound404Exception();
        }
        if (job.exception != null) {
            throw new InternalApiException(job.exception);
        }
        return job.result;
    }
}
