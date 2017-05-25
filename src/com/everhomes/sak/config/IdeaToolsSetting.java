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
@State(name = "IdeaToolsSetting", storages = {@Storage(id = "app-default", file = "$APP_CONFIG$/IdeaTools-settings.xml")})
public class IdeaToolsSetting implements PersistentStateComponent<IdeaToolsSetting> {

    private static final Logger LOGGER = Logger.getInstance(IdeaToolsSetting.class);

    private Map<String, String> codeTemplates;

    public IdeaToolsSetting() {
    }

    public Map<String, String> getCodeTemplates() {
        if (codeTemplates == null) {
            loadDefaultSettings();
        }
        return codeTemplates;
    }

    @Nullable
    @Override
    public IdeaToolsSetting getState() {
        if (this.codeTemplates == null) {
            loadDefaultSettings();
        }
        return this;
    }

    private void loadDefaultSettings() {
        try {
            String classVm = FileUtil.loadTextAndClose(IdeaToolsSetting.class.getResourceAsStream("/template/Class.vm"));
            String methodVm = FileUtil.loadTextAndClose(IdeaToolsSetting.class.getResourceAsStream("/template/Method.vm"));
            String constVm = FileUtil.loadTextAndClose(IdeaToolsSetting.class.getResourceAsStream("/template/Const.vm"));
            String restDocCommentVm = FileUtil.loadTextAndClose(IdeaToolsSetting.class.getResourceAsStream("/template/DocComment.vm"));
            String restAPIVm = FileUtil.loadTextAndClose(IdeaToolsSetting.class.getResourceAsStream("/template/RestAPI.vm"));
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
    public void loadState(IdeaToolsSetting ideaToolsSetting) {
        XmlSerializerUtil.copyBean(ideaToolsSetting, this);
    }

    public String getTemplate(String key) {
        return getCodeTemplates().get(key);
    }
}
