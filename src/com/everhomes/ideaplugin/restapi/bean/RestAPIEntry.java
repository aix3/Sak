package com.everhomes.ideaplugin.restapi.bean;

import com.google.common.collect.Lists;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * Created by xq.tian on 2017/5/24.
 */
public class RestAPIEntry {

    private String apiDesc;
    private String api;
    private String fieldName;
    private String fieldDesc;
    private String cmdQName;
    private String respQName;
    private boolean link;
    private boolean respCollectionFlag;
    private List<RestAPIEntry> linkTo = Lists.newArrayList();

    public String getApiDesc() {
        return apiDesc;
    }

    public void setApiDesc(String apiDesc) {
        this.apiDesc = apiDesc;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldDesc() {
        return fieldDesc;
    }

    public void setFieldDesc(String fieldDesc) {
        this.fieldDesc = fieldDesc;
    }

    public boolean isLink() {
        return link;
    }

    public void setLink(boolean link) {
        this.link = link;
    }

    public List<RestAPIEntry> getLinkTo() {
        return linkTo;
    }

    public void setLinkTo(List<RestAPIEntry> linkTo) {
        this.linkTo = linkTo;
    }

    public String getCmdQName() {
        return cmdQName;
    }

    public void setCmdQName(String cmdQName) {
        this.cmdQName = cmdQName;
    }

    public boolean isRespCollectionFlag() {
        return respCollectionFlag;
    }

    public void setRespCollectionFlag(boolean respCollectionFlag) {
        this.respCollectionFlag = respCollectionFlag;
    }

    public String getRespQName() {
        return respQName;
    }

    public void setRespQName(String respQName) {
        this.respQName = respQName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("apiDesc", apiDesc)
                .append("api", api)
                .append("fieldName", fieldName)
                .append("fieldDesc", fieldDesc)
                .append("cmdQName", cmdQName)
                .append("respQName", respQName)
                .append("link", link)
                .append("respCollectionFlag", respCollectionFlag)
                .append("linkTo", linkTo)
                .toString();
    }
}
