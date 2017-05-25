package com.everhomes.sak.unittool;

import com.everhomes.sak.config.SakToolSettings;
import com.everhomes.sak.util.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

/**
 * Created by xq.tian on 2017/5/18.
 */
public class UnitToolAction extends AnAction implements DumbAware {

    private SakToolSettings settings;

    public UnitToolAction() {
        this.settings = ServiceManager.getService(SakToolSettings.class);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiClass psiClass = PsiUtil.getPsiClass(e);
        if (psiClass == null) {
            errDialog("Can not find a class");
            return;
        }

        PsiElement element = PsiUtil.getCurrentElement(e);
        if (element == null) {
            errDialog("Can not find a element");
            return;
        }

        String result = UnitToolService.genUnitTest(psiClass, element, settings);
        if (!"OK".equals(result)) {
            errDialog(result);
        }
    }

    private void errDialog(String message) {
        Messages.showMessageDialog(
                message,
                "Error",
                Messages.getErrorIcon()
        );
    }

    /*@Override
    public void update(AnActionEvent event) {
        // 在Action显示之前，先判定是否显示此Action
        // 只有当焦点为 package 或者 class 时，显示此Action
        IdeView ideView = PsiUtil.getIdeView(event);
        Project project = PsiUtil.getProject(event);
        boolean isPackage = true;
        if (ideView != null && project != null) {
            PsiDirectory psiDirectory = DirectoryChooserUtil.getOrChooseDirectory(ideView);
            if (psiDirectory != null) {
                isPackage = PsiDirectoryFactory.getInstance(project).isPackage(psiDirectory);
            }
        }
        this.getTemplatePresentation().setVisible(isPackage);
        this.getTemplatePresentation().setEnabled(isPackage);
    }*/
}
