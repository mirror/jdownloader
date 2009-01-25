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

package jd.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;

public class GetRequest extends Request {

    public GetRequest(String url) throws MalformedURLException {
        super(url);

    }

    @Override
    public void postRequest(HTTPConnection httpConnection) throws IOException {

    }

    @Override
    public void preRequest(HTTPConnection httpConnection) throws IOException {
        httpConnection.setRequestMethod("GET");

    }

 

  

}
