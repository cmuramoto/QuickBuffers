package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo.FieldInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 08 Aug 2019
 */
class PrimitiveField extends FieldGenerator {

    PrimitiveField(FieldInfo info) {
        super(info);
        m.put("default", getDefaultValue());
    }

    @Override
    public void generateMemberFields(TypeSpec.Builder type) {
        FieldSpec value = FieldSpec.builder(typeName, info.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .build();
        type.addField(value);
    }

    @Override
    protected void generateSetter(TypeSpec.Builder type) {
        MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(info.getTypeName(), "value")
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$setHas:L;\n" +
                        "$name:L = value;\n" +
                        "return this;\n", m)
                .build();
        type.addMethod(setter);
    }

    @Override
    public void generateClearCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:L = $default:L;\n", m);
    }

    @Override
    public void generateCopyFromCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:L = other.$name:L;\n", m);
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "$name:L = input.read$capitalizedType:L();\n" +
                "$setHas:L;\n", m);
    }

    @Override
    protected String getNamedNotEqualsStatement() {
        if (typeName == TypeName.FLOAT)
            return "(Float.floatToIntBits($name:L) != Float.floatToIntBits(other.$name:L))";
        else if (typeName == TypeName.DOUBLE)
            return "(Double.doubleToLongBits($name:L) != Double.doubleToLongBits(other.$name:L))";
        return "($name:L != other.$name:L)";
    }

    protected String getDefaultValue() {
        String value = info.getDescriptor().getDefaultValue();
        if (value.isEmpty()) {
            if (typeName == TypeName.FLOAT)
                return "0F";
            if (typeName == TypeName.DOUBLE)
                return "0D";
            if (typeName == TypeName.LONG)
                return "0L";
            if (typeName == TypeName.BOOLEAN)
                return "false";
            return "0";
        }

        // Special cased default values
        boolean isFloat = (typeName == TypeName.FLOAT);
        String constantClass = isFloat ? "Float" : "Double";
        switch (value) {
            case "nan":
                return constantClass + ".NaN";
            case "-inf":
                return constantClass + ".NEGATIVE_INFINITY";
            case "+inf":
            case "inf":
                return constantClass + ".POSITIVE_INFINITY";
        }

        // Add modifiers if needed
        char lastChar = Character.toUpperCase(value.charAt(value.length() - 1));

        if (typeName == TypeName.FLOAT && lastChar != 'F')
            return value + 'F';
        if (typeName == TypeName.DOUBLE && lastChar != 'D')
            return value + 'D';
        if (typeName == TypeName.LONG && lastChar != 'L')
            return value + 'L'; // lower case L may make it difficult to read number 1 (l=1?)
        return value;
    }

}