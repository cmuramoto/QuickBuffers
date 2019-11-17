/*-
 * #%L
 * quickbuf-generator
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

package us.hebi.quickbuf.generator;

import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.squareup.javapoet.*;
import us.hebi.quickbuf.generator.RequestInfo.EnumInfo;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
class EnumGenerator {

    EnumGenerator(EnumInfo info) {
        this.info = info;
    }

    TypeSpec generate() {
        TypeSpec.Builder type = TypeSpec.enumBuilder(info.getTypeName())
                .addModifiers(Modifier.PUBLIC);

        // Add enum constants
        for (EnumValueDescriptorProto value : info.getValues()) {
            String constName = value.getName() + "_VALUE";
            String enumName = NamingUtil.filterKeyword(value.getName());

            FieldSpec constField = FieldSpec.builder(int.class, constName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("$L", value.getNumber()).build();

            type.addField(constField);
            type.addEnumConstant(enumName, TypeSpec.anonymousClassBuilder("$L", value.getNumber()).build());
        }

        generateGetValue(type);
        generateForNumber(type);
        generateConstructor(type);
        return type.build();
    }

    private void generateConstructor(TypeSpec.Builder typeSpec) {
        typeSpec.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(int.class, "value")
                .addStatement("this.$1N = $1N", "value")
                .build());
        typeSpec.addField(FieldSpec.builder(int.class, "value", Modifier.PRIVATE, Modifier.FINAL).build());
    }

    private void generateGetValue(TypeSpec.Builder typeSpec) {
        typeSpec.addMethod(MethodSpec.methodBuilder("getNumber")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return value")
                .build());
    }

    private void generateForNumber(TypeSpec.Builder typeSpec) {

        MethodSpec.Builder forNumber = MethodSpec.methodBuilder("forNumber")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(info.getTypeName())
                .addParameter(TypeName.INT, "value");

        if (info.isUsingArrayLookup()) {

            // (fast) lookup using array index
            forNumber.beginControlFlow("if (value < 0 || value > lookup.length)", info.getHighestNumber())
                    .addStatement("return null")
                    .endControlFlow();
            forNumber.addStatement("return lookup[value]");

            TypeName arrayType = ArrayTypeName.of(info.getTypeName());
            typeSpec.addField(FieldSpec.builder(arrayType, "lookup", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T[$L]", info.getTypeName(), info.getHighestNumber() + 1)
                    .build());

            CodeBlock.Builder initBlock = CodeBlock.builder();
            for (EnumValueDescriptorProto value : info.getValues()) {
                initBlock.addStatement("lookup[$L] = $L", value.getNumber(), NamingUtil.filterKeyword(value.getName()));
            }
            typeSpec.addStaticBlock(initBlock.build());

        } else {

            // lookup using switch statement
            forNumber.beginControlFlow("switch(value)");
            for (EnumValueDescriptorProto value : info.getValues()) {
                forNumber.addStatement("case $L: return $L", value.getNumber(), NamingUtil.filterKeyword(value.getName()));
            }
            forNumber.addStatement("default: return null");
            forNumber.endControlFlow();

        }

        typeSpec.addMethod(forNumber.build());

        typeSpec.addMethod(MethodSpec.methodBuilder("forNumberOr")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(info.getTypeName())
                .addParameter(int.class, "number")
                .addParameter(info.getTypeName(), "other")
                .addStatement("$T value = forNumber(number)", info.getTypeName())
                .addStatement("return value == null ? other : value")
                .build());

    }

    final EnumInfo info;

}