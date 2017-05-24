package com.everhomes.ideaplugin.unittool.bean;

import com.google.common.collect.Lists;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * Created by xq.tian on 2017/5/23.
 */
public class CmdEntry {

    private String name;
    private Object value;
    private String type;
    private boolean isList;
    private boolean hasChild;
    private List<CmdEntry> cmdChild = Lists.newArrayList();
    private List<CmdEntry> listChild = Lists.newArrayList();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }

    public boolean isHasChild() {
        return hasChild;
    }

    public void setHasChild(boolean hasChild) {
        this.hasChild = hasChild;
    }

    public List<CmdEntry> getCmdChild() {
        return cmdChild;
    }

    public void setCmdChild(List<CmdEntry> cmdChild) {
        this.cmdChild = cmdChild;
    }

    public List<CmdEntry> getListChild() {
        return listChild;
    }

    public void setListChild(List<CmdEntry> listChild) {
        this.listChild = listChild;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("value", value)
                .append("type", type)
                .append("isList", isList)
                .append("hasChild", hasChild)
                .append("cmdChild", cmdChild)
                .append("listChild", listChild)
                .toString();
    }
}
