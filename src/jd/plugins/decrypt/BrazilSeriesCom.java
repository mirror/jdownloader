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

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;

public class BrazilSeriesCom extends PluginForDecrypt {
    private static final String  PATTERN_SUPPORTED_BASE    = "http://[\\w\\.]*?brazil-series\\.com";
    private static final Pattern PATTERN_SUPPORTED_EPISODE = Pattern.compile(PATTERN_SUPPORTED_BASE+"/\\w+/\\w+/\\w+_ep(\\d+)\\.htm",Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_STAFFEL = Pattern.compile(PATTERN_SUPPORTED_BASE+"/\\w+/\\d+t/\\w+_intro.htm",Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_SERIE = Pattern.compile(PATTERN_SUPPORTED_BASE+"/\\w+/\\w+_intro.htm",Pattern.CASE_INSENSITIVE);
    /* TODO WALLPAPER:
     * http://www.brazil-series.com/iasip/wp/iasip_wp_01.htm
     * */

    
    //Key for settings
    private static final String AVI_ONLY = "BRAZIL_SERIES_COM_AVI_ONLY";
    

    //Decrypt episode

    private static final Pattern PATTERN_SERIEN_TITEL = Pattern.compile("<title>(.+?)</title>");
    private static final Pattern PATTERN_EPISODE_NAME = Pattern.compile("<!-- InstanceBeginEditable name=\"Nome ingles\" -->(.+?)<!-- InstanceEndEditable -->");

    private static final Pattern PATTERN_AVI_LINKS       = Pattern.compile("Download \\( Formato : AVI sem Legenda \\)(.+?)Download \\( Formato : RMVB Legendado \\)",Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);  
    private static final Pattern PATTERN_RMVB_LINKS      = Pattern.compile("Download \\( Formato : RMVB Legendado \\)(.+?)Download \\( Legendas \\)",Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);  
    private static final Pattern PATTERN_Legendas_LINKS  = Pattern.compile("Download \\( Legendas \\)(.+)",Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);  
    private static final Pattern PATTERN_DOWNLOAD_LINK   = Pattern.compile("<a href=\"(.+?)\" target=\"_blank\" onmouseover=\"MM_swapImage\\(",Pattern.CASE_INSENSITIVE);

    //Funktionieren nur teilweise zu viele Sonderfälle!
    //private static final Pattern PATTERN_DOWNLOADLINK_AVI = Pattern.compile("<a href=\"(.+?)\" target=\"_blank\" onMouseOver=\"MM_swapImage\\('Parte\\d+','','\\.\\./\\.\\./img/downloads/parte\\d+d\\.gif',1\\)\" onMouseOut=\"MM_swapImgRestore\\(\\)\"><img src=\"\\.\\./\\.\\./img/downloads/parte\\d+\\.gif\" name=\"Parte\\d+\" border=\"0\" id=\"Parte\\d+\"( /)?></a>",Pattern.CASE_INSENSITIVE);                              
    //private static final Pattern PATTERN_DOWNLOADLINK_RMVB_SUBTITLE    = Pattern.compile("<a href=\"(.+?)\" target=\"_blank\" onMouseOver=\"MM_swapImage\\('Image\\d+','','\\.\\./\\.\\./img/downloads/donwloadswd\\.gif',1\\)\" onMouseOut=\"MM_swapImgRestore\\(\\)\"><img src=\"\\.\\./\\.\\./img/downloads/donwloadws\\.gif\" name=\"Image\\d+\" width=\"97\" height=\"19\" border=\"0\" id=\"Image451\"( /)?></a>",Pattern.CASE_INSENSITIVE);

    //Decrypt staffel

    private static final Pattern PATTERN_EPISODE_LINK = Pattern.compile("<div align=\"center\"><a href=\"(\\w+_ep\\d+.htm)\"><img src=\"");
    //Decrypt serie
    private static final Pattern PATTERN_STAFFEL_LINK = Pattern.compile("<a href=\"(\\d+t/\\w+_intro.htm)\">");

    public BrazilSeriesCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = cryptedLink.getCryptedUrl();
        if(new Regex(url, PATTERN_SUPPORTED_EPISODE).matches()){
            String page = br.getPage(url);
            String titel = new Regex(page, PATTERN_SERIEN_TITEL).getMatch(0);
            titel += "-Episodio " + new Regex(url,PATTERN_SUPPORTED_EPISODE).getMatch(0) + "-" + new Regex(page,PATTERN_EPISODE_NAME).getMatch(0); 

            String aviLinks      = new Regex(page,PATTERN_AVI_LINKS).getMatch(0);
            String rmvbLinks     = new Regex(page,PATTERN_RMVB_LINKS).getMatch(0);
            String legendasLinks = new Regex(page,PATTERN_Legendas_LINKS).getMatch(0);

            FilePackage fpAvi      = new FilePackage();
            FilePackage fpRmvb     = new FilePackage();
            FilePackage fpLegendas = new FilePackage();

            fpAvi.setName      (titel+" (Formato : AVI sem Legenda)");
            fpRmvb.setName     (titel+" (Formato : RMVB Legendado)");
            fpLegendas.setName (titel+" (Legendas)");

            for(String link:new Regex(aviLinks,PATTERN_DOWNLOAD_LINK).getColumn(0)){
                DownloadLink dlLink = createDownloadlink(link);
                dlLink.setFilePackage(fpAvi);
                decryptedLinks.add(dlLink);
            }
            for(String link:new Regex(rmvbLinks,PATTERN_DOWNLOAD_LINK).getColumn(0)){
                DownloadLink dlLink = createDownloadlink(link);
                dlLink.setFilePackage(fpRmvb);
                if(!getPluginConfig().getBooleanProperty(AVI_ONLY)){
                    decryptedLinks.add(dlLink);
                }
            }
            for(String link:new Regex(legendasLinks,PATTERN_DOWNLOAD_LINK).getColumn(0)){
                DownloadLink dlLink = createDownloadlink(link);
                dlLink.setFilePackage(fpLegendas);
                if(!getPluginConfig().getBooleanProperty(AVI_ONLY)){
                    decryptedLinks.add(dlLink);
                }
            }
        }else if(new Regex(url, PATTERN_SUPPORTED_STAFFEL).matches()){
            String page = br.getPage(url);
            for(String s:new Regex(page,PATTERN_EPISODE_LINK).getColumn(0)){
                decryptedLinks.add(createDownloadlink(br.getBaseURL()+s));
            }
        }else if(new Regex(url, PATTERN_SUPPORTED_SERIE).matches()){
            String page = br.getPage(url);
            for(String s:new Regex(page,PATTERN_STAFFEL_LINK).getColumn(0)){
                decryptedLinks.add(createDownloadlink(br.getBaseURL()+s));
            }
        }else{
            logger.severe("Uncorrect url in the brazil-series.com decrypter: " + url);
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), AVI_ONLY, JDLocale.L("plugins.decrypt.brazil-series.com1", "Download-Formato : AVI sem Legenda somente")).setDefaultValue(false));
    }
}
