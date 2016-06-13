//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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

package org.jdownloader.encoding;

import java.util.ArrayList;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

public class AADecoder {
    private static final String   HEX_HASH_MARKER    = "(oﾟｰﾟo)+ ";
    private static final String   BLOCK_START_MARKER = "(ﾟДﾟ)[ﾟεﾟ]+";

    private static final String[] BYTES              = new String[] { "(c^_^o)", "(ﾟΘﾟ)", "((o^_^o) - (ﾟΘﾟ))", "(o^_^o)", "(ﾟｰﾟ)", "((ﾟｰﾟ) + (ﾟΘﾟ))", "((o^_^o) +(o^_^o))", "((ﾟｰﾟ) + (o^_^o))", "((ﾟｰﾟ) + (ﾟｰﾟ))", "((ﾟｰﾟ) + (ﾟｰﾟ) + (ﾟΘﾟ))", "(ﾟДﾟ) .ﾟωﾟﾉ", "(ﾟДﾟ) .ﾟΘﾟﾉ", "(ﾟДﾟ) ['c']", "(ﾟДﾟ) .ﾟｰﾟﾉ", "(ﾟДﾟ) .ﾟДﾟﾉ", "(ﾟДﾟ) [ﾟΘﾟ]" };
    public static boolean         DEBUG              = false;

    private String                input              = null;

    public AADecoder() {
    }

    public AADecoder(final String input) {
        this.input = input;
    }

    public boolean isAAEncoded(String js) {
        if (!js.startsWith("ﾟωﾟﾉ= /｀ｍ´）ﾉ ~┻━┻   //*´∇｀*/ ['_']; o=(ﾟｰﾟ)  =_=3; c=(ﾟΘﾟ) =(ﾟｰﾟ)-(ﾟｰﾟ); ")) {
            return false;
        }
        if (!js.endsWith("(ﾟДﾟ)[ﾟoﾟ]) (ﾟΘﾟ)) ('_');")) {
            return false;
        }
        return true;
    }

    /** returns the first result */
    protected String fetchJs() {
        if (input == null) {
            throw new WTFException("Problemo, with finding JS");
        }
        final String js = new Regex(input, "ﾟωﾟ.*?\\('_'\\);").getMatch(-1);
        return js;
    }

    public String decode() throws Exception {
        return decode(null);
    }

    // Long version. Contains additional info, but lot's of unused variables that may be important for botguard
    public String decode(String js) throws Exception {
        if (StringUtils.isEmpty(js)) {
            js = fetchJs();
            if (StringUtils.isEmpty(js)) {
                return null;
            }
        }
        js = js.replace("/*´∇｀*/", "");
        // trim
        js = js.replaceAll("^\\s+|\\s+$", "");

        String data = new Regex(js, "\\(ﾟДﾟ\\)\\[ﾟoﾟ\\]\\+ (.+?)\\(ﾟДﾟ\\)\\[ﾟoﾟ\\]\\)").getMatch(0);

        String out = "";
        while (StringUtils.isNotEmpty(data)) {
            int index = data.indexOf(BLOCK_START_MARKER);
            if (index != 0) {
                throw new WTFException("No AAEncode");
            }
            data = data.substring(BLOCK_START_MARKER.length());
            String encodedBlock = null;
            index = data.indexOf(BLOCK_START_MARKER);
            if (index == -1) {
                encodedBlock = data;
                data = "";
            } else {
                encodedBlock = data.substring(0, index);
                data = data.substring(encodedBlock.length());
            }

            int radix = 8;
            if (encodedBlock.indexOf(HEX_HASH_MARKER) == 0) {
                encodedBlock = encodedBlock.substring(HEX_HASH_MARKER.length());
                radix = 16;
            }
            if (DEBUG) {
                System.out.println("Rad " + radix);
            }
            String uniCodeNumString = this.decodeBlock(encodedBlock, radix);
            if (StringUtils.isEmpty(uniCodeNumString)) {
                throw new WTFException("Bad decoding for " + encodedBlock);
            }
            out += Character.toString((char) Integer.parseInt(uniCodeNumString, radix));
            if (DEBUG) {
                System.out.println(out);
            }
        }
        return out;

    }

    // http://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form
    // public domain
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                this.ch = (++this.pos < str.length()) ? str.charAt(this.pos) : -1;
            }

            boolean eat(int charToEat) {
                while (this.ch == ' ') {
                    this.nextChar();
                }
                if (this.ch == charToEat) {
                    this.nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                this.nextChar();
                double x = this.parseExpression();
                if (this.pos < str.length()) {
                    throw new RuntimeException("Unexpected: " + (char) this.ch);
                }
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            // | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = this.parseTerm();
                for (;;) {
                    if (this.eat('+')) {
                        x += this.parseTerm(); // addition
                    } else if (this.eat('-')) {
                        x -= this.parseTerm(); // subtraction
                    } else if (this.eat('~')) {
                        x -= this.parseTerm(); // subtraction

                    } else {
                        return x;
                    }
                }
            }

            double parseTerm() {
                double x = this.parseFactor();
                for (;;) {
                    if (this.eat('*')) {
                        x *= this.parseFactor(); // multiplication
                    } else if (this.eat('/')) {
                        x /= this.parseFactor(); // division
                    } else if (this.eat('~')) {
                        double pf = this.parseFactor();
                        x = ~(int) Math.floor(x); // division
                    } else {
                        return x;
                    }
                }
            }

            double parseFactor() {
                if (this.eat('+')) {
                    return this.parseFactor(); // unary plus
                }
                if (this.eat('-')) {
                    return -this.parseFactor(); // unary minus
                }
                if (this.eat('~')) {
                    return ~(int) Math.floor(this.parseFactor()); // unary minus
                }
                double x;
                int startPos = this.pos;
                if (this.eat('(')) { // parentheses
                    x = this.parseExpression();
                    this.eat(')');
                } else if ((this.ch >= '0' && this.ch <= '9') || this.ch == '.') { // numbers
                    while ((this.ch >= '0' && this.ch <= '9') || this.ch == '.') {
                        this.nextChar();
                    }
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (this.ch >= 'a' && this.ch <= 'z') { // functions
                    while (this.ch >= 'a' && this.ch <= 'z') {
                        this.nextChar();
                    }
                    String func = str.substring(startPos, this.pos);
                    x = this.parseFactor();
                    if (func.equals("sqrt")) {
                        x = Math.sqrt(x);
                    } else if (func.equals("sin")) {
                        x = Math.sin(Math.toRadians(x));
                    } else if (func.equals("cos")) {
                        x = Math.cos(Math.toRadians(x));
                    } else if (func.equals("tan")) {
                        x = Math.tan(Math.toRadians(x));
                    } else {
                        throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected: " + (char) this.ch);
                }

                if (this.eat('^')) {
                    x = Math.pow(x, this.parseFactor()); // exponentiation
                }

                return x;
            }
        }.parse();
    }

    private String decodeBlock(String encodedBlock, int radix) {
        String org = encodedBlock;
        for (int i = 0; i < BYTES.length; i++) {
            encodedBlock = encodedBlock.replace(BYTES[i], String.valueOf(i));
        }

        StringBuilder exp = new StringBuilder();
        ArrayList<String> expressions = new ArrayList<String>();
        int braceCount = 0;

        for (int i = 0; i < encodedBlock.length(); i++) {
            char c = encodedBlock.charAt(i);

            if (c == '(') {
                if (exp.length() > 0 && braceCount == 0) {
                    expressions.add(exp.toString());
                    exp.setLength(0);
                }
                braceCount++;
                if (!Character.isWhitespace(c)) {
                    exp.append(c);
                }
                continue;
            } else if (c == ')') {
                braceCount--;
                if (!Character.isWhitespace(c)) {
                    exp.append(c);
                }

                continue;
            } else {
                if (!Character.isWhitespace(c)) {
                    exp.append(c);
                } else if (i > 0 && c == ' ' && encodedBlock.charAt(i - 1) == '+') {
                    // block end
                    if (braceCount == 0) {
                        if (exp.length() > 0) {
                            expressions.add(exp.toString());
                            exp.setLength(0);
                        }
                    }

                }
            }
        }
        if (exp.length() > 0) {
            expressions.add(exp.toString());
            exp.setLength(0);
        }

        String ret = "";
        for (String expression : expressions) {
            expression = expression.trim().replaceAll("\\+$", "");

            ret += Integer.toString((int) eval(expression), radix);
        }
        if (DEBUG) {
            System.out.println(ret + " - " + encodedBlock);
        }
        return ret;

    }

}