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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.Random;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jd.PluginWrapper;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 12299 $", interfaceVersion = 2, names = { "pandora.tv" }, urls = { "http://channel\\.pandora\\.tv/channel/video\\.ptv?.+" }, flags = { 0 })
public class PandoraTV extends PluginForHost {

    private static final String MAINPAGE = "http://www.pandora.tv";
    private static final String DLPAGE   = "http://trans-idx.pandora.tv/flvorgx.pandora.tv";

    public PandoraTV(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://info.pandora.tv/?m=service_use_1";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("title\": \"(.*?)\",").getMatch(0);
        if (filename == null) filename = br.getRegex("\"title\":\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("filesize\": \"(.*?)\",").getMatch(0);
        if (filesize == null) filesize = br.getRegex("\"filesize\":\"(.*?)\"").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim() + ".flv");
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setCookiesExclusive(true);
        requestFileInformation(downloadLink);
        Random rnddummy = new Random();
        int dummy = rnddummy.nextInt(50000 - 10000) + 10000;
        String urlpath = br.getRegex("var vod = \"http.*?\\.tv(.*?)\"").getMatch(0);
        /* check for high quality */
        String hqurl = br.getRegex("flvInfo\":\\{\"flv\":\"http.*?\\.tv(.*?)\"").getMatch(0);
        if (hqurl != null) {
            urlpath = hqurl.replaceAll("\\\\/", "/");
        }
        /* KEY1 */
        br.getPage("http://channel.pandora.tv/channel/cryptKey.ptv?dummy=" + dummy + "?");
        String keyOne = br.getRegex("\"(.*?)\"").getMatch(0, 1);
        /* set JS Cookies */
        String pcid = KEY_EncryptionCreate("PCID", "cookie");
        br.setCookie(MAINPAGE, "PCID", pcid);
        String rc = KEY_EncryptionCreate("RC", "cookie");
        br.setCookie(MAINPAGE, "RC", rc);
        /* KEY2 */
        String keys = KEY_EncryptionCreate(keyOne, "encrypt");
        br.getPage(DLPAGE + urlpath + keys + "&class=normal&country=DE&method=differ");
        if (br.containsHTML("error") || br.getRequest().getHttpConnection().getResponseCode() != 200) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = br.getRegex("\"(.*?)\"").getMatch(0, 1);
        dl = BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public String KEY_EncryptionCreate(String fun, String value) throws Exception {
        Object result = new Object();
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        Invocable inv = (Invocable) engine;
        String algorythmus = "var now = new Date();var encryptionStartTime = now.getTime(); function LoadEncryptionCode(fun){ orgEncryptionKEY = fun; KEY_EncryptionCreate(); return (vodEncryptionCode);}; function KEY_EncryptionReturn(vArray, rArray){ var EncryptionKEY = rArray[0] + rArray[1] + rArray[2] + rArray[3] + vArray[11] + vArray[1] + vArray[10] + vArray[2] + vArray[9] + vArray[3] + vArray[8] + vArray[4] + vArray[7] + vArray[6] + vArray[5]; vodEncryptionCode = \"?key1=\" + orgEncryptionKEY + \"&key2=\" + EncryptionKEY.toUpperCase() + \"&ft=FC\";}; function KEY_RandomCreate(mod, k, num){ var _loc2 = Math.floor(Math.random()*num); if (mod == \"jjak\") { if (k == 0) { var _loc3 = _loc2 % 2; if (_loc3 == 0) { _loc2 = _loc3; } else { _loc2 = _loc2 - 1; } } } else { _loc3 = _loc2 % 2; if (_loc3 == 0) { _loc2 = _loc2 + 1; } else { _loc2 = _loc3; } } var _loc1 = _loc2.toString(16); if (_loc1.length < 2) { _loc1 = \"0\" + _loc1; }  _loc1 = _loc1; return (_loc1);}; function KEY_EncryptionCreate(){ var _loc4 = new Array(); var _loc10 = new Array(); var _loc19 = new Array(); var _loc14 = 8; var _loc11 = 2; var _loc17 = new Array(); var _loc22 = 256; var _loc18 = 4; var _loc23 = 11; var _loc13 = new Array(); var _loc21; var _loc20; _loc21 = \"hol\"; _loc20 = skipAndEncryption(); _loc17 = _loc20.split(\"/+/\"); var _loc9 = 0; for (var _loc8 = 0; _loc8 < _loc14 - 1; ++_loc8) { _loc4[_loc8] = orgEncryptionKEY.substr(_loc14 * _loc8, _loc14); var _loc3; var _loc16 = new Array(); for (var _loc2 = 0; _loc2 < _loc14 / _loc11; ++_loc2) { _loc10[_loc2] = \"0x\" + _loc4[_loc8].substr(_loc11 * _loc2, _loc11); if (_loc3 == undefined) { _loc3 = _loc10[_loc2]; var _loc15 = \"0x\" + _loc17[_loc9]; _loc3 = _loc3 ^ _loc15; ++_loc9; if (_loc9 > _loc18 - 1) { _loc9 = 0; }  } else { _loc3 = _loc3 ^ _loc10[_loc2]; } _loc16.push(_loc10[_loc2]); } _loc13[_loc8 + 5] = _loc3.toString(16); _loc4[_loc8] = _loc16; _loc3 = undefined; } _loc9 = 0; for (var _loc6 = 0; _loc6 < _loc14 / _loc11; ++_loc6) { _loc16 = new Array(); for (var _loc7 = 0; _loc7 < _loc4.length; ++_loc7) { _loc16.push(_loc4[_loc7][_loc6]); }  _loc19[_loc6] = _loc16; _loc3 = undefined; for (var _loc5 = 0; _loc5 < _loc4.length; ++_loc5) { if (_loc3 == undefined) { _loc3 = _loc4[_loc5][_loc6]; _loc15 = \"0x\" + _loc17[_loc9]; _loc3 = _loc3 ^ _loc15; ++_loc9; if (_loc9 > _loc18 - 1) { _loc9 = 0; } continue; } _loc3 = _loc3 ^ _loc4[_loc5][_loc6]; } _loc13[_loc6 + 1] = _loc3.toString(16); } for (var _loc12 = 1; _loc12 < _loc13.length; ++_loc12) { if (_loc13[_loc12].length < 2) { _loc13[_loc12] = \"0\" + _loc13[_loc12]; } } KEY_EncryptionReturn(_loc13, _loc17);}; function skipAndEncryption(){ var _loc13 = \"hol\"; var _loc19 = 256; var _loc14; var _loc12; var _loc9; var _loc10; var _loc5; var _loc20 = \"0xFA\"; var _loc16 = \"0xCE\"; var _loc11 = Array(\"11\", \"10\", \"01\", \"00\"); var _loc3 = new Array(); var _loc6; var _loc17; var _loc15; var _loc4; var _loc18; _loc14 = KEY_RandomCreate(_loc13, 0, _loc19); var now = new Date(); var encryptionLapseTime = Math.floor((now.getTime() - encryptionStartTime) / 1000); encryptionLapseTime = encryptionLapseTime.toString(16); if (encryptionLapseTime.length == 1) { _loc12 = \"000\" + encryptionLapseTime; } else if (encryptionLapseTime.length == 2) { _loc12 = \"00\" + encryptionLapseTime; } else if (encryptionLapseTime.length == 3) { _loc12 = \"0\" + encryptionLapseTime; } else { _loc12 = encryptionLapseTime; } _loc9 = \"0x\" + _loc12.substr(0, 2); _loc10 = \"0x\" + _loc12.substr(2, 2); _loc9 = _loc9 ^ _loc20; _loc10 = _loc10 ^ _loc16; _loc9 = _loc9.toString(16); _loc10 = _loc10.toString(16); if (_loc9.length == 1) { _loc9 = \"0\" + _loc9; } if (_loc10.length == 1) { _loc10 = \"0\" + _loc10; } _loc5 = _loc9 + _loc10; for (var _loc2 = 0; _loc2 < _loc5.length; ++_loc2) { _loc3[_loc2] = {cod: _loc11[_loc2], val: _loc5.substr(_loc2, 1)}; } _loc3.prototypeShuffle(); for (var _loc2 = 0; _loc2 < _loc5.length; ++_loc2) { if (_loc2 == 0) { _loc6 = _loc3[_loc2].val; _loc4 = _loc3[_loc2].cod; continue; } _loc6 = _loc6 + _loc3[_loc2].val; _loc4 = _loc4 + _loc3[_loc2].cod; } _loc17 = _loc6.substr(0, 2); _loc15 = _loc6.substr(2, 2); _loc4 = parseInt(_loc4, 2); _loc4 = \"0x\" + _loc4.toString(16); _loc4 = _loc4 ^ \"0xA7\"; _loc18 = _loc14 + \"/+/\" + _loc4.toString(16) + \"/+/\" + _loc17 + \"/+/\" + _loc15; return (_loc18);}; Array.prototype.prototypeShuffle = function (){ var _loc3 = this.length; for (var _loc2 = 0; _loc2 < _loc3; ++_loc2) { rnd = Math.floor(Math.random()*_loc3); temp = this[_loc2]; this[_loc2] = this[rnd]; this[rnd] = temp; }}; function makeCookie(length){ var today = new Date(); var cookie; var value; var values = new Array(); for ( i=0; i < length ; i++ ) { values[i] = \"\" + Math.random(); } value = today.getTime(); for ( i=0; i < length ; i++ ) { value += values[i].charAt(2); } return(value);};";
        try {
            engine.eval(algorythmus);
            if (value == "cookie") {
                result = inv.invokeFunction("makeCookie", 10);
            } else {
                result = inv.invokeFunction("LoadEncryptionCode", fun);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return (String) result;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
