package com.everhomes.sak.restapi;

import com.everhomes.sak.config.SakToolSettings;
import com.everhomes.sak.util.PsiUtil;
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

    private SakToolSettings settings;

    public RestAPIAction() {
        this.settings = ServiceManager.getService(SakToolSettings.class);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiClass psiClass = PsiUtil.getPsiClass(e);
        if (psiClass == null) {
            showMessage("Can not find a class");
            return;
        }

        PsiElement element = PsiUtil.getCurrentElement(e);
        if (element == null) {
            showMessage("Can not find a element");
            return;
        }

        String result = RestAPIService.genRestAPI(psiClass, element, settings);
        if (!"OK".equals(result)) {
            showMessage(result);
        }
    }

    private void showMessage(String message) {
        Messages.showMessageDialog(
                message,
                "Error",
                Messages.getErrorIcon()
        );
    }
}
