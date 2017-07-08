package com.everhomes.sak.fmtoff;

import com.everhomes.sak.util.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;

/**
 * Created by xq.tian on 2017/7/8.
 */
public class FmtOffAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiClass psiClass = PsiUtil.getPsiClass(e);
        if (psiClass == null) {
            errDialog("Can not find a class");
            return;
        }
        WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () -> {
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
            PsiComment comment = factory.createCommentFromText("// @formatter:off", psiClass.getContext());
            PsiElement context = psiClass.getContext();
            if (context != null) {
                PsiFile psiFile = context.getContainingFile();
                psiFile.getFirstChild().addBefore(comment, psiFile.getFirstChild());
            }
        });
    }

    private void errDialog(String message) {
        Messages.showMessageDialog(
                message,
                "Error",
                Messages.getErrorIcon()
        );
    }
}
