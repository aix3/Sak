package com.everhomes.sak.restapi;

import com.everhomes.sak.config.SakToolSettings;
import com.everhomes.sak.restapi.bean.RestAPIEntry;
import com.everhomes.sak.util.Util;
import com.everhomes.sak.util.VelocityUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiTypeElementImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.util.ui.TextTransferable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xq.tian on 2017/5/23.
 */
class RestAPIService {

    private int recursiveCount = 100;// 递归的最大层数，避免过深的递归

    private static final Logger log = Util.getLogger(RestAPIService.class);

    private static final String JAVA_UTIL_LIST_REGEX = "java\\.util\\.List<(.*?)>";
    private static final String REQUEST_MAPPING_REGEX = "@RequestMapping\\(\"(.*?)\"\\)";
    private static final String FIELD_COMMENT_REGEX = "<li>(.*?)[：:](.*?)</li>";

    private static final String REQUEST_MAPPING_ANNO = "@RequestMapping";
    private Project project;
    private PsiMethod currMethod;
    private SakToolSettings settings;

    static String genRestAPI(PsiClass psiClass, PsiElement element, SakToolSettings settings) {
        RestAPIService service = RestAPIService.getInstance();
        service.settings = settings;
        service.project = psiClass.getProject();
        return service.genRestAPI(psiClass, element);
    }

    private String genRestAPI(PsiClass psiClass, PsiElement element) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return "Can not find psiClass qualifiedName";
        }

        if (!qualifiedName.endsWith("Controller")) {
            return "Please in controller method";
        }

        String clzBody = psiClass.getText();
        String clzRequestMapping = this.getRequestMapping(clzBody);

        currMethod = this.getCurrentMethod(element);

        if (currMethod == null) {
            return "Please in controller method";
        }

        String methodBody = currMethod.getText();
        String methodRequestMapping = this.getRequestMapping(methodBody);

        final String requestMapping = clzRequestMapping + "/" + methodRequestMapping;
        log.debug("find request mapping is " + requestMapping);

        final List<String> cmdList = getCmdList(currMethod);

        project = psiClass.getProject();

        RestAPIEntry apiEntry = this.getRestAPIEntry(requestMapping);
        List<RestAPIEntry> cmdEntryList = this.getCmdEntries(cmdList);
        List<RestAPIEntry> respEntryList = this.getRespEntries(apiEntry.getRespQName());

        Map<String, Object> templateMap = Maps.newHashMap();
        apiEntry.setCmdQName("参数：" + apiEntry.getCmdQName());
        apiEntry.setRespQName("返回值：" + apiEntry.getRespQName());
        templateMap.put("apiEntry", apiEntry);
        templateMap.put("cmdEntries", cmdEntryList);
        templateMap.put("respEntries", respEntryList);
        templateMap.put("render", new RestAPIEntryRender());

        String template = settings.getTemplate("RestAPI");
        String content = VelocityUtil.evaluate(template, templateMap);

        CopyPasteManager.getInstance().setContents(new TextTransferable(content));

        Util.setStatusBarText(project, "The API has been copied to the clipboard");
        return "OK";
    }

    private List<RestAPIEntry> getRespEntries(String respQName) {
        if ("OK".equals(respQName)) {
            return Lists.newArrayList();
        }
        PsiClass aClass = Util.getClassByQName(project, respQName);
        if (aClass != null) {
            Map<String, String> commentMap = getDocCommentMap(aClass);
            return processAPIEntries(aClass, commentMap, false);
        }
        return Lists.newArrayList();
    }

    private RestAPIEntry getRestAPIEntry(String requestMapping) {
        RestAPIEntry entry = new RestAPIEntry();
        entry.setApi(requestMapping);
        entry.setApiDesc("");
        PsiDocComment docComment = currMethod.getDocComment();
        if (docComment != null) {
            String text = docComment.getText();
            Pattern compile = Pattern.compile("<p>(.*?)</p>");
            Matcher matcher = compile.matcher(text);
            if (matcher.find()) {
                entry.setApiDesc(matcher.group(1));
            }
        }
        List<String> cmdList = getCmdList(currMethod);
        if (cmdList.size() > 0) {
            entry.setCmdQName(cmdList.get(0));
        } else {
            entry.setCmdQName("无");
        }
        PsiAnnotation annotation = currMethod.getModifierList().findAnnotation("com.everhomes.discover.RestReturn");
        if (annotation != null) {
            PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
            PsiAnnotationMemberValue collection = annotation.findAttributeValue("collection");
            String respQName = ((PsiTypeElementImpl) value.getFirstChild()).getType().getCanonicalText();
            String collectionFlag = collection.getText();
            entry.setRespQName(respQName.equals("java.lang.String") ? "OK" : respQName);
            if (collectionFlag != null) {
                entry.setRespCollectionFlag(Boolean.valueOf(collectionFlag));
            }
        }
        return entry;
    }

    private List<RestAPIEntry> getCmdEntries(List<String> cmdList) {
        for (String cmd : cmdList) {
            PsiClass aClass = Util.getClassByQName(project, cmd);
            Map<String, String> commentMap = getDocCommentMap(aClass);
            return processAPIEntries(aClass, commentMap, false);
        }
        return Lists.newArrayList();
    }

    @NotNull
    private Map<String, String> getDocCommentMap(PsiClass aClass) {
        Map<String, String> commentMap = Maps.newHashMap();
        PsiDocComment docComment = aClass.getDocComment();
        if (docComment != null) {
            String text = docComment.getText();
            Pattern compile = Pattern.compile(FIELD_COMMENT_REGEX);
            Matcher matcher = compile.matcher(text);
            while (matcher.find()) {
                commentMap.put(matcher.group(1).trim(), matcher.group(2).trim());
            }
        }
        return commentMap;
    }

    private List<RestAPIEntry> processAPIEntries(PsiClass psiClass, Map<String, String> commentMap, boolean isEnum) {
        List<RestAPIEntry> entryList = Lists.newArrayList();

        recursiveCount--;
        if (recursiveCount < 0) {
            return entryList;
        }

        for (PsiField field : psiClass.getAllFields()) {
            if (isEnum && !field.getType().getCanonicalText().equals(psiClass.getQualifiedName())) {
                continue;
            }
            RestAPIEntry entry = new RestAPIEntry();
            entry.setFieldName(field.getName());

            if (isEnum) {
                entry.setFieldName(field.getText());
            }

            String fieldDesc = getFieldDesc(commentMap, field);

            String fieldQualifiedName = field.getType().getCanonicalText();
            if (fieldQualifiedName.startsWith("com.everhomes.rest")) {
                if (!isEnum && !fieldDesc.contains("@link")) {
                    fieldDesc += String.format("%s {@link %s}", field.getName(), fieldQualifiedName);
                }
                entry.setLink(true);
                PsiClass fieldClass = Util.getClassByQName(project, fieldQualifiedName);
                if (!isEnum) {
                    boolean selfRef = false;
                    if (fieldClass.isEnum() || fieldQualifiedName.equals(psiClass.getQualifiedName())) {
                        selfRef = true;
                    }
                    entry.getLinkTo().addAll(processAPIEntries(fieldClass, getDocCommentMap(fieldClass), selfRef));
                }
            } else if (fieldQualifiedName.startsWith("java.util.List")) {
                Pattern compile = Pattern.compile(JAVA_UTIL_LIST_REGEX);
                Matcher matcher = compile.matcher(fieldQualifiedName);
                if (matcher.find()) {
                    String group = matcher.group(1);
                    if (group.startsWith("com.everhomes.rest")) {
                        if (!fieldDesc.contains("@link")) {
                            fieldDesc = String.format("%s {@link %s}", fieldDesc, group);
                        }
                        entry.setLink(true);
                        PsiClass fieldClass = Util.getClassByQName(project, group);
                        boolean selfRef = false;
                        if (fieldClass.isEnum() || group.equals(psiClass.getQualifiedName())) {
                            selfRef = true;
                        }
                        entry.getLinkTo().addAll(processAPIEntries(fieldClass, getDocCommentMap(fieldClass), selfRef));
                    }
                }
            } else if (fieldDesc != null && fieldDesc.contains("@link")) {
                Pattern compile = Pattern.compile("\\{@link\\s*(.*?)\\s*\\}");
                Matcher matcher = compile.matcher(fieldDesc);
                if (matcher.find()) {
                    String linkTo = matcher.group(1);
                    PsiClass linkToClass = Util.getClassByQName(project, linkTo);
                    if (linkToClass != null) {
                        boolean selfRef = false;
                        if (linkToClass.isEnum() || linkTo.equals(psiClass.getQualifiedName())) {
                            selfRef = true;
                        }

                        entry.getLinkTo().addAll(processAPIEntries(linkToClass, getDocCommentMap(linkToClass), selfRef));
                        entry.setLink(true);
                    }
                }
            }
            entry.setFieldDesc(fieldDesc);
            entryList.add(entry);
        }
        return entryList;
    }

    private String getFieldDesc(Map<String, String> commentMap, PsiField field) {
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
        fieldDesc = (fieldDesc != null && fieldDesc.trim().length() > 0) ? fieldDesc : field.getName();
        return fieldDesc;
    }

    private static RestAPIService getInstance() {
        return new RestAPIService();
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

    private String getRequestMapping(String methodBody) {
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
    }

    private List<String> getCmdList(PsiMethod currMethod) {
        List<String> cmdList = Lists.newArrayList();
        PsiParameterList parameterList = currMethod.getParameterList();
        for (PsiParameter param : parameterList.getParameters()) {
            PsiTypeElement typeElement = param.getTypeElement();
            if (typeElement != null) {
                String cmdQualifyName = param.getType().getCanonicalText();
                cmdList.add(cmdQualifyName);
            }
        }
        return cmdList;
    }
}
