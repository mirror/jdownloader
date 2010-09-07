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

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.HashMap;

import jd.nutils.SimpleFTP;

import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class FTP {

    // private static final String URL = null;
    private SimpleFTP               ftp;
    private HashMap<String, String> links;

    @Before
    public void setUp() throws Exception {
        ftp = new SimpleFTP();
        links = TestUtils.getHosterLinks("ftp");
        ftp.connect(new URL(links.get("NORMAL_DOWNLOADLINK_1")));

    }

    @Test
    public void getFileInfo() {

        try {

            String[] info = ftp.getFileInfo(new URL(links.get("NORMAL_DOWNLOADLINK_1")).getPath());
            assertTrue(info != null);

            info = ftp.getFileInfo(new URL(links.get("FNF_DOWNLOADLINK_1")).getPath());
            assertTrue(info == null);

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

    }
}
