package com.everhomes.sak.unittool;

import com.everhomes.sak.unittool.bean.CmdEntry;
import com.everhomes.sak.util.Util;

import java.util.List;

/**
 * Created by xq.tian on 2017/5/23.
 */
public class CmdEntryRender {

    public String evaluate(List<CmdEntry> setters) {
        StringBuilder sb = new StringBuilder(500);
        for (CmdEntry setter : setters) {
            sb.append(evaluate("cmd", setter));
        }
        return sb.toString();
    }

    private String evaluate(String objName, CmdEntry setter) {
        StringBuilder sb = new StringBuilder(500);
        if (setter.isHasChild()) {
            sb.append(setter.getType()).append(" ").append(setter.getName())
                    .append(" = ").append("new ").append(setter.getType()).append("();");
            for (CmdEntry ce : setter.getCmdChild()) {
                sb.append(evaluate(setter.getName(), ce));
            }
        }
        if (setter.isList()) {
            sb.append("List<").append(setter.getType()).append("> ").append(setter.getName()).append("List")
                    .append(" = ").append("new ArrayList<>();");
            for (CmdEntry ce : setter.getListChild()) {
                sb.append(evaluate(setter.getName()+"List", ce));
            }
        }
        if (objName.endsWith("List")) {
            sb.append(objName).append(".add(").append(setter.getValue()).append(");");
        } else {
            sb.append(objName).append(".set").append(Util.cap(setter.getName())).append("(").append(setter.getValue()).append(");");
        }
        return sb.toString();
    }
}
