package com.everhomes.sak.restapi;

import com.everhomes.sak.restapi.bean.RestAPIEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by xq.tian on 2017/5/23.
 */
public class RestAPIEntryRender {

    public String evaluateHtml(List<RestAPIEntry> entries) {
        StringBuilder sb = new StringBuilder(1000);
        for (RestAPIEntry entry : entries) {
            evaluateHtml(entry, sb);
        }
        return sb.toString();
    }

    private void evaluateHtml(RestAPIEntry entry, StringBuilder sb) {
        sb.append("<li>").append(entry.getFieldName()).append(": ").append(entry.getFieldDesc()).append("</li>").append("\r\n");
        if (entry.isLink()) {
            sb.append("<ul>");
            for (RestAPIEntry e : entry.getLinkTo()) {
                evaluateHtml(e, sb);
            }
            sb.append("</ul>");
        }
    }

    public String evaluateMarkdown(List<RestAPIEntry> entries) {
        StringBuilder sb = new StringBuilder(1000);
        for (RestAPIEntry entry : entries) {
            evaluateMarkdown(entry, sb, 1);
        }
        if (sb.length() > 2) {
            return sb.substring(0, sb.length() - 2);
        }
        return sb.toString();
    }

    private void evaluateMarkdown(RestAPIEntry entry, StringBuilder sb, int prefixCount) {
        String prefix = getMarkdownPrefix(prefixCount);
        sb.append(prefix).append(entry.getFieldName()).append(": ").append(entry.getFieldDesc()).append("\r\n");
        if (entry.isLink()) {
            prefixCount++;
            for (RestAPIEntry e : entry.getLinkTo()) {
                evaluateMarkdown(e, sb, prefixCount);
            }
        }
    }

    public String evaluateRedmine(List<RestAPIEntry> entries) {
        StringBuilder sb = new StringBuilder(1000);
        for (RestAPIEntry entry : entries) {
            evaluateRedmine(entry, sb, 1);
        }
        if (sb.length() > 2) {
            return sb.substring(0, sb.length() - 2);
        }
        return sb.toString();
    }

    private void evaluateRedmine(RestAPIEntry entry, StringBuilder sb, int prefixCount) {
        String prefix = getRedminePrefix(prefixCount);
        sb.append(prefix).append(entry.getFieldName()).append(": ").append(entry.getFieldDesc()).append("\r\n");
        if (entry.isLink()) {
            prefixCount++;
            for (RestAPIEntry e : entry.getLinkTo()) {
                evaluateRedmine(e, sb, prefixCount);
            }
        }
    }

    @NotNull
    private String getMarkdownPrefix(int prefixCount) {
        String prefix = "";
        for (int i = 1; i < prefixCount; i++) {
            prefix += "\t";
        }
        prefix += "- ";
        return prefix;
    }

    @NotNull
    private String getRedminePrefix(int prefixCount) {
        String prefix = "";
        for (int i = 1; i < prefixCount; i++) {
            prefix += "*";
        }
        prefix += "*** ";
        return prefix;
    }
}
