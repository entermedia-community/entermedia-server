package org.openedit.entermedia.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RFC3986 {
    private static final Pattern ptn = Pattern.compile("%[A-Fa-f0-9]{2}");
    private static final String character = "range [";
    private static final String overRange = "over";
    private static final String string = "text";
    private static final String containInvalidEncode = "] Invalid";

    private static String dec2hex2(int charcode) {
        String[] hexequiv = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        return hexequiv[(charcode >> 4) & 0xF] + hexequiv[charcode & 0xF];
    }

    private static String dec2char(int n) {
        if (n < 0xFFFF) {
            return String.valueOf(Character.toChars(n));
        } else if (n <= 0x10FFF) {
            StringBuilder sb = new StringBuilder();
            sb.append(Character.toChars(0xD800 | (n >> 10))).append(0xDC00 | (n & 0x3FF));
            return sb.toString();
        }
        throw new IllegalArgumentException(character + n + overRange);
    }

    public static String encode(String textString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < textString.length(); i++) {
            int n = textString.codePointAt(i);
            if (n == 0x20) {
                sb.append("%20");
            } else if ((n >= 0x41 && n <= 0x5A) || (n >= 0x61 && n <= 0x7A) || (n >= 0x30 && n <= 0x39)
                    || n == 0x2D || n == 0x2E || n == 0x5F || n == 0x7E) {
                sb.append(textString.charAt(i));
            } else if (n <= 0x7F) {
                sb.append('%').append(dec2hex2(n));
            } else if (n <= 0x7FF) {
                sb.append('%').append(dec2hex2(0xC0 | ((n >> 6) & 0x1F)))
                        .append('%').append(dec2hex2(0x80 | (n & 0x3F)));
            } else if (n <= 0xFFFF) {
                sb.append('%').append(dec2hex2(0xE0 | ((n >> 12) & 0x0F)))
                        .append('%').append(dec2hex2(0x80 | ((n >> 6) & 0x3F)))
                        .append('%').append(dec2hex2(0x80 | (n & 0x3F)));
            } else if (n <= 0x10FFFF) {
                sb.append('%').append(dec2hex2(0xF0 | ((n >> 18) & 0x07)))
                        .append('%').append(dec2hex2(0x80 | ((n >> 12) & 0x3F)))
                        .append('%').append(dec2hex2(0x80 | ((n >> 6) & 0x3F)))
                        .append('%').append(dec2hex2(0x80 | (n & 0x3F)));
            }
        }
        return sb.toString();
    }

    public static String decode(String urlString) {
        StringBuilder sb = new StringBuilder();
        Matcher m = ptn.matcher(urlString);
        int pos = 0, counter = 0, n = 0;
        while (m.find()) {
            sb.append(urlString.substring(pos, m.start()));
            pos = m.end();
            String code = urlString.substring(m.start() + 1, m.end());
            Integer b = Integer.parseInt(code, 16);
            switch (counter) {
                case 0:
                    if (0 <= b && b <= 0x7F) {
                        sb.append(dec2char(b));
                    } else if (0xC0 <= b && b <= 0xDF) {
                        counter = 1;
                        n = b & 0x1F;
                    } else if (0xE0 <= b && b <= 0xEF) {  // 1110xxxx
                        counter = 2;
                        n = b & 0xF;
                    } else if (0xF0 <= b && b <= 0xF7) {  // 11110xxx
                        counter = 3;
                        n = b & 0x7;
                    } else {
                        throw new IllegalArgumentException(string.concat(urlString).concat(containInvalidEncode));
                    }
                    break;
                case 1:
                    if (b < 0x80 || b > 0xBF) {
                        throw new IllegalArgumentException(string.concat(urlString).concat(containInvalidEncode));
                    }
                    counter--;
                    sb.append(dec2char((n << 6) | (b - 0x80)));
                    n = 0;
                    break;
                case 2:
                case 3:
                    if (b < 0x80 || b > 0xBF) {
                        throw new IllegalArgumentException(string.concat(urlString).concat(containInvalidEncode));
                    }
                    n = (n << 6) | (b - 0x80);
                    counter--;
                    break;
            }

        }
        if (pos < urlString.length()) {
            sb.append(urlString.substring(pos));
        }
        return sb.toString();
    }
    
    public static Map<String, String> getQueryMap(String url)
    {
    	Map<String, String> map = new HashMap<String, String>();
    	String[] urlparts = url.split("\\?");
    	if (urlparts.length>1 && urlparts[1] != null) {
	        String[] params = urlparts[1].split("&");
	        for (String param : params)
	        {
	            String name = param.split("=")[0];
	            String value = param.split("=")[1];
	            map.put(name, value);
	        }
    	}
        return map;
    }
    
    
}