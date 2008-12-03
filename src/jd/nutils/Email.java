package jd.nutils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Email {
    private DataOutputStream os = null;
    private BufferedReader is = null;
    private String sRt = "";


    public synchronized final String sendEmail(String sSmtpServer, String sFromAdr, String sFromRealName, String sToAdr, String sToRealName, String sSubject, String sText) throws IOException, Exception {
        Socket so = null;
        try {
            sRt = "";
            if (null == sSmtpServer || 0 >= sSmtpServer.length() || null == sFromAdr || 0 >= sFromAdr.length() || null == sToAdr || 0 >= sToAdr.length() || ((null == sSubject || 0 >= sSubject.length()) && (null == sText || 0 >= sText.length()))) throw new Exception("Invalid Parameters for SmtpSimple.sendEmail().");
            if (null == sFromRealName || 0 >= sFromRealName.length()) sFromRealName = sFromAdr;
            if (null == sToRealName || 0 >= sToRealName.length()) sToRealName = sToAdr;
            so = new Socket(sSmtpServer, 25);
            os = new DataOutputStream(so.getOutputStream());
            is = new BufferedReader(new InputStreamReader(so.getInputStream()));
            so.setSoTimeout(100000);
            writeRead(true, "220", null);
            writeRead(true, "250", "HELO " + sSmtpServer + "\r\n");
            writeRead(true, "250", "RSET\r\n");
            writeRead(true, "250", "MAIL FROM:<" + sFromAdr + ">\r\n");
            writeRead(true, "250", "RCPT TO:<" + sToAdr + ">\r\n");
            writeRead(true, "354", "DATA\r\n");
            writeRead(false, null, "To: " + sToRealName + " <" + sToAdr + ">\r\n");
            writeRead(false, null, "From: " + sFromRealName + " <" + sFromAdr + ">\r\n");
            writeRead(false, null, "Subject: " + sSubject + "\r\n");
            writeRead(false, null, "Mime-Version: 1.0\r\n");
            writeRead(false, null, "Content-Type: text/plain; charset=\"iso-8859-1\"\r\n");
            writeRead(false, null, "Content-Transfer-Encoding: quoted-printable\r\n\r\n");
            writeRead(false, null, sText + "\r\n");
            writeRead(true, "250", ".\r\n");
            writeRead(true, "221", "QUIT\r\n");
            return sRt;
        } finally {
            if (is != null) try {
                is.close();
            } catch (Exception ex) {
            }
            if (os != null) try {
                os.close();
            } catch (Exception ex) {
            }
            if (so != null) try {
                so.close();
            } catch (Exception ex) {
            }
            is = null;
            os = null;
        }
    }

    private final void writeRead(boolean bReadAnswer, String sAnswerMustStartWith, String sWrite) throws IOException, Exception {
        if (null != sWrite && 0 < sWrite.length()) {
            sRt += sWrite;
//            System.out.println(">>"+sWrite);
            os.writeBytes(sWrite);
        }
        if (bReadAnswer) {
            String sRd = is.readLine() + "\r\n";
//            System.out.print(sRd);
            sRt += sRd;
            if (null != sAnswerMustStartWith && 0 < sAnswerMustStartWith.length() && !sRd.startsWith(sAnswerMustStartWith)) throw new Exception(sRt);
        }
    }
}
