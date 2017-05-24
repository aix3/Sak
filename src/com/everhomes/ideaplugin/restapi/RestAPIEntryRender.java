package com.everhomes.ideaplugin.restapi;

import com.everhomes.ideaplugin.restapi.bean.RestAPIEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by xq.tian on 2017/5/23.
 */
public class RestAPIEntryRender {

    public String evaluate(List<RestAPIEntry> entries) {
        StringBuilder sb = new StringBuilder(1000);
        for (RestAPIEntry entry : entries) {
            evaluate(entry, sb);
        }
        return sb.toString();
    }

    private void evaluate(RestAPIEntry entry, StringBuilder sb) {
        sb.append("<li>").append(entry.getFieldName()).append(": ").append(entry.getFieldDesc()).append("</li>").append("\r\n");
        if (entry.isLink()) {
            sb.append("<ul>");
            for (RestAPIEntry e : entry.getLinkTo()) {
                evaluate(e, sb);
            }
            sb.append("</ul>");
        }
    }

    public String evaluateMkd(List<RestAPIEntry> entries) {
        StringBuilder sb = new StringBuilder(1000);
        for (RestAPIEntry entry : entries) {
            evaluateMkd(entry, sb, 1);
        }
        return sb.toString();
    }

    private void evaluateMkd(RestAPIEntry entry, StringBuilder sb, int prefixCount) {
        String prefix = getPrefix(prefixCount);
        sb.append(prefix).append(entry.getFieldName()).append(": ").append(entry.getFieldDesc()).append("<br>");
        if (entry.isLink()) {
            prefixCount++;
            for (RestAPIEntry e : entry.getLinkTo()) {
                evaluateMkd(e, sb, prefixCount);
            }
        }
    }

    @NotNull
    private String getPrefix(int prfixCount) {
        String prefix = "";
        for (int i = 1; i < prfixCount; i++) {
            prefix += "&nbsp;&nbsp;";
        }
        prefix += "* ";
        return prefix;
    }
}
