package com.eclypses.ecdh;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;


public class EclypsesECDH {

  //------------------------------------------------------------------
  // A Note regarding security:
  //
  // To make it harder to reverse engineer this class, please consider
  // changing the declaration of "DEBUG" to "false".
  //
  // When "DEBUG" is false the compiler will be able to remove code
  // which will never be executed from the compiled class. Removing
  // clear text strings is always a good idea to make compiled classes
  // less readable / searchable.
  //------------------------------------------------------------------
  private final boolean DEBUG = true;
  private boolean logging;
  private KeyPair kp;
    
  public EclypsesECDH() {
    this(false);
  }
  
  public EclypsesECDH(boolean doLogging) {
    if (DEBUG)
      logging = doLogging;
    else
      logging = false;
    kp = null;
    if (DEBUG && logging)
      log("Instance has been initialized.");
  }
  
  public byte[] getDevicePublicKey() {
    //-----------------------------------------------------------------------
    // Instantiate a key pair generator using the "elliptic curve" algorithm.
    // Elliptic curve cryptography has been standardized by NIST in FIPS 186.
    // After instantiation, set the key size to 256.
    //----------------------------------------------------------------------
	  if(kp == null) {
		  KeyPairGenerator kpg;
		  try {
			  kpg = KeyPairGenerator.getInstance("EC");
		  } catch (NoSuchAlgorithmException e) {
			  if (DEBUG && logging) {
				  e.printStackTrace();
				  log("Creating a KeyPairGenerator instance failed, algorithm type is invalid.");
			  }
			  return null;
		  }
		  kpg.initialize(256);
		  //-----------------------------------------------------------------------------------
		  // Generate a key pair and get the encoded (binary) representation of our public key.
		  // The key itself is formatted according to its standard format
		  // (based on the key's algorithm).
		  // For elliptic curve keys, the public key is formatted in "X.509".
		  //-----------------------------------------------------------------------------------
		  kp = kpg.generateKeyPair();
	  }
    return kp.getPublic().getEncoded();
  }


  public byte[] createSharedSecret(byte[] partnerPublicKey) {
    //--------------------------------------------------------------
    // Instantiate a key factory using the elliptic curve algorithm.
    //--------------------------------------------------------------
    KeyFactory kf;
    try {
      kf = KeyFactory.getInstance("EC");
    } catch (NoSuchAlgorithmException e) {
      if (DEBUG && logging) {
        e.printStackTrace();
        log("Creating a KeyFactory failed, algorithm type is invalid.");
      }
      kp = null;
      return null;
    }
    //-----------------------------------------------------------
    // The partner's public key comes in X.509 formatted.
    // We use the key factory to create the partner's public key.
    //-----------------------------------------------------------
    X509EncodedKeySpec partnerKeySpec = new X509EncodedKeySpec(partnerPublicKey);
    PublicKey partnerKey;
    try {
      partnerKey = kf.generatePublic(partnerKeySpec);
    } catch (InvalidKeySpecException e) {
      if (DEBUG && logging) {
        e.printStackTrace();
        log("KeyFactory failed, public partner's key specification is invalid.");
      }
      kp = null;
      return null;
    }
    //-------------------------------------------------------------------------------
    // Instantiate a key agreement so that we can start generating the shared secret.
    //-------------------------------------------------------------------------------
    KeyAgreement ka;
    try {
      ka = KeyAgreement.getInstance("ECDH");
    } catch (NoSuchAlgorithmException e) {
      if (DEBUG && logging) {
        e.printStackTrace();
        log("Creating a KeyAgreement instance failed, algorithm type is invalid.");
      }
      kp = null;
      return null;
    }
    //----------------------------------------------------------------------------------
    // Generate the shared secret now using our private key and the server's public key.
    //----------------------------------------------------------------------------------
    try {
      ka.init(kp.getPrivate());
      ka.doPhase(partnerKey, true);
    } catch (InvalidKeyException e) {
      if (DEBUG && logging) {
        e.printStackTrace();
        log("Creating the shared secret failed, public partner's key is invalid.");
      }
      kp = null;
      return null;
    }
    byte[] sharedSecret = ka.generateSecret();
    //-----------------------------------------------------------------------------------
    // The partner does a SHA256 hash on the shared secret and uses that hash as entropy.
    // The SHA256 hashing is part of C#'s ECDiffieHellman.DeriveKeyMaterial() function.
    // The SHA256 hashing always creates 32 bytes of data.
    // 32 bytes of entropy data are enough data to use as entropy even for SHA-512.
    //-----------------------------------------------------------------------------------
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      Arrays.fill(sharedSecret, (byte) 0);
      if (DEBUG && logging) {
        e.printStackTrace();
        log("Creating a MessageDigest instance failed, algorithm is invalid.");
      }
      kp = null;
      return null;
    }
    //---------------------------------------------------------------------------
    // The MessageDigest has been created; last step is to hash the shared secret
    // in order to generate the final entropy.
    //---------------------------------------------------------------------------
    digest.reset();
    sharedSecret = digest.digest(sharedSecret);
    kp = null;
    return sharedSecret;
  }
  
  /**
   * Clear the Device KeyPair
   */
  public void ClearDeviceKeyPair() {
	  kp = null;
  }


  protected void log(String message) {
    if (DEBUG && logging)
      System.out.println("EclypsesECDH: " + message);
  }
}
