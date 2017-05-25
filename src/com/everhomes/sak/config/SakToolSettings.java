package com.everhomes.sak.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xq.tian on 2017/5/22.
 */
@State(name = "SakToolSettings", storages = {@Storage(id = "app-default", file = "$APP_CONFIG$/SakTool-settings.xml")})
public class SakToolSettings implements PersistentStateComponent<SakToolSettings> {

    private static final Logger LOGGER = Logger.getInstance(SakToolSettings.class);

    private Map<String, String> codeTemplates;

    public SakToolSettings() { }

    public Map<String, String> getCodeTemplates() {
        if (codeTemplates == null) {
            loadDefaultSettings();
        }
        return codeTemplates;
    }

    @Nullable
    @Override
    public SakToolSettings getState() {
        if (this.codeTemplates == null) {
            loadDefaultSettings();
        }
        return this;
    }

    private void loadDefaultSettings() {
        try {
            String classVm = FileUtil.loadTextAndClose(SakToolSettings.class.getResourceAsStream("/fileTemplates/Class.vm"));
            String methodVm = FileUtil.loadTextAndClose(SakToolSettings.class.getResourceAsStream("/fileTemplates/Method.vm"));
            String constVm = FileUtil.loadTextAndClose(SakToolSettings.class.getResourceAsStream("/fileTemplates/Const.vm"));
            String restDocCommentVm = FileUtil.loadTextAndClose(SakToolSettings.class.getResourceAsStream("/fileTemplates/DocComment.vm"));
            String restAPIVm = FileUtil.loadTextAndClose(SakToolSettings.class.getResourceAsStream("/fileTemplates/RestAPI.vm"));
            Map<String, String> codeTemplates = new HashMap<>();
            codeTemplates.put("Class", classVm);
            codeTemplates.put("Method", methodVm);
            codeTemplates.put("Const", constVm);
            codeTemplates.put("DocComment", restDocCommentVm);
            codeTemplates.put("RestAPI", restAPIVm);
            this.codeTemplates = codeTemplates;
        } catch (Exception e) {
            LOGGER.error("loadDefaultSettings failed", e);
        }
    }

    @Override
    public void loadState(SakToolSettings sakToolSettings) {
        XmlSerializerUtil.copyBean(sakToolSettings, this);
    }

    public String getTemplate(String key) {
        return getCodeTemplates().get(key);
    }
}
