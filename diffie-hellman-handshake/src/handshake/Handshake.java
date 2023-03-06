package handshake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

import com.eclypses.ecdh.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import handshake.Models.*;

//-------------------------------------------------------------------------
// This simple program demonstrates Diffie-Hellman key exchange with server
// -------------------------------------------------------------------------
// The purpose of the handshake is to create unique entropy for MTE
// in a secure manner for the encoder and decoder.
// The "client" creates the personalization string or ConversationIdentifier.
// the "server" creates the nonce in the form of a timeStamp.
// -------------------------------------------------------------------------

public class Handshake {
	
	// --------------------------------------------------------
	// Create gson object that be compatible with C-Sharp json
	// --------------------------------------------------------
	public static final Gson _gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter()).create();

	// ------------------------
	// Using base64 libraries
	// ------------------------
	private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
		public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return Base64.getDecoder().decode(json.getAsString());
		}

		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
		}
	}

	public static void main(String[] args) throws Exception {
		//------------------------------
		// Initialize client parameters 
		//------------------------------
		HandshakeResponse handshake = new HandshakeResponse();
		String clientId = UUID.randomUUID().toString();
		
		//-----------------
		// Buffered input.
		//-----------------
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		//-------------------
		// Perform Handshake 
		//-------------------
		ResponseModel<HandshakeResponse> handshakeResponse = HandshakeWithServer(clientId);
		if (!handshakeResponse.Success) {
			throw new Exception("Handshake unsuccessful, message:" + handshakeResponse.Message);
		}
		
		handshake = handshakeResponse.Data;
		
		//---------------------------------------
    	// For demonstration purposes ONLY 
		// output shared secret to the screen
    	//---------------------------------------
    	System.out.println("Completed Handshake for client " + clientId + " .");
    	System.out.println("Encoder Shared Secret: " + Base64.getEncoder().encodeToString(handshake.EncoderSharedSecret));
    	System.out.println("Decoder Shared Secret: " + Base64.getEncoder().encodeToString(handshake.DecoderSharedSecret));
    	System.out.println("Press enter to end program.");
    	br.readLine();
    	
		
	}
	
	/**
	 * Handshake with the server to create shared secrets
	 * for encoder and decoder
	 * 
	 * @param clientId            --> Current client id
	 * @return
	 */
	private static ResponseModel<HandshakeResponse> HandshakeWithServer(String clientId) {
		ResponseModel<HandshakeResponse> response = new ResponseModel<HandshakeResponse>();
		response.Data = new HandshakeResponse();
		try {
			System.out.println("Performing Handshake for Client " + clientId);

			// --------------------------------
			// create clientId for this client
			// --------------------------------
			HandshakeModel handshake = new handshake.Models.HandshakeModel();
			handshake.ConversationIdentifier = clientId;

			// -------------------------------------------
			// Create eclypses DH containers for handshake
			// -------------------------------------------
			EclypsesECDH encoderEcdh = new EclypsesECDH();
			EclypsesECDH decoderEcdh = new EclypsesECDH();

			// -------------------------------------------
			// Get the public key to send to other side
			// -------------------------------------------
			handshake.ClientEncoderPublicKey = encoderEcdh.getDevicePublicKey();
			handshake.ClientDecoderPublicKey = decoderEcdh.getDevicePublicKey();

			// -------------------
			// Perform handshake
			// -------------------
			String handshakeString = _gson.toJson(handshake);
			String handshakeResponse = MakeHttpCall(Constants.RestAPIName + Constants.HandshakeRoute, "POST",
					handshake.ConversationIdentifier, Constants.JsonContentType, handshakeString);

			// ---------------------------------------
			// Deserialize the result from handshake
			// ---------------------------------------
			Type handshakeResponseType = new TypeToken<ResponseModel<HandshakeModel>>() {
			}.getType();
			ResponseModel<HandshakeModel> serverResponse = 
					_gson.fromJson(handshakeResponse, handshakeResponseType);

			// ---------------------------------------
			// If handshake was not successful end
			// ---------------------------------------
			if (!serverResponse.Success) {
				response.Message = serverResponse.Message;
				response.Success = serverResponse.Success;
				response.ResultCode = serverResponse.ResultCode;
				System.out.println("Error making DH handshake for Client " + clientId + ": " + serverResponse.Message);
				return response;
			}
			
			// ----------------------
			// Create shared secret
			// ----------------------
			response.Data.EncoderSharedSecret = encoderEcdh.createSharedSecret(serverResponse.Data.ClientEncoderPublicKey);
			response.Data.DecoderSharedSecret = decoderEcdh.createSharedSecret(serverResponse.Data.ClientDecoderPublicKey);

			// ----------------------------------------------------------
			// Clear container to ensure key is different for each client
			// ----------------------------------------------------------
			encoderEcdh = null;
			decoderEcdh = null;

			
		} catch (Exception ex) {
			ex.printStackTrace();
			response.Message = "Exception during handshake: " + ex.getMessage();
			response.Success = false;
			response.ResultCode = Constants.RC_HANDSHAKE_EXCEPTION;
			return response;
		}
		return response;
	}
	
	/**
	 * Make the Http Call
	 * 
	 * @param connectionUrl    --> Connection URL
	 * @param connectionMethod --> Connection Method
	 * @param clientId         --> Current client id
	 * @param contentType      --> http content type
	 * @param payload          --> http call payload
	 * @return
	 */
	private static String MakeHttpCall(String connectionUrl, String connectionMethod, String clientId,
			String contentType, String payload) {
		String returnPayload = "";
		try {
			// -----------------------------------------
			// Set URI and other default Http settings
			// -----------------------------------------
			URL url = new URL(connectionUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			// ---------------------------
			// Check if we have a payload
			// ---------------------------
			if (payload != null && !payload.isEmpty()) {
				conn.setDoOutput(true);
			}
			conn.setRequestMethod(connectionMethod);
			conn.setRequestProperty("Content-Type", contentType);

			// -------------------------
			// Add client id to header
			// -------------------------
			conn.setRequestProperty(Constants.ClientIdHeader, clientId);

			OutputStream os = conn.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			// --------------------------------------------------
			// If we did not get a good connection return error
			// --------------------------------------------------
			if (conn.getResponseCode() < HttpURLConnection.HTTP_OK || conn.getResponseCode() > 299) {

				// ------------------------------------------------------
				// Create error response to send back to calling method
				// ------------------------------------------------------
				ResponseModel<Object> errorResponse = new ResponseModel<Object>();
				errorResponse.Data = null;
				errorResponse.Message = "Failed : HTTP error code : " + conn.getResponseCode();
				errorResponse.ResultCode = Constants.RC_HTTP_ERROR;
				returnPayload = _gson.toJson(errorResponse);
				return returnPayload;
			}

			// -------------------------
			// Read in the inputStream
			// -------------------------
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			// --------------------
			// Assign the payload
			// --------------------
			returnPayload = br.readLine();

			// ---------------------------
			// Disconnect the connection
			// ---------------------------
			conn.disconnect();

		} catch (MalformedURLException e) {

			e.printStackTrace();
			ResponseModel<Object> errorResponse = new ResponseModel<Object>();
			errorResponse.Data = null;
			errorResponse.Message = "MalformedURLException in MakeHtteCall : " + e.getMessage();
			errorResponse.ResultCode = Constants.RC_HTTP_EXCEPTION;
			returnPayload = _gson.toJson(errorResponse);

		} catch (IOException e) {

			e.printStackTrace();
			ResponseModel<Object> errorResponse = new ResponseModel<Object>();
			errorResponse.Data = null;
			errorResponse.Message = "IOException in MakeHttpCall : " + e.getMessage();
			errorResponse.ResultCode = Constants.RC_HTTP_EXCEPTION;
			returnPayload = _gson.toJson(errorResponse);

		}
		return returnPayload;
	}

}
