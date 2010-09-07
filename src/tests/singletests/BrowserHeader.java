//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package tests.singletests;

import jd.http.Browser;

import org.junit.Assert;
import org.junit.Test;

import tests.utils.TestUtils;

public class BrowserHeader {

    @Test
    public void CheckPortRequest() {
        try {
            Browser br = new Browser();
            TestUtils.wikiLogin(br);
            br.getPage("http://jdownloader.net:8081/knowledge/wiki/development/intern/brunittest");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

}
