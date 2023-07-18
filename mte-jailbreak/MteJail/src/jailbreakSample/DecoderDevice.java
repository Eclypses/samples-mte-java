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

package jailbreakSample;

import com.eclypses.mte.*;
import com.eclypses.mte.MteBase.StrStatus;

public class DecoderDevice {
	public MteResponse CallDecoderDevice(MteJail.Algo jailAlgorithm, String encodedInput, long nonce, String personal)
	{
		try {
			MteResponse response = new MteResponse();
			//--------------------
			// Check MTE license
			//--------------------
			if(!MteBase.initLicense("YOUR_COMPANY", "YOUR_LICENSE"))
			{
				response.Status = MteStatus.mte_status_license_error;
                System.out.printf("License initialization error ({0}): {1}", 
                		MteBase.getStatusName(response.Status), 
                		MteBase.getStatusDescription(response.Status));
                return response;
			}
			//----------------
			// Create decoder
			//----------------
			MteDec decoder = new MteDec();
			//---------------------------------------------------------------
			// Check how long entropy we need, set default all 0's 
         	// should be treated like encryption keys - this is just example
			//---------------------------------------------------------------
         	int entropyMinBytes = MteBase.getDrbgsEntropyMinBytes(decoder.getDrbg());
         	StringBuffer outputBuffer = new StringBuffer(entropyMinBytes);
         	for (int i = 0; i < entropyMinBytes; i++){
         	    outputBuffer.append("0");
         	}
         	String entropy = outputBuffer.toString();
         	entropy.replace(' ', '0');
			//-------------------------
         	// Instantiate the decoder
         	//-------------------------
         	decoder.setEntropy(entropy.getBytes());
         	//--------------------
         	// JailBreak callback
         	//--------------------
         	Cbs cb = new Cbs();
            cb.setAlgo(jailAlgorithm);
            cb.setNonceSeed(nonce);
            decoder.setNonceCallback(cb);
            
            response.Status = decoder.instantiate(personal.getBytes());
            if(response.Status != MteStatus.mte_status_success) {
            	System.out.println("Decoder instantiate error (" + 
            			               MteBase.getStatusName(response.Status) + "): " + 
            			               MteBase.getStatusDescription(response.Status)); 
            	return response;
            }
            
            StrStatus decoderResult = decoder.decodeStrB64(encodedInput);
            if(decoderResult.status != MteStatus.mte_status_success) {
           	 response.Status = decoderResult.status;
           	 //-------------------------------------------------
           	 // If this specific error happens after first run
             // we know the encoder device has been jail broken
           	 //-------------------------------------------------
             if (response.Status == MteStatus.mte_status_token_does_not_exist && jailAlgorithm != MteJail.Algo.aNone)
             {
            	 System.out.println("Paired device has been compromised, possible jail broken device.");
                 return response;
             }
                System.out.printf("License initialization error (" + 
                		              MteBase.getStatusName(response.Status) +  "): " + 
                		              MteBase.getStatusDescription(response.Status)); 
                return response;
            }
            //------------------------
            // set response and return
            //------------------------
            response.Message = decoderResult.str;
            response.Status = decoderResult.status;
			
			return response;
		} catch(Exception ex) {
			ex.printStackTrace(System.out);
			throw ex;
		}
	}
}
