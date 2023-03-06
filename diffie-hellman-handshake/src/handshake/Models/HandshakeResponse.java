package handshake.Models;

public class HandshakeResponse {
	public byte[] EncoderSharedSecret;
	public byte[] DecoderSharedSecret;
	
	public HandshakeResponse() {
		this.EncoderSharedSecret = new byte[0];
		this.DecoderSharedSecret = new byte[0];
	}
}