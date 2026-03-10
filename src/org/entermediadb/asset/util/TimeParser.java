package org.entermediadb.asset.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class TimeParser {


	public Duration parseDuration(String expr) {
	    if (expr == null) {
	        throw new IllegalArgumentException("duration expr is null");
	    }
	    final String s = expr.trim();
	    final String sIso = s.toUpperCase(java.util.Locale.ROOT);

	    // ISO-8601 (Duration) path; do not fall through to shorthand/cron if it looks ISO.
	    if (isLikelyIso8601Duration(sIso)) {
	        try {
	            return Duration.parse(sIso);  // e.g., PT1M, P1DT2H
	        } catch (Exception isoEx) {
	            throw new IllegalArgumentException("Invalid ISO-8601 duration: " + s, isoEx);
	        }
	    }

	    // POSIX 5-field cron path
	    if (looksLikeCron(s)) {
	        return cronToDuration(s, Instant.now());
	    }

	    // Shorthand path (10m, 2h, 1d, 1500, etc.)
	    long millis = this.parse(s);
	    return Duration.ofMillis(millis);
	}

	/**
	 * Shorthand parser to milliseconds.
	 * Supported suffixes:
	 *   'M' = months (30 days each, coarse)  | 'd' = days | 'h' = hours | 'm' = minutes | 's' = seconds
	 * Bare number = milliseconds.
	 */
	public long parse(String inPeriodString) {
	    inPeriodString = inPeriodString.trim();

	    if (inPeriodString.endsWith("ms")) {
	        long val = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 2));
	        return val; // already milliseconds
	    }
	    else if (inPeriodString.endsWith("M")) {
	        long months = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
	        return months * 30L * 24L * 60L * 60L * 1000L;
	    }
	    else if (inPeriodString.endsWith("d")) {
	        long days = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
	        return days * 24L * 60L * 60L * 1000L;
	    }
	    else if (inPeriodString.endsWith("h")) {
	        long hours = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
	        return hours * 60L * 60L * 1000L;
	    }
	    else if (inPeriodString.endsWith("m")) {
	        long min = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
	        return min * 60L * 1000L;
	    }
	    else if (inPeriodString.endsWith("s")) {
	        long sec = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
	        return sec * 1000L;
	    }
	    else {
	        return Long.parseLong(inPeriodString); // bare millis
	    }
	}

	// ===================== Cron support =====================

	private boolean isLikelyIso8601Duration(String sUpper) {
	    // Accepts leading +/-, then P… (Duration requires uppercase)
	    return sUpper.startsWith("P") || sUpper.startsWith("+P") || sUpper.startsWith("-P");
	}

	private boolean looksLikeCron(String s) {
	    // 5 space-separated fields (minute hour day-of-month month day-of-week)
	    String[] f = s.split("\\s+");
	    return f.length == 5;
	}

	private Duration cronToDuration(String expr, Instant now) {
	    CronSpec spec = parseCron(expr);

	    ZoneId zone = ZoneId.systemDefault();
	    ZonedDateTime z = ZonedDateTime.ofInstant(now, zone)
	            .withSecond(0).withNano(0)
	            .plusMinutes(1); // strictly after 'now'

	    // Field-wise advancement for performance
	    for (;;) {
	        // Month
	        if (!spec.months.get(z.getMonthValue())) {
	            z = advanceMonth(z, spec.months);
	            continue;
	        }

	        // Day (POSIX: DOM and DOW are OR unless one is '*')
	        boolean domMatch = spec.domAny || spec.dom.get(z.getDayOfMonth());
	        int dow = z.getDayOfWeek().getValue() % 7; // Java: Mon=1..Sun=7 -> 0=Sun..6=Sat
	        boolean dowMatch = spec.dowAny || spec.dow.get(dow);

	        boolean dayOk;
	        if (spec.domAny && spec.dowAny) {
	            dayOk = true; // both wildcards
	        } else if (spec.domAny) {
	            dayOk = dowMatch;
	        } else if (spec.dowAny) {
	            dayOk = domMatch;
	        } else {
	            dayOk = domMatch || dowMatch;
	        }
	        if (!dayOk) {
	            z = z.plusDays(1).withHour(0).withMinute(0);
	            continue;
	        }

	        // Hour
	        if (!spec.hours.get(z.getHour())) {
	            z = advanceHour(z, spec.hours);
	            continue;
	        }

	        // Minute
	        if (!spec.minutes.get(z.getMinute())) {
	            z = advanceMinute(z, spec.minutes);
	            continue;
	        }

	        // All matched
	        break;
	    }

	    return Duration.between(now, z.toInstant());
	}

	// ---------- Parsing into sets ----------

	private static final class CronSpec {
	    final BitSet minutes = new BitSet(60);   // 0-59
	    final BitSet hours   = new BitSet(24);   // 0-23
	    final BitSet dom     = new BitSet(32);   // 1-31
	    final BitSet months  = new BitSet(13);   // 1-12
	    final BitSet dow     = new BitSet(7);    // 0-6 (0=Sun)
	    boolean domAny;
	    boolean dowAny;
	}

	private CronSpec parseCron(String expr) {
	    String[] f = expr.trim().toUpperCase().split("\\s+");
	    if (f.length != 5) {
	        throw new IllegalArgumentException("Invalid cron (need 5 fields): " + expr);
	    }

	    Map<String,Integer> MONTHS = new HashMap<>();
	    MONTHS.put("JAN",1); MONTHS.put("FEB",2); MONTHS.put("MAR",3); MONTHS.put("APR",4);
	    MONTHS.put("MAY",5); MONTHS.put("JUN",6); MONTHS.put("JUL",7); MONTHS.put("AUG",8);
	    MONTHS.put("SEP",9); MONTHS.put("OCT",10); MONTHS.put("NOV",11); MONTHS.put("DEC",12);

	    Map<String,Integer> DOW = new HashMap<>();
	    DOW.put("SUN",0); DOW.put("MON",1); DOW.put("TUE",2); DOW.put("WED",3);
	    DOW.put("THU",4); DOW.put("FRI",5); DOW.put("SAT",6);

	    CronSpec spec = new CronSpec();

	    // minute, hour, DOM, month, DOW
	    parseFieldInto(f[0], 0, 59, false, false, null,    spec.minutes, null);
	    parseFieldInto(f[1], 0, 23, false, false, null,    spec.hours,   null);
	    parseFieldInto(f[2], 1, 31, false, false, null,    spec.dom,     (any) -> spec.domAny = any);
	    parseFieldInto(f[3], 1, 12, true,  false, MONTHS,  spec.months,  null);
	    parseFieldInto(f[4], 0,  6, true,  true,  DOW,     spec.dow,     (any) -> spec.dowAny = any);

	    // Ensure non-empty
	    if (spec.minutes.isEmpty() || spec.hours.isEmpty() || spec.months.isEmpty()
	            || (spec.dom.isEmpty() && !spec.domAny) || (spec.dow.isEmpty() && !spec.dowAny)) {
	        throw new IllegalArgumentException("Cron field resolved to empty set: " + expr);
	    }
	    return spec;
	}

	/**
	 * Parses a cron field into a BitSet.
	 *
	 * @param token      field text
	 * @param min        inclusive minimum
	 * @param max        inclusive maximum
	 * @param allowWrap  allow ranges that wrap (e.g., NOV-FEB or FRI-SUN)
	 * @param dowMode    treat numbers '7' as '0' and support wrap around 7→0
	 * @param aliases    optional name→number map (e.g., JAN, MON)
	 * @param out        destination BitSet
	 * @param wildcardCb optional callback to receive whether field was exactly "*"
	 */
	private void parseFieldInto(
	        String token,
	        int min, int max,
	        boolean allowWrap,
	        boolean dowMode,
	        Map<String,Integer> aliases,
	        BitSet out,
	        java.util.function.Consumer<Boolean> wildcardCb
	) {
	    out.clear();
	    boolean isWildcard = token.equals("*");
	    if (wildcardCb != null) wildcardCb.accept(isWildcard);

	    if (isWildcard) {
	        fillRange(out, min, max, 1);
	        return;
	    }

	    for (String part : token.split(",")) {
	        String p = part.trim();
	        if (p.isEmpty()) continue;

	        int step = 1;
	        String range = p;
	        int slash = p.indexOf('/');
	        if (slash >= 0) {
	            range = p.substring(0, slash);
	            String stepStr = p.substring(slash + 1);
	            step = Integer.parseInt(stepStr);
	            if (step <= 0) throw new IllegalArgumentException("Invalid step: " + p);
	        }

	        int start, end;
	        if (range.equals("*")) {
	            start = min; end = max;
	        } else {
	            int dash = range.indexOf('-');
	            if (dash >= 0) {
	                String aStr = range.substring(0, dash);
	                String bStr = range.substring(dash + 1);
	                start = parseCronValue(aStr, aliases, dowMode);
	                end   = parseCronValue(bStr, aliases, dowMode);
	            } else {
	                start = parseCronValue(range, aliases, dowMode);
	                end   = start;
	            }
	        }

	        // Normalize and clamp
	        if (dowMode) {
	            start = normalizeDow(start);
	            end   = normalizeDow(end);
	        }
	        start = clamp(start, min, max);
	        end   = clamp(end,   min, max);

	        if (allowWrap && end < start) {
	            // Wrap around (e.g., NOV-FEB or FRI-SUN)
	            fillRange(out, start, max, step);
	            fillRange(out, min, end, step);
	        } else {
	            fillRange(out, start, end, step);
	        }
	    }
	}

	private int parseCronValue(String s, Map<String,Integer> aliases, boolean dowMode) {
	    String t = s.trim();
	    if (aliases != null && aliases.containsKey(t)) {
	        return aliases.get(t);
	    }
	    int v = Integer.parseInt(t);
	    if (dowMode && v == 7) v = 0; // 7 == Sunday -> 0
	    return v;
	}

	// ---------- Advancement helpers ----------

	private int clamp(int v, int min, int max) {
	    if (v < min) return min;
	    if (v > max) return max;
	    return v;
	}

	private void fillRange(BitSet bs, int start, int end, int step) {
	    for (int i = start; i <= end; i += step) {
	        bs.set(i);
	    }
	}

	private ZonedDateTime advanceMonth(ZonedDateTime z, BitSet months) {
	    int m = z.getMonthValue();
	    int next = months.nextSetBit(m + 1);
	    if (next < 0) {
	        int first = months.nextSetBit(1);
	        z = z.plusYears(1).withMonth(first).withDayOfMonth(1).withHour(0).withMinute(0);
	    } else {
	        z = z.withMonth(next).withDayOfMonth(1).withHour(0).withMinute(0);
	    }
	    return z;
	}

	private ZonedDateTime advanceHour(ZonedDateTime z, BitSet hours) {
	    int h = z.getHour();
	    int next = hours.nextSetBit(h + 1);
	    if (next < 0) {
	        int first = hours.nextSetBit(0);
	        z = z.plusDays(1).withHour(first).withMinute(0);
	    } else {
	        z = z.withHour(next).withMinute(0);
	    }
	    return z;
	}

	private ZonedDateTime advanceMinute(ZonedDateTime z, BitSet minutes) {
	    int m = z.getMinute();
	    int next = minutes.nextSetBit(m + 1);
	    if (next < 0) {
	        int first = minutes.nextSetBit(0);
	        z = z.plusHours(1).withMinute(first);
	    } else {
	        z = z.withMinute(next);
	    }
	    return z;
	}

	private int normalizeDow(int v) {
	    // 0=Sun, 1=Mon, ..., 6=Sat
	    if (v == 7) return 0;
	    return v;
	}
}
