package us.hebi.robobuf.compiler;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import us.hebi.robobuf.parser.ParserUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Florian Enner
 * @since 05 Aug 2019
 */
public class CompilerPlugin {

    /**
     * The protoc-gen-plugin communicates via proto messages on System.in/.out
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        handleRequest(System.in).writeTo(System.out);
    }

    public static CodeGeneratorResponse handleRequest(InputStream input) throws IOException {
        try {

            // Compile files
            CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(input);
            return handleRequest(request);

        } catch (Exception e) {
            return ParserUtil.asErrorWithStackTrace(e);
        }
    }

    public static CodeGeneratorResponse handleRequest(CodeGeneratorRequest request) throws IOException {
        CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();

        response.addFile(CodeGeneratorResponse.File.newBuilder()
                .setContent(String.valueOf(request))
                .setName("test"));

        for (DescriptorProtos.FileDescriptorProto file : request.getProtoFileList()) {
            response.addFile(CodeGeneratorResponse.File.newBuilder()
                    .setName(file.getName())
                    .setContent(String.valueOf(file)));
        }

        return response.build();

    }

}