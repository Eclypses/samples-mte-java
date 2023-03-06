package handshake.Models;

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