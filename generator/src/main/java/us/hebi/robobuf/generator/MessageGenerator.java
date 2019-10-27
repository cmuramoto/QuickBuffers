/*-
 * #%L
 * robobuf-generator
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package us.hebi.robobuf.generator;

import com.squareup.javapoet.*;
import us.hebi.robobuf.generator.RequestInfo.ExpectedIncomingOrder;
import us.hebi.robobuf.generator.RequestInfo.MessageInfo;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static us.hebi.robobuf.generator.BitField.*;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class MessageGenerator {

    MessageGenerator(MessageInfo info) {
        this.info = info;
        info.getFields().forEach(f -> fields.add(new FieldGenerator(f)));
        numBitFields = BitField.getNumberOfFields(info.getFields().size());
    }

    TypeSpec generate() {
        TypeSpec.Builder type = TypeSpec.classBuilder(info.getTypeName())
                .superclass(ParameterizedTypeName.get(RuntimeClasses.AbstractMessage, info.getTypeName()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        if (info.isNested()) {
            type.addModifiers(Modifier.STATIC);
        }

        if (!info.isNested()) {
            // Note: constants from enums and fields may have the same names
            // as constants in the nested classes. This causes Java warnings,
            // but is not fatal, so we suppress those warnings in the top-most
            // class declaration /javanano
            type.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "hiding")
                    .build());
        }

        // Nested Enums
        info.getNestedEnums().stream()
                .map(EnumGenerator::new)
                .map(EnumGenerator::generate)
                .forEach(type::addType);

        // Nested Types
        info.getNestedTypes().stream()
                .map(MessageGenerator::new)
                .map(MessageGenerator::generate)
                .forEach(type::addType);

        // newInstance() method
        type.addMethod(MethodSpec.methodBuilder("newInstance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(info.getTypeName())
                .addStatement("return new $T()", info.getTypeName())
                .build());

        // Constructor
        type.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addStatement("$L", BitField.setBit(0)) // dummy for triggering clear
                .addStatement("clear()")
                .build());

        // Member state (the first two bitfields are in the parent class)
        for (int i = 2; i < numBitFields; i++) {
            type.addField(FieldSpec.builder(int.class, BitField.fieldName(i), Modifier.PRIVATE).build());
        }
        fields.forEach(f -> f.generateMemberFields(type));

        // Fields accessors
        fields.forEach(f -> f.generateMemberMethods(type));
        generateCopyFrom(type);
        generateClear(type);
        generateEquals(type);
        generateHashCode(type);
        generateWriteTo(type);
        generateComputeSerializedSize(type);
        generateMergeFrom(type);
        generateIsInitialized(type);
        generatePrint(type);
        generateClone(type);

        // Static utilities
        generateParseFrom(type);
        generateMessageFactory(type);
        type.addField(FieldSpec.builder(TypeName.LONG, "serialVersionUID")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("0L")
                .build());

        return type.build();
    }

    private void generateClear(TypeSpec.Builder type) {
        type.addMethod(generateClearCode("clear", true));
        type.addMethod(generateClearCode("clearQuick", false));
    }

    private MethodSpec generateClearCode(String name, boolean isFullClear) {
        MethodSpec.Builder clear = MethodSpec.methodBuilder(name)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName());

        // no fields set -> no need to clear (e.g. unused nested messages)
        // NOTE: always make sure that the constructor creates conditions that clears everything
        clear.beginControlFlow("if ($N)", BitField.hasNoBits(numBitFields))
                .addStatement("return this")
                .endControlFlow();

        // clear has state
        clear.addStatement("cachedSize = -1");
        for (int i = 0; i < numBitFields; i++) {
            clear.addStatement("$L = 0", BitField.fieldName(i));
        }

        if (isFullClear) {
            fields.forEach(field -> field.generateClearCode(clear));
        } else {
            fields.forEach(field -> field.generateClearQuickCode(clear));
        }

        clear.addStatement("return this");
        return clear.build();
    }

    private void generateEquals(TypeSpec.Builder type) {
        MethodSpec.Builder equals = MethodSpec.methodBuilder("equals")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(Object.class, "o");

        // Reference equality check
        equals.beginControlFlow("if (o == this)")
                .addStatement("return true")
                .endControlFlow();

        // Type check
        equals.beginControlFlow("if (!(o instanceof $T))", info.getTypeName())
                .addStatement("return false")
                .endControlFlow();
        equals.addStatement("$1T other = ($1T) o", info.getTypeName());

        // Check whether all of the same fields are set
        if (info.getFieldCount() > 0) {
            equals.addCode("return $1L == other.$1L$>", BitField.fieldName(0));
            for (int i = 1; i < numBitFields; i++) {
                equals.addCode("\n&& $1L == other.$1L", BitField.fieldName(i));
            }

            for (FieldGenerator field : fields) {
                equals.addCode("\n&& (!$1N() || ", field.getInfo().getHazzerName());
                field.generateEqualsStatement(equals);
                equals.addCode(")");
            }

            equals.addCode(";$<\n");
        } else {
            equals.addCode("return true;\n");
        }

        type.addMethod(equals.build());
    }

    private void generateHashCode(TypeSpec.Builder type) {
        MethodSpec.Builder hashCode = MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class);
        hashCode.addJavadoc("" +
                "Messages have no immutable state and should not\n" +
                "be used in hashing structures. This implementation\n" +
                "returns a constant value in order to satisfy the\n" +
                "contract.\n");
        hashCode.addStatement("return 0");
        type.addMethod(hashCode.build());
    }

    private void generateMergeFrom(TypeSpec.Builder type) {
        MethodSpec.Builder mergeFrom = MethodSpec.methodBuilder("mergeFrom")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName())
                .addParameter(RuntimeClasses.ProtoSource, "input", Modifier.FINAL)
                .addException(IOException.class);

        // Fallthrough optimization:
        //
        // Reads tag after case parser and checks if it can fall-through. In the ideal case if all fields are set
        // and the expected order matches the incoming data, the switch would only need to be executed once
        // for the first field.
        //
        // Packable fields make this a bit more complex since they need to generate two cases to preserve
        // backwards compatibility. However, any production proto file should already be using the packed
        // option whenever possible, so we don't need to optimize the non-packed case.
        final boolean enableFallthroughOptimization = info.getExpectedIncomingOrder() != ExpectedIncomingOrder.Random;
        final List<FieldGenerator> sortedFields = new ArrayList<>(fields);
        switch (info.getExpectedIncomingOrder()) {
            case AscendingNumber:
                sortedFields.sort(FieldUtil.AscendingNumberSorter);
                break;
            case Robobuf: // keep existing order
            case Random: // no optimization
                break;
        }

        if (enableFallthroughOptimization) {
            mergeFrom.addComment("Enabled Fall-Through Optimization (" + info.getExpectedIncomingOrder() + ")");
            mergeFrom.addStatement("int tag = input.readTag()");
            mergeFrom.beginControlFlow("while (true)");
        } else {
            mergeFrom.beginControlFlow("while (true)");
            mergeFrom.addStatement("int tag = input.readTag()");
        }
        mergeFrom.beginControlFlow("switch (tag)");

        // Add fields by the expected order and type
        for (int i = 0; i < sortedFields.size(); i++) {
            FieldGenerator field = sortedFields.get(i);

            // Assume all packable fields are written packed. Add non-packed cases to the end.
            if (field.getInfo().isPackable()) {
                mergeFrom.beginControlFlow("case $L:", field.getInfo().getPackedTag());
                field.generateMergingCodeFromPacked(mergeFrom);
            } else {
                mergeFrom.beginControlFlow("case $L:", field.getInfo().getTag());
                field.generateMergingCode(mergeFrom);
            }

            if (enableFallthroughOptimization) {
                // try falling to 0 (exit) at last field
                final int nextCase = (i == sortedFields.size() - 1) ? 0 : getPackedTagOrTag(sortedFields.get(i + 1));
                mergeFrom.beginControlFlow("if((tag = input.readTag()) != $L)", nextCase);
                mergeFrom.addStatement("break");
                mergeFrom.endControlFlow();
            } else {
                mergeFrom.addStatement("break");
            }
            mergeFrom.endControlFlow();

        }

        // zero means invalid tag / end of data
        mergeFrom.beginControlFlow("case 0:")
                .addStatement("return this")
                .endControlFlow();

        // default case -> skip field
        mergeFrom.beginControlFlow("default:")
                .beginControlFlow("if (!input.skipField(tag))")
                .addStatement("return this")
                .endControlFlow();
        if (enableFallthroughOptimization) {
            mergeFrom.addStatement("tag = input.readTag()");
        }
        mergeFrom.addStatement("break").endControlFlow();

        // Generate missing non-packed cases for packable fields for compatibility reasons
        for (FieldGenerator field : sortedFields) {
            if (field.getInfo().isPackable()) {
                mergeFrom.beginControlFlow("case $L:", field.getInfo().getTag());
                field.generateMergingCode(mergeFrom);
                if (enableFallthroughOptimization) {
                    mergeFrom.addStatement("tag = input.readTag()");
                }
                mergeFrom.addStatement("break").endControlFlow();
            }
        }

        mergeFrom.endControlFlow();
        mergeFrom.endControlFlow();
        type.addMethod(mergeFrom.build());
    }

    private int getPackedTagOrTag(FieldGenerator field) {
        if (field.getInfo().isPackable())
            return field.getInfo().getPackedTag();
        return field.getInfo().getTag();
    }

    private void generateWriteTo(TypeSpec.Builder type) {
        MethodSpec.Builder writeTo = MethodSpec.methodBuilder("writeTo")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(RuntimeClasses.ProtoSink, "output", Modifier.FINAL)
                .addException(IOException.class);
        fields.forEach(f -> {
            writeTo.beginControlFlow("if " + f.getInfo().getHasBit());
            f.generateSerializationCode(writeTo);

            if (f.getInfo().isRequired()) {
                String error = "Message is missing required field (" + f.getInfo().getLowerName() + ")";
                writeTo.nextControlFlow("else")
                        .addStatement("throw new $T($S)", IllegalStateException.class, error);
            }

            writeTo.endControlFlow();
        });
        type.addMethod(writeTo.build());
    }

    private void generateComputeSerializedSize(TypeSpec.Builder type) {
        MethodSpec.Builder computeSerializedSize = MethodSpec.methodBuilder("computeSerializedSize")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(int.class);
        computeSerializedSize.addStatement("int size = 0");
        fields.forEach(f -> {
            computeSerializedSize.beginControlFlow("if " + f.getInfo().getHasBit());
            f.generateComputeSerializedSizeCode(computeSerializedSize);
            computeSerializedSize.endControlFlow();
        });
        computeSerializedSize.addStatement("return size");
        type.addMethod(computeSerializedSize.build());
    }

    private void generateCopyFrom(TypeSpec.Builder type) {
        MethodSpec.Builder copyFrom = MethodSpec.methodBuilder("copyFrom")
                .addAnnotation(Override.class)
                .addParameter(info.getTypeName(), "other", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName());
        copyFrom.addStatement("cachedSize = other.cachedSize");
        for (int i = 0; i < numBitFields; i++) {
            copyFrom.addStatement("$1L = other.$1L", BitField.fieldName(i));
        }
        fields.forEach(field -> field.generateCopyFromCode(copyFrom));
        copyFrom.addStatement("return this"); // TODO: remember dirty bit
        type.addMethod(copyFrom.build());
    }

    private void generateClone(TypeSpec.Builder type) {
        type.addSuperinterface(Cloneable.class);
        type.addMethod(MethodSpec.methodBuilder("clone")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName())
                .addStatement("return new $T().copyFrom(this)", info.getTypeName())
                .build());
    }

    private void generateParseFrom(TypeSpec.Builder type) {
        type.addMethod(MethodSpec.methodBuilder("parseFrom")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addException(RuntimeClasses.InvalidProtocolBufferException)
                .addParameter(byte[].class, "data", Modifier.FINAL)
                .returns(info.getTypeName())
                .addStatement("return $T.mergeFrom(new $T(), data)", RuntimeClasses.AbstractMessage, info.getTypeName())
                .build());
    }

    private void generateIsInitialized(TypeSpec.Builder type) {
        MethodSpec.Builder isInitialized = MethodSpec.methodBuilder("isInitialized")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(boolean.class);

        // check if all required bits are set
        final int numFields = fields.size();
        int i = 0;
        for (int bitFieldIndex = 0; bitFieldIndex < numBitFields && i < numFields; bitFieldIndex++) {

            // Generate a single number that contains all required bits
            int bits = 0;
            for (int bit = 0; bit < BITS_PER_FIELD && i < numFields; bit++, i++) {
                if (fields.get(i).getInfo().isRequired()) {
                    bits |= 1 << bit;
                }
            }

            // Check up to 32 fields at the same time
            if (bits != 0) {
                isInitialized.beginControlFlow("if ($L)", BitField.isMissingRequiredBits(bitFieldIndex, bits))
                        .addStatement("return false")
                        .endControlFlow();
            }

        }

        // Check sub-messages (including optional and repeated)
        fields.stream()
                .map(FieldGenerator::getInfo)
                .filter(RequestInfo.FieldInfo::isMessageOrGroup)
                .forEach(field -> {
                    if (field.isRequired()) {
                        // has bit was already checked
                        isInitialized.beginControlFlow("if (!$N.isInitialized())", field.getFieldName());
                    } else {
                        // We need to check has bit ourselves
                        isInitialized.beginControlFlow("if ($L() && !$N.isInitialized())", field.getHazzerName(), field.getFieldName());
                    }
                    isInitialized
                            .addStatement("return false")
                            .endControlFlow();
                });

        isInitialized.addStatement("return true");
        type.addMethod(isInitialized.build());
    }

    private void generatePrint(TypeSpec.Builder type) {
        MethodSpec.Builder print = MethodSpec.methodBuilder("print")
                .addAnnotation(Override.class)
                .addParameter(RuntimeClasses.ProtoPrinter, "printer", Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC);

        // add every set field
        for (FieldGenerator field : fields) {
            print.beginControlFlow("if ($L)", BitField.hasBit(field.getInfo().getFieldIndex()));
            field.generatePrintCode(print);
            print.endControlFlow();
        }

        type.addMethod(print.build());
    }

    private void generateMessageFactory(TypeSpec.Builder type) {
        ParameterizedTypeName factoryReturnType = ParameterizedTypeName.get(RuntimeClasses.MessageFactory, info.getTypeName());
        ClassName factoryTypeName = info.getTypeName().nestedClass(info.getTypeName().simpleName() + "Factory");

        MethodSpec factoryMethod = MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getTypeName())
                .addStatement("return $T.newInstance()", info.getTypeName())
                .build();

        TypeSpec factoryEnum = TypeSpec.enumBuilder(factoryTypeName.simpleName())
                .addModifiers(Modifier.PRIVATE)
                .addSuperinterface(factoryReturnType)
                .addEnumConstant("INSTANCE")
                .addMethod(factoryMethod)
                .build();

        type.addType(factoryEnum);

        type.addMethod(MethodSpec.methodBuilder("getFactory")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(factoryReturnType)
                .addStatement("return $T.INSTANCE", factoryTypeName)
                .build());

    }

    final MessageInfo info;
    final List<FieldGenerator> fields = new ArrayList<>();
    final int numBitFields;

}
