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

package jd.plugins.decrypt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import com.sun.org.apache.xerces.internal.impl.dtd.models.CMLeaf;

import jd.PluginWrapper;
import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class SpiegelDe extends PluginForDecrypt {
    
    private static final Pattern PATTERN_SUPPORTED = Pattern.compile("http://[\\w\\.]*?spiegel\\.de/video/video-(\\d+).html" , Pattern.CASE_INSENSITIVE);
    
    
    private static final Pattern PATTERN_THEMA        = Pattern.compile("<headline>(.+?)</headline>");
    private static final Pattern PATTERN_HEADLINE     = Pattern.compile("<thema>(.+?)</thema>");
    private static final Pattern PATTERN_TEASER       = Pattern.compile("<teaser>(.+?)</teaser>");
    
    private static final Pattern PATTERN_FILENAME     = Pattern.compile("<filename>(.+?)</filename>");
    private static final Pattern PATTERN_FILENAME_T5  = Pattern.compile("^\\s+<type5>\\s+$\\s+^\\s+"+PATTERN_FILENAME.toString(),Pattern.MULTILINE);
    private static final Pattern PATTERN_FILENAME_T6  = Pattern.compile("^\\s+<type6>\\s+$\\s+^\\s+"+PATTERN_FILENAME.toString(),Pattern.MULTILINE);
    private static final Pattern PATTERN_FILENAME_T8  = Pattern.compile("^\\s+<type8>\\s+$\\s+^\\s+"+PATTERN_FILENAME.toString(),Pattern.MULTILINE);
    private static final Pattern PATTERN_FILENAME_T9  = Pattern.compile("^\\s+<type9>\\s+$\\s+^\\s+"+PATTERN_FILENAME.toString(),Pattern.MULTILINE);

    /*
     * Type 1: h263 flv
     * Type 2: flv mid (VP6)
     * Type 3: h263 low
     * Type 4: flv low (VP6)
     * Type 5: flv high (VP6) (680*544)
     * Type 6: h263 3gp 
     * Type 7: h263 3gp low
     * Type 8: iphone mp4
     * Type 9: podcast mp4 640*480
     */
    public SpiegelDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String id = new Regex(cryptedLink.getCryptedUrl(),PATTERN_SUPPORTED).getMatch(0);
        String xmlEncodings = br.getPage("http://video.spiegel.de/flash/" + id+".xml");
        String xmlInfos     = br.getPage("http://www1.spiegel.de/active/playlist/fcgi/playlist.fcgi/asset=flashvideo/mode=id/id="+id);
        String name = new Regex(xmlInfos,PATTERN_THEMA).getMatch(0)+"-"+new Regex(xmlInfos,PATTERN_HEADLINE).getMatch(0);
        String comment = name + "\t" + new Regex(xmlInfos,PATTERN_TEASER);
        
        Vector<ConversionMode> possibleconverts = new Vector<ConversionMode>();
        possibleconverts.add(ConversionMode.VIDEOFLV);
        possibleconverts.add(ConversionMode.AUDIOMP3);
        possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);
        possibleconverts.add(ConversionMode.VIDEO3GP);
        possibleconverts.add(ConversionMode.VIDEOMP4);
        possibleconverts.add(ConversionMode.PODCAST);
        possibleconverts.add(ConversionMode.IPHONE);
        ConversionMode convertTo = ConvertDialog.DisplayDialog(possibleconverts.toArray(), name);
        
        if (convertTo != null) {
            DownloadLink downloadLink = null;
            String fileName;
            if(convertTo == ConversionMode.VIDEO3GP){
                //type 6
                fileName = new Regex(xmlEncodings,PATTERN_FILENAME_T6).getMatch(0);
                downloadLink = createDownloadlink("http://video.promobil2spiegel.netbiscuits.com/"+fileName);
                downloadLink.setFinalFileName(name + ".3gp");
                System.out.println(downloadLink.getDownloadURL());
            }else if(convertTo == ConversionMode.IPHONE){
                //type 8
                fileName = new Regex(xmlEncodings,PATTERN_FILENAME_T8).getMatch(0);
                downloadLink = createDownloadlink("http://video.promobil2spiegel.netbiscuits.com/"+fileName);
                downloadLink.setFinalFileName(name + ".mp4");
            }else if(convertTo == ConversionMode.VIDEOMP4 || convertTo == ConversionMode.PODCAST){
                //type9
                fileName = new Regex(xmlEncodings,PATTERN_FILENAME_T9).getMatch(0);
                downloadLink = createDownloadlink("http://video.promobil2spiegel.netbiscuits.com/"+fileName);
                downloadLink.setFinalFileName(name + ".mp4");
            }else{
                //type5
                fileName = new Regex(xmlEncodings,PATTERN_FILENAME_T5).getMatch(0);
                downloadLink = createDownloadlink("http://video.spiegel.de/flash/"+fileName);
                downloadLink.setFinalFileName(name + ".tmp");
            }
            downloadLink.setSourcePluginComment(comment);
            downloadLink.setBrowserUrl(cryptedLink.getCryptedUrl());
            downloadLink.setProperty("convertto", convertTo.name());
            decryptedLinks.add(downloadLink);
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 3210 $", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
