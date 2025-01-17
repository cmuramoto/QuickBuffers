/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.quickbuf;

import com.google.quickbuf.Struct;
import com.google.quickbuf.Value;
import org.junit.Test;
import protos.test.quickbuf.*;
import protos.test.quickbuf.LazyTypes.LazyMessage;
import protos.test.quickbuf.TestAllTypes.NestedEnum;
import protos.test.quickbuf.UnittestFieldOrder.MessageWithMultibyteNumbers;
import protos.test.quickbuf.UnittestRequired.TestAllTypesRequired;
import protos.test.quickbuf.external.ImportEnum;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;
import static us.hebi.quickbuf.ProtoUtil.Charsets.*;

/**
 * @author Florian Enner
 * @since 13 Aug 2019
 */
public class ProtoTests {

    @Test
    public void testOutputStreamSink() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.writeTo(ProtoSink.newInstance(baos));
        byte[] expected = msg.toByteArray();
        byte[] actual = baos.toByteArray();
        assertEquals(msg, TestAllTypes.parseFrom(actual));
        assertArrayEquals(expected, actual);
        assertArrayEquals(msg.toByteArray(), baos.toByteArray());
    }

    @Test
    public void testInputStreamSource() throws IOException {
        byte[] bytes = CompatibilityTest.getCombinedMessage();
        TestAllTypes actual = TestAllTypes.parseFrom(ProtoSource.newInstance(new ByteArrayInputStream(bytes)));
        assertEquals(TestAllTypes.parseFrom(bytes), actual);
    }

    @Test
    public void testByteBufferSource() throws IOException {
        byte[] bytes = CompatibilityTest.getCombinedMessage();
        TestAllTypes actual = TestAllTypes.parseFrom(ProtoSource.newInstance(ByteBuffer.wrap(bytes)));
        assertEquals(TestAllTypes.parseFrom(bytes), actual);
    }

    @Test
    public void testByteBufferSink() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        ByteBuffer buffer = ByteBuffer.allocate(msg.getSerializedSize());
        msg.writeTo(ProtoSink.newInstance(buffer));
        byte[] expected = msg.toByteArray();
        byte[] actual = buffer.array();
        assertEquals(msg, TestAllTypes.parseFrom(actual));
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testRepeatedByteSerialization() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        RepeatedByte bytes = RepeatedByte.newEmptyInstance();
        msg.writeTo(ProtoSink.newInstance(bytes));
        assertEquals(msg.getSerializedSize(), bytes.length);
        assertEquals(msg, TestAllTypes.parseFrom(ProtoSource.newInstance(bytes)));
    }

    @Test
    public void testSourceWithOffset() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        int offset = 31;
        int size = msg.getSerializedSize();
        byte[] bytes = new byte[offset + size];
        msg.writeTo(ProtoSink.newInstance(bytes, offset, size));
        assertEquals(msg, TestAllTypes.newInstance().mergeFrom(ProtoSource.newInstance(bytes, offset, size)));
    }

    @Test
    public void testDefaults() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        for (int i = 0; i < 2; i++) {
            assertTrue(msg.getDefaultBool());
            assertEquals(41, msg.getDefaultInt32());
            assertEquals(42, msg.getDefaultInt64());
            assertEquals(43, msg.getDefaultUint32());
            assertEquals(44, msg.getDefaultUint64());
            assertEquals(-45, msg.getDefaultSint32());
            assertEquals(46, msg.getDefaultSint64());
            assertEquals(47, msg.getDefaultFixed32());
            assertEquals(48, msg.getDefaultFixed64());
            assertEquals(49, msg.getDefaultSfixed32());
            assertEquals(-50, msg.getDefaultSfixed64());
            assertEquals(51.5f, msg.getDefaultFloat(), 0);
            assertEquals(52.0e3, msg.getDefaultDouble(), 0.0);
            assertEquals("hello", msg.getDefaultString());
            assertEquals("world", new String(msg.getDefaultBytes().toArray(), UTF_8));
            assertEquals("dünya", msg.getDefaultStringNonascii());
            assertEquals("dünyab", new String(msg.getDefaultBytesNonascii().toArray(), UTF_8));
            assertEquals(NestedEnum.BAR, msg.getDefaultNestedEnum());
            assertEquals(ForeignEnum.FOREIGN_BAR, msg.getDefaultForeignEnum());
            assertEquals(ImportEnum.IMPORT_BAR, msg.getDefaultImportEnum());
            assertEquals(Float.POSITIVE_INFINITY, msg.getDefaultFloatInf(), 0);
            assertEquals(Float.NEGATIVE_INFINITY, msg.getDefaultFloatNegInf(), 0);
            assertEquals(Float.NaN, msg.getDefaultFloatNan(), 0);
            assertEquals(Double.POSITIVE_INFINITY, msg.getDefaultDoubleInf(), 0);
            assertEquals(Double.NEGATIVE_INFINITY, msg.getDefaultDoubleNegInf(), 0);
            assertEquals(Double.NaN, msg.getDefaultDoubleNan(), 0);

            byte[] result = msg.toByteArray();
            int msgSerializedSize = msg.getSerializedSize();
            assertEquals(0, msgSerializedSize);
            assertEquals(result.length, msgSerializedSize);
            msg.clear();
        }
    }

    @Test
    public void testExtremeDefaults() {
        TestExtremeDefaultValues msg = TestExtremeDefaultValues.newInstance();
        assertEquals("[0, 1, 7, 8, 12, 10, 13, 9, 11, 92, 39, 34, -2]", msg.getEscapedBytes().toString());
        assertEquals(0xFFFFFFFF, msg.getLargeUint32());
        assertEquals(0xFFFFFFFFFFFFFFFFL, msg.getLargeUint64());
        assertEquals(2123456789, msg.getLargeFixed32());
        assertEquals(-8323287284586094827L, msg.getLargeFixed64());
        assertEquals(-0x7FFFFFFF, msg.getSmallInt32());
        assertEquals(-0x7FFFFFFFFFFFFFFFL, msg.getSmallInt64());
        assertEquals(-0x80000000, msg.getReallySmallInt32());
        assertEquals(-0x8000000000000000L, msg.getReallySmallInt64());
        assertEquals(new String("\341\210\264".getBytes(ISO_8859_1), UTF_8), msg.getUtf8String());

        assertTrue(ProtoUtil.isEqual(0f, msg.getZeroFloat()));
        assertTrue(ProtoUtil.isEqual(1f, msg.getOneFloat()));
        assertTrue(ProtoUtil.isEqual(1.5f, msg.getSmallFloat()));
        assertTrue(ProtoUtil.isEqual(-1f, msg.getNegativeOneFloat()));
        assertTrue(ProtoUtil.isEqual(-1.5f, msg.getNegativeFloat()));
        assertTrue(ProtoUtil.isEqual(2E8f, msg.getLargeFloat()));
        assertTrue(ProtoUtil.isEqual(-8e-28f, msg.getSmallNegativeFloat()));

        assertTrue(ProtoUtil.isEqual(Double.POSITIVE_INFINITY, msg.getInfDouble()));
        assertTrue(ProtoUtil.isEqual(Double.NEGATIVE_INFINITY, msg.getNegInfDouble()));
        assertTrue(ProtoUtil.isEqual(Double.NaN, msg.getNanDouble()));
        assertTrue(ProtoUtil.isEqual(Float.POSITIVE_INFINITY, msg.getInfFloat()));
        assertTrue(ProtoUtil.isEqual(Float.NEGATIVE_INFINITY, msg.getNegInfFloat()));
        assertTrue(ProtoUtil.isEqual(Float.NaN, msg.getNanFloat()));

        assertEquals("? ? ?? ?? ??? ??/ ??-", msg.getCppTrigraph());
        assertEquals("hel\000lo", msg.getStringWithZero());
        assertEquals("[119, 111, 114, 0, 108, 100]", msg.getBytesWithZero().toString());
        assertEquals("ab\000c", msg.getStringPieceWithZero());
        assertEquals("12\0003", msg.getCordWithZero());
        assertEquals("${unknown}", msg.getReplacementString());
    }

    @Test
    public void testOptionalPrimitives() throws IOException {
        TestAllTypes emptyMsg = TestAllTypes.newInstance();
        assertFalse(emptyMsg.hasOptionalBool());
        assertFalse(emptyMsg.hasOptionalDouble());
        assertFalse(emptyMsg.hasOptionalFloat());
        assertFalse(emptyMsg.hasOptionalFixed32());
        assertFalse(emptyMsg.hasOptionalFixed64());
        assertFalse(emptyMsg.hasOptionalSfixed32());
        assertFalse(emptyMsg.hasOptionalSfixed64());
        assertFalse(emptyMsg.hasOptionalSint32());
        assertFalse(emptyMsg.hasOptionalSint64());
        assertFalse(emptyMsg.hasOptionalInt32());
        assertFalse(emptyMsg.hasOptionalInt64());
        assertFalse(emptyMsg.hasOptionalUint32());
        assertFalse(emptyMsg.hasOptionalUint64());

        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.optionalPrimitives());
        assertNotEquals(msg, emptyMsg);

        assertTrue(msg.hasOptionalBool());
        assertTrue(msg.hasOptionalDouble());
        assertTrue(msg.hasOptionalFloat());
        assertTrue(msg.hasOptionalFixed32());
        assertTrue(msg.hasOptionalFixed64());
        assertTrue(msg.hasOptionalSfixed32());
        assertTrue(msg.hasOptionalSfixed64());
        assertTrue(msg.hasOptionalSint32());
        assertTrue(msg.hasOptionalSint64());
        assertTrue(msg.hasOptionalInt32());
        assertTrue(msg.hasOptionalInt64());
        assertTrue(msg.hasOptionalUint32());
        assertTrue(msg.hasOptionalUint64());

        assertTrue(msg.getOptionalBool());
        assertEquals(100.0d, msg.getOptionalDouble(), 0);
        assertEquals(101.0f, msg.getOptionalFloat(), 0);
        assertEquals(102, msg.getOptionalFixed32());
        assertEquals(103, msg.getOptionalFixed64());
        assertEquals(104, msg.getOptionalSfixed32());
        assertEquals(105, msg.getOptionalSfixed64());
        assertEquals(106, msg.getOptionalSint32());
        assertEquals(107, msg.getOptionalSint64());
        assertEquals(108, msg.getOptionalInt32());
        assertEquals(109, msg.getOptionalInt64());
        assertEquals(110, msg.getOptionalUint32());
        assertEquals(111, msg.getOptionalUint64());

        TestAllTypes manualMsg = TestAllTypes.newInstance()
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
                .setOptionalUint64(111);
        assertEquals(msg, manualMsg);

        // Test round-trip
        TestAllTypes msg3 = TestAllTypes.parseFrom(manualMsg.toByteArray());
        assertEquals(msg, msg3);

        // Test quick clear
        TestAllTypes msg4 = msg3.clone();
        assertEquals(msg3.clear(), msg4.clearQuick());
        assertArrayEquals(msg3.toByteArray(), msg4.toByteArray());

    }

    @Test
    public void testUnsignedRangeOverflow() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance()
                .setOptionalUint32(-1)
                .setOptionalUint64(-1)
                .addAllRepeatedUint32(0,12345,12345,12345,-1,0,1,-1,-1,1)
                .addAllRepeatedUint64(0,12345,12345,12345,-1,0,1,-1,-1,1);
        TestAllTypes result = TestAllTypes.parseFrom(msg.toByteArray());
        assertEquals(msg, result);
    }

    @Test
    public void testRepeatedPrimitives() throws IOException {
        RepeatedPackables.Packed emptyMsg = RepeatedPackables.Packed.newInstance();
        assertFalse(emptyMsg.hasBools());
        assertFalse(emptyMsg.hasDoubles());
        assertFalse(emptyMsg.hasFloats());
        assertFalse(emptyMsg.hasFixed32S());
        assertFalse(emptyMsg.hasFixed64S());
        assertFalse(emptyMsg.hasSfixed32S());
        assertFalse(emptyMsg.hasSfixed64S());
        assertFalse(emptyMsg.hasSint32S());
        assertFalse(emptyMsg.hasSint64S());
        assertFalse(emptyMsg.hasInt32S());
        assertFalse(emptyMsg.hasInt64S());
        assertFalse(emptyMsg.hasUint32S());
        assertFalse(emptyMsg.hasUint64S());

        RepeatedPackables.Packed msg = RepeatedPackables.Packed.parseFrom(CompatibilityTest.repeatedPackablesNonPacked());
        assertNotEquals(msg, emptyMsg);

        assertTrue(msg.hasBools());
        assertTrue(msg.hasDoubles());
        assertTrue(msg.hasFloats());
        assertTrue(msg.hasFixed32S());
        assertTrue(msg.hasFixed64S());
        assertTrue(msg.hasSfixed32S());
        assertTrue(msg.hasSfixed64S());
        assertTrue(msg.hasSint32S());
        assertTrue(msg.hasSint64S());
        assertTrue(msg.hasInt32S());
        assertTrue(msg.hasInt64S());
        assertTrue(msg.hasUint32S());
        assertTrue(msg.hasUint64S());

        assertArrayEquals(new boolean[]{true, false, true, true}, msg.getBools().toArray());
        assertArrayEquals(new double[]{Double.POSITIVE_INFINITY, -2d, 3d, 4d}, msg.getDoubles().toArray(), 0);
        assertArrayEquals(new float[]{10f, 20f, -30f, Float.NaN}, msg.getFloats().toArray(), 0);
        assertArrayEquals(new int[]{2, -2, 4, 67423}, msg.getFixed32S().toArray());
        assertArrayEquals(new long[]{3231313L, 6L, -7L, 8L}, msg.getFixed64S().toArray());
        assertArrayEquals(new int[]{2, -3, 4, 5}, msg.getSfixed32S().toArray());
        assertArrayEquals(new long[]{5L, -6L, 7L, -8L}, msg.getSfixed64S().toArray());
        assertArrayEquals(new int[]{2, -3, 4, 5}, msg.getSint32S().toArray());
        assertArrayEquals(new long[]{5L, 6L, -7L, 8L}, msg.getSint64S().toArray());
        assertArrayEquals(new int[]{2, 3, -4, 5}, msg.getInt32S().toArray());
        assertArrayEquals(new long[]{5L, -6L, 7L, 8L}, msg.getInt64S().toArray());
        assertArrayEquals(new int[]{2, 300, 4, 5}, msg.getUint32S().toArray());
        assertArrayEquals(new long[]{5L, 6L, 23L << 40, 8L}, msg.getUint64S().toArray());

        RepeatedPackables.Packed manualMsg = RepeatedPackables.Packed.newInstance()
                .addAllBools(true, false, true, true)
                .addAllDoubles(Double.POSITIVE_INFINITY, -2d, 3d, 4d)
                .addAllFloats(10f, 20f, -30f, Float.NaN)
                .addAllFixed32S(2, -2, 4, 67423)
                .addAllFixed64S(3231313L, 6L, -7L, 8L)
                .addAllSfixed32S(2, -3, 4, 5)
                .addAllSfixed64S(5L, -6L, 7L, -8L)
                .addAllSint32S(2, -3, 4, 5)
                .addAllSint64S(5L, 6L, -7L, 8L)
                .addAllInt32S(2, 3, -4, 5)
                .addAllInt64S(5L, -6L, 7L, 8L)
                .addAllUint32S(2, 300, 4, 5)
                .addAllUint64S(5L, 6L, 23L << 40, 8L);
        assertEquals(msg, manualMsg);

        // Make sure packed fields can be parsed from non-packed data to maintain forwards compatibility
        byte[] packed = RepeatedPackables.Packed.parseFrom(CompatibilityTest.repeatedPackablesPacked()).toByteArray();
        byte[] nonPacked = RepeatedPackables.NonPacked.parseFrom(CompatibilityTest.repeatedPackablesNonPacked()).toByteArray();

        assertEquals(msg, RepeatedPackables.Packed.parseFrom(packed));
        assertEquals(RepeatedPackables.Packed.parseFrom(packed), RepeatedPackables.Packed.parseFrom(nonPacked));
        assertEquals(RepeatedPackables.NonPacked.parseFrom(packed), RepeatedPackables.NonPacked.parseFrom(nonPacked));

    }

    @Test
    public void testOptionalEnums() throws IOException {
        TestAllTypes emptyMsg = TestAllTypes.newInstance();
        assertFalse(emptyMsg.hasOptionalNestedEnum());
        assertFalse(emptyMsg.hasOptionalForeignEnum());
        assertFalse(emptyMsg.hasOptionalImportEnum());

        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.optionalEnums());
        assertNotEquals(msg, emptyMsg);
        assertTrue(msg.hasOptionalNestedEnum());
        assertTrue(msg.hasOptionalForeignEnum());
        assertTrue(msg.hasOptionalImportEnum());

        assertEquals(NestedEnum.FOO, msg.getOptionalNestedEnum());
        assertEquals(ForeignEnum.FOREIGN_BAR, msg.getOptionalForeignEnum());
        assertEquals(ImportEnum.IMPORT_BAZ, msg.getOptionalImportEnum());

        TestAllTypes manualMsg = TestAllTypes.newInstance()
                .setOptionalNestedEnum(NestedEnum.FOO)
                .setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR)
                .setOptionalImportEnum(ImportEnum.IMPORT_BAZ);
        assertEquals(msg, manualMsg);

        try {
            manualMsg.setOptionalNestedEnum(null);
            fail();
        } catch (NullPointerException npe) {
            assertTrue(manualMsg.hasOptionalNestedEnum());
            assertEquals(ForeignEnum.FOREIGN_BAR, msg.getOptionalForeignEnum());
        }

        // Test round-trip
        TestAllTypes msg3 = TestAllTypes.parseFrom(manualMsg.toByteArray());
        assertEquals(msg, msg3);
    }

    @Test
    public void testUnknownEnumValue() throws IOException {
        int value = NestedEnum.BAZ_VALUE + 1;
        TestAllTypes msg = TestAllTypes.parseFrom(TestAllTypes.newInstance()
                .setOptionalNestedEnumValue(value)
                .toByteArray());
        assertEquals(0, msg.getOptionalNestedEnumValue());
        assertNull(msg.getOptionalNestedEnum());
    }

    @Test
    public void testRepeatedEnums() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedEnums());
        assertEquals(4, msg.getRepeatedNestedEnum().length());
        assertEquals(NestedEnum.FOO, msg.getRepeatedNestedEnum().get(0));
        assertEquals(NestedEnum.BAR, msg.getRepeatedNestedEnum().get(1));
        assertEquals(NestedEnum.BAZ, msg.getRepeatedNestedEnum().get(2));
        assertEquals(NestedEnum.BAZ, msg.getRepeatedNestedEnum().get(3));
        TestAllTypes actual = TestAllTypes.parseFrom(TestAllTypes.newInstance().copyFrom(msg).toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testStrings() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();

        // Setter
        assertFalse(msg.hasOptionalString());
        msg.setOptionalString("optionalString\uD83D\uDCA9");
        assertTrue(msg.hasOptionalString());

        // Mutable getter
        assertFalse(msg.hasOptionalCord());
        StringBuilder builder = new StringBuilder().append("he").append("llo!");
        msg.setOptionalCord(builder);
        assertTrue(msg.hasOptionalCord());

        // Parse
        TestAllTypes actual = TestAllTypes.parseFrom(msg.toByteArray());
        assertEquals(msg, actual);

        assertEquals("optionalString\uD83D\uDCA9", actual.getOptionalString());
        assertEquals("hello!", actual.getOptionalCord());

        // Custom decoder
        final String testString = "decoder-test\uD83D\uDCA9";
        msg.clearQuick().setDecoder(testString);
        actual = TestAllTypes.parseFrom(msg.toByteArray());
        assertEquals("customString", actual.getDecoderBytes().getString(new Utf8Decoder() {
            @Override
            public String decode(byte[] bytes, int offset, int length) {
                assertEquals(0, offset);
                assertEquals(testString.getBytes(UTF_8).length, length);
                return "customString"; // just for testing - should be a valid string
            }
        }));
        assertEquals("customString", actual.getDecoder()); // test caching

    }

    @Test
    public void testRepeatedStrings() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedStrings());
        assertEquals(4, msg.getRepeatedString().length());
        assertEquals("hello", msg.getRepeatedString().get(0));
        assertEquals("world", msg.getRepeatedString().get(1));
        assertEquals("ascii", msg.getRepeatedString().get(2));
        assertEquals("utf8\uD83D\uDCA9", msg.getRepeatedString().get(3));

        TestAllTypes msg2 = TestAllTypes.newInstance();
        msg2.getMutableRepeatedString().copyFrom(msg.getRepeatedString());

        TestAllTypes actual = TestAllTypes.parseFrom(msg2.toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testBytes() throws IOException {
        byte[] utf8Bytes = "optionalByteString\uD83D\uDCA9".getBytes(UTF_8);
        byte[] randomBytes = new byte[256];
        new Random(0).nextBytes(randomBytes);

        TestAllTypes msg = TestAllTypes.newInstance();

        assertFalse(msg.hasOptionalBytes());
        msg.addAllOptionalBytes(utf8Bytes);
        assertTrue(msg.hasOptionalBytes());
        assertArrayEquals(utf8Bytes, msg.getOptionalBytes().toArray());

        assertFalse(msg.hasDefaultBytes());
        msg.getMutableDefaultBytes()
                .copyFrom(randomBytes);
        assertTrue(msg.hasDefaultBytes());
        assertArrayEquals(randomBytes, msg.getDefaultBytes().toArray());

        // Parse
        TestAllTypes parsedMsg = TestAllTypes.parseFrom(msg.toByteArray());
        assertEquals(msg, parsedMsg);
        assertArrayEquals(utf8Bytes, parsedMsg.getOptionalBytes().toArray());
        assertArrayEquals(randomBytes, parsedMsg.getDefaultBytes().toArray());
    }

    @Test
    public void testRepeatedBytes() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedBytes());
        assertEquals(2, msg.getRepeatedBytes().length());
        assertArrayEquals("ascii".getBytes(UTF_8), msg.getRepeatedBytes().get(0).toArray());
        assertArrayEquals("utf8\uD83D\uDCA9".getBytes(UTF_8), msg.getRepeatedBytes().get(1).toArray());
        TestAllTypes actual = TestAllTypes.parseFrom(TestAllTypes.newInstance().copyFrom(msg).toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testOptionalMessages() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();

        // Setter
        assertFalse(msg.hasOptionalNestedMessage());
        msg.getMutableOptionalNestedMessage()
                .setBb(2);
        assertTrue(msg.hasOptionalNestedMessage());

        // Mutable getter
        assertFalse(msg.hasOptionalForeignMessage());
        msg.setOptionalForeignMessage(ForeignMessage.newInstance().setC(3));
        assertTrue(msg.hasOptionalForeignMessage());

        // Copy from
        assertFalse(msg.hasOptionalGroup());
        msg.getMutableOptionalGroup().copyFrom(TestAllTypes.OptionalGroup.newInstance().setA(4));
        assertTrue(msg.hasOptionalGroup());

        // Compare w/ gen-Java and round-trip parsing
        assertEquals(msg, TestAllTypes.parseFrom(CompatibilityTest.optionalMessages()));
        assertEquals(msg, TestAllTypes.parseFrom(msg.toByteArray()));
    }

    @Test
    public void testRepeatedMessages() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedMessages());
        assertEquals(3, msg.getRepeatedForeignMessage().length());
        assertEquals(ForeignMessage.newInstance().setC(0), msg.getRepeatedForeignMessage().get(0));
        assertEquals(ForeignMessage.newInstance().setC(1), msg.getRepeatedForeignMessage().get(1));
        assertEquals(ForeignMessage.newInstance().setC(2), msg.getRepeatedForeignMessage().get(2));

        TestAllTypes msg2 = TestAllTypes.newInstance()
                .addRepeatedForeignMessage(ForeignMessage.newInstance().setC(0))
                .addRepeatedForeignMessage(ForeignMessage.newInstance().setC(1))
                .addRepeatedForeignMessage(ForeignMessage.newInstance().setC(2))
                .addRepeatedGroup(TestAllTypes.RepeatedGroup.newInstance().setA(3))
                .addRepeatedGroup(TestAllTypes.RepeatedGroup.newInstance().setA(4));
        assertEquals(msg, msg2);

        TestAllTypes actual = TestAllTypes.parseFrom(TestAllTypes.newInstance().copyFrom(msg2).toByteArray());
        assertEquals(msg, actual);
    }

    @Test
    public void testHighFieldNumbers() throws IOException {
        MessageWithMultibyteNumbers expected = MessageWithMultibyteNumbers.newInstance()
                .setTagSize1(1)
                .setTagSize2(2)
                .setTagSize3(3)
                .setTagSize4(4)
                .setTagSize5(5)
                .setTagSizeMax(6);
        MessageWithMultibyteNumbers actual = MessageWithMultibyteNumbers.parseFrom(expected.toByteArray());
        assertEquals(expected, actual);
    }

    @Test
    public void testAllTypesRequired() throws IOException {
        TestAllTypesRequired expected = TestAllTypesRequired.newInstance()
                .setRequiredBool(true)
                .setRequiredDouble(100.0d)
                .setRequiredFloat(101.0f)
                .setRequiredFixed32(102)
                .setRequiredFixed64(103)
                .setRequiredSfixed32(104)
                .setRequiredSfixed64(105)
                .setRequiredSint32(106)
                .setRequiredSint64(107)
                .setRequiredInt32(108)
                .setRequiredInt64(109)
                .setRequiredUint32(110)
                .setRequiredUint64(111)
                .setRequiredString("test")
                .addRequiredBytes((byte) 0)
                .setRequiredNestedEnum(TestAllTypesRequired.NestedEnum.BAR)
                .setRequiredNestedMessage(UnittestRequired.SimpleMessage.newInstance().setRequiredField(0));
        byte[] output = expected.toByteArray();

        TestAllTypesRequired actual = TestAllTypesRequired.parseFrom(expected.toByteArray());
        assertEquals(expected, actual);
        assertArrayEquals(output, actual.toByteArray());

        try {
            expected.clearRequiredBool().toByteArray();
            fail("should not serialize with missing required field");
        } catch (UninitializedMessageException missingRequired) {
        }

        // Check isInitialized()
        assertFalse(expected.isInitialized());
        expected.setRequiredBool(false);
        assertTrue(expected.isInitialized());
        expected.clearRequiredInt32();
        assertFalse(expected.isInitialized());
        expected.setRequiredInt32(108);
        assertTrue(expected.isInitialized());
        expected.getMutableRequiredNestedMessage().clear();
        assertFalse(expected.isInitialized());
        expected.getMutableRequiredNestedMessage().setRequiredField(0);
        assertTrue(expected.isInitialized());

    }

    @Test
    public void testMergeFromMessage() throws IOException {
        TestAllTypes binaryMerged = TestAllTypes.newInstance();
        TestAllTypes messageMerged = TestAllTypes.newInstance();

        RepeatedByte combined = RepeatedByte.newEmptyInstance();
        for (byte[] input : CompatibilityTest.getAllMessages()) {
            combined.addAll(input);
            binaryMerged.mergeFrom(ProtoSource.newInstance(input));
            messageMerged.mergeFrom(TestAllTypes.parseFrom(input));
            assertEquals(binaryMerged, messageMerged);
        }

        // Compare to parsing a single large message
        byte[] array = combined.toArray();
        assertEquals(TestAllTypes.parseFrom(array), messageMerged);

        // Compare with Protobuf-Java merged-bytes (must be same as CompatibilityTest::getCombinedMessage)
        assertEquals(TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage()), messageMerged
                .addAllRepeatedPackedInt32(-1, 0, 1, 2, 3, 4, 5)
                .addAllRepeatedInt32(-2, -1, 0, 1, 2, 3, 4, 5)
        );

    }

    @Test
    public void testSkipUnknownFields() throws IOException {
        ProtoSource source = ProtoSource.newInstance(CompatibilityTest.getCombinedMessage());
        TestAllTypes.NestedMessage.newInstance().mergeFrom(source);
        assertTrue(source.isAtEnd());
    }

    @Test
    public void testStoreUnknownFields() throws IOException {
        byte[] bytes = CompatibilityTest.getCombinedMessage();
        TestAllTypes expected = TestAllTypes.parseFrom(bytes);

        TestAllTypes.NestedMessage wrongMessage = TestAllTypes.NestedMessage.newInstance();
        wrongMessage.mergeFrom(ProtoSource.newInstance(bytes));
        byte[] unknowns = wrongMessage.toByteArray();

        assertEquals(bytes.length, unknowns.length);
        assertEquals(TestAllTypes.parseFrom(bytes), TestAllTypes.parseFrom(unknowns));

        assertEquals(2, TestAllTypes.NestedMessage.parseFrom(
                        ProtoSource.newInstance(bytes).discardUnknownFields())
                .getSerializedSize());
    }

    @Test
    public void clearFirstBit() throws IOException {
        TestAllTypes.NestedMessage msg = TestAllTypes.NestedMessage.newInstance();
        msg.setBb(1);
        assertTrue(msg.hasBb());
        msg.clearBb();
        assertFalse(msg.hasBb());
    }

    @Test
    public void testRepeatableMessageIterator() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.repeatedMessages());
        int sum = 0;
        for (ForeignMessage foreignMessage : msg.getRepeatedForeignMessage()) {
            sum += foreignMessage.getC();
        }
        assertEquals(3, sum);
    }

    @Test
    public void testOneofFields() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        assertFalse(msg.hasOneofField());

        msg.setOneofFixed64(10);
        assertTrue(msg.hasOneofField());
        assertTrue(msg.hasOneofFixed64());
        byte[] fixed64 = msg.toByteArray();

        msg.getMutableOneofNestedMessage().setBb(2);
        assertTrue(msg.hasOneofField());
        assertTrue(msg.hasOneofNestedMessage());
        assertFalse(msg.hasOneofFixed64());
        assertEquals(0, msg.getOneofFixed64());
        byte[] nestedMsg = msg.toByteArray();

        msg.setOneofString("oneOfString");
        assertTrue(msg.hasOneofField());
        assertTrue(msg.hasOneofString());
        assertFalse(msg.hasOneofNestedMessage());
        assertEquals(0, msg.getOneofNestedMessage().getBb());
        byte[] string = msg.toByteArray();

        msg.clearOneofField();
        assertFalse(msg.hasOneofField());
        assertFalse(msg.hasOneofString());
        assertFalse(msg.hasOneofNestedMessage());
        assertEquals("", msg.getOneofString());

        msg.setOneofString("test");
        assertTrue(msg.hasOneofString());
        msg.mergeFrom(ProtoSource.newInstance(fixed64));
        assertTrue(msg.hasOneofFixed64());
        assertFalse(msg.hasOneofString());

        msg.mergeFrom(ProtoSource.newInstance(nestedMsg));
        assertTrue(msg.hasOneofNestedMessage());
        assertFalse(msg.hasOneofFixed64());

        msg.mergeFrom(ProtoSource.newInstance(string));
        assertTrue(msg.hasOneofString());
        assertFalse(msg.hasOneofNestedMessage());

    }

    @Test
    public void testToString() throws IOException {
        // known fields
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        msg.setOptionalDouble(103 + 1E-15);
        assertNotNull(msg.toString());

        // add an unknown field
        final int unknownFieldNumber = 12313;
        int numBytes = ProtoSink.computeDoubleSize(unknownFieldNumber, Double.NaN);
        byte[] unknownBytes = msg.getUnknownBytes().setLength(numBytes).array();
        ProtoSink.newInstance(unknownBytes, 0, numBytes).writeDouble(unknownFieldNumber, Double.NaN);
        assertNotNull(msg.toString());
    }

    @Test
    public void testUnknownFields() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        byte[] expected = msg.clearOptionalInt32().toByteArray(); // one number conflicts
        byte[] actual = TestAllTypes.NestedMessage.parseFrom(expected).toByteArray();
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testDelimitedStream() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());

        // Write varint delimited message
        byte[] outData = msg.toByteArray();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ProtoSink.writeUInt32(outputStream, outData.length);
        outputStream.write(outData);

        // Read varint delimited message
        byte[] result = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(result);
        int length = ProtoSource.readRawVarint32(inputStream);
        assertEquals(outData.length, length);

        byte[] inData = new byte[length];
        if (inputStream.read(inData) != length) {
            fail();
        }
        assertArrayEquals(outData, inData);
    }

    @Test
    public void testLazyInitialization() throws IOException {
        assertNotNull(Value.newInstance().getMutableListValue());
        assertNotNull(Value.newInstance().getMutableStringValueBytes());
        assertNotNull(Value.newInstance().getMutableStructValue());
        assertNotNull(Value.newInstance().getMutableListValue());

        Struct struct = Struct.newInstance();

        // Singular (getMutable)
        assertNotNull(LazyMessage.newInstance().getMutableOptionalStringBytes());
        assertNotNull(LazyMessage.newInstance().getMutableOptionalBytes());
        assertNotNull(LazyMessage.newInstance().getMutableOptionalGroup());
        assertNotNull(LazyMessage.newInstance().getMutableOptionalNestedMessage());

        // Repeated (getMutable)
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedInt32());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedInt64());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedUint32());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedUint64());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedSint32());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedSint64());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedFixed32());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedFixed64());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedFloat());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedDouble());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedBool());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedString());
        assertNotNull(LazyMessage.newInstance().getMutableRepeatedBytes());

        // Initialize everything
        LazyMessage msg = LazyMessage.newInstance();
        msg.setOptionalString("test");
        msg.addAllOptionalBytes(new byte[20]);
        msg.setOptionalGroup(LazyMessage.OptionalGroup.newInstance().setA(2));
        msg.setOptionalNestedMessage(LazyMessage.NestedMessage.newInstance().setRecursiveMessage(LazyMessage.newInstance()));
        msg.getMutableRepeatedInt32().add(1);
        msg.getMutableRepeatedInt64().add(2);
        msg.getMutableRepeatedUint32().add(3);
        msg.getMutableRepeatedUint64().add(4);
        msg.getMutableRepeatedSint32().add(5);
        msg.getMutableRepeatedSint64().add(6);
        msg.getMutableRepeatedFixed32().add(7);
        msg.getMutableRepeatedFixed64().add(8);
        msg.getMutableRepeatedFloat().add(9);
        msg.getMutableRepeatedDouble().add(10);
        msg.getMutableRepeatedBool().add(true);
        msg.getMutableRepeatedString().add("string");
        msg.getMutableRepeatedBytes().add(new byte[20]);

        // Check initialization of required fields in lazy nested messages
        assertTrue(msg.isInitialized());
        msg.getMutableRepeatedNestedMessage().next();
        assertFalse(msg.isInitialized());
        msg.getRepeatedNestedMessage().get(0).getMutableRecursiveMessage();
        assertTrue(msg.isInitialized());

        // Copy in various ways
        LazyMessage copy = msg.clone();
        copy.copyFrom(LazyMessage.newInstance());
        LazyMessage.newInstance().copyFrom(copy);
        LazyMessage.newInstance().copyFrom(msg);

        // Clear various states
        msg.clone().clear().clear();
        copy.clear().clone().clear();
        msg.clone().clearQuick();
        copy.clearQuick().clone().clearQuick();

        // Individual clears
        LazyMessage empty = LazyMessage.newInstance();
        assertTrue(empty.isEmpty());
        empty.clearOptionalString();
        empty.clearOptionalBytes();
        empty.clearOptionalGroup();
        empty.clearOptionalNestedMessage();
        empty.clearRepeatedInt32();
        empty.clearRepeatedInt64();
        empty.clearRepeatedUint32();
        empty.clearRepeatedUint64();
        empty.clearRepeatedSint32();
        empty.clearRepeatedSint64();
        empty.clearRepeatedFixed32();
        empty.clearRepeatedFixed64();
        empty.clearRepeatedFloat();
        empty.clearRepeatedDouble();
        empty.clearRepeatedBool();
        empty.clearRepeatedString();
        empty.clearRepeatedBytes();
        assertTrue(empty.isEmpty());

        empty.copyFrom(msg);
        assertFalse(empty.isEmpty());
        empty.clearOptionalString();
        empty.clearOptionalBytes();
        empty.clearOptionalGroup();
        empty.clearOptionalNestedMessage();
        empty.clearRepeatedInt32();
        empty.clearRepeatedInt64();
        empty.clearRepeatedUint32();
        empty.clearRepeatedUint64();
        empty.clearRepeatedSint32();
        empty.clearRepeatedSint64();
        empty.clearRepeatedFixed32();
        empty.clearRepeatedFixed64();
        empty.clearRepeatedFloat();
        empty.clearRepeatedDouble();
        empty.clearRepeatedBool();
        empty.clearRepeatedString();
        empty.clearRepeatedBytes();
        empty.clearRepeatedNestedMessage();
        assertTrue(empty.isEmpty());

        // merge
        copy.mergeFrom(msg).mergeFrom(msg);
        empty = LazyMessage.newInstance().mergeFrom(copy);
        assertFalse(empty.isEmpty());

    }

    @Test
    public void testRecursionLimit() throws IOException {
        LazyMessage msg = LazyMessage.newInstance();
        LazyMessage nested = msg;
        for (int i = 0; i < 33; i++) { // above default limit 64
            nested = nested.getMutableOptionalNestedMessage().getMutableRecursiveMessage();
        }
        byte[] lotsOfNestedMessages = msg.toByteArray();


        try {
            ProtoSource source = ProtoSource.newInstance(lotsOfNestedMessages);
            LazyMessage.parseFrom(source);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("limit"));
        }

        ProtoSource source = ProtoSource.newInstance(lotsOfNestedMessages);
        assertEquals(64, source.setRecursionLimit(66));
        assertEquals(msg, LazyMessage.parseFrom(source));

    }

}
