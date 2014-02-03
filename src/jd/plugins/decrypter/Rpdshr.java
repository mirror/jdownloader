//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidshare.com" }, urls = { "https?://(www\\.)?rapidshare\\.com/(download/)?share/[A-Fa-f0-9]{32}" }, flags = { 0 })
public class Rpdshr extends PluginForDecrypt {

    private static final String FOLDER = "folder:";
    private static final String FILE   = "file:";

    public Rpdshr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String shareID = new Regex(param.getCryptedUrl(), "/share/([A-Fa-f0-9]+)").getMatch(0);

        // sub=sharelinkcontent
        // Description: list folder content
        // Parameters: share=32digit hex share id
        // sharepassword=string (password to access the share if needed)
        // Reply fields: [contentlist|NONE|ERROR: msg]
        // Reply format: file:fileid,filename,serverid,owner,ctime,size\n
        // folder:folderid,owner,foldername,parent,owner,ctime\n
        // Error messages: [Login invalid.|Share not found.|Too many downloads.|Sharelink expired.|Wrong password.]
        //
        //

        br.getPage("https://api.rapidshare.com/cgi-bin/rsapi.cgi?rsource=jd&sub=sharelinkcontent&share=" + shareID);

        parseLines(param, decryptedLinks, shareID, null, Regex.getLines(br.getRequest().getHtmlCode()));
        return decryptedLinks;
    }

    protected void parseLines(final CryptedLink param, final ArrayList<DownloadLink> decryptedLinks, final String shareID, final String packageName, final String[] lines) throws IOException {
        FilePackage fp = null;
        if (StringUtils.isNotEmpty(packageName)) {
            fp = FilePackage.getInstance();
            fp.setName(packageName);

        }
        for (final String line : lines) {
            if (line.startsWith(FILE)) {
                final String[] properties = line.substring(FILE.length()).split("\\,");
                final String fileID = properties[0];
                final String fileName = properties[1];
                // String serverID = properties[2];
                // String owner = properties[3];
                // String ctime = properties[4];
                String size = properties[5];
                final DownloadLink newLink = new DownloadLink(JDUtilities.getPluginForHost("rapidshare.com"), fileName, getHost(), "http://rapidshare.com/files/" + fileID + "/" + fileName, true);
                newLink.setAvailable(true);
                newLink.setBrowserUrl(param.getCryptedUrl());
                newLink.setFinalFileName(fileName);
                newLink.setProperty("shareid", shareID);
                newLink.setProperty("fileid", fileID);
                newLink.setDownloadSize(Long.parseLong(size));

                decryptedLinks.add(newLink);
                if (fp != null) {
                    fp.add(newLink);
                }

            } else if (line.startsWith(FOLDER)) {
                String[] properties = line.substring(FOLDER.length()).split("\\,");
                String folderID = properties[0];
                String owner = properties[1];
                String foldername = properties[2];
                // String parent = properties[3];
                // String ctime = properties[4];
                // sub=sharelinkfoldercontent
                // Description: list folder content
                // Parameters: share=32digit hex share id
                // sharepassword=string (password to access the share if needed)
                // folderid=ushort
                // owner=ulong
                // Reply fields: [contentlist|NONE|ERROR: msg]
                // Reply format: file:fileid,filename,serverid,owner,ctime,size\n
                // folder:folderid,owner,foldername,parent,owner,ctime\n
                // Error messages: [Login invalid.|Share not found.|Too many downloads.|Sharelink expired.|Wrong password.]

                br.getPage("https://api.rapidshare.com/cgi-bin/rsapi.cgi?rsource=jd&share=" + shareID + "&sub=sharelinkfoldercontent&folderid=" + folderID + "&owner=" + owner);

                parseLines(param, decryptedLinks, shareID, foldername, Regex.getLines(br.getRequest().getHtmlCode()));
            }

        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}