package jd.utils;

import java.io.File;
import java.util.regex.Pattern;

import jd.parser.Regex;

public final class Sniffy {
    public static Pattern[] blackList = new Pattern[] {

    Pattern.compile("(Wireshark|PRTG Traffic Grapher|INTEREST Security Scanner|NETCORtools|PIAFCTM|Attack Tool Kit 4.1|Big Mother|Colasoft Capsa|cb_PMM|easy Look at Net|easy Look at Packets|Ethereal|Etherscan Analyzer|SoftX HTTP Debugger Pro|SoftX HTTP Debugger Lite|HTTP Monitor|NetControl|Packetyzer|Traffic Analyzer|TCP Spy|tcpdump|windump|NetworkMiner|CommView|NetworkActiv|Network General|Packet Analyzer|OmniPeek|HTTP Analyzer|URL Helper|URL Snooper|ettercap|FritzCap|Network Monitor|Network Monitor|Essential NetTools|Gobbler|Ethload|Netman|PacketView|Network Analyzer|LAN Analyzer|NetMinder|LANdecoder32|Protocol Analyzer|EvilMonkey|Trivial Proxy|Proxomitron)", Pattern.CASE_INSENSITIVE)

    };
    public static Pattern[] whiteList = new Pattern[] { Pattern.compile("Sniffy", Pattern.CASE_INSENSITIVE) };

    public static boolean hasSniffer() {
        if (OSDetector.isWindows()) { return hasWinSnifer();

        }
        return false;
    }

    public static void main(String args[]) {

        System.out.println(Sniffy.hasSniffer());

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
            if (!Regex.matches(prem, "could be a sniffer")) return false;
            for (Pattern white : whiteList) {

                list = white.matcher(list).replaceAll("");

            }
            for (Pattern black : blackList) {
                Regex r;
                if ((r = new Regex(list, black)).matches()) {

                return true; }
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
