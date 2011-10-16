package jd;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JPanel;

import jd.http.Browser;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;

public class Tester extends JPanel {

    public static String loginAPI(Browser br) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (br == null) br = new Browser();
        try {
            br.getHeaders().put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; MS Web Services Client Protocol 2.0.50727.4952)");
            br.getHeaders().put("SOAPAction", "\"urn:FileserveAPIWebServiceAction\"");
            br.postPage("http://api.fileserve.com/api/fileserveAPIServer.php", "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tns=\"urn:FileserveAPI\" xmlns:types=\"urn:FileserveAPI/encodedTypes\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><soap:Body soap:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><tns:login><username xsi:type=\"xsd:string\">JDAffiliate</username><password xsi:type=\"xsd:string\">2xDKKb6585</password></tns:login></soap:Body></soap:Envelope>");
        } finally {
            br.getHeaders().remove("SOAPAction");
        }
        String loginResp = br.getRegex("<loginReturn.*?>(.*?)</loginReturn").getMatch(0);
        loginResp = decrypt(loginResp);
        String cookie = new Regex(loginResp, "cookie_str=.*?cookie=(.+)(;| |$)").getMatch(0);
        br.setCookie("http://fileserve.com", "cookie", cookie);
        return loginResp;
    }

    public static String decrypt(String string) throws UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        if (string == null) return null;
        byte[] decoded = Base64.decode(string);
        final byte[] key = Encoding.Base64Decode("L3hpTDJGaFNPVVlnc2FUdg==").getBytes();
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher c = Cipher.getInstance("AES/ECB/NoPadding");
        c.init(Cipher.DECRYPT_MODE, skeySpec);
        return new String(c.doFinal(decoded));
    }

    public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Object test1[] = new Object[2000000];
        ArrayList<Object> test2 = new ArrayList<Object>();
        for (int i = 0; i < 2000000; i++) {
            test2.add(null);
        }

        long a = System.currentTimeMillis();
        for (int o = 0; o < 100000; o++) {
            for (int i = 0; i < 2000000; i++) {
                Object kk = test1[i];
            }
        }
        System.out.println(System.currentTimeMillis() - a);

        a = System.currentTimeMillis();
        for (int o = 0; o < 100000; o++) {
            for (int i = 0; i < 2000000; i++) {
                Object kk = test2.get(i);
            }
        }
        System.out.println(System.currentTimeMillis() - a);
        System.out.println(new Browser().getPage("https://www.1fichier.com/en/login.pl"));
        // String login = loginAPI(br);
        // String jj =
        // decrypt("0J5fKHNZ2NV5dULQ4BQwAnr7SiYmQDaRYYMK7CXKXGReo9G4NrVoAf6oi0s2DUd7DLif4sGAu+5HdAfVF82mUWrKnVzTsd0q/yJwRPxNVZzBGzAmEPc2eQwzpg2iQAsdn94OZa6oOZiZ16BIJ6X7x5yGwvMsS5iJzG3md0LcfDTOE999xVY7X4UQCaiQelrVDVXIrRxbveVnDwdJ68qO++5gIrSV/J+QNXmAzitc17/7GIJsaIMa+uV9nPoIe0NWJfGL/0dkrDj/YxLaCBm5ODqSjKQwVKjIw1ecBOETPdlGZFczVW6sa3buo7AC6FnyzGT7Sh+EY5afFCad/KSDam/90A17imMJLv4SzftoBwik3yIWtYev3OVYOdyrTKGfrf9C8DCqvs/x6rYxfRqbpg==");
        // ArrayList<JACMethod> dd = JACMethod.parseJACInfo(new
        // File("/home/daniel/test"));
        // int oo = 2;
        // Browser br = new Browser();
        // Logger log = Logger.getLogger("bla");
        // log.setLevel(Level.ALL);
        // ConsoleHandler c;
        // log.addHandler(c = new ConsoleHandler());
        // c.setLevel(Level.ALL);
        // br.setVerbose(true);
        // br.setLogger(log);
        // br.getPage("http://update3.jdownloader.org/advert/getLanguage.php?r=bla");
        // br.getPage("http://update3.jdownloader.org/advert/getLanguage.php?r=bla");

    }

    public static String[][] getGrid(String data, String deliminator) {
        String[] lines = getLines(data);
        String[][] ret = new String[lines.length][];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = lines[i].split(deliminator, -1);
        }
        return ret;
    }

    /**
     * Splits multiple rows to a String array. Row Deliminator is "\r\n"
     * 
     * @param data
     * @return
     */
    public static String[] getLines(String data) {
        return data.split("[\r\n]+");
    }
}
