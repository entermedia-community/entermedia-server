/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.entermediadb.video.VTT.webvtt;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.video.VTT.Cue;
import org.openedit.OpenEditException;

/**
 * A simple WebVTT parser.
 * <p>
 * @see <a href="http://dev.w3.org/html5/webvtt">WebVTT specification</a>
 */
public final class WebvttParser {

	private static final Log log = LogFactory.getLog(WebvttParser.class);

	
	
  private static final String TAG = "WebvttParser";

  private static final String WEBVTT_FILE_HEADER_STRING = "^\uFEFF?WEBVTT((\\u0020|\u0009).*)?$";
  private static final Pattern WEBVTT_FILE_HEADER =
      Pattern.compile(WEBVTT_FILE_HEADER_STRING);

  private static final String WEBVTT_METADATA_HEADER_STRING = "\\S*[:=]\\S*";
  private static final Pattern WEBVTT_METADATA_HEADER =
      Pattern.compile(WEBVTT_METADATA_HEADER_STRING);

  private static final String WEBVTT_CUE_IDENTIFIER_STRING = "^(?!.*(-->)).*$";
  private static final Pattern WEBVTT_CUE_IDENTIFIER =
      Pattern.compile(WEBVTT_CUE_IDENTIFIER_STRING);

  private static final String WEBVTT_TIMESTAMP_STRING = "(\\d+:)?[0-5]\\d:[0-5]\\d\\.\\d{3}";
  private static final Pattern WEBVTT_TIMESTAMP = Pattern.compile(WEBVTT_TIMESTAMP_STRING);

  private static final String WEBVTT_CUE_SETTING_STRING = "\\S*:\\S*";
  private static final Pattern WEBVTT_CUE_SETTING = Pattern.compile(WEBVTT_CUE_SETTING_STRING);

  private static final String NON_NUMERIC_STRING = ".*[^0-9].*";

  private final StringBuilder textBuilder;

  private final boolean strictParsing;

  /**
   * Equivalent to {@code WebvttParser(false)}.
   */
  public WebvttParser() {
    this(false);
  }

  /**
   * @param strictParsing If true, {@link #parse(InputStream)} will throw a {@link OpenEditException}
   *     if the stream contains invalid data. If false, the parser will make a best effort to ignore
   *     minor errors in the stream. Note however that a {@link OpenEditException} will still be
   *     thrown when this is not possible.
   */
  public WebvttParser(boolean strictParsing) {
    this.strictParsing = strictParsing;
    textBuilder = new StringBuilder();
  }

  
  public final WebvttSubtitle parse(InputStream inputStream) throws IOException {
    ArrayList<WebvttCue> subtitles = new ArrayList<>();

    BufferedReader webvttData = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    String line;

    // file should start with "WEBVTT"
    line = webvttData.readLine();
    if (line == null || !WEBVTT_FILE_HEADER.matcher(line).matches()) {
      throw new OpenEditException("Expected WEBVTT. Got " + line);
    }

    // parse the remainder of the header
    while (true) {
      line = webvttData.readLine();
      if (line == null) {
        // we reached EOF before finishing the header
        throw new OpenEditException("Expected an empty line after webvtt header");
      } else if (line.isEmpty()) {
        // we've read the newline that separates the header from the body
        break;
      }

      if (strictParsing) {
        Matcher matcher = WEBVTT_METADATA_HEADER.matcher(line);
        if (!matcher.find()) {
          throw new OpenEditException("Unexpected line: " + line);
        }
      }
    }

    // process the cues and text
    while ((line = webvttData.readLine()) != null) {

      // parse the cue identifier (if present) {
      Matcher matcher = WEBVTT_CUE_IDENTIFIER.matcher(line);
      if (matcher.find()) {
        // ignore the identifier (we currently don't use it) and read the next line
        line = webvttData.readLine();
      }

      long startTime = Cue.UNSET_VALUE;
      long endTime = Cue.UNSET_VALUE;
      CharSequence text = null;
      int lineNum = Cue.UNSET_VALUE;
      int position = Cue.UNSET_VALUE;
      String alignment = null;
      int size = Cue.UNSET_VALUE;

      // parse the cue timestamps
      matcher = WEBVTT_TIMESTAMP.matcher(line);

      // parse start timestamp
      if (!matcher.find()) {
        throw new OpenEditException("Expected cue start time: " + line);
      } else {
        startTime = parseTimestampUs(matcher.group());
      }

      // parse end timestamp
      String endTimeString;
      if (!matcher.find()) {
        throw new OpenEditException("Expected cue end time: " + line);
      } else {
        endTimeString = matcher.group();
        endTime = parseTimestampUs(endTimeString);
      }

      // parse the (optional) cue setting list
      line = line.substring(line.indexOf(endTimeString) + endTimeString.length());
      matcher = WEBVTT_CUE_SETTING.matcher(line);
      while (matcher.find()) {
        String match = matcher.group();
        String[] parts = match.split(":", 2);
        String name = parts[0];
        String value = parts[1];

        try {
          if ("line".equals(name)) {
            if (value.endsWith("%")) {
              lineNum = parseIntPercentage(value);
            } else if (value.matches(NON_NUMERIC_STRING)) {
              log.info( "Invalid line value: " + value);
            } else {
              lineNum = Integer.parseInt(value);
            }
          } else if ("align".equals(name)) {
            // TODO: handle for RTL languages
            if ("start".equals(value)) {
              alignment = "start";
            } else if ("middle".equals(value)) {
              alignment ="middle";
            } else if ("end".equals(value)) {
              alignment = "end";
            } else if ("left".equals(value)) {
              alignment = "left";
            } else if ("right".equals(value)) {
              alignment = "right";
            } else {
              log.info( "Invalid align value: " + value);
            }
          } else if ("position".equals(name)) {
            position = parseIntPercentage(value);
          } else if ("size".equals(name)) {
            size = parseIntPercentage(value);
          } else {
            log.info("Unknown cue setting " + name + ":" + value);
          }
        } catch (NumberFormatException e) {
          log.info( " contains an invalid value " + value, e);
        }
      }

      // parse text
      textBuilder.setLength(0);
      while (((line = webvttData.readLine()) != null) && (!line.isEmpty())) {
        if (textBuilder.length() > 0) {
          textBuilder.append("<br>");
        }
        textBuilder.append(line.trim());
      }
      text =textBuilder.toString();

      WebvttCue cue = new WebvttCue(startTime, endTime, text, lineNum, position, alignment, size);
      subtitles.add(cue);
    }

    return new WebvttSubtitle(subtitles);
  }

  
  

  private static int parseIntPercentage(String s) throws NumberFormatException {
    if (!s.endsWith("%")) {
      throw new NumberFormatException(s + " doesn't end with '%'");
    }

    s = s.substring(0, s.length() - 1);
    if (s.matches(NON_NUMERIC_STRING)) {
      throw new NumberFormatException(s + " contains an invalid character");
    }

    int value = Integer.parseInt(s);
    if (value < 0 || value > 100) {
      throw new NumberFormatException(value + " is out of range [0-100]");
    }
    return value;
  }

  private static long parseTimestampUs(String s) throws NumberFormatException {
    if (!s.matches(WEBVTT_TIMESTAMP_STRING)) {
      throw new NumberFormatException("has invalid format");
    }

    String[] parts = s.split("\\.", 2);
    long value = 0;
    for (String group : parts[0].split(":")) {
      value = value * 60 + Long.parseLong(group);
    }
    long finalval = (value * 1000 + Long.parseLong(parts[1])) * 1000;
    return finalval;
  }

}
