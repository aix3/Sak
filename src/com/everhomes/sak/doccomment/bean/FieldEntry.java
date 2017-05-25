package com.everhomes.sak.doccomment.bean;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Created by xq.tian on 2017/5/23.
 */
public class FieldEntry {

    private String name;
    private String desc;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("desc", desc)
                .toString();
    }
}
