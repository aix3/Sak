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
import com.intellij.psi.impl.source.PsiTypeElementImpl;
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
        if (psiClass != null) {
            service.project = psiClass.getProject();
            return service.genRestDocComment(psiClass);
        }
        return "OK";
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

        List<FieldEntry> entries = getFieldEntries(psiClass, alreadyComment);
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
    private List<FieldEntry> getFieldEntries(PsiClass psiClass, Map<String, String> alreadyComment) {
        List<FieldEntry> entries = Lists.newArrayList();
        boolean isEnum = psiClass.isEnum();
        for (PsiField field : psiClass.getAllFields()) {
            if (isEnum && !field.getType().getCanonicalText().equals(psiClass.getQualifiedName())) {
                continue;
            }

            String fieldQualifiedName = field.getType().getCanonicalText();
            PsiModifierList modifierList = field.getModifierList();
            if (modifierList == null) {
                return Lists.newArrayList();
            }
            PsiAnnotation itemType = modifierList.findAnnotation("com.everhomes.discover.ItemType");

            FieldEntry entry = new FieldEntry();
            String name = isEnum ? field.getText() : field.getName();
            entry.setName(name);
            String desc = getDesc(alreadyComment, field);
            if (desc != null && desc.trim().length() > 0) {
                if (itemType != null) {
                    PsiAnnotationMemberValue value = itemType.findAttributeValue("value");
                    if (value != null) {
                        String itemTypeQName = ((PsiTypeElementImpl) value.getFirstChild()).getType().getCanonicalText();
                        if (itemTypeQName.startsWith("com.everhomes.rest") && !desc.contains("@link")) {
                            desc = desc.trim() + String.format(" {@link %s}", itemTypeQName);
                        }
                    }
                }
                entry.setDesc(desc.trim());
            } else {
                if (!isEnum && fieldQualifiedName.startsWith("com.everhomes.rest")) {
                    entry.setDesc(String.format("%s {@link %s}", field.getName(), fieldQualifiedName));
                } else if (itemType != null) {
                    PsiAnnotationMemberValue value = itemType.findAttributeValue("value");
                    if (value != null) {
                        String itemTypeQName = ((PsiTypeElementImpl) value.getFirstChild()).getType().getCanonicalText();
                        if (itemTypeQName.startsWith("com.everhomes.rest")) {
                            entry.setDesc(String.format("%s {@link %s}", field.getName(), itemTypeQName));
                        } else {
                            entry.setDesc(field.getName());
                        }
                    }
                    /*Pattern compile = Pattern.compile(JAVA_UTIL_LIST_REGEX);
                    Matcher matcher = compile.matcher(fieldQualifiedName);
                    if (matcher.find()) {
                        String group = matcher.group(1);
                        if (group.startsWith("com.everhomes.rest")) {
                            entry.setDesc(String.format("%s {@link %s}", field.getName(), group));
                        } else {
                            entry.setDesc(field.getName());
                        }
                    }*/
                } else {
                    entry.setDesc(field.getName());
                }
            }
            entries.add(entry);
        }
        return entries;
    }

    private static DocCommentService getInstance() {
        return new DocCommentService();
    }

    private String getDesc(Map<String, String> commentMap, PsiField field) {
        String fieldDesc = commentMap.get(field.getName());
        if (fieldDesc == null) {
            fieldDesc = commentMap.get(field.getText());
        }
        if (fieldDesc == null) {
            fieldDesc = commentMap.get(field.getText().replaceAll("\\(byte\\)", ""));
        }
        if (fieldDesc == null) {
            fieldDesc = commentMap.get(field.getText().replaceAll("\"", ""));
        }
        return fieldDesc;
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
