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
import java.nio.ByteBuffer;

public class Cbs extends MteJail
{
	//-------------------------
	// Return the mutated nonce.
	//-------------------------
	public byte[] getMutated()
	{
		return myMutated;
	}

	//-----------------------------------------------------
	// Override the callback to capture the mutated nonce.
	//-----------------------------------------------------
	public int nonceCallback(int minLength, int maxLength, ByteBuffer nonce)
	{	
		//---------
		// Super.
		//---------
		int ret = super.nonceCallback(minLength, maxLength, nonce);

		//------------------------------------
		// Retain a copy of the mutated nonce.
		//------------------------------------
		myMutated = new byte[ret];
		nonce.get(myMutated);
		nonce.position(0);

		//----------------------------------
		// Return the original return value.
		//----------------------------------
		return ret;
  }

	//---------------
	// Mutated nonce.
	//---------------
	private byte[] myMutated;
}