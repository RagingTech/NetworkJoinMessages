package xyz.earthcow.networkjoinmessages.util;

import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class HexChat {

	public static final Pattern HEX_PATTERN = Pattern.compile("&#(\\w{5}[0-9a-f])");
	
	public static String translateHexCodes(String message) {
		// Translate RGB codes
		return ChatColor.translateAlternateColorCodes('&', message.replaceAll("(?i)\\&(x|#)([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])", "&x&$2&$3&$4&$5&$6&$7"));
	}

//	public static String translateHexCodes (String textToTranslate) {
//
//	        Matcher matcher = HEX_PATTERN.matcher(textToTranslate);
//	        StringBuffer buffer = new StringBuffer();
//
//	        while(matcher.find()) {
//	            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
//	        }
//
//	        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
//	    }
}
