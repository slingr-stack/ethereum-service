package io.slingr.service.ethereum;

import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.utils.Json;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SolidityUtils {
    private static final Logger logger = LoggerFactory.getLogger(SolidityUtils.class);

    public String exportResource(String resourceName) throws Exception {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        String destFolder = "/usr/bin/";
        try {
            stream = Ethereum.class.getClassLoader().getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }
            int readBytes;
            byte[] buffer = new byte[4096];
            resStreamOut = new FileOutputStream(destFolder + resourceName);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
            if (resStreamOut != null) {
                resStreamOut.close();
            }
        }
        File f = new File(destFolder + resourceName);
        f.setExecutable(true);
        f.setReadable(true);
        f.setWritable(false);
        return destFolder + resourceName;
    }

    public synchronized Json compile(String sourceCode, Json libraries) {
        String filename = null;
        try {
            File temp = File.createTempFile("contract", ".sol");
            filename = temp.getAbsolutePath();
            FileUtils.writeStringToFile(temp, sourceCode, "UTF-8");
        } catch (IOException e) {
            logger.error("Error writing source code to file", e);
            throw ServiceException.permanent(ErrorCode.GENERAL, "Error writing source code to file", e);
        }

        List<String> commandParams = new ArrayList<>();
        commandParams.add("/usr/bin/solc");
        commandParams.add("--optimize");
        commandParams.add("--pretty-json");
        commandParams.add("-o");
        commandParams.add("/tmp");
        commandParams.add("--overwrite");
        if (libraries != null && libraries.isNotEmpty()) {
            commandParams.add("--libraries");
            StringBuilder librariesArgument = new StringBuilder();
            for (String libraryName : libraries.keys()) {
                librariesArgument.append(libraryName);
                librariesArgument.append(":");
                librariesArgument.append(libraries.string(libraryName));
                librariesArgument.append(" ");
            }
            commandParams.add(librariesArgument.toString());
        }
        commandParams.add("--combined-json");
        commandParams.add("bin,abi");
        commandParams.add(filename);

        ProcessBuilder pb;
        Process process = null;
        try {
            pb = new ProcessBuilder(commandParams);
            pb.inheritIO();
            process = pb.start();
            process.waitFor();
            if (process.exitValue() != 0) {
                BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String errorMessage = "";
                String s = null;
                while ((s = output.readLine()) != null) {
                    errorMessage += s + "   ";
                }
                logger.error("Error compiling code: "+errorMessage);
                throw ServiceException.permanent(ErrorCode.GENERAL, "Error compiling code: "+errorMessage);
            }
            StringWriter writer = new StringWriter();
            IOUtils.copy(FileUtils.openInputStream(new File("/tmp/combined.json")), writer, "UTF-8");
            String jsonString = writer.toString();
            return Json.parse(jsonString);
        } catch (Exception e) {
            logger.error("Unknown error compiling code", e);
            throw ServiceException.permanent(ErrorCode.GENERAL, "Unknown error compiling code", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
