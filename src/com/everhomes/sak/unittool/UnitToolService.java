package com.everhomes.sak.unittool;

import com.everhomes.sak.config.SakToolSettings;
import com.everhomes.sak.unittool.bean.CmdEntry;
import com.everhomes.sak.util.Util;
import com.everhomes.sak.util.VelocityUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiTypeElementImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by xq.tian on 2017/5/23.
 */
class UnitToolService {

    private static final Logger log = Util.getLogger(UnitToolService.class);

    private int recursiveCount = 15;// 递归的最大层数，避免过深的递归

    // private static final String REQUEST_MAPPING_ANNO = "@RequestMapping";
    private static final String JAVA_UTIL_LIST_REGEX = "java\\.util\\.List<(.*?)>";

    // private static final String REQUEST_MAPPING_REGEX = "@RequestMapping\\(\"(.*?)\"\\)";
    private List<String> importList = Lists.newArrayList();
    private Project project;
    private PsiMethod currMethod;
    private String dirPath = "ehtest-integration/src/test/java/";
    private String basePackageName = "com.everhomes.test.junit";
    private String cmdName;
    private String currClzQName;
    private PsiClass testClz;

    private VirtualFile vf;
    private String responseType;

    private String testComment;
    private PsiJavaFile testJavaFile;
    private SakToolSettings settings;
    private boolean createClassSuccess = false;

    private boolean testClassNotFound = false;

    static String genUnitTest(PsiClass psiClass, PsiElement element, SakToolSettings settings) {
        UnitToolService service = UnitToolService.getInstance();
        service.settings = settings;
        return service.generateUnitTest(psiClass, element);
    }

    private static UnitToolService getInstance() {
        return new UnitToolService();
    }

    private String generateUnitTest(PsiClass psiClass, PsiElement element) {
        currClzQName = psiClass.getQualifiedName();
        if (currClzQName == null) {
            return "Can not find psiClass qualifiedName";
        }

        if (!currClzQName.endsWith("Controller")) {
            return "Please in a controller method";
        }

        // String clzBody = psiClass.getText();
        String clzRequestMapping = this.getRequestMapping(psiClass);

        currMethod = this.getCurrentMethod(element);

        if (currMethod == null) {
            return "Please in a controller method";
        }

        // String methodBody = currMethod.getText();
        String methodRequestMapping = this.getRequestMapping(currMethod);

        final String requestMapping = clzRequestMapping + "/" + methodRequestMapping;
        log.debug("find request mapping is " + requestMapping);

        final List<String> cmdList = getCmdList(currMethod);

        processTestComment();
        processCmdName();
        processResponseType(requestMapping);

        project = psiClass.getProject();

        final String testQClassName = basePackageName +
                currClzQName.substring(13, currClzQName.indexOf("Controller")) + "Test";

        // PsiClass selectedClass = Util.chooseClass(project, psiClass);
        //
        // PsiDirectory psiDirectory = PsiUtil.browseForFile(project);
        /*UnitToolDialog dialog = new UnitToolDialog();
        dialog.project = project;
        dialog.pack();
        dialog.setVisible(true);*/

        final List<CmdEntry> entryList = this.getCmdEntries(cmdList);

        /*WriteCommandAction.runWriteCommandAction(project, () -> {
            getOrCreateTestClass(testQClassName);
        });*/

        /*if (createClassSuccess) {
            try {Thread.sleep(20000);} catch (InterruptedException ignored) {}
            testClz = Util.getClassByQName(project, testQClassName);
            testJavaFile = (PsiJavaFile) testClz.getContainingFile();
        }*/

        WriteCommandAction.runWriteCommandAction(project, () -> {

            getOrCreateTestClass(testQClassName);

            if (this.testClassNotFound) {
                return;
            }

            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

            Map<String, Object> map = Maps.newHashMap();
            map.put("testComment", testComment);
            map.put("requestUriName", methodRequestMapping + "URL");
            map.put("requestUri", requestMapping);
            String template = settings.getTemplate("Const");
            String content = VelocityUtil.evaluate(template, map);

            PsiField field = factory.createFieldFromText(content, testClz);

            map = Maps.newHashMap();
            map.put("cmdName", cmdName);
            map.put("testComment", testComment);
            map.put("responseType", responseType);
            map.put("methodName", methodRequestMapping);
            map.put("requestUriName", methodRequestMapping + "URL");
            map.put("setterBlock", entryList);
            map.put("render", new CmdEntryRender());
            template = settings.getTemplate("Method");
            content = VelocityUtil.evaluate(template, map);

            PsiMethod method = factory.createMethodFromText(content, testClz);

            testClz.addBefore(field, testClz.getFields()[0]);
            testClz.addBefore(method, testClz.getMethods()[0]);

            processImportList(factory);

            // CodeStyleManager.getInstance(project).reformat(testClz);

            ApplicationManager.getApplication().invokeLater(() -> {
                Editor editor = FileEditorManager.getInstance(project).openTextEditor(
                        new OpenFileDescriptor(project, vf), true);

                if (editor != null) {
                    int offset = testClz.getMethods()[0].getTextOffset();
                    editor.getScrollingModel().scrollTo(editor.offsetToLogicalPosition(offset), ScrollType.CENTER);
                    editor.getCaretModel().moveToLogicalPosition(editor.offsetToLogicalPosition(offset));
                }
            });
        });

        return "OK";
    }

    private void processCmdName() {
        PsiParameterList parameterList = currMethod.getParameterList();
        for (PsiParameter psiParameter : parameterList.getParameters()) {
            if (psiParameter.getType().getCanonicalText().startsWith("com.everhomes.rest")) {
                this.cmdName = psiParameter.getType().getPresentableText();
            }
        }
    }

    private void processResponseType(String requestMapping) {
        PsiAnnotation annotation = currMethod.getModifierList().findAnnotation("com.everhomes.discover.RestReturn");
        if (annotation != null) {
            PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
            PsiType type = ((PsiTypeElementImpl) value.getFirstChild()).getType();
            String respQName = type.getCanonicalText();
            if ("java.lang.String".equals(respQName)) {
                this.responseType = "RestResponseBase";
                importList.add("com.everhomes.rest.RestResponseBase");
            } else {
                if (requestMapping.startsWith("/")) {
                    requestMapping = requestMapping.substring(1);
                }
                String[] split = requestMapping.split("/");
                List<String> strings = Lists.newArrayList(split);
                if (strings.size() > 0) {
                    strings.remove(0);
                    strings = strings.stream().map(Util::cap).collect(Collectors.toList());
                    String responseShortName = StringUtils.join(strings, "");
                    this.responseType = responseShortName + "RestResponse";
                    String responseClass = "com.everhomes.rest" + currClzQName.substring(13, currClzQName.lastIndexOf(".") + 1) + responseType;
                    importList.add(responseClass);
                }

                /*annotation = currMethod.getModifierList().findAnnotation("org.springframework.web.bind.annotation.RequestMapping");
                if (annotation != null) {
                    value = annotation.findAttributeValue("value");
                    if (value != null) {
                        String text = value.getText();
                        text = text.replaceAll("\"", "");
                        // importList.add(respQName);
                        String responseClass = "com.everhomes.rest" + currClzQName.substring(13, currClzQName.lastIndexOf(".")+1) + responseType;
                        importList.add(responseClass);
                    }
                }*/
            }
        }
    }

    private void processTestComment() {
        PsiDocComment docComment = currMethod.getDocComment();
        if (docComment != null) {
            String text = docComment.getText();
            Pattern compile = Pattern.compile("<p>(.*?)</p>");
            Matcher matcher = compile.matcher(text);
            if (matcher.find()) {
                this.testComment = matcher.group(1);
            } else {
                this.testComment = "";
            }
        }
    }

    private void processImportList(PsiElementFactory factory) {
        for (String im : importList) {
            PsiClass classByQName = Util.getClassByQName(project, im);
            if (classByQName != null) {
                PsiImportStatement importStatement = factory.createImportStatement(classByQName);
                List<String> stringList = Util.getImportList(testJavaFile);
                if (!stringList.contains(im)) {
                    testJavaFile.getImportList().add(importStatement);
                }
            }
        }
    }

    private void getOrCreateTestClass(String testClassQName) {
        VirtualFileManager manager = VirtualFileManager.getInstance();
        String outputFilePath = project.getBasePath() + "/" + dirPath + "/" + testClassQName.replaceAll("\\.", "/") + ".java";
        String url = VfsUtil.pathToUrl(outputFilePath);

        vf = manager.refreshAndFindFileByUrl(url);

        if (vf != null && vf.exists()) {
            testClz = Util.getClassByQName(project, testClassQName);
            testJavaFile = (PsiJavaFile) testClz.getContainingFile();
        } else {
            this.testClassNotFound = true;

            int confirm = Messages.showYesNoDialog(
                    String.format("Can not find '%s', do you want to create it from template?", testClassQName),
                    "Ask",
                    Messages.getQuestionIcon()
            );
            if (confirm == Messages.NO) {
                return;
            }

            File file = new File(outputFilePath);
            File parentFile = file;
            do {
                parentFile = parentFile.getParentFile();
                if (parentFile.exists()) {
                    break;
                }
                parentFile.mkdirs();
            } while (true);

            String packageName = testClassQName.substring(0, testClassQName.lastIndexOf("."));
            try (FileWriter fileWriter = new FileWriter(file)) {
                HashMap<String, Object> map = Maps.newHashMap();
                map.put("packageName", packageName);
                map.put("testClassName", testClassQName.substring(testClassQName.lastIndexOf(".") + 1));
                String classTemplate = settings.getTemplate("Class");
                String content = VelocityUtil.evaluate(classTemplate, map);
                fileWriter.write(content);
                createClassSuccess = true;
            } catch (Exception e) {
                log.error("some err", e);
            }

            /*ProgressIndicator progressIndicator = RefreshProgress.create("Start update index.");
            progressIndicator.start();

            VirtualFile vf = manager.refreshAndFindFileByUrl(url);
            FileContentUtil.reparseFiles(vf);

            progressIndicator.stop();*/

            // testClz = Util.getClassByQName(project, testClassQName);
            // testJavaFile = (PsiJavaFile) testClz.getContainingFile();

            vf = manager.refreshAndFindFileByUrl(url);

            ApplicationManager.getApplication().invokeLater(() -> {
                FileEditorManager.getInstance(project).openTextEditor(
                        new OpenFileDescriptor(project, vf), true);
            });
        }
    }

    @NotNull
    private List<CmdEntry> getCmdEntries(List<String> cmdList) {
        List<CmdEntry> entryList = Lists.newArrayList();
        for (String cmd : cmdList) {
            PsiClass aClass = Util.getClassByQName(project, cmd);
            PsiField[] allFields = aClass.getAllFields();
            List<CmdEntry> setters = processCmdEntries(allFields);
            entryList.addAll(setters);
        }
        return entryList;
    }

    private List<String> getCmdList(PsiMethod currMethod) {
        List<String> cmdList = Lists.newArrayList();
        PsiParameterList parameterList = currMethod.getParameterList();
        for (PsiParameter param : parameterList.getParameters()) {
            PsiTypeElement typeElement = param.getTypeElement();
            if (typeElement != null) {
                String cmdQualifyName = param.getType().getCanonicalText();
                cmdList.add(cmdQualifyName);
                importList.add(cmdQualifyName);
            }
        }
        return cmdList;
    }

    @Nullable
    private PsiMethod getCurrentMethod(PsiElement element) {
        do {
            if (element instanceof PsiMethod) {
                return ((PsiMethod) element);
            }
            element = element.getParent();
        } while (element != null);
        // ...
        return null;
    }

    private List<CmdEntry> processCmdEntries(PsiField[] allFields) {
        List<CmdEntry> entries = Lists.newArrayList();
        recursiveCount--;
        if (recursiveCount < 0) {
            return entries;
        }
        for (PsiField field : allFields) {
            CmdEntry newSetter = new CmdEntry();
            processCmdEntries(field, newSetter);
            entries.add(newSetter);
        }
        return entries;
    }

    private CmdEntry processCmdEntries(PsiField field, CmdEntry setter) {
        String fieldName = field.getName();
        String fieldQualifiedName = field.getType().getCanonicalText();

        switchCmdType(setter, fieldName, fieldQualifiedName);
        return setter;
    }

    private void switchCmdType(CmdEntry entry, String fieldName, String fieldQualifiedName) {
        PsiClass fieldClass = Util.getClassByQName(project, fieldQualifiedName);
        entry.setName(fieldName);
        switch (fieldQualifiedName) {
            case "java.lang.String":
                entry.setValue(String.format("\"%s\"", fieldName));
                break;
            case "java.lang.Byte":
                entry.setValue("(byte) 1");
                break;
            case "java.lang.Long":
                entry.setValue("1L");
                break;
            case "java.lang.Integer":
                entry.setValue("1");
                break;
            default:
                if (fieldQualifiedName.startsWith("java.util.List")) {
                    importList.add("java.util.List");
                    importList.add("java.util.ArrayList");

                    Pattern compile = Pattern.compile(JAVA_UTIL_LIST_REGEX);
                    Matcher matcher = compile.matcher(fieldQualifiedName);
                    String qualifiedName;
                    if (matcher.find()) {
                        qualifiedName = matcher.group(1);
                        PsiClass typeClass = Util.getClassByQName(project, qualifiedName);
                        entry.getListChild().addAll(processTypedEntry(project, typeClass, fieldName));
                        entry.setList(true);
                        entry.setValue(fieldName + "List");
                        entry.setType(typeClass.getName());
                    } else {
                        log.error("do not find list typed" + fieldQualifiedName);
                    }
                } else if (fieldQualifiedName.startsWith("com.everhomes.rest")) {
                    importList.add(fieldQualifiedName);
                    entry.getCmdChild().addAll(processCmdEntries(fieldClass.getAllFields()));
                    entry.setValue(fieldName);
                    entry.setHasChild(true);
                    entry.setType(fieldClass.getName());
                }
                break;
        }
    }

    private Collection<? extends CmdEntry> processTypedEntry(Project project, PsiClass typeClass, String fieldName) {
        List<CmdEntry> entries = Lists.newArrayList();
        CmdEntry entry = new CmdEntry();
        switchCmdType(entry, fieldName, typeClass.getQualifiedName());
        entries.add(entry);
        return entries;
    }

    /*private PsiDirectory getOrCreatePsiDirectory(List<String> dirPaths, Project project, VirtualFile vf) {
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
    }*/

    /*private String getRequestMapping(String methodBody) {
        String requestMapping = "";
        if (methodBody.contains(REQUEST_MAPPING_ANNO)) {
            Pattern pattern = Pattern.compile(REQUEST_MAPPING_REGEX);
            Matcher matches = pattern.matcher(methodBody);
            if (matches.find()) {
                requestMapping = matches.group(1);
            } else {
                log.warn("do not find request mapping");
            }
        } else {
            log.warn("do not find request mapping");
        }
        return requestMapping;
    }*/

    private String getRequestMapping(PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList != null) {
            PsiAnnotation annotation = modifierList.findAnnotation("org.springframework.web.bind.annotation.RequestMapping");
            if (annotation != null) {
                PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                if (value != null) {
                    String text = value.getText();
                    text = text.replaceAll("\"", "");
                    return text;
                }
            }
        }
        return "";
    }
}
