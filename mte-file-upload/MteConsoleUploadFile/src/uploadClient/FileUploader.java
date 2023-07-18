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

package uploadClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.eclypses.ecdh.EclypsesECDH;
import com.eclypses.mte.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import uploadClient.Models.*;

public class FileUploader {
	//------------------------------
	// Max Re-Seed Interval for DRGB
	//------------------------------
	private static long maxSeed = 0;
	private static float reseedPercentage = .9;

	// --------------------------------------------------------
	// Create gson object that be compatible with C-Sharp json
	// --------------------------------------------------------
	public static final Gson gson = new GsonBuilder()
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

		// -----------------
		// Buffered input.
		// -----------------
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		// -------------------------------
		// Perform Handshake if using MTE
		// -------------------------------
		HandshakeResponse handshake = new HandshakeResponse();
		String clientId = UUID.randomUUID().toString();

		ResponseModel<HandshakeResponse> handshakeResponse = HandshakeWithServer(clientId);
		if (!handshakeResponse.Success) {
			throw new Exception("Handshake unsuccessful, message:" + handshakeResponse.Message);
		}
		handshake = handshakeResponse.Data;


		// ---------------------------------------
		// Allow multiple uploads till user ends
		// ---------------------------------------
		while (true) {

			// ----------------------
			// Prompt for file path
			// ----------------------
			System.out.print("Enter full path to file to upload: ");
			String filename = br.readLine();
			File textFile = new File(filename);
			String textFileName = textFile.getName();

			// ---------------
			// Set url to API
			// ---------------
			String url = Constants.RestAPIName + "/FileUpload/mte?name=" + textFileName;
			String charset = "UTF-8";

			// ---------------------
			// Open url connection
			// ---------------------
			URLConnection connection = new URL(url).openConnection();
			connection.setDoOutput(true);
			connection.setRequestProperty("Accept-Charset", charset);

			// ------------------------------------------------
			// set the content type based on using MTE or not
			// ------------------------------------------------
			String contentType = "application/octet-stream;charset=";
			connection.setRequestProperty("Content-Type", contentType + charset);

			// -------------------------
			// Add client id to header
			// -------------------------
			connection.setRequestProperty(Constants.ClientIdHeader, clientId);

			// ---------------------------
			// Create MKE Encoder object
			// ---------------------------
			MteMkeEnc mkeEncoder = new MteMkeEnc();

			// ----------------------------
			// Restore Encoder
			// ----------------------------

			MteStatus encoderStatus = mkeEncoder.restoreStateB64(handshake.EncoderState);
			if (encoderStatus != MteStatus.mte_status_success) {
				System.out.println("Error restoring the Encoder mte state for Client " + clientId + ": "
						+ MteBase.getStatusDescription(encoderStatus));
				throw new Exception("Error restoring the Encoder mte state for Client " + clientId + ": "
						+ MteBase.getStatusDescription(encoderStatus));
			}

			// -----------------------------
			// Initialize chunking session
			// -----------------------------
			encoderStatus = mkeEncoder.startEncrypt();
			if (encoderStatus != MteStatus.mte_status_success) {
				throw new Exception("Failed to start encode chunk. Status: " + MteBase.getStatusName(encoderStatus)
						+ " / " + MteBase.getStatusDescription(encoderStatus));
			}

			// -----------------
			// Set buffer size
			// -----------------
			byte[] dataBuffer = new byte[1024];

			// --------------------
			// Set out put stream
			// --------------------
			try (OutputStream output = connection.getOutputStream();) {
				// --------------------------------
				// Write the actual file contents
				// --------------------------------
				FileInputStream inputStream = new FileInputStream(textFile);

				// -----------------------------------------
				// read contents of file into input stream
				// -----------------------------------------
				int bytesRead;
				while ((bytesRead = inputStream.read(dataBuffer)) != -1) {

					// ------------------------------------------------------------
					// Encode the data in place - encoded data put back in buffer
					// ------------------------------------------------------------
					MteStatus chunkStatus = mkeEncoder.encryptChunk(dataBuffer, 0, bytesRead);
					if (chunkStatus != MteStatus.mte_status_success) {
						throw new Exception("Failed to encode chunk. Status: " + MteBase.getStatusName(chunkStatus)
								+ " / " + MteBase.getStatusDescription(chunkStatus));
					}

					// ------------------------
					// Write to output writer
					// ------------------------
					output.write(dataBuffer, 0, bytesRead);

				}

				// ----------------------
				// finalize MTE session
				// ----------------------

				MteBase.ArrStatus finalEncodedChunk = mkeEncoder.finishEncrypt();
				if (finalEncodedChunk.status != MteStatus.mte_status_success) {
					// -----------------------------------------------
					// First close inputStream to prevent memory leak
					// -----------------------------------------------
					inputStream.close();
					throw new Exception(
							"Failed to finish encode chunk. Status: " + MteBase.getStatusName(finalEncodedChunk.status)
									+ " / " + MteBase.getStatusDescription(finalEncodedChunk.status));

				}
				// -------------------------------------
				// Write final encoded chunk to output
				// -------------------------------------
				output.write(finalEncodedChunk.arr);

				// -------------------------
				// close/flush output
				// close inputStream
				// -------------------------
				output.flush();
				inputStream.close();
				
				//-------------------------
				// Check current seed life
				//-------------------------
				long currentSeed = mkeEncoder.getReseedCounter();
				
				if(currentSeed > (maxSeed * reseedPercentage)) {
					// Uninstantiate the Decoder
					encoderStatus = mkeEncoder.uninstantiate();
					if(encoderStatus != MteStatus.mte_status_success)
					{
					    // MTE was not uninstantiated as desired so handle failure appropriately
					    // Below is only an example
					    throw new Exception("Failed to uninstantiate Encoder. Status: "
					        + MteBase.getStatusName(encoderStatus)+ " / "
					        + MteBase.getStatusDescription(encoderStatus));
					}
					
					ResponseModel<HandshakeResponse> updateHandshakeResponse = HandshakeWithServer(clientId);
					if (!updateHandshakeResponse.Success) {
						throw new Exception("Re-Handshake unsuccessful, message:" + updateHandshakeResponse.Message);
					}
					
					// Need to update the Encoder AND Decoder since handshake updates both
					handshake.DecoderState = updateHandshakeResponse.Data.DecoderState;
					handshake.EncoderState = updateHandshakeResponse.Data.EncoderState;
				}else {

					// ----------------------------
					// save updated Encoder state
					// ----------------------------
					handshake.EncoderState = mkeEncoder.saveStateB64();
				}

				
				

			}

			// ---------------------------------------------------------------
			// Request is lazily fired whenever you need to obtain response.
			// ---------------------------------------------------------------
			int responseCode = ((HttpURLConnection) connection).getResponseCode();

			// ---------------
			// Get inStream
			// ---------------
			InputStream in = new BufferedInputStream(connection.getInputStream());
			int bytesReadInstream;
			ByteArrayOutputStream inBuffer = new ByteArrayOutputStream();
			// -----------------------
			// Iterate through input
			// -----------------------
			while ((bytesReadInstream = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
				inBuffer.write(dataBuffer, 0, bytesReadInstream);
			}
			// --------------------------------
			// Flush buffer and set to string
			// --------------------------------
			inBuffer.flush();
			String text = inBuffer.toString();
			// -------------------------------
			// Deserialize to response model
			// -------------------------------
			Type serverResponseType = new TypeToken<ResponseModel<byte[]>>() {
			}.getType();
			ResponseModel<byte[]> serverResponse = gson.fromJson(text, serverResponseType);

			String finalData = "";
			// --------------------------------------------------
			// If we get a successful response Decoder response
			// --------------------------------------------------
			if (serverResponse.Success) {

				// ----------------
				// Create Decoder
				// ----------------
				MteMkeDec mkeDecoder = new MteMkeDec();

				// -----------------
				// Restore Decoder
				// -----------------
				MteStatus decoderStatus = mkeDecoder.restoreStateB64(handshake.DecoderState);
				if (decoderStatus != MteStatus.mte_status_success) {
					System.out.println("Error restoring the Decoder mte state for Client " + clientId + ": "
							+ MteBase.getStatusDescription(decoderStatus));
					throw new Exception("Error restoring the Decoder mte state for Client " + clientId + ": "
							+ MteBase.getStatusDescription(decoderStatus));
				}
				// ------------------------
				// Start chunking session
				// ------------------------
				decoderStatus = mkeDecoder.startDecrypt();
				if (decoderStatus != MteStatus.mte_status_success) {
					throw new Exception("Failed to start decode chunk. Status: " + MteBase.getStatusName(decoderStatus)
							+ " / " + MteBase.getStatusDescription(decoderStatus));
				}

				// -----------------------------------------------
				// We know the response is short from the server
				// calling decryptChunk only one time then final
				// -----------------------------------------------
				byte[] decodedBytes = mkeDecoder.decryptChunk(serverResponse.Data);
				// -----------------------------
				// Finish the chunking session
				// -----------------------------
				MteBase.ArrStatus finalEncodedChunk = mkeDecoder.finishDecrypt();
				if (finalEncodedChunk.status != MteStatus.mte_status_success) {
					throw new Exception(
							"Failed to finish decode chunk. Status: " + MteBase.getStatusName(finalEncodedChunk.status)
									+ " / " + MteBase.getStatusDescription(finalEncodedChunk.status));
				}
				// -----------------------------------------------------------------------
				// Check if there is additional bytes if not initialize empty byte array
				// -----------------------------------------------------------------------
				if (finalEncodedChunk.arr == null) {
					finalEncodedChunk.arr = new byte[0];
				}
				// ---------------
				// Concat bytes
				// ---------------
				byte[] finalBytes = new byte[decodedBytes.length + finalEncodedChunk.arr.length];
				System.arraycopy(decodedBytes, 0, finalBytes, 0, decodedBytes.length);
				System.arraycopy(finalEncodedChunk.arr, 0, finalBytes, decodedBytes.length,
						finalEncodedChunk.arr.length);
				// ------------------------------------
				// Convert final byte array to string
				// ------------------------------------
				finalData = new String(finalBytes, StandardCharsets.UTF_8);
				
				//-------------------------
				// Check current seed life
				long currentSeed = mkeDecoder.getReseedCounter();
				
				if(currentSeed > (maxSeed * reseedPercentage)) {
					// Uninstantiate the Decoder
					decoderStatus = mkeDecoder.uninstantiate();
					if(decoderStatus != MteStatus.mte_status_success)
					{
					    // MTE was not uninstantiated as desired so handle failure appropriately
					    // Below is only an example
					    throw new Exception("Failed to uninstantiate Decoder. Status: "
					        + MteBase.getStatusName(decoderStatus)+ " / "
					        + MteBase.getStatusDescription(decoderStatus));
					}
					
					ResponseModel<HandshakeResponse> updateHandshakeResponse = HandshakeWithServer(clientId);
					if (!updateHandshakeResponse.Success) {
						throw new Exception("Re-Handshake unsuccessful, message:" + updateHandshakeResponse.Message);
					}
					
					// Need to update the Encoder AND Decoder since handshake updates both
					handshake.DecoderState = updateHandshakeResponse.Data.DecoderState;
					handshake.EncoderState = updateHandshakeResponse.Data.EncoderState;
				}else {
					// ----------------------
					// Update Decoder State
					// ----------------------
					handshake.DecoderState = mkeDecoder.saveStateB64();
				}

			} else {
				// -----------------------------
				// else show the error message
				// -----------------------------
				finalData = serverResponse.Message;
			}
			// -----------------------------------
			// Output response code and response
			// -----------------------------------
			System.out.println(responseCode); // Should be 200
			System.out.println(finalData);

			// ---------------------------------------
			// Prompt user to run tasks again or end
			// ---------------------------------------
			System.out.println("Would you like to upload another file? (y/n)");
			String sendAdditional = br.readLine();
			if (sendAdditional != null && sendAdditional.equalsIgnoreCase("n")) {
				break;
			}
		}
	}

	/**
	 * Handshake with the server and create MTE states
	 * 
	 * @param clientId            --> Current client id
	 * @param clients             --> Client hash map
	 * @param currentConversation --> current conversation ID
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
			HandshakeModel handshake = new uploadClient.Models.HandshakeModel();
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
			String handshakeString = gson.toJson(handshake);
			String handshakeResponse = MakeHttpCall(Constants.RestAPIName + Constants.HandshakeRoute, "POST",
					handshake.ConversationIdentifier, Constants.JsonContentType, handshakeString);

			// ---------------------------------------
			// Deserialize the result from handshake
			// ---------------------------------------
			Type handshakeResponseType = new TypeToken<ResponseModel<HandshakeModel>>() {
			}.getType();
			ResponseModel<HandshakeModel> serverResponse = gson.fromJson(handshakeResponse, handshakeResponseType);

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
			var encoderSharedSecret = encoderEcdh.createSharedSecret(serverResponse.Data.ClientEncoderPublicKey);
			var decoderSharedSecret = decoderEcdh.createSharedSecret(serverResponse.Data.ClientDecoderPublicKey);

			// ----------------------------------------------------------
			// Clear container to ensure key is different for each client
			// ----------------------------------------------------------
			encoderEcdh = null;
			decoderEcdh = null;

			// --------------------
			// Create MTE Encoder
			// --------------------
			MteEnc encoder = new MteEnc();
			encoder.setEntropy(encoderSharedSecret);
			encoder.setNonce(Long.parseLong(serverResponse.Data.Timestamp));
			MteStatus status = encoder.instantiate(clientId);
			if (status != MteStatus.mte_status_success) {
				response.Message = "Error creating Encoder: Status: " + MteBase.getStatusName(status) + " / "
						+ MteBase.getStatusDescription(status);
				response.ResultCode = Constants.RC_MTE_ENCODE_EXCEPTION;
				response.Success = false;
				System.out.println(response.Message);
				return response;
			}
			//----------------------------
			// Set Max seed for this DRBG
			//----------------------------
			maxSeed = MteBase.getDrbgsReseedInterval(encoder.getDrbg());

			// ------------------------
			// Get Encoder State
			// ------------------------
			response.Data.EncoderState = encoder.saveStateB64();

			// --------------------
			// Create MTE Decoder
			// --------------------
			MteDec decoder = new MteDec();
			decoder.setEntropy(decoderSharedSecret);
			decoder.setNonce(Long.parseLong(serverResponse.Data.Timestamp));
			status = decoder.instantiate(clientId);
			if (status != MteStatus.mte_status_success) {
				response.Message = "Error creating Decoder: Status: " + MteBase.getStatusName(status) + " / "
						+ MteBase.getStatusDescription(status);
				response.ResultCode = Constants.RC_MTE_DECODE_EXCEPTION;
				response.Success = false;
				System.out.println(response.Message);
				return response;
			}

			// ------------------------
			// Set MTE decoder state
			// ------------------------
			response.Data.DecoderState = decoder.saveStateB64();

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
	private static String MakeHttpCall(String connectionUrl, 
								String connectionMethod, 
								String clientId,
								String contentType, 
								String payload) 
	{
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
				returnPayload = gson.toJson(errorResponse);
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
			returnPayload = gson.toJson(errorResponse);

		} catch (IOException e) {

			e.printStackTrace();
			ResponseModel<Object> errorResponse = new ResponseModel<Object>();
			errorResponse.Data = null;
			errorResponse.Message = "IOException in MakeHttpCall : " + e.getMessage();
			errorResponse.ResultCode = Constants.RC_HTTP_EXCEPTION;
			returnPayload = gson.toJson(errorResponse);

		}
		return returnPayload;
	}
}
