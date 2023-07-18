module MteConsoleUploadFile {
	requires com.google.gson;
	requires eclypses.ecdh;
	
	
	exports uploadClient.Models to com.google.gson;
}