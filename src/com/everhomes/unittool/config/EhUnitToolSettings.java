package com.everhomes.unittool.config;

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
@State(name = "EhUnitToolSettings", storages = {@Storage(id = "app-default", file = "$APP_CONFIG$/EhUnitTool-settings.xml")})
public class EhUnitToolSettings implements PersistentStateComponent<EhUnitToolSettings> {

    private static final Logger LOGGER = Logger.getInstance(EhUnitToolSettings.class);

    private Map<String, String> codeTemplates;

    public EhUnitToolSettings() {
    }

    public Map<String, String> getCodeTemplates() {
        if (codeTemplates == null) {
            loadDefaultSettings();
        }
        return codeTemplates;
    }

    @Nullable
    @Override
    public EhUnitToolSettings getState() {
        if (this.codeTemplates == null) {
            loadDefaultSettings();
        }
        return this;
    }

    private void loadDefaultSettings() {
        try {
            String classVm = FileUtil.loadTextAndClose(EhUnitToolSettings.class.getResourceAsStream("/template/Class.vm"));
            String methodVm = FileUtil.loadTextAndClose(EhUnitToolSettings.class.getResourceAsStream("/template/Method.vm"));
            String constVm = FileUtil.loadTextAndClose(EhUnitToolSettings.class.getResourceAsStream("/template/Const.vm"));
            Map<String, String> codeTemplates = new HashMap<>();
            codeTemplates.put("Class", classVm);
            codeTemplates.put("Method", methodVm);
            codeTemplates.put("Const", constVm);
            this.codeTemplates = codeTemplates;
        } catch (Exception e) {
            LOGGER.error("loadDefaultSettings failed", e);
        }
    }

    @Override
    public void loadState(EhUnitToolSettings ehUnitToolSettings) {
        XmlSerializerUtil.copyBean(ehUnitToolSettings, this);
    }

    public String getTemplate(String key) {
        return getCodeTemplates().get(key);
    }
}
