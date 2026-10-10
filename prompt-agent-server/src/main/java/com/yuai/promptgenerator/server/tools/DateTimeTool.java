package com.yuai.promptgenerator.server.tools;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class DateTimeTool {
    @Tool(description = "Return the current date and time for an IANA time zone")
    public String currentDateTime(@ToolParam(description = "IANA time zone such as Asia/Shanghai") String zoneId) {
        try {
            return ZonedDateTime.now(java.time.ZoneId.of(zoneId)).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
        catch (ZoneRulesException exception) {
            return "Invalid time zone: " + zoneId;
        }
    }
}
