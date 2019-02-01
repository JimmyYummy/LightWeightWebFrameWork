package edu.upenn.cis.cis455.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.implementations.BasicRequestHandler;

public abstract class DateTimeUtil {
	final static Logger logger = LogManager.getLogger(DateTimeUtil.class);

	private static DateTimeFormatter formater1 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");
	private static DateTimeFormatter formater2 = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz");
	private static DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy zzz");


	public static String getDate() {
    	ZoneId zone = ZoneId.of("GMT");
    	ZonedDateTime zonedDT = ZonedDateTime.of(LocalDateTime.now(), zone);
    	return zonedDT.format(DateTimeFormatter.RFC_1123_DATE_TIME);
	}
	
	public static ZonedDateTime parseDate(String dateStr) {

    	try {
    		return ZonedDateTime.parse(dateStr, formater1);
    	} catch (DateTimeParseException e) {
    	}
    	try {
    		return ZonedDateTime.parse(dateStr, formater2);
    	} catch (DateTimeParseException e) {
    	}
    	try {
    		return ZonedDateTime.parse(dateStr + " GMT", formatter3);
    	} catch (DateTimeParseException e) {
    	}
    	logger.info("Failed parsing date: " + dateStr);
    	return null;	
    }

}
