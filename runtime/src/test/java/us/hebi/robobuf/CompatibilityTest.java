/*-
 * #%L
 * robobuf-runtime
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

package us.hebi.robobuf;

import com.google.protobuf.ByteString;
import org.junit.Test;
import protos.test.java.ForeignEnum;
import protos.test.java.ForeignMessage;
import protos.test.java.RepeatedPackables;
import protos.test.java.TestAllTypes;
import protos.test.java.TestAllTypes.NestedEnum;
import protos.test.java.external.ImportEnum;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * "ground-truth" data generated by protobuf-Java
 *
 * @author Florian Enner
 * @since 13 Aug 2019
 */
public class CompatibilityTest {

    /**
     * Make sure Java bindings are still equal after doing a round-trip
     */
    @Test
    public void testCompatibilityWithProtobufJava() throws IOException {
        byte[] serializedMsg = getCombinedMessage();
        TestAllTypes.Builder expected = TestAllTypes.newBuilder();
        protos.test.robo.TestAllTypes msg = protos.test.robo.TestAllTypes.newInstance();

        // multiple merges to check expanding repeated behavior
        for (int i = 0; i < 3; i++) {
            expected.mergeFrom(serializedMsg);
            msg.mergeFrom(ProtoSource.newInstance(serializedMsg));
        }

        assertEquals(expected.build(), TestAllTypes.parseFrom(msg.toByteArray()));
        assertEquals(msg, protos.test.robo.TestAllTypes.parseFrom(msg.toByteArray()));

    }

    static byte[] getCombinedMessage() throws IOException {
        return TestAllTypes.newBuilder()
                .mergeFrom(optionalPrimitives())
                .mergeFrom(optionalMessages())
                .mergeFrom(optionalBytes())
                .mergeFrom(optionalEnums())
                .mergeFrom(optionalString())
                .mergeFrom(repeatedMessages())
                .mergeFrom(repeatedBytes())
                .mergeFrom(repeatedStrings())
                .mergeFrom(repeatedBytes())
                .addAllRepeatedPackedInt32(Arrays.asList(-1, 0, 1, 2, 3, 4, 5))
                .addAllRepeatedInt32(Arrays.asList(-2, -1, 0, 1, 2, 3, 4, 5))
                .build()
                .toByteArray();
    }

    static byte[] optionalPrimitives() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalBool(true)
                .setOptionalDouble(100.0d)
                .setOptionalFloat(101.0f)
                .setOptionalFixed32(102)
                .setOptionalFixed64(103)
                .setOptionalSfixed32(104)
                .setOptionalSfixed64(105)
                .setOptionalSint32(106)
                .setOptionalSint64(107)
                .setOptionalInt32(108)
                .setOptionalInt64(109)
                .setOptionalUint32(110)
                .setOptionalUint64(111)
                .build();
        return msg.toByteArray();
    }

    static byte[] repeatedPackablesNonPacked() {
        RepeatedPackables.NonPacked msg = RepeatedPackables.NonPacked.newBuilder()
                .addAllBools(Arrays.asList(true, false, true, true))
                .addAllDoubles(Arrays.asList(Double.POSITIVE_INFINITY, -2d, 3d, 4d))
                .addAllFloats(Arrays.asList(10f, 20f, -30f, Float.NaN))
                .addAllFixed32S(Arrays.asList(2, -2, 4, 67423))
                .addAllFixed64S(Arrays.asList(3231313L, 6L, -7L, 8L))
                .addAllSfixed32S(Arrays.asList(2, -3, 4, 5))
                .addAllSfixed64S(Arrays.asList(5L, -6L, 7L, -8L))
                .addAllSint32S(Arrays.asList(2, -3, 4, 5))
                .addAllSint64S(Arrays.asList(5L, 6L, -7L, 8L))
                .addAllInt32S(Arrays.asList(2, 3, -4, 5))
                .addAllInt64S(Arrays.asList(5L, -6L, 7L, 8L))
                .addAllUint32S(Arrays.asList(2, 300, 4, 5))
                .addAllUint64S(Arrays.asList(5L, 6L, 23L << 40, 8L))
                .build();
        return msg.toByteArray();
    }

    static byte[] repeatedPackablesPacked() {
        RepeatedPackables.Packed msg = RepeatedPackables.Packed.newBuilder()
                .addAllBools(Arrays.asList(true, false, true, true))
                .addAllDoubles(Arrays.asList(Double.POSITIVE_INFINITY, -2d, 3d, 4d))
                .addAllFloats(Arrays.asList(10f, 20f, -30f, Float.NaN))
                .addAllFixed32S(Arrays.asList(2, -2, 4, 67423))
                .addAllFixed64S(Arrays.asList(3231313L, 6L, -7L, 8L))
                .addAllSfixed32S(Arrays.asList(2, -3, 4, 5))
                .addAllSfixed64S(Arrays.asList(5L, -6L, 7L, -8L))
                .addAllSint32S(Arrays.asList(2, -3, 4, 5))
                .addAllSint64S(Arrays.asList(5L, 6L, -7L, 8L))
                .addAllInt32S(Arrays.asList(2, 3, -4, 5))
                .addAllInt64S(Arrays.asList(5L, -6L, 7L, 8L))
                .addAllUint32S(Arrays.asList(2, 300, 4, 5))
                .addAllUint64S(Arrays.asList(5L, 6L, 23L << 40, 8L))
                .build();
        return msg.toByteArray();
    }

    static byte[] optionalEnums() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalNestedEnum(NestedEnum.FOO)
                .setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR)
                .setOptionalImportEnum(ImportEnum.IMPORT_BAZ)
                .build();
        return msg.toByteArray();
    }

    static byte[] repeatedEnums() {
        return TestAllTypes.newBuilder()
                .addRepeatedNestedEnum(NestedEnum.FOO)
                .addRepeatedNestedEnum(NestedEnum.BAR)
                .addRepeatedNestedEnum(NestedEnum.BAZ)
                .addRepeatedNestedEnum(NestedEnum.BAZ)
                .build()
                .toByteArray();
    }

    static byte[] optionalString() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalString("optionalString\uD83D\uDCA9")
                .setOptionalCord("hello!")
                .build();
        return msg.toByteArray();
    }

    static byte[] optionalMessages() {
        TestAllTypes msg = TestAllTypes.newBuilder()
                .setOptionalNestedMessage(TestAllTypes.NestedMessage.newBuilder().setBb(2).build())
                .setOptionalForeignMessage(ForeignMessage.newBuilder().setC(3).build())
                .build();
        return msg.toByteArray();
    }

    static byte[] repeatedMessages() {
        return TestAllTypes.newBuilder()
                .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(0))
                .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(1))
                .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(2))
                .build()
                .toByteArray();
    }

    static byte[] repeatedStrings() {
        return TestAllTypes.newBuilder()
                .addAllRepeatedString(Arrays.asList("hello", "world", "ascii", "utf8\uD83D\uDCA9"))
                .build()
                .toByteArray();
    }

    static byte[] optionalBytes() {
        byte[] randomBytes = new byte[256];
        new Random(0).nextBytes(randomBytes);
        return TestAllTypes.newBuilder()
                .setOptionalBytes(ByteString.copyFromUtf8("utf8\uD83D\uDCA9"))
                .setDefaultBytes(ByteString.copyFrom(randomBytes))
                .build()
                .toByteArray();
    }

    static byte[] repeatedBytes() {
        return TestAllTypes.newBuilder()
                .addRepeatedBytes(ByteString.copyFromUtf8("ascii"))
                .addRepeatedBytes(ByteString.copyFromUtf8("utf8\uD83D\uDCA9"))
                .build()
                .toByteArray();
    }

}


