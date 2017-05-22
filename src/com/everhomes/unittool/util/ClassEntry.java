package com.everhomes.unittool.util;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;

import java.util.List;

/**
 * @author hansong.xhs
 * @version $Id: ClassEntry.java, v 0.1 2017-01-22 9:53 hansong.xhs Exp $$
 */
public class ClassEntry {

    private String className;

    private String packageName;

    private List<String> importList;

    private List<Field> fields;

    private List<Field> allFields;

    private List<Method> methods;

    private List<Method> allMethods;

    public void setClassName(String className) {
        this.className = className;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setImportList(List<String> importList) {
        this.importList = importList;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public void setAllFields(List<Field> allFields) {
        this.allFields = allFields;
    }

    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

    public void setAllMethods(List<Method> allMethods) {
        this.allMethods = allMethods;
    }

    public ClassEntry(String className, String packageName, List<String> importList, List<Field> fields, List<Field> allFields, List<Method> methods, List<Method> allMethods) {
        this.className = className;
        this.packageName = packageName;
        this.importList = importList;
        this.fields = fields;
        this.allFields = allFields;
        this.methods = methods;
        this.allMethods = allMethods;
    }

    public static class Method {
        /**
         * method name
         */
        private String name;

        /**
         * the method modifier, like "private",or "@Setter private" if include annotations
         */
        private String modifier;

        /**
         * the method returnType
         */
        private String returnType;

        /**
         * the method params, like "(String name)"
         */
        private String params;

        public void setName(String name) {
            this.name = name;
        }

        public void setModifier(String modifier) {
            this.modifier = modifier;
        }

        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }

        public void setParams(String params) {
            this.params = params;
        }

        public Method(String name, String modifier, String returnType, String params) {
            this.name = name;
            this.modifier = modifier;
            this.returnType = returnType;
            this.params = params;
        }
    }

    public static class Field {
        /**
         * field type
         */
        private String type;

        /**
         * field name
         */
        private String name;

        /**
         * the field modifier, like "private",or "@Setter private" if include annotations
         */
        private String modifier;

        public Field(String type, String name, String modifier) {
            this.type = type;
            this.name = name;
            this.modifier = modifier;
        }

        public void setType(String type) {

            this.type = type;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setModifier(String modifier) {
            this.modifier = modifier;
        }
    }

    private ClassEntry() {

    }

    public static ClassEntry create(PsiClass psiClass) {
        PsiJavaFile javaFile = (PsiJavaFile) psiClass.getContainingFile();
        ClassEntry classEntry = new ClassEntry();
        classEntry.setClassName(psiClass.getName());
        classEntry.setPackageName(javaFile.getPackageName());
        classEntry.setImportList(CodeMakerUtil.getImportList(javaFile));
        classEntry.setFields(CodeMakerUtil.getFields(psiClass));
        classEntry.setAllFields(CodeMakerUtil.getAllFields(psiClass));
        classEntry.setMethods(CodeMakerUtil.getMethods(psiClass));
        classEntry.setAllMethods(CodeMakerUtil.getAllMethods(psiClass));
        return classEntry;
    }

}
