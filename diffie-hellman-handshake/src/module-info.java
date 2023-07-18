module HandshakeSample {
	requires com.google.gson;
	requires eclypses.ecdh;
	
	exports handshake.Models to com.google.gson;
}