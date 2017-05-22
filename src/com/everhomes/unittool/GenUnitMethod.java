package com.everhomes.unittool;

import com.everhomes.unittool.config.EhUnitToolSettings;
import com.everhomes.unittool.util.CodeMakerUtil;
import com.everhomes.unittool.util.VelocityUtil;
import com.google.common.collect.Maps;
import com.intellij.ide.IdeView;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xq.tian on 2017/5/18.
 */
public class GenUnitMethod extends AnAction implements DumbAware {

    private EhUnitToolSettings settings;

    public GenUnitMethod() {
        this.settings = ServiceManager.getService(EhUnitToolSettings.class);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiClass psiClass = PsiUtil.getPsiClass(e);
        if (psiClass == null) {
            Messages.showMessageDialog(
                    "Do not find class",
                    "Error",
                    Messages.getErrorIcon()
            );
        }

        PsiElement element = PsiUtil.getCurrentElement(e);

        boolean success = generateUnitTest(psiClass, element);
        if (!success) {
            Messages.showMessageDialog(
                    "Some error",
                    "Error",
                    Messages.getErrorIcon()
            );
        }
    }

    private boolean generateUnitTest(PsiClass psiClass, PsiElement element) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return false;
        }

        if (!qualifiedName.endsWith("Controller")) {
            return false;
        }

        String clzBody = psiClass.getText();
        String clzRequestMapping = getRequestMapping(clzBody);

        PsiMethod currentMethod = null;
        PsiMethod[] allMethods = psiClass.getAllMethods();
        for (PsiMethod method : allMethods) {
            if (method.getName().equals(element.getText())) {
                currentMethod = method;
                break;
            }
        }

        if (currentMethod == null) {
            return false;
        }

        PsiMethod currMethod = currentMethod;

        String methodBody = currentMethod.getText();
        String methodRequestMapping = getRequestMapping(methodBody);

        String requestMapping = clzRequestMapping + "/" + methodRequestMapping;
        System.out.println(requestMapping);

        final List<String> importList = new ArrayList<>();
        final List<String> cmdList = new ArrayList<>();

        PsiParameterList parameterList = currentMethod.getParameterList();
        for (PsiParameter param : parameterList.getParameters()) {
            PsiTypeElement psiTypeElement = param.getTypeElement();
            if (psiTypeElement != null) {
                String cmdName = psiTypeElement.getText();
                String cmdQualifyName = param.getType().getCanonicalText();

                cmdList.add(cmdName);
                importList.add(cmdQualifyName);
            }
        }

        System.out.println(importList);
        System.out.println(cmdList);

        final Project project = psiClass.getProject();
        final String dirPath = "src/com/everhomes/unittool";
        final String clazzName = "FlowTest";
        final String packageName = "com.everhomes.unittool";
        final String urlVarName = "private static final String A = \"1\";";

        for (String im : importList) {
            PsiClass aClass = CodeMakerUtil.getClassByQName(project, im);
            System.out.println("===============>>"+aClass);
        }

        String[] split = dirPath.split("/");
        List<String> pathList = new ArrayList<>();
        Collections.addAll(pathList, split);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            final PsiDirectory dir = getOrCreatePsiDirectory(pathList, project, project.getBaseDir());

            PsiClass clazz;
            PsiJavaFile javaFile;

            VirtualFileManager manager = VirtualFileManager.getInstance();
            String outputFilePath = project.getBasePath() + "/" + dirPath + "/" + clazzName + ".java";
            String url = VfsUtil.pathToUrl(outputFilePath);
            VirtualFile virtualFile = manager
                    .refreshAndFindFileByUrl(url);

            if (virtualFile != null && virtualFile.exists()) {
                clazz = CodeMakerUtil.getClassByQName(project, packageName + "." + clazzName);
                javaFile = (PsiJavaFile) clazz.getContainingFile();
            } else {
                File file = new File(outputFilePath);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdir();
                }
                try (FileWriter fileWriter = new FileWriter(file)) {
                    String content = VelocityUtil.evaluate(settings.getTemplate("Class"), Maps.newHashMap());
                    fileWriter.write(content);
                } catch (Exception e) {
                    //
                }

                manager.refreshAndFindFileByUrl(url);

                clazz = CodeMakerUtil.getClassByQName(project, packageName + "." + clazzName);
                javaFile = (PsiJavaFile) clazz.getContainingFile();

                /*clazz = JavaDirectoryService.getInstance().createClass(
                        dir, clazzName, "Class", true, Maps.newHashMap());*/

                javaFile.setPackageName(packageName);
            }

            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);


            for (String im : importList) {
                PsiClass classByQName = CodeMakerUtil.getClassByQName(project, im);
                PsiImportStatement importStatement = factory.createImportStatement(classByQName);
                List<String> stringList = CodeMakerUtil.getImportList(javaFile);
                if (!stringList.contains(im)) {
                    javaFile.getImportList().add(importStatement);
                }
            }

            PsiField field = factory.createFieldFromText(urlVarName, clazz);

            Map<String, Object> map = Maps.newHashMap();
            map.put("methodName", currMethod.getName());
            String template = settings.getTemplate("Method");
            String content = VelocityUtil.evaluate(template, map);

            content = content.replaceAll("\\s\\s+", "");

            PsiMethod method = factory.createMethodFromText(content, clazz);

            clazz.add(field);
            clazz.add(method);

            CodeStyleManager.getInstance(project).reformat(clazz);

            FileEditorManager.getInstance(project).openTextEditor(
                    new OpenFileDescriptor(project, clazz.getContainingFile().getVirtualFile()), true);
        });

        return true;
    }

    private PsiDirectory getOrCreatePsiDirectory(List<String> dirPaths, Project project, VirtualFile vf) {
        String path = dirPaths.remove(0);

        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(vf);
        PsiDirectory subDir = baseDir.findSubdirectory(path);

        boolean notExist = subDir == null;
        if (notExist) {
            subDir = baseDir.createSubdirectory(path);
        }

        if (dirPaths.size() > 0) {
            return getOrCreatePsiDirectory(dirPaths, project, subDir.getVirtualFile());
        } else {
            return subDir;
        }
    }

    private String getRequestMapping(String methodBody) {
        String methodRequestMapping = "";
        if (methodBody.contains("@SuppressWarnings")) {
            Pattern pattern = Pattern.compile("@SuppressWarnings\\(\"(.*?)\"\\)");
            Matcher matches = pattern.matcher(methodBody);
            if (matches.find()) {
                methodRequestMapping = matches.group(1);
                System.out.println(methodRequestMapping);
            }
        }
        return methodRequestMapping;
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
