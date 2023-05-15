package com.revnomix.revseed.Util;

public class StaahXmlUtil {


    public static String removeRemarks(String reservations) {
        for (; ; ) {
            try {
                String str = reservations.substring(reservations.indexOf("<remarks>"), reservations.indexOf("</remarks>") + 10);
                reservations = reservations.replace(str, "");
            } catch (Exception e) {
                break;
            }
        }
        return reservations;
    }
    public static String removeComment(String reservations) {
        for (; ; ) {
            try {
                String str = reservations.substring(reservations.indexOf("<Comment>"), reservations.indexOf("</Comment>") + 10);
                reservations = reservations.replace(str, "");
            } catch (Exception e) {
                break;
            }
        }
        return reservations;
    }
    
    public static String removeAmpercent (String xmlStr) {
    	return xmlStr.replaceAll("&(?!amp;)", "&amp;");
    }


}