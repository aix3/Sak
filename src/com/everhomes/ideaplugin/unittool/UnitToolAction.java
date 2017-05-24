package com.everhomes.ideaplugin.unittool;

import com.everhomes.ideaplugin.config.IdeaToolsSetting;
import com.everhomes.ideaplugin.util.PsiUtil;
import com.intellij.ide.IdeView;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.file.PsiDirectoryFactory;

/**
 * Created by xq.tian on 2017/5/18.
 */
public class UnitToolAction extends AnAction implements DumbAware {

    private IdeaToolsSetting settings;

    public UnitToolAction() {
        this.settings = ServiceManager.getService(IdeaToolsSetting.class);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiClass psiClass = PsiUtil.getPsiClass(e);
        if (psiClass == null) {
            errDialog("Do not find class");
        }

        PsiElement element = PsiUtil.getCurrentElement(e);
        if (element == null) {
            errDialog("Do not find element");
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
                Messages.getInformationIcon()
        );
    }

    @Override
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
    }
}
