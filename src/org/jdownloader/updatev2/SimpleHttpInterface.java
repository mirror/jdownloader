package org.jdownloader.updatev2;

import java.io.IOException;

public interface SimpleHttpInterface {

    SimpleHttpResponse get(String url) throws IOException;

}
