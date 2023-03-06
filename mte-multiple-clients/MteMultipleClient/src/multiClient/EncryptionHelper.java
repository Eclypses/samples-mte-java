//**************************************************************************************************
// The MIT License (MIT)
//
// Copyright (c) Eclypses, Inc.
//
// All rights reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, subLicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//**************************************************************************************************

package multiClient;

import java.util.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper
{
    /**
     * Encryption mode enumeration
     */
    private enum EncryptMode {
        ENCRYPT, DECRYPT;
    }

    //-------------------------------------------------
    // cipher to be used for encryption and decryption
    //-------------------------------------------------
    Cipher cx;

    //------------------------------------------
    // encryption key and initialization vector
    //------------------------------------------
    byte[] key, iv;

    public EncryptionHelper()  {
        try {
        	//----------------------------------------------------------------
            // initialize the cipher with transformation AES/CBC/PKCS5Padding
        	//----------------------------------------------------------------
        	cx = Cipher.getInstance("AES/CBC/PKCS5Padding");
        	key = new byte[32]; //256 bit key space
        	iv = new byte[16]; //128 bit IV
        }catch(NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchPaddingException ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Note: This function is no longer used.
     * This function generates md5 hash of the input string
     * @param inputString
     * @return md5 hash of the input string
     */
    public static final String md5(final String inputString) {
        final String MD5 = "MD5";
        try {
        	//------------------
            // Create MD5 Hash
        	//------------------
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(inputString.getBytes());
            byte messageDigest[] = digest.digest();

            //------------------
            // Create Hex String
            //------------------
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    /**
     * Get bytes from UUID
     * @param uuid
     * @return
     */
    public static byte[] getBytesFromUUID(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        return bb.array();
    }

    /**
     * get UUID from bytes
     * @param bytes
     * @return
     */
    public static UUID getUUIDFromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        Long high = byteBuffer.getLong();
        Long low = byteBuffer.getLong();

        return new UUID(high, low);
    }

    /**
     *
     * @param _inputText
     *            Text to be encrypted or decrypted
     * @param _encryptionKey
     *            Encryption key to used for encryption / decryption
     * @param _mode
     *            specify the mode encryption / decryption
     * @param _initVector
     * 	      Initialization vector
     * @return encrypted or decrypted string based on the mode
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private String encryptDecrypt(String _inputText, String _encryptionKey,
                                  EncryptMode _mode, String _initVector) throws UnsupportedEncodingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        String _out = "";// output string

        //------------------------------------------
        // Change so encryption works with c# --> 
        // use hash digest NOT first 32 bytes of hash
        //------------------------------------------        
        int len = hexStringToByteArray(_encryptionKey).length;
        if (hexStringToByteArray(_encryptionKey).length > key.length)
            len = key.length;

        int ivlen = _initVector.getBytes().length;

        if(_initVector.getBytes().length > iv.length)
            ivlen = iv.length;

        byte[] hashDigest = hexStringToByteArray(_encryptionKey);

        System.arraycopy(hashDigest, 0, key, 0, len);
        System.arraycopy(_initVector.getBytes(), 0, iv, 0, ivlen);

        SecretKeySpec keySpec = new SecretKeySpec(key, "AES"); // Create a new SecretKeySpec
        // for the
        // specified key
        // data and
        // algorithm
        // name.

        IvParameterSpec ivSpec = new IvParameterSpec(iv); // Create a new
        // IvParameterSpec
        // instance with the
        // bytes from the
        // specified buffer
        // iv used as
        // initialization
        // vector.

        // encryption
        if (_mode.equals(EncryptMode.ENCRYPT)) {
            // Potentially insecure random numbers on Android 4.3 and older.
            // Read
            // https://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html
            // for more info.
        	cx.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);// Initialize this cipher instance
            byte[] results = cx.doFinal(_inputText.getBytes("UTF-8")); // Finish
            // multi-part
            // transformation
            // (encryption)
            _out = Base64.getEncoder().encodeToString(results); // ciphertext
            // output
        }

        //-------------
        // de-cryption
        //-------------
        if (_mode.equals(EncryptMode.DECRYPT)) {
        	cx.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);// Initialize this ipher instance

            byte[] decodedValue = Base64.getDecoder().decode(_inputText.getBytes());
            byte[] decryptedVal = cx.doFinal(decodedValue); // Finish
            // multi-part
            // transformation
            // (decryption)
            _out = new String(decryptedVal);
        }

        //System.out.println(_out);
        return _out; // return encrypted/decrypted string
    }

    /***
     * This function computes the SHA256 hash of input string
     * @param text input text whose SHA256 hash has to be computed
     * @param length length of the text to be returned
     * @return returns SHA256 hash of input text
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String SHA256 (String text, int length) {

        String resultStr = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] mybytes = text.getBytes("UTF-8");

            byte[] digest = md.digest(mybytes);

            StringBuffer result = new StringBuffer();
            for (byte b : digest) {
                result.append(String.format("%02x", b)); //convert to hex
            }

            StringBuffer result2 = new StringBuffer();
            for (byte byt : digest) result2.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));

            if(length > result.toString().length())
            {
                resultStr = result.toString();
            }
            else
            {
                resultStr = result.toString().substring(0, length);
            }

        }catch(NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
            catch (UnsupportedEncodingException ex)
        {
            ex.printStackTrace();
        }

        return resultStr;

    }

    /**
     * hex String to byte array
     * @param s
     * @return
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /***
     * This function encrypts the plain text to cipher text using the key
     * provided. You'll have to use the same key for de-cryption
     *
     * @param _plainText
     *            Plain text to be encrypted
     * @param _key
     *            Encryption Key. You'll have to use the same key for de-cryption
     * @param _iv
     * 	    initialization Vector
     * @return returns encrypted (cipher) text
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */

    public String encrypt(String _plainText, String _key, String _iv)
    {
        String returnValue = "";
        try
        {
            returnValue = encryptDecrypt(_plainText, _key, EncryptMode.ENCRYPT, _iv);
        }catch(InvalidKeyException e)
        {

        }
        catch(UnsupportedEncodingException e)
        {

        }
        catch(InvalidAlgorithmParameterException e)
        {

        }
        catch(IllegalBlockSizeException e)
        {

        }
        catch(BadPaddingException e)
        {

        }
        return returnValue;
    }

    /***
     * This funtion decrypts the encrypted text to plain text using the key
     * provided. You'll have to use the same key which you used during
     * encryprtion
     *
     * @param _encryptedText
     *            Encrypted/Cipher text to be decrypted
     * @param _key
     *            Encryption key which you used during encryption
     * @param _iv
     * 	    initialization Vector
     * @return encrypted value
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public String decrypt(String _encryptedText, String _key, String _iv)
             {
        String returnValue = "";
        try {
            returnValue = encryptDecrypt(_encryptedText, _key, EncryptMode.DECRYPT, _iv);
        }catch(InvalidKeyException e)
        {

        }
        catch(UnsupportedEncodingException e)
        {

        }
        catch(InvalidAlgorithmParameterException e)
        {

        }
        catch(IllegalBlockSizeException e)
        {

        }
        catch(BadPaddingException e)
        {

        }
        return returnValue;
    }

    /**
     * this function generates random string for given length
     * @param length
     * 				Desired length
     * * @return
     */
    public static String generateRandomIV(int length)
    {
        SecureRandom ranGen = new SecureRandom();
        byte[] aesKey = new byte[16];
        ranGen.nextBytes(aesKey);
        StringBuffer result = new StringBuffer();
        for (byte b : aesKey) {
            result.append(String.format("%02x", b)); //convert to hex
        }
        if(length> result.toString().length())
        {
            return result.toString();
        }
        else
        {
            return result.toString().substring(0, length);
        }
    }
}
