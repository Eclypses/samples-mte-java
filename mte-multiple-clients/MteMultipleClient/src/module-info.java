module MultiClientConsole {
	requires ehcache;
	requires com.google.gson;
	requires jdk.unsupported;
	
	exports multiClient.Models to com.google.gson;
}