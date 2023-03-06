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

public class JailBreakMain {
	
	public static void main(String[] args) throws Exception {
		try {
			//-------
			// Input
			//-------
			String input = "hello";
			
			//------------------------
			// Personalization string
			//------------------------
			String personal = "demo";
			
			MteJail.Algo mteJailAlgorithm;
			//-----------
			// set Nonce
			//-----------
            long nonce = 123;
            int timesToRun = 2;
            for (int i = 0; i < timesToRun; i++)
            {
            	//-------------------------------------
            	// Set algorithm to none first time 
            	// all other times set to aIosX86_64im
            	//-------------------------------------
            	mteJailAlgorithm = (i == 0) ? MteJail.Algo.aNone : MteJail.Algo.aIosX86_64Sim;
            	
            	//----------------
            	// Create encoder
            	//----------------
            	EncoderDevice encoder = new EncoderDevice();
            	
            	MteResponse encoderResponse = encoder.CallEncoderDevice(mteJailAlgorithm, input, nonce, personal);
            	if(encoderResponse.Status != MteStatus.mte_status_success){
            		System.exit(encoderResponse.Status.getValue());
            	}
            	
            	//----------------
            	// Create decoder
            	//----------------
            	DecoderDevice decoder = new DecoderDevice();
            	
            	MteResponse decoderResponse = decoder.CallDecoderDevice(mteJailAlgorithm, encoderResponse.Message, nonce, personal);
            	if(decoderResponse.Status != MteStatus.mte_status_success){
            		System.exit(decoderResponse.Status.getValue());
            	}
            	
            	//------------------------
            	// output decoded message
            	//------------------------
            	System.out.println("Decoded data: " + decoderResponse.Message);
            	
            	//-----------------------------------------------------
            	// Compare the decoded data against the original data.
            	//-----------------------------------------------------
                if (decoderResponse.Message == input)
                {
                	System.out.println("The original data and decoded data match.");
                }
                else
                {
                	System.out.println("The original data and decoded data DO NOT match.");
                    System.exit(-1);
                }
            	//-----------------
                // Exit on Success
                //-----------------
                System.exit(0);
            }
		}catch(Exception ex) {
			throw ex;
	  }
	}
}
