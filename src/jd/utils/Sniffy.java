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

package jd.utils;

import java.io.File;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.parser.Regex;

public final class Sniffy {
    public static Pattern blackList = Pattern.compile("(Wireshark|PRTG Traffic Grapher|INTEREST Security Scanner|NETCORtools|PIAFCTM|Attack Tool Kit 4.1|Big Mother|Colasoft Capsa|cb_PMM|easy Look at Net|easy Look at Packets|Ethereal|Etherscan Analyzer|SoftX HTTP Debugger Pro|SoftX HTTP Debugger Lite|HTTP Monitor|NetControl|Packetyzer|Traffic Analyzer|TCP Spy|tcpdump|windump|NetworkMiner|CommView|NetworkActiv|Network General|Packet Analyzer|OmniPeek|HTTP Analyzer|URL Helper|URL Snooper|ettercap|FritzCap|Network Monitor|Network Monitor|Essential NetTools|Gobbler|Ethload|Netman|PacketView|Network Analyzer|LAN Analyzer|NetMinder|LANdecoder32|Protocol Analyzer|EvilMonkey)", Pattern.CASE_INSENSITIVE);
    public static Pattern blackListproxy = Pattern.compile("(thisisjustadummyproxy)", Pattern.CASE_INSENSITIVE);

    public static Pattern whiteList = Pattern.compile("(2Network Monitor|2NetMan|Sniffy|sygate|kaspersky|Linksys Wireless Network Monitor)", Pattern.CASE_INSENSITIVE);

    public static boolean hasSniffer() {
        // if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_ENV ||
        // JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) { return
        // false; }
        if (OSDetector.isWindows()) { return Sniffy.hasWinSnifer();

        }
        return false;
    }

    public static void main(String args[]) {

        System.out.println(Sniffy.hasWinSnifer());

    }

    public static boolean hasWinSnifer() {
        try {

            File reader2 = JDUtilities.getResourceFile("tools/windows/p.exe");
          
            String hash2 = JDUtilities.getLocalHash(reader2);
            if (hash2==null||!hash2.equals("3c2298676457b5c49e55dbee3451c4b1")) {
                System.out.println("p Manipulated");
                return true;
            }

            Executer exec = new Executer(reader2.toString());
            exec.setWaitTimeout(10);
            exec.start();
            exec.waitTimeout();
            String list = exec.getStream() + " \r\n " + exec.getErrorStream();
            
        
            boolean check2 = false;
            String[][] whit = new Regex(list, whiteList).getMatches();
            for (String[] m : whit) {
                for (String s : m) {
                    JDUtilities.getLogger().finer("Found " + s + " is ok");
                }
            }
          

            list = whiteList.matcher(list).replaceAll("");

            String r;
            if ((r = new Regex(list, blackList).getMatch(0)) != null) {
                JDUtilities.getLogger().warning("Sniffy(forbidden sniffer):2" + r);
                check2 = true;
            }
            if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_PROXY, false) || JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_SOCKS, false)) {

                if ((r = new Regex(list, blackListproxy).getMatch(0)) != null) {
                    JDUtilities.getLogger().warning("Sniffy:(forbidden proxytools)" + r);
                    check2 = true;
                }
            }

            return  check2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
