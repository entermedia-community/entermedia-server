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
package org.entermediadb.video.VTT;



/**
 * Contains information about a specific cue, including textual content and formatting data.
 */
public class Cue {

  /**
   * Used by some methods to indicate that no value is set.
   */
  public static final int UNSET_VALUE = -1;

  public  CharSequence text;
  public  int fieldLine;
  public  int fieldPosition;  //percentage of the width of the video viewpoint
  public  long fieldStartTime;
  public  long fieldEndTime;
  public  String fieldAlignment;  //Text aligment
  public  int fieldSize; //How big of a font to use
  
  
  
  public long getStartTime()
{
	return fieldStartTime;
}

public void setStartTime(long inStartTime)
{
	fieldStartTime = inStartTime;
}

public long getEndTime()
{
	return fieldEndTime;
}

public void setEndTime(long inEndTime)
{
	fieldEndTime = inEndTime;
}

  
  public String getAlignment()
{
	return fieldAlignment;
}

public void setAlignment(String inAlignment)
{
		fieldAlignment = inAlignment;
	}


  public CharSequence getText()
{
	return text;
}

public void setText(CharSequence inText)
{
	text = inText;
}

public int getLine()
{
	return fieldLine;
}

public void setLine(int inLine)
{
	fieldLine = inLine;
}

public int getPosition()
{
	return fieldPosition;
}

public void setPosition(int inPosition)
{
	fieldPosition = inPosition;
}

public int getSize()
{
	return fieldSize;
}

public void setSize(int inSize)
{
	fieldSize = inSize;
}

public Cue() {
    this(null);
  }

  public Cue(CharSequence text) {
    this(text, UNSET_VALUE, UNSET_VALUE, null, UNSET_VALUE);
  }

  public Cue(CharSequence text, int line, int position, String alignment, int size) {
    this.text = text;
    this.fieldLine = line;
    this.fieldPosition = position;
    this.fieldAlignment = alignment;
    this.fieldSize = size;
    this.fieldStartTime = fieldStartTime;
    this.fieldEndTime = fieldEndTime;
  }

}
