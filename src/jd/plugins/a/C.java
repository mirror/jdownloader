//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.a;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;

public class C extends PluginsC {

    public C(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    public static byte[] getBytesFromFile(File file) {
        InputStream is;
        try {
            is = new FileInputStream(file);

            long length = file.length();
            if (length > Integer.MAX_VALUE) { return null; }
            byte[] bytes = new byte[(int) length];

            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                JDLogger.getLogger().severe("Could not completely read file " + file.getName());
            }
            is.close();
            return bytes;
        } catch (FileNotFoundException e) {

            JDLogger.exception(e);
        } catch (IOException e) {

            JDLogger.exception(e);
        }
        return null;
    }

    // @Override
    public ContainerStatus callDecryption(File lc) {
        ContainerStatus cs = new ContainerStatus(lc);

        // if (!getProperties().getBooleanProperty("USECCFRSDF", false)) {
        Vector<URL> services;
        try {
            services = new Vector<URL>();

            Collections.sort(services, new Comparator<Object>() {
                public int compare(Object a, Object b) {
                    return (int) (Math.random() * 4.0 - 2.0);
                }

            });
            services.add(0, new URL("http://service.jdownloader.net/dlcrypt/getDLC.php"));
            Iterator<URL> it = services.iterator();

            String dlc = null;
            // Ua6LDQoSRm6XrkfussF7t1iM5D+WAT9y5QB0U3gwmkELcT+6+4
            // U0RUeKvv256VUPZdHttMCuf0/C
            // Ua6LDRJGbpeuR+6ywXu3WIzkj5YBP3LlAHRTeDCaQQtxkLr7hTRFR4q+/
            // bnpVQ9l0e20wK5/T8L1

            while (it.hasNext() && dlc == null) {
                URL service = it.next();
                try {
                    // RequestInfo ri;
                    // requestInfo = Plugin.postRequest(new
                    // URL(ri.getLocation()), null, null, null, "src=ccf&data="
                    // + data, true);
                    Browser br = new Browser();
                    br.setDebug(true);
                    PostFormDataRequest r = (PostFormDataRequest) br.createPostFormDataRequest(service + "");
                    r.addFormData(new FormData("upload", lc.getName(), lc));

                    try {
                        r.addFormData(new FormData("l", getClass().forName("jd.plugins.a.Config").getField("CCF").get(null) + ""));

                    } catch (Throwable e) {

                    }
                    r.addFormData(new FormData("src", "ccf"));

                    br.openRequestConnection(r);

                    String ret = br.loadConnection(null) + "";

                    if (ret != null && ret.contains("<dlc>")) {
                        dlc = new Regex(ret, "<dlc>(.*)</dlc>").getMatch(0);
                        if (dlc.length() < 90) {
                            dlc = null;
                        }
                    }

                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }
            if (dlc != null) {
                lc = new File(lc.getAbsolutePath().substring(0, lc.getAbsolutePath().length() - 3) + "dlc");
                JDIO.writeLocalFile(lc, dlc);
                JDUtilities.getController().loadContainerFile(lc);
                cs.setStatus(ContainerStatus.STATUS_FINISHED);

                return cs;

            }
        } catch (MalformedURLException e) {

        }

        cs.setStatus(ContainerStatus.STATUS_FAILED);

        return cs;

    }

    // @Override
    public String[] encrypt(String plain) {

        return null;
    }

    // @Override
    public String extractDownloadURL(DownloadLink downloadLink) {
        return null;
    }

    // @Override
    public String getCoder() {
        return "JD-Team";
    }

    // @Override
    public ArrayList<DownloadLink> getContainedDownloadlinks() {
        return new ArrayList<DownloadLink>();
    }

    // @Override
    public long getVersion() {
        return 4;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jd.plugins.PluginForContainer#getContainedDownloadlinks(java.lang.String)
     */
    // @Override
    public void initContainer(final String filename) {
        callDecryption(new File(filename));
    }

}