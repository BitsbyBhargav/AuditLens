package com.auditlens.portal.web;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Used in templates as ${@dates.format(...)}. */
@Component("dates")
public class Dates {
    private static final DateTimeFormatter F =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.of("Asia/Kolkata"));

    public String format(Instant i) { return i == null ? "" : F.format(i); }

    public String waiting(Instant since) {
        if (since == null) return "";
        long mins = Duration.between(since, Instant.now()).toMinutes();
        if (mins < 1) return "just now";
        if (mins < 60) return mins + " min";
        long hours = mins / 60;
        if (hours < 48) return hours + " h";
        return (hours / 24) + " days";
    }
}
