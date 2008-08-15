package jd.utils;

import java.io.File;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.parser.Regex;

public final class Sniffy {
    public static Pattern blackList = Pattern.compile("(Wireshark|PRTG Traffic Grapher|INTEREST Security Scanner|NETCORtools|PIAFCTM|Attack Tool Kit 4.1|Big Mother|Colasoft Capsa|cb_PMM|easy Look at Net|easy Look at Packets|Ethereal|Etherscan Analyzer|SoftX HTTP Debugger Pro|SoftX HTTP Debugger Lite|HTTP Monitor|NetControl|Packetyzer|Traffic Analyzer|TCP Spy|tcpdump|windump|NetworkMiner|CommView|NetworkActiv|Network General|Packet Analyzer|OmniPeek|HTTP Analyzer|URL Helper|URL Snooper|ettercap|FritzCap|Network Monitor|Network Monitor|Essential NetTools|Gobbler|Ethload|Netman|PacketView|Network Analyzer|LAN Analyzer|NetMinder|LANdecoder32|Protocol Analyzer|EvilMonkey)", Pattern.CASE_INSENSITIVE);
    public static Pattern blackListproxy = Pattern.compile("(thisisjustadummyproxy)", Pattern.CASE_INSENSITIVE);

    public static Pattern whiteList = Pattern.compile("(Sniffy|sygate|kaspersky|Linksys Wireless Network Monitor)", Pattern.CASE_INSENSITIVE);

    public static boolean hasSniffer() {
//        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_ENV || JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) { return false; }
        if (OSDetector.isWindows()) { return Sniffy.hasWinSnifer();

        }
        return false;
    }

    public static void main(String args[]) {

        System.out.println(Sniffy.hasWinSnifer());

    }

    public static boolean hasWinSnifer() {
        try {
            File reader1 = JDUtilities.getResourceFile("tools/windows/promiscdetect.exe");
            File reader2 = JDUtilities.getResourceFile("tools/windows/p.exe");
            String hash1 = JDUtilities.getLocalHash(reader1);
            String hash2 = JDUtilities.getLocalHash(reader2);
            if (!hash1.equals("117ec27602980ae13307a7c2021a5d90") || !hash2.equals("3c2298676457b5c49e55dbee3451c4b1")) {
                System.out.println("p Manipulated");
                return true;
            }

            Executer exec = new Executer(reader2.toString());
            exec.setWaitTimeout(10);
            exec.start();
            exec.waitTimeout();
            String list = exec.getStream() + " \r\n " + exec.getErrorStream();
            exec = new Executer(reader1.toString());
            exec.setWaitTimeout(10);
            exec.start();
            exec.waitTimeout();
            String prem = exec.getStream() + " \r\n " + exec.getErrorStream();
            boolean check1 = false;
            boolean check2 = false;
            String[][] whit = new Regex(list, whiteList).getMatches();
            for (String[] m : whit) {
                for (String s : m) {
                    JDUtilities.getLogger().finer("Found " + s + " is ok");
                }
            }
            if (Regex.matches(prem, "could be a sniffer") && !Regex.matches(list, whiteList)) {

                JDUtilities.getLogger().warning("Sniffy:1");
                check1 = true;

            }

            list = whiteList.matcher(list).replaceAll("");

            String r;
            if ((r = new Regex(list, blackList).getFirstMatch()) != null) {
                JDUtilities.getLogger().warning("Sniffy(forbidden sniffer):2" + r);
                check2 = true;
            }
            if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_PROXY, false) || JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_SOCKS, false)) {

                if ((r = new Regex(list, blackListproxy).getFirstMatch()) != null) {
                    JDUtilities.getLogger().warning("Sniffy:(forbidden proxytools)" + r);
                    check2 = true;
                }
            }

            return check1 | check2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
