package noppes.npcs.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import noppes.npcs.shared.common.util.LogWriter;
import org.apache.commons.codec.digest.DigestUtils;

import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.scripts.IScriptHandler;
import noppes.npcs.controllers.scripts.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.controllers.data.ClientScriptData;

public class ScriptEncryption {

    public static boolean encryptScript(File outputFile, String fileName, String scriptCode, boolean onlyTab,ScriptContainer sContainer, IScriptHandler handler) {
        if (handler instanceof ClientScriptData) {
            LogWriter.error("Error trying to encrypt script code: Trying to encrypt script for client: " + outputFile.getAbsolutePath());
            return false;
        }
        SecretKey secretKey;
        Cipher cipher;
        try {
            secretKey = generateSecretKey();
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }
        catch (Exception e) {
            LogWriter.error("Error trying to encrypt script code: Failed to create key-password for file: " + outputFile.getAbsolutePath(), e);
            return false;
        }
        try (OutputStream outStream = Files.newOutputStream(outputFile.toPath())) {
            byte[] iv = new byte[cipher.getBlockSize()];
            Arrays.fill(iv, (byte)0x00);
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
            byte[] encryptedData = cipher.doFinal(scriptCode.getBytes());
            outStream.write(iv);
            outStream.write(encryptedData);
            LogWriter.info("Script code (length:" + scriptCode.length() + ") is encrypted into a file: \""+ outputFile.getAbsolutePath() + "\" for " + handler.getClass().getSimpleName());
        } catch (Exception e) {
            LogWriter.error("Error while trying to encrypt script code: Failed to encrypt and save code to file: " + outputFile.getAbsolutePath(), e);
            return false;
        }
        ScriptController sData = ScriptController.Instance;

        // Save in handler
        sData.sizes.put(fileName, outputFile.length());
        sData.scripts.remove(fileName);
        sData.encrypts.put(fileName, outputFile);

        // Remove old from container
        sContainer.script = "";
        if (!onlyTab) { sContainer.scripts.clear(); }
        sContainer.scripts.add(fileName); // add new encrypt script
        sContainer.lastCreated = 0L; // reset
        return true;
    }

    public static String decryptScriptFromFile(File inputFile) {
        SecretKey secretKey;
        Cipher cipher;
        try {
            secretKey = generateSecretKey();
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }  catch (Exception e) {
            LogWriter.error("Error trying to encrypt script code: Failed to create key-password", e);
            return null;
        }

        try (InputStream inStream = Files.newInputStream(inputFile.toPath())) {
            byte[] iv = new byte[cipher.getBlockSize()];
            if (inStream.read(iv) == -1) {
                LogWriter.error("Error decrypting file - \"Failed to read IV\"");
                return null;
            }
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = inStream.read(buffer)) != -1) {
                baos.write(cipher.update(buffer, 0, readBytes));
            }
            baos.write(cipher.doFinal());
            String scriptCode = baos.toString();
            LogWriter.info("File: \"" + inputFile.getAbsolutePath() + "\" has been decrypted into script code (length:" + scriptCode.length() + ")");
            return scriptCode;
        } catch (Exception e) {
            LogWriter.error("Error decrypting file", e);
        }
        return null;
    }

    private static SecretKey generateSecretKey() {
        return new SecretKeySpec(DigestUtils.md5(CustomNpcs.ScriptPassword.getBytes()), "AES");
    }

}
