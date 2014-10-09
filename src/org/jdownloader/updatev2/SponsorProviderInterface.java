package org.jdownloader.updatev2;


public interface SponsorProviderInterface {

    void setHttpClient(SimpleHttpInterface simpleHttpInterface);

    String getNextUrl();

    void init() throws Exception;

}
