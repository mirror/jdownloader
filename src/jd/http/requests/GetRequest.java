//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.http.requests;

import java.io.IOException;
import java.net.MalformedURLException;

import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.URLConnectionAdapter.METHOD;

public class GetRequest extends Request {

    public GetRequest(String url) throws MalformedURLException {
        super(Browser.correctURL(url));

    }

    public long postRequest(URLConnectionAdapter httpConnection) throws IOException {
        return 0;
    }

    public void preRequest(URLConnectionAdapter httpConnection) throws IOException {
        httpConnection.setRequestMethod(METHOD.GET);
    }

}
