package org.jdownloader.extensions.eventscripter.sandboxobjects;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;

public class CrawlerJobSandbox {

    private LinkCollectingJob job;

    public CrawlerJobSandbox(LinkCollectingJob job) {
        this.job = job;
    }

    public CrawlerJobSandbox() {
        job = new LinkCollectingJob(new LinkOriginDetails(LinkOrigin.PASTE_LINKS_ACTION, "Test Job for EventScripter"));
        job.setText("http://jdownloader.org/_media/knowledge/wiki/development/open_preferences.png");
        job.setDeepAnalyse(false);
    }

    public String getOrigin() {
        return job.getOrigin() == null ? null : job.getOrigin().getOrigin().name();

    }

    public String getPassword() {
        return job.getCrawlerPassword();
    }

    public String getSourceUrl() {
        return job.getCustomSourceUrl();
    }

    public String getText() {
        return job.getText();
    }

    public boolean isDeepAnalysisEnabled() {
        return job.isDeepAnalyse();
    }

    public void setDeepAnalysisEnabled(boolean enabled) {
        job.setDeepAnalyse(enabled);
    }

    public void setPassword(String text) {
        job.setCrawlerPassword(text);
    }

    public void setText(String text) {
        job.setText(text);
    }
}
