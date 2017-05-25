package com.everhomes.sak.util;

import com.google.common.collect.Lists;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopeUtil;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.psi.util.CreateClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.text.StringTokenizer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Util {

    public static Logger getLogger(Class clazz) {
        return Logger.getInstance(clazz);
    }

    public static PsiClass chooseClass(Project project, PsiClass defaultClass) {
        TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project)
                .createProjectScopeChooser("Select a class", defaultClass);

        chooser.showDialog();

        return chooser.getSelected();
    }

    public static String getSourcePath(PsiClass clazz) {
        String classPath = clazz.getContainingFile().getVirtualFile().getPath();
        return classPath.substring(0, classPath.lastIndexOf('/'));
    }

    public static String generateClassPath(String sourcePath, String className) {
        return sourcePath + "/" + className + ".java";
    }

    public static List<String> getImportList(PsiJavaFile javaFile) {
        PsiImportList importList = javaFile.getImportList();
        if (importList == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(importList.getImportStatements())
                .map(PsiImportStatement::getQualifiedName).collect(Collectors.toList());
    }

    /**
     * find the method belong to  name
     * @param psiMethod
     * @return null if not found
     */
    public static String findClassNameOfSuperMethod(PsiMethod psiMethod) {
        PsiMethod[] superMethods = psiMethod.findDeepestSuperMethods();
        if (superMethods.length == 0 || superMethods[0].getContainingClass() == null) {
            return null;
        }
        return superMethods[0].getContainingClass().getQualifiedName();
    }

    /**
     * Gets all classes in the element.
     *
     * @param element the Element
     * @return the Classes
     */
    public static List<PsiClass> getClasses(PsiElement element) {
        List<PsiClass> elements = Lists.newArrayList();
        List<PsiClass> classElements = PsiTreeUtil.getChildrenOfTypeAsList(element, PsiClass.class);
        elements.addAll(classElements);
        for (PsiClass classElement : classElements) {
            elements.addAll(getClasses(classElement));
        }
        return elements;
    }

    public static PsiClass getClassByQName(Project project, String qName) {
        GlobalSearchScope searchScope =
                GlobalSearchScopeUtil.toGlobalSearchScope(new ProjectAndLibrariesScope(project), project);
        return JavaPsiFacade.getInstance(project).findClass(qName, searchScope);
    }

    public static void setStatusBarText(Project project, String message) {
        if (project != null) {
            final StatusBarEx statusBar = (StatusBarEx) WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(message);
            }
        }
    }

    /*public static void tooltip(Project project, String message) {
        if (project != null) {
            JToolTip jToolTip = new JToolTip();
            jToolTip.setTipText("sdasdsadsadsadsa");
            TooltipWithClickableLinks tooltip = new TooltipWithClickableLinks(jToolTip, "<h3>sda</h3>", new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    System.out.println(e);
                }
            });
            tooltip.setToCenter(true);
        }
    }*/

    public static PsiDirectory createParentDirectories(@NotNull PsiDirectory directoryRoot, @NotNull String className) throws IncorrectOperationException {
        final PsiPackage currentPackage = JavaDirectoryService.getInstance().getPackage(directoryRoot);
        final String packagePrefix = currentPackage == null ? null : currentPackage.getQualifiedName() + ".";
        final String packageName = CreateClassUtil.extractPackage(packagePrefix != null && className.startsWith(packagePrefix)?
                className.substring(packagePrefix.length()) : className);
        final StringTokenizer tokenizer = new StringTokenizer(packageName, ".");
        while (tokenizer.hasMoreTokens()) {
            String packagePart = tokenizer.nextToken();
            PsiDirectory subdirectory = directoryRoot.findSubdirectory(packagePart);
            if (subdirectory == null) {
                directoryRoot.checkCreateSubdirectory(packagePart);
                subdirectory = directoryRoot.createSubdirectory(packagePart);
            }
            directoryRoot = subdirectory;
        }
        return directoryRoot;
    }

    public static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }

        int length = str.length();

        for (int i = 0; i < length; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static String cap(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
