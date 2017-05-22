package com.everhomes.unittool.util;

import com.google.common.collect.Lists;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopeUtil;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hansong.xhs
 * @version $Id: CodeMakerUtil.java, v 0.1 2017-01-20 10:15 hansong.xhs Exp $$
 */
public class CodeMakerUtil {

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

    public static List<ClassEntry.Field> getFields(PsiClass psiClass) {
        return Arrays
                .stream(psiClass.getFields())
                .map(
                        psiField -> new ClassEntry.Field(psiField.getType().getPresentableText(), psiField
                                .getName(), psiField.getModifierList() == null ? "" : psiField
                                .getModifierList().getText())).collect(Collectors.toList());
    }

    public static List<ClassEntry.Field> getAllFields(PsiClass psiClass) {
        return Arrays
                .stream(psiClass.getAllFields())
                .map(
                        psiField -> new ClassEntry.Field(psiField.getType().getPresentableText(), psiField
                                .getName(), psiField.getModifierList() == null ? "" : psiField
                                .getModifierList().getText())).collect(Collectors.toList());
    }

    public static List<ClassEntry.Method> getMethods(PsiClass psiClass) {
        return Arrays
                .stream(psiClass.getMethods())
                .map(
                        psiMethod -> {
                            String returnType = psiMethod.getReturnType() == null ? "" : psiMethod
                                    .getReturnType().getPresentableText();
                            return new ClassEntry.Method(psiMethod.getName(), psiMethod.getModifierList()
                                    .getText(), returnType, psiMethod.getParameterList().getText());
                        }).collect(Collectors.toList());
    }

    public static List<ClassEntry.Method> getAllMethods(PsiClass psiClass) {
        return Arrays
                .stream(psiClass.getAllMethods())
                .map(
                        psiMethod -> {
                            String returnType = psiMethod.getReturnType() == null ? "" : psiMethod
                                    .getReturnType().getPresentableText();
                            return new ClassEntry.Method(psiMethod.getName(), psiMethod.getModifierList()
                                    .getText(), returnType, psiMethod.getParameterList().getText());
                        }).collect(Collectors.toList());
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
}
