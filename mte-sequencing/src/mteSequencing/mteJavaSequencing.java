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

package mteSequencing;

import com.eclypses.mte.*;

public class mteJavaSequencing {
	public static void main(String[] args)
	  {
		//---------
	    // Status.
		//---------
	    MteStatus status;
	    //----------
	    // Inputs.
	    //----------
	    String[] inputs =
	    {
	      "message 1",
	      "message 2",
	      "message 3",
	      "message 4"
	    };
	    //-------------------------
	    // Personalization string.
	    //-------------------------
	    String personal = "demo";
	    //-------------------------------------------------------------------------
	    // Initialize MTE license. If a license code is not required (e.g., trial
	    // mode), this can be skipped. This demo attempts to load the license info
	    // from the environment if required.
	    //-------------------------------------------------------------------------
	    if (!MteBase.initLicense("YOUR_COMPANY", "YOUR_LICENSE"))
	    {
	      String company = System.getenv("MTE_COMPANY");
	      String license = System.getenv("MTE_LICENSE");
	      if (company == null || license == null ||
	          !MteBase.initLicense(company, license))
	      {
	        status = MteStatus.mte_status_license_error;
	        System.err.println("Encode error (" +
	                           MteBase.getStatusName(status) + "): " +
	                           MteBase.getStatusDescription(status));
	        System.exit(status.getValue());
	      }
	    }
	    //---------------------
	    // Create the encoder.
	    //---------------------
	    MteEnc encoder = new MteEnc();
	    //--------------------------------------------------------------------------
	    // Create all-zero entropy for this demo. The nonce will also be set to 0.
	    // This should never be done in real applications.
	    //--------------------------------------------------------------------------
	    int entropyBytes = MteBase.getDrbgsEntropyMinBytes(encoder.getDrbg());
	    byte[] entropy = new byte[entropyBytes];
	    //-------------------------
	    // Instantiate the encoder.
	    //-------------------------
	    encoder.setEntropy(entropy);
	    encoder.setNonce(0);
	    status = encoder.instantiate(personal);
	    if (status != MteStatus.mte_status_success)
	    {
	      System.err.println("Encoder instantiate error (" +
	                         MteBase.getStatusName(status) + "): " +
	                         MteBase.getStatusDescription(status));
	      System.exit(status.getValue());
	    }
	    //--------------------
	    // Encode the inputs.
	    //--------------------
	    String[] encodings = new String[inputs.length];
	    for (int i = 0; i < inputs.length; ++i)
	    {
	      MteBase.StrStatus encoded = encoder.encodeB64(inputs[i]);
	      if (encoded.status != MteStatus.mte_status_success)
	      {
	        System.err.println("Encode error ("+
	                           MteBase.getStatusName(encoded.status)+"): "+
	                           MteBase.getStatusDescription(encoded.status));
	        System.exit(encoded.status.getValue());
	      }
	      encodings[i] = encoded.str;
	      System.out.printf("Encode #%d: %s -> %s\n", i + 1, inputs[i], encodings[i]);
	    }
	    //---------------------------------------------------
	    // Create decoders with different sequence windows.
	    //---------------------------------------------------
	    MteDec decoderV = new MteDec(0, 0);
	    MteDec decoderF = new MteDec(0, 2);
	    MteDec decoderA = new MteDec(0, -2);
	    //---------------------------
	    // Instantiate the decoders.
	    //---------------------------
	    decoderV.setEntropy(entropy);
	    decoderV.setNonce(0);
	    status = decoderV.instantiate(personal);
	    if (status == MteStatus.mte_status_success)
	    {
	      decoderF.setEntropy(entropy);
	      decoderF.setNonce(0);
	      status = decoderF.instantiate(personal);
	      if (status == MteStatus.mte_status_success)
	      {
	        decoderA.setEntropy(entropy);
	        decoderA.setNonce(0);
	        status = decoderA.instantiate(personal);
	      }
	    }
	    if (status != MteStatus.mte_status_success)
	    {
	      System.err.println("Decoder instantiate error (" +
	                         MteBase.getStatusName(status) + "): " +
	                         MteBase.getStatusDescription(status));
	      System.exit(status.getValue());
	    }
	    //------------------------------
	    // Save the async decoder state.
	    //------------------------------
	    byte[] dsaved = decoderA.saveState();
	    //---------------------
	    // String to decode to.
	    //---------------------
	    MteBase.StrStatus decoded;
	    //-------------------------------------------
	    // Create the corrupt version of message #3.
	    //-------------------------------------------
	    char[] e2 = encodings[2].toCharArray();
	    ++e2[0];
	    String corrupt = new String(e2);
	    //----------------------------------
	    // Decode in verification-only mode.
	    //----------------------------------
	    System.out.println("\nVerification-only mode (sequence window = 0):");
	    decoded = decoderV.decodeStrB64(encodings[0]);
	    System.out.println("Decode #1: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderV.decodeStrB64(encodings[0]);
	    System.out.println("Decode #1: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderV.decodeStrB64(encodings[2]);
	    System.out.println("Decode #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderV.decodeStrB64(encodings[1]);
	    System.out.println("Decode #2: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderV.decodeStrB64(encodings[2]);
	    System.out.println("Decode #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderV.decodeStrB64(encodings[3]);
	    System.out.println("Decode #4: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));

	    //------------------------------
	    // Decode in forward-only mode.
	    //------------------------------
	    System.out.println("\nForward-only mode (sequence window = 2):");
	    decoded = decoderF.decodeStrB64(encodings[0]);
	    System.out.println("Decode #1: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderF.decodeStrB64(encodings[0]);
	    System.out.println("Decode #1: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderF.decodeStrB64(corrupt);
	    System.out.println("Corrupt #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderF.decodeStrB64(encodings[2]);
	    System.out.println("Decode #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderF.decodeStrB64(encodings[1]);
	    System.out.println("Decode #2: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderF.decodeStrB64(encodings[2]);
	    System.out.println("Decode #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderF.decodeStrB64(encodings[3]);
	    System.out.println("Decode #4: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    //-----------------------
	    // Decode in async mode.
	    //-----------------------
	    System.out.println("\nAsync mode (sequence window = -2):");
	    decoded = decoderA.decodeStrB64(encodings[0]);
	    System.out.println("Decode #1: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(encodings[0]);
	    System.out.println("Decode #1: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(corrupt);
	    System.out.println("Corrupt #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(encodings[2]);
	    System.out.println("Decode #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(encodings[2]);
	    System.out.println("Decode #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(encodings[1]);
	    System.out.println("Decode #2: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(encodings[2]);
	    System.out.println("Decode #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(encodings[3]);
	    System.out.println("Decode #4: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    //-------------------------------------------------
	    // Restore and decode again in a different order.
	    //-------------------------------------------------
	    decoderA.restoreState(dsaved);
	    System.out.println("\nAsync mode (sequence window = -2):");
	    decoded = decoderA.decodeStrB64(encodings[3]);
	    System.out.println("Decode #4: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(encodings[0]);
	    System.out.println("Decode #1: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(encodings[2]);
	    System.out.println("Decode #3: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    decoded = decoderA.decodeStrB64(encodings[1]);
	    System.out.println("Decode #2: " + MteBase.getStatusName(decoded.status) +
	                       ", " + (decoded.str == null ? "" : decoded.str));
	    //----------
	    // Success.
	    //----------
	    System.exit(0);
	  }
}
