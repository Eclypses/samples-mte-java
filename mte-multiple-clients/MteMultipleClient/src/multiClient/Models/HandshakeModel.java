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

package multiClient.Models;

import com.google.gson.annotations.SerializedName;

public class HandshakeModel {
	
	//--------------------------------------------
    // Calculated on the server 
	// sent back to the client and used for Nonce
	//--------------------------------------------
	@SerializedName(value="Timestamp")
	public String Timestamp;
	
	//--------------------------------------------
	// Session identifier determined by the client
    // used as PK for storing the MTE STATE as well 
	// as looking up the shared secret
	//--------------------------------------------
	@SerializedName(value="ConversationIdentifier")
	public String ConversationIdentifier;
	
	//-------------------------------------------------
	// Diffie-Hellman public key of the client Encoder
    // This should be used for server decoder
	//-------------------------------------------------
	@SerializedName(value="ClientEncoderPublicKey")
	public byte[] ClientEncoderPublicKey;
	
	//-------------------------------------------------
	// Diffie-Hellman public key of the client decoder
    // This should be used for server encoder
	//-------------------------------------------------
	@SerializedName(value="ClientDecoderPublicKey")
	public byte[] ClientDecoderPublicKey;
	
	//--------------------------------
	// Create default HandshakeModel
	//--------------------------------
	public HandshakeModel() {
		this.Timestamp = "";
		this.ConversationIdentifier = "";
		this.ClientDecoderPublicKey = new byte[0];
		this.ClientEncoderPublicKey = new byte[0];
	}
}

