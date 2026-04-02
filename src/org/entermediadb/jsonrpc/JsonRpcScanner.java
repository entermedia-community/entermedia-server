package org.entermediadb.jsonrpc;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.util.JSONParser;

public class JsonRpcScanner implements Closeable
{
	protected final BufferedReader fieldReader;
	protected final JSONParser fieldParser;
	protected String fieldPendingLine;

	public JsonRpcScanner(InputStream inInput, String inCharset)
	{
		if (inInput == null)
		{
			throw new OpenEditException("Input stream cannot be null");
		}
		Charset charset = Charset.forName(inCharset);
		fieldReader = new BufferedReader(new InputStreamReader(inInput, charset));
		fieldParser = new JSONParser();
	}

	public boolean hasNextLine()
	{
		if (fieldPendingLine != null)
		{
			return true;
		}
		try
		{
			fieldPendingLine = fieldReader.readLine();
			return fieldPendingLine != null;
		}
		catch (IOException e)
		{
			throw new OpenEditException("Failed reading MCP stream", e);
		}
	}

	@SuppressWarnings("unchecked")
	public JSONObject nextEvent()
	{
		while (hasNextLine())
		{
			String firstLine = consumeLine();
			if (firstLine == null)
			{
				return null;
			}

			if (firstLine.length() == 0 || firstLine.startsWith(":"))
			{
				continue;
			}

			String eventName = null;
			StringBuilder data = new StringBuilder();
			processLine(firstLine, data);
			if (firstLine.startsWith("event:"))
			{
				eventName = firstLine.substring(6).trim();
			}

			while (hasNextLine())
			{
				String line = consumeLine();
				if (line == null || line.length() == 0)
				{
					break;
				}
				if (line.startsWith(":"))
				{
					continue;
				}
				if (line.startsWith("event:"))
				{
					eventName = line.substring(6).trim();
					continue;
				}
				processLine(line, data);
			}

			if (data.length() == 0)
			{
				continue;
			}

			JSONObject event = parseEventPayload(data.toString());
			if (eventName != null && !eventName.isEmpty())
			{
				event.put("_event", eventName);
			}
			return event;
		}
		return null;
	}

	protected void processLine(String inLine, StringBuilder inData)
	{
		String value = inLine;
		if (inLine.startsWith("data:"))
		{
			value = inLine.substring(5).trim();
		}

		if (value.length() == 0)
		{
			return;
		}

		if (inData.length() > 0)
		{
			inData.append('\n');
		}
		inData.append(value);
	}

	@SuppressWarnings("unchecked")
	protected JSONObject parseEventPayload(String inPayload)
	{
		try
		{
			Object parsed = fieldParser.parse(inPayload);
			if (parsed instanceof JSONObject)
			{
				return (JSONObject) parsed;
			}
			if (parsed instanceof JSONArray)
			{
				JSONObject wrapped = new JSONObject();
				wrapped.put("data", parsed);
				return wrapped;
			}
		}
		catch (Exception e)
		{
			// Non-JSON payloads (for example endpoint URLs) are passed through.
		}

		JSONObject wrapped = new JSONObject();
		wrapped.put("data", inPayload);
		return wrapped;
	}

	protected String consumeLine()
	{
		if (fieldPendingLine != null)
		{
			String value = fieldPendingLine;
			fieldPendingLine = null;
			return value;
		}
		try
		{
			return fieldReader.readLine();
		}
		catch (IOException e)
		{
			throw new OpenEditException("Failed reading MCP stream", e);
		}
	}

	@Override
	public void close() throws IOException
	{
		fieldReader.close();
	}
}
