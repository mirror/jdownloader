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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 11612 $", interfaceVersion = 2, names = { "antena3.com" }, urls = { "http://[\\w\\.]*?antena3.com/[-/\\dA-Za-c]+_\\d+.html" }, flags = { 0 })
public class Antena3Com extends PluginForHost {

	private String baseLink = "http://desprogresiva.antena3.com/";
	static private final String AGB = "http://www.antena3.com/a3tv2004/web/html/legal/index.html";
	private String finalURL;

	public Antena3Com(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Override
	public String getAGBLink() {
		return AGB;
	}

	@Override
	public AvailableStatus requestFileInformation(DownloadLink downloadLink)
			throws Exception {
		// link online?
		this.setBrowserExclusive();
		String html = br.getPage(downloadLink.getDownloadURL());
		if (br.containsHTML("<h1>¡Uy! No encontramos la página que buscas.</h1>"))
			throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

		// set file and package name
		String name = new Regex(html, "<title>(.*?)</title>").getMatch(0);
		downloadLink.setName(name + ".mp4");
		FilePackage fp = FilePackage.getInstance();
		fp.setName(name);
		downloadLink.setParentNode(fp);

		// get final url (.mp4)
		String xml = getXML(downloadLink);
		finalURL = baseLink + getXmlLabels(xml, "archivo").get(0);

		// set real size
		URLConnectionAdapter con = null;
		try {
			con = br.openGetConnection(finalURL);
			downloadLink.setDownloadSize(con.getLongContentLength());
		} finally {
			try {
				con.disconnect();
			} catch (Throwable e) {
			}
		}

		return AvailableStatus.TRUE;
	}

	private String getXML(DownloadLink downloadLink) throws IOException {
		String urlxml = new Regex(
				br.getPage(downloadLink.getDownloadURL()),
				"<link rel=\"video_src\" href=\"http://www.antena3.com/static/swf/A3Player.swf\\?xml=(.*?)\"/>")
				.getMatch(0);

		return br.getPage(urlxml);
	}

	private List<String> getXmlLabels(String xml, String key) {
		Regex rlist = new Regex(xml, "<" + key + ">(.*?)</" + key + ">");

		List<String> list = new LinkedList<String>();
		for (int i = 0; i < rlist.count(); i++) {
			list.add(rlist.getMatch(0, i).replace("<![CDATA[", "")
					.replace("]]>", ""));
		}
		return list;
	}

	@Override
	public void handleFree(DownloadLink downloadLink) throws Exception {

		String xml = getXML(downloadLink);
		finalURL = baseLink + getXmlLabels(xml, "archivo").get(0);

		dl = BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);

		if (!dl.getConnection().getContentType().contains("mp4")) {
			logger.warning("The final dllink seems not to be a mp4 file!");
			br.followConnection();
			throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
		}

		dl.startDownload();
	}

	@Override
	public void reset() {
	}

	@Override
	public void resetDownloadlink(DownloadLink link) {
	}

}
