package jd.controlling;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.PluginForHost;

import org.appwork.utils.logging.Log;

public class LinkChecker {

    public class LinkCheckJob {

        private DownloadLink[]                            links        = null;
        private HashMap<String, LinkedList<DownloadLink>> linksHostMap = null;
        private boolean                                   forceRecheck;

        private LinkCheckJob(DownloadLink[] links, boolean forceRecheck) {
            this.links = links;
            this.forceRecheck = forceRecheck;
            linksHostMap = new HashMap<String, LinkedList<DownloadLink>>();
            for (DownloadLink link : links) {
                String host = link.getHost();

                if ("ftp".equalsIgnoreCase(host) || "DirectHTTP".equalsIgnoreCase(host) || "http links".equalsIgnoreCase(host)) {
                    /* direct and ftp links are divided by their hostname */
                    String specialHost = Browser.getHost(link.getDownloadURL());
                    if (specialHost != null) host = specialHost;
                }
                LinkedList<DownloadLink> map = linksHostMap.get(host);
                if (map == null) {
                    map = new LinkedList<DownloadLink>();
                    linksHostMap.put(host, map);
                }
                map.add(link);
            }
        }

        public boolean isChecked() {
            synchronized (this) {
                return this.linksHostMap.isEmpty();
            }
        }

        public DownloadLink[] getLinks() {
            return links.clone();
        }

        public void abort() {
            synchronized (this) {
                this.linksHostMap.clear();
                this.notify();
            }
        }

        public boolean waitChecked() {
            while (true) {
                synchronized (LinkCheckJob.this) {
                    if (this.linksHostMap.isEmpty()) break;
                    try {
                        LinkCheckJob.this.wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            return isChecked();
        }

    }

    private static LinkChecker INSTANCE = new LinkChecker();

    public static LinkChecker getInstance() {
        return INSTANCE;
    }

    private HashMap<String, Thread>                   checkThreads  = new HashMap<String, Thread>();
    private HashMap<String, LinkedList<LinkCheckJob>> jobs          = new HashMap<String, LinkedList<LinkCheckJob>>();

    private AtomicLong                                jobsRequested = new AtomicLong(0);
    private AtomicLong                                jobsDone      = new AtomicLong(0);

    private LinkChecker() {
    }

    public LinkCheckJob check(DownloadLink[] links, boolean forceReCheck) {
        if (links == null) throw new NullPointerException("no links?!");
        LinkCheckJob job = null;
        synchronized (LinkChecker.this) {
            job = new LinkCheckJob(links, forceReCheck);
            jobsRequested.incrementAndGet();
            /* enqueue and start this job in joblist for each existing hoster */
            Set<Entry<String, LinkedList<DownloadLink>>> sets = job.linksHostMap.entrySet();
            for (Entry<String, LinkedList<DownloadLink>> set : sets) {
                String host = set.getKey();
                LinkedList<LinkCheckJob> hostJobs = jobs.get(host);
                if (hostJobs == null) {
                    hostJobs = new LinkedList<LinkCheckJob>();
                    jobs.put(host, hostJobs);
                }
                hostJobs.add(job);
                /* get thread to check this hoster */
                Thread thread = checkThreads.get(host);
                if (thread == null || !thread.isAlive()) {
                    final String threadHost = host;
                    final Thread newThread = new Thread(new Runnable() {

                        public void run() {
                            /*
                             * we want thread to keep alive 10 secs
                             */
                            int stopDelay = 10;
                            while (true) {
                                LinkedList<LinkCheckJob> currentJobList = new LinkedList<LinkCheckJob>();
                                try {
                                    HashSet<DownloadLink> linksToCheck = new HashSet<DownloadLink>();
                                    synchronized (LinkChecker.this) {
                                        /* fetch new jobs for this checker */
                                        LinkedList<LinkCheckJob> nextJobs = jobs.get(threadHost);
                                        if (nextJobs != null && nextJobs.size() > 0) {
                                            /* reset stopDelay */
                                            // System.out.println("got new jobs");
                                            stopDelay = 3;
                                            currentJobList.addAll(nextJobs);
                                            for (LinkCheckJob nextJob : nextJobs) {
                                                /* add links from jobs */
                                                synchronized (nextJob) {
                                                    LinkedList<DownloadLink> jobLinks = nextJob.linksHostMap.get(threadHost);
                                                    if (jobLinks != null) {
                                                        for (DownloadLink link : jobLinks) {
                                                            if (nextJob.forceRecheck) link.setAvailableStatus(AvailableStatus.UNCHECKED);
                                                            linksToCheck.add(link);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        /*
                                         * remove the fetched jobs from todo
                                         * list
                                         */
                                        jobs.remove(threadHost);
                                    }
                                    if (!linksToCheck.isEmpty()) {
                                        DownloadLink linksList[] = new DownloadLink[linksToCheck.size()];
                                        Iterator<DownloadLink> it = linksToCheck.iterator();
                                        int i = 0;
                                        while (it.hasNext()) {
                                            linksList[i++] = it.next();
                                        }
                                        /* now we check the links */
                                        PluginForHost plg = linksList[0].getDefaultPlugin().getWrapper().getNewPluginInstance();
                                        plg.setBrowser(new Browser());
                                        plg.init();
                                        boolean ret = false;
                                        try {
                                            ret = plg.checkLinks(linksList);
                                        } catch (final Throwable e) {
                                            Log.exception(e);
                                        }
                                        if (!ret) {
                                            for (DownloadLink link : linksList) {
                                                link.getAvailableStatus(plg);
                                            }
                                        }
                                    }
                                } catch (Throwable e) {
                                    Log.exception(e);
                                } finally {
                                    for (LinkCheckJob nextJob : currentJobList) {
                                        synchronized (nextJob) {
                                            nextJob.linksHostMap.remove(threadHost);
                                            if (nextJob.linksHostMap.isEmpty()) {
                                                /* job is finished */
                                                jobsDone.incrementAndGet();
                                            }
                                            nextJob.notify();
                                        }
                                    }
                                    currentJobList.clear();
                                }

                                synchronized (LinkChecker.this) {
                                    LinkedList<LinkCheckJob> stopCheck = jobs.get(threadHost);
                                    if (stopCheck == null || stopCheck.size() == 0) {
                                        stopDelay--;
                                        // System.out.println("only " +
                                        // stopDelay + " left to stop");
                                        if (stopDelay == 0) {
                                            // System.out.println("thread died");
                                            checkThreads.remove(threadHost);
                                            return;
                                        }
                                    }
                                }
                                synchronized (this) {
                                    try {
                                        this.wait(1000);
                                    } catch (InterruptedException e) {
                                    }
                                }

                            }

                        }

                    });
                    thread = newThread;
                    thread.setName("LinkChecker: " + host);
                    thread.setDaemon(true);
                    checkThreads.put(host, thread);
                    thread.start();
                } else {
                    synchronized (thread) {
                        thread.notify();
                    }
                }
            }
        }
        return job;
    }

    public boolean isRunning() {
        return this.jobsRequested.get() != this.jobsDone.get();
    }

}
