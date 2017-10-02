import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.sun.xml.internal.ws.util.StringUtils;
import org.apache.http.util.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by krishna on 30/09/17.
 */
public class ModelCreatorWriteAction extends WriteCommandAction.Simple {
    private static final String SPACE = " ";
    private PsiDirectory psiDirectory;
    private List<Pair<String, String>> columnsList;
    private String className;
    private String tableName;
    private List<String> keywordList = Arrays.asList("primary key", "foreign key");

    protected ModelCreatorWriteAction(Project project, PsiDirectory psiClass, String tableCreationQuery) {
        super(project, psiClass.getContainingFile());
        psiDirectory = psiClass;
        columnsList = new ArrayList<>();
        parseQuery(tableCreationQuery);
    }

    private void parseQuery(String tableCreationQuery) {
        //get table name
        int tableWordIndex = !tableCreationQuery.contains("TABLE") ? tableCreationQuery.indexOf("table") : tableCreationQuery.indexOf("TABLE");
        tableName = tableCreationQuery.substring(tableWordIndex + 5, tableCreationQuery.indexOf("("));
        tableName = tableName.trim();
        className = StringUtils.capitalize(getColName(tableName));

        //get all columns with data type
        String columnsStr = tableCreationQuery.substring(tableCreationQuery.indexOf('(') + 1, tableCreationQuery.indexOf(");"));
        columnsStr = columnsStr.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\\(.*\\)", "");
        String columnsWithType[] = columnsStr.trim().split(",");
        for (String line : columnsWithType) {
            String columns[] = line.trim().split(" +", -1);
            if (columns.length >= 2) {
                String colName = columns[0].trim();
                String colType = columns[1].trim();
                if (isValidIdentifier(colName, colType))
                    columnsList.add(Pair.create(colName, colType));
            }
        }
    }

    private boolean isValidIdentifier(String colName, String colType) {
        if (TextUtils.isEmpty(colName) || TextUtils.isEmpty(colType))
            return false;
        return !keywordList.contains((colName + " " + colType).toLowerCase());
    }

    private String getColName(String name) {
        String splittedName[] = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String aSplittedName : splittedName) {
            sb.append(StringUtils.capitalize(aSplittedName));
        }
        return StringUtils.decapitalize(sb.toString());
    }

    @Override
    protected void run() throws Throwable {
        PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(getProject());
        PsiClass psiClass = psiElementFactory.createClass(className);
        psiClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        addFieldsToClass(psiClass);
        addColumnClassToClass(psiClass);
        generateQueryMethod(psiClass);
        addAccesor(psiClass);
        addMutator(psiClass);
        organiseCodesInsideClass(psiClass);
        psiDirectory.add(psiClass);
    }

    private void addColumnClassToClass(PsiClass psiClass) {
        PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(getProject());
        PsiClass columnClass = psiElementFactory.createClass("Column");
        columnClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        columnClass.getModifierList().setModifierProperty(PsiModifier.STATIC, true);

        StringBuilder columnsArrBuilder = new StringBuilder();
        for (Pair<String, String> columnsPair : columnsList) {
            String columnName = "COL_" + columnsPair.getFirst().toUpperCase();
            String fieldStr = "public static final String" + SPACE + columnName
                    + " = \""
                    + columnsPair.getFirst()
                    + "\";";
            columnsArrBuilder.append(columnName).append(",");
            PsiField fieldFromText = psiElementFactory.createFieldFromText(fieldStr, columnClass);
            columnClass.add(fieldFromText);
        }
        //delete last comma
        columnsArrBuilder.deleteCharAt(columnsArrBuilder.length() - 1);
        String allColumnsArr = "public static final String columns[] = {" + columnsArrBuilder.toString() + "};";
        PsiField allColumnsField = psiElementFactory.createFieldFromText(allColumnsArr, columnClass);
        columnClass.add(allColumnsField);

        psiClass.add(columnClass);
    }

    private void addFieldsToClass(PsiClass psiClass) {
        PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(getProject());
        // Add TABLE_NAME field
        String tableNameField = "public static final String TABLE_NAME = \"" + tableName + "\";";
        PsiField tableNameFieldFromText = psiElementFactory.createFieldFromText(tableNameField, psiClass);
        psiClass.add(tableNameFieldFromText);
        for (Pair<String, String> columnsPair : columnsList) {
            String fieldStr = "private" + SPACE + getFieldType(columnsPair.getSecond()) + SPACE + getColName(columnsPair.getFirst()) + ";";
            PsiField fieldFromText = psiElementFactory.createFieldFromText(fieldStr, psiClass);
            psiClass.add(fieldFromText);
        }
    }

    private void organiseCodesInsideClass(PsiClass psiClass) {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        styleManager.optimizeImports(psiClass.getContainingFile());
        styleManager.shortenClassReferences(psiClass.getContainingFile());
    }

    private void generateQueryMethod(PsiClass psiClass) {
        String classObjName = StringUtils.decapitalize(className);
        String classObjList = StringUtils.decapitalize(className) + "List";
        String methodName = "getAll" + className;
        //fetch all rows
        StringBuilder sb = new StringBuilder();
        sb.append("public static java.util.List<").append(className).append("> ").append(methodName).append("(android.content.Context context){")
                .append("java.util.List<").append(className).append("> ").append(classObjList).append(" = new java.util.ArrayList<>();")
                .append("android.database.sqlite.SQLiteDatabase db = DBHandler.getInstance(context).getReadableDatabase();")
                .append("android.database.Cursor cursor = db.query(TABLE_NAME, Column.columns, null, null, null, null, null);")
                .append("if(cursor != null && cursor.moveToFirst()){")
                .append("do {").append(className).append(SPACE).append(classObjName).append(" = new ").append(className).append("();")
                .append(getAllSettersLine(classObjName))
                .append(classObjList).append(".add(").append(classObjName).append(");")
                .append("} while(cursor.moveToNext());")
                .append("cursor.close();")
                .append("}")
                .append("return ").append(classObjList).append(";")
                .append("}");
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        psiClass.add(factory.createMethodFromText(sb.toString(), psiClass));
    }

    private String getAllSettersLine(String classObjName) {
        StringBuilder sb = new StringBuilder();
        for (Pair<String, String> columnPair : columnsList) {
            String columnName = getColName(columnPair.getFirst());
            sb.append(classObjName).append(".")
                    .append(getSetterForColumn(columnName))
                    .append("cursor.")
                    .append(getCursorMethodForType(columnPair.getSecond()))
                    .append("cursor.getColumnIndex(")
                    .append("Column.COL_")
                    .append(columnPair.getFirst().toUpperCase()).append(")")
                    .append(")")
                    .append(");");
        }
        return sb.toString();
    }

    private String getSetterForColumn(String rawColumn) {
        return "set" + StringUtils.capitalize(rawColumn) + "(";
    }

    private String getFieldType(String typeField) {
        String type = typeField.toLowerCase();
        switch (type) {
            case "int":
            case "integer":
                return "int";
            case "bigint":
                return "long";
            case "real":
            case "double":
            case "decimal":
            case "numeric":
                return "double";
            case "boolean":
                return "boolean";
            case "char":
                return "char";
            default:
                return "String";
        }
    }

    private String getCursorMethodForType(String typeField) {
        String type = typeField.toLowerCase();
        switch (type) {
            case "int":
            case "integer":
            case "boolean":
                return "getInt(";
            case "bigint":
                return "getLong(";
            case "real":
            case "double":
            case "decimal":
            case "numeric":
                return "getDouble(";
            default:
                return "getString(";
        }
    }

    private PsiClass addAccesor(PsiClass aClass) {
        PsiField[] psiFields = aClass.getFields();
        for (PsiField psiField : psiFields) {
            if (psiField.getModifierList().hasExplicitModifier(PsiModifier.STATIC))
                continue;
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(aClass.getProject());
            String method = generateGetter(psiField);
            aClass.add(factory.createMethodFromText(method, aClass));
        }
        return aClass;
    }

    public String generateGetter(PsiField psiField) {
        String name = psiField.getName();
        return "public " + psiField.getType().getPresentableText() + " get" + StringUtils.capitalize(name) + "() { return " + psiField.getName() + ";} ";
    }

    private PsiClass addMutator(PsiClass aClass) {
        PsiField[] psiFields = aClass.getFields();
        for (PsiField psiField : psiFields) {
            if (psiField.getModifierList().hasExplicitModifier(PsiModifier.FINAL))
                continue;
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(aClass.getProject());
            String method = generateSetter(psiField);
            aClass.add(factory.createMethodFromText(method, aClass));
        }
        return aClass;
    }

    private String generateSetter(PsiField psiField) {
        String name = psiField.getName();
        return "public void" + " set" + StringUtils.capitalize(name) + "(" + psiField.getType().getPresentableText() + " " + psiField.getName() + ") { this." + psiField.getName() + "=" + psiField.getName() + ";} ";
    }
}
