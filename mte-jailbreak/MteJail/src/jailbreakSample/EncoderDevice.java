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

public class EncoderDevice {
		
	public MteResponse CallEncoderDevice(MteJail.Algo jailAlgorithm, String input, long nonce, String personal)
	{
		try {
			MteResponse response = new MteResponse();
			
			if(!MteBase.initLicense("YOUR_COMPANY", "YOUR_LICENSE"))
			{
				response.Status = MteStatus.mte_status_license_error;
                System.out.printf("License initialization error ({0}): {1}", 
                		MteBase.getStatusName(response.Status), 
                		MteBase.getStatusDescription(response.Status));
                return response;
			}
			//---------------------
			// Create the encoder
			//---------------------
            MteEnc encoder = new MteEnc();
			//----------------------------------------------------------------
            // Check how long entropy we need, set default all 0's 
         	// should be treated like encryption keys - this is just example
            //----------------------------------------------------------------
         	int entropyMinBytes = MteBase.getDrbgsEntropyMinBytes(encoder.getDrbg());
         	StringBuffer outputBuffer = new StringBuffer(entropyMinBytes);
         	for (int i = 0; i < entropyMinBytes; i++){
         	    outputBuffer.append("0");
         	}
         	String entropy = outputBuffer.toString();
         	entropy.replace(' ', '0');
            
         	//-------------------------
         	// Instantiate the encoder
         	//-------------------------
         	encoder.setEntropy(entropy.getBytes());
         	//--------------------
         	// JailBreak callback
         	//--------------------
         	Cbs cb = new Cbs();
            cb.setAlgo(jailAlgorithm);
            cb.setNonceSeed(nonce);
            encoder.setNonceCallback(cb);
            
            response.Status = encoder.instantiate(personal.getBytes());
            if(response.Status != MteStatus.mte_status_success) {
            	System.out.printf("Encoder instantiate error (" +  
            						MteBase.getStatusName(response.Status) +  "): " + 
            			            MteBase.getStatusDescription(response.Status)); 
            	return response;
            }
            
             StrStatus encoderResult = encoder.encodeB64(input);
             if(encoderResult.status != MteStatus.mte_status_success) {
            	 response.Status = encoderResult.status;
                 System.out.printf("License initialization error (" +   
                 					MteBase.getStatusName(response.Status) +  "): " + 
                 					MteBase.getStatusDescription(response.Status)); 
                 return response;
             }
             //-------------------------
             // Set response and return
             //-------------------------
             response.Message = encoderResult.str;
             response.Status = encoderResult.status;
             
             return response;
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
			throw ex;
		}
	}
}
