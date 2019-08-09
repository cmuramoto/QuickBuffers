package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo.FieldInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class StringField extends FieldGenerator {

    protected StringField(FieldInfo info) {
        super(info);
        m.put("serializableValue", info.getFieldName() + ".toString()");
        m.put("default", info.getDescriptor().getDefaultValue());
    }

    @Override
    public void generateMemberFields(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(RuntimeClasses.STRING_STORAGE_CLASS, info.getFieldName())
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T(0)", RuntimeClasses.STRING_STORAGE_CLASS)
                .build());
    }

    @Override
    protected void generateSetter(TypeSpec.Builder type) {
        MethodSpec setter = MethodSpec.methodBuilder(info.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(info.getTypeName(), "value")
                .returns(info.getParentType())
                .addNamedCode("" +
                        "$setHas:L;\n" +
                        "$name:L.setLength(0);\n" +
                        "$name:L.append(value);\n" +
                        "return this;\n", m)
                .build();
        type.addMethod(setter);
    }

    @Override
    public void generateClearCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:N.setLength(0);\n", m);
        if (!info.getDescriptor().getDefaultValue().isEmpty()) {
            method.addNamedCode("$name:N.append($default:S);\n", m);
        }
    }

    @Override
    public void generateCopyFromCode(MethodSpec.Builder method) {
        method.addNamedCode("$name:N.setLength(0);\n", m);
        method.addNamedCode("$name:N.append(other);\n", m);
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "$name:N.setLength(0);\n" +
                "$name:N.append(input.readString());\n" +
                "$setHas:L;\n", m);
    }

    @Override
    protected String getNamedNotEqualsStatement() {
        return "!$roboUtil:T.equals($name:N, other.$name:N)";
    }

}