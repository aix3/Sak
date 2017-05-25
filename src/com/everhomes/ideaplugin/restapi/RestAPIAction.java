package com.everhomes.ideaplugin.restapi;

import com.everhomes.ideaplugin.config.IdeaToolsSetting;
import com.everhomes.ideaplugin.util.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

/**
 * Created by xq.tian on 2017/5/24.
 */
public class RestAPIAction extends AnAction {

    private IdeaToolsSetting settings;

    public RestAPIAction() {
        this.settings = ServiceManager.getService(IdeaToolsSetting.class);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiClass psiClass = PsiUtil.getPsiClass(e);
        if (psiClass == null) {
            errDialog("Do not find class");
            return;
        }

        PsiElement element = PsiUtil.getCurrentElement(e);
        if (element == null) {
            errDialog("Do not find element");
            return;
        }

        String result = RestAPIService.genRestAPI(psiClass, element, settings);
        if (!"OK".equals(result)) {
            errDialog(result);
        }
    }

    private void errDialog(String message) {
        Messages.showMessageDialog(
                message,
                "Error",
                Messages.getInformationIcon()
        );
    }
}
