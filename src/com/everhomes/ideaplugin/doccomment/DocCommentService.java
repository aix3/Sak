package com.everhomes.ideaplugin.doccomment;

import com.everhomes.ideaplugin.config.IdeaToolsSetting;
import com.everhomes.ideaplugin.doccomment.bean.FieldEntry;
import com.everhomes.ideaplugin.util.Util;
import com.everhomes.ideaplugin.util.VelocityUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xq.tian on 2017/5/23.
 */
class DocCommentService {

    private static final Logger log = Util.getLogger(DocCommentService.class);

    private static final String JAVA_UTIL_LIST_REGEX = "java\\.util\\.List<(.*?)>";

    private Project project;
    private IdeaToolsSetting settings;

    static String genRestDocComment(PsiClass psiClass, IdeaToolsSetting settings) {
        DocCommentService service = DocCommentService.getInstance();
        service.settings = settings;
        service.project = psiClass.getProject();
        return service.genRestDocComment(psiClass);
    }

    private String genRestDocComment(PsiClass psiClass) {
        boolean alreadyCommentFlag = false;
        PsiDocComment docComment = psiClass.getDocComment();

        Map<String, String> alreadyComment = Maps.newHashMap();
        if (docComment != null) {
            alreadyCommentFlag = true;
            String text = docComment.getText();
            Pattern compile = Pattern.compile("<li>(.*?)[：:](.*?)</li>");
            Matcher matcher = compile.matcher(text);
            while (matcher.find()) {
                alreadyComment.put(matcher.group(1).trim(), matcher.group(2).trim());
            }
        }

        PsiField[] allFields = psiClass.getAllFields();

        List<FieldEntry> entries = getFieldEntries(allFields, alreadyComment);
        String template = settings.getTemplate("DocComment");
        Map<String, Object> map = Maps.newHashMap();
        map.put("entries", entries);
        String content = VelocityUtil.evaluate(template, map);

        log.debug("doc comment content: " + content);

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiDocComment newDocComment = factory.createDocCommentFromText(content);

        final boolean flag = alreadyCommentFlag;
        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (flag) {
                psiClass.getDocComment().delete();
            }
            pushPostponedChanges(psiClass);
            // psiClass.addBefore(newDocComment, psiClass.getFirstChild());
            psiClass.getNode().addChild(newDocComment.getNode(), psiClass.getFirstChild().getNode());
            CodeStyleManager.getInstance(project).reformat(psiClass);
            // reformatJavaDoc(psiClass);
        });
        return "OK";
    }

    @NotNull
    private List<FieldEntry> getFieldEntries(PsiField[] allFields, Map<String, String> alreadyComment) {
        List<FieldEntry> entries = Lists.newArrayList();
        for (PsiField field : allFields) {
            FieldEntry entry = new FieldEntry();
            String name = field.getName();
            entry.setName(name);
            if (alreadyComment.containsKey(name)) {
                entry.setDesc(alreadyComment.get(name).trim());
            } else {
                String fieldQualifiedName = field.getType().getCanonicalText();
                if (fieldQualifiedName.startsWith("com.everhomes.rest")) {
                    entry.setDesc(String.format("{@link %s}", fieldQualifiedName));
                } else if (fieldQualifiedName.startsWith("java.util.List")) {
                    Pattern compile = Pattern.compile(JAVA_UTIL_LIST_REGEX);
                    Matcher matcher = compile.matcher(fieldQualifiedName);
                    if (matcher.find()) {
                        entry.setDesc(String.format("{@link %s}", matcher.group(1)));
                    }
                } else {
                    entry.setDesc("");
                }
            }
            entries.add(entry);
        }
        return entries;
    }

    private static DocCommentService getInstance() {
        return new DocCommentService();
    }

    /**
     * save the current change
     * @param element
     */
    private static void pushPostponedChanges(PsiElement element) {
        Editor editor = PsiUtilBase.findEditor(element.getContainingFile());
        if (editor != null) {
            PsiDocumentManager.getInstance(element.getProject())
                    .doPostponedOperationsAndUnblockDocument(editor.getDocument());
        }
    }

    private static void reformatJavaDoc(PsiElement theElement) {
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(theElement.getProject());
        try {
            int javadocTextOffset = findJavaDocTextOffset(theElement);
            int javaCodeTextOffset = findJavaCodeTextOffset(theElement);
            codeStyleManager.reformatText(theElement.getContainingFile(), javadocTextOffset, javaCodeTextOffset + 1);
        } catch (Exception e) {
            log.error("reformat code failed", e);
        }
    }

    private static int findJavaDocTextOffset(PsiElement theElement) {
        PsiElement javadocElement = theElement.getFirstChild();
        if (!(javadocElement instanceof PsiDocComment)) {
            throw new IllegalStateException("Cannot find element of type PsiDocComment");
        }
        return javadocElement.getTextOffset();
    }

    private static int findJavaCodeTextOffset(PsiElement theElement) {
        if (theElement.getChildren().length < 2) {
            throw new IllegalStateException("Can not find offset of java code");
        }
        return theElement.getChildren()[1].getTextOffset();
    }
}
