package org.entermediadb.controller;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.util.TimeParser;

public class TimeParserTest extends BaseEnterMediaTest
{

	private static final Log log = LogFactory.getLog(TimeParserTest.class);

	// --- Helpers ---
	private static void assertDurationMillis(Duration d, long expectedMs)
	{
		assertNotNull("Duration should not be null", d);
		assertEquals("Duration millis mismatch", expectedMs, d.toMillis());
	}

	// ---------------- ISO-8601 Duration.parse() path ----------------

	public void testIsoMinutesHoursDays()
	{
		TimeParser p = new TimeParser();

		assertDurationMillis(p.parseDuration("PT10M"), 10L * 60_000);
		assertDurationMillis(p.parseDuration("PT2H"), 2L * 3_600_000);
		assertDurationMillis(p.parseDuration("P1D"), 1L * 86_400_000);

		// Whitespace tolerant
		assertDurationMillis(p.parseDuration("  PT15M  "), 15L * 60_000);
	}

	public void testIsoZeroAndNegative()
	{
		TimeParser p = new TimeParser();

		assertDurationMillis(p.parseDuration("PT0S"), 0L);

		// Java Duration supports leading '-' formats like "-PT5M"
		assertDurationMillis(p.parseDuration("-PT5M"), -5L * 60_000);
	}

	public void testIsoUnsupportedMonthShouldFail()
	{
		TimeParser p = new TimeParser();
		try
		{
			p.parseDuration("P1M"); // months are not supported by java.time.Duration
			fail("Expected exception for P1M (months not supported in Duration)");
		}
		catch (Exception expected)
		{
			// ok
		}
	}

	// ---------------- Shorthand fallback via TimeParser.parse(String) ----------------

	public long parse(String inPeriodString)
	{
		inPeriodString = inPeriodString.trim();
		if (inPeriodString.isEmpty())
		{
			throw new IllegalArgumentException("empty period string");
		}

		if (inPeriodString.endsWith("ms"))
		{
			long val = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 2));
			return val;
		}
		else if (inPeriodString.endsWith("M"))
		{ // months (coarse)
			long months = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			return months * 30L * 24L * 60L * 60L * 1000L;
		}
		else if (inPeriodString.endsWith("d"))
		{
			long days = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			return days * 24L * 60L * 60L * 1000L;
		}
		else if (inPeriodString.endsWith("h"))
		{
			long hours = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			return hours * 60L * 60L * 1000L;
		}
		else if (inPeriodString.endsWith("m"))
		{
			long min = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			return min * 60L * 1000L;
		}
		else if (inPeriodString.endsWith("s"))
		{
			long sec = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			return sec * 1000L;
		}
		else
		{
			return Long.parseLong(inPeriodString); // bare millis
		}
	}

	public void testShorthandLargeValuesNoOverflow()
	{
		TimeParser p = new TimeParser();

		// 365 days
		assertEquals(365L * 86_400_000, p.parse("365d"));

		// 1440 minutes = 1 day
		assertEquals(86_400_000L, p.parse("1440m"));
	}

	public void testShorthandInvalidsThrow()
	{
		TimeParser p = new TimeParser();

		String[] bad = { "", " ", "abc", "PT", "m10", "*/15", "10x", "1q", "9999999999999999999999m" };
		for (String s : bad)
		{
			try
			{
				p.parse(s);
				fail("Expected parse() to throw for: '" + s + "'");
			}
			catch (Exception expected)
			{
				// ok
			}
		}
	}

	// ---------------- Integration: parseDuration() fallback into TimeParser.parse() ----------------

	public void testParseDurationFallsBackToShorthand()
	{
		TimeParser p = new TimeParser();

		assertDurationMillis(p.parseDuration("10m"), 10L * 60_000);
		assertDurationMillis(p.parseDuration("2h"), 2L * 3_600_000);
		assertDurationMillis(p.parseDuration("1d"), 1L * 86_400_000);
	}

	public void testParseDurationInvalidsThrow()
	{
		TimeParser p = new TimeParser();

		String[] bad = { "", " ", "abc", "P", "PT", "m10", "*/5" };
		for (String s : bad)
		{
			try
			{
				p.parseDuration(s);
				fail("Expected parseDuration() to throw for: '" + s + "'");
			}
			catch (Exception expected)
			{
				// ok
			}
		}
	}

	// ---------------- Edge handling ----------------

	public void testTrimsAndNormalizesWhitespace()
	{
		TimeParser p = new TimeParser();

		assertEquals(60_000L, p.parse("  1m "));
		assertDurationMillis(p.parseDuration("  PT1M "), 60_000L);
	}

	public void testNegativeShorthandShouldThrowOrReturnNegativeConsistently()
	{
		TimeParser p = new TimeParser();
		// Define expected behavior: if negatives are unsupported in shorthand, an exception should be thrown.
		// If your TimeParser supports negatives, replace with assertEquals(-300000L, p.parse("-5m"));
		try
		{
			p.parse("-5m");
			// If it didn't throw, enforce consistent behavior via duration path too:
			assertDurationMillis(p.parseDuration("-5m"), -5L * 60_000);
		}
		catch (Exception okIfUnsupported)
		{
			// ok
		}
	}
}
