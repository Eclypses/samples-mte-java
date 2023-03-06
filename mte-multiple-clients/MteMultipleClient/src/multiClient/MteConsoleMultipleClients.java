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

package multiClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.eclypses.ecdh.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.eclypses.mte.*;
import com.eclypses.mte.MteBase.StrStatus;

import multiClient.Models.Constants;
import multiClient.Models.HandshakeModel;
import multiClient.Models.ResponseModel;

public class MteConsoleMultipleClients {
	//-----------------------------------------
    // Set max number of trips made per client
    //-----------------------------------------
	private static int maxNumberOfTrips = 5;
	private static String encIV;
	private static long maxSeedInterval = 0;
	
	//------------------------------------------
    // Declare different possible content types
    //------------------------------------------
    private static String jsonContentType = "application/json";
    private static String textContentType = "text/plain";
    
    //------------------
    // Set Rest API URL
    //------------------
    //private static String _restAPIName = "http://localhost:52603";
    private static String restAPIName = "https://dev-echo.eclypses.com";
    
    //--------------------------------------------------------
    // Create gson object that be compatible with C-Sharp json
    //--------------------------------------------------------
    public static final Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(byte[].class,
            new ByteArrayToBase64TypeAdapter()).create();
    
    //------------------------
    // Using base64 libraries 
    //------------------------
    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.getDecoder().decode(json.getAsString());
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
        }
    }
    
    //--------------
    // State Helpers
    //--------------
    private static CacheHelper mteStateCacheHelper;
    
    /**
     * Main program Handshake with server then send messages
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
    	try {
    		//-------------------
            // Create session IV 
            //-------------------
    		encIV = UUID.randomUUID().toString();
    		
    		//----------------------------
    		// Create cache for MTE state
    		//----------------------------
    		mteStateCacheHelper = new CacheHelper();
    		
    		//----------------
    		// Buffered input
    		//----------------
    	    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	    
    	    //-----------------------------------------------
            // Prompt user to ask how many clients to create
            //-----------------------------------------------
    		System.out.println("How many clients? (Enter number between 1-50)");
    		int clientNum = Integer.parseInt(br.readLine());
    		
    		//--------------------------------------
            // Create array for all conversationID's
            //--------------------------------------
    		HashMap<Integer, String> clients = new HashMap<Integer, String>();
    		
    		//-----------------------------------------
            // Run handshake and state for each client
            //-----------------------------------------
            for (int i = 1; i <= clientNum; i++)
            {
                //-----------------------------
                // Handshake client with server
                //-----------------------------
                boolean handshakeSuccessful = HandshakeWithServer(i, clients, null);
                if (!handshakeSuccessful)
                {
                	System.out.println("Handshake unsuccessful!");
                    throw new Exception("Handshake unsuccessful!");
                }
            }
            
            //----------------------------------------------------
            // Completed creating mte states for number of clients
            //----------------------------------------------------
            System.out.println("Created MTE state for " + clientNum + "'s");
            
            while (true)
            {
            	Random rnd = new Random();
            	//--------------------------------
            	// Create array of all async tasks
            	//--------------------------------
            	CompletableFuture<?>[] tasks = new CompletableFuture<?>[clients.size()];
            	int count = 0;
            	//-------------------------
            	// Iterate through clients
            	//-------------------------
            	for (Entry<Integer, String> mapElement : clients.entrySet()) {
            		//--------------------------
            		// Create actual async task
            		//--------------------------
            		CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
            			ContactServer(rnd, mapElement.getValue(), mapElement.getKey(), clients);
            			System.out.println("Completed ContactServer on " + mapElement.getKey());
            		});
            		//-------------------------
            		// Add future to task list
            		//-------------------------
            		tasks[count] = cf;
            		count++;
            	}
            	//------------------------------------------------------
            	// Join all created tasks to run asynchronously and run
            	//------------------------------------------------------
            	CompletableFuture<Void> allFutures = CompletableFuture.allOf(tasks);
            	allFutures.get();         	            	
            	
            	//---------------------------------------
            	// Prompt user to run tasks again or end
            	//---------------------------------------
            	System.out.println("Completed sending messages to " + clientNum + " clients.");
            	System.out.println("Would you like to send additional messages to clients? (y/n)");
            	String sendAdditional = br.readLine();
            	if(sendAdditional != null && sendAdditional.equalsIgnoreCase("n")) {
            		break;
            	}
            }
    	}catch(Exception ex) {
    		throw ex;
    	}
    }
    
    /**
     * Contact Server will send an encoded message to the server 
     * then decode what comes back
     * @param rnd -->Random generator for number of trips 
     * @param currentConversation -->Client conversation
     * @param clientNum --> current client number 
     * @param clients --> client hash map
     */
    private static void ContactServer(Random rnd, String currentConversation, int clientNum, HashMap<Integer, String> clients){
    	try {
    		//--------------------------
    		// Create encryption helper
    		//--------------------------
    		EncryptionHelper crypt = new EncryptionHelper();
    		
    		//-------------------------------------------------
            // Randomly select number of trips between 1 and max number of trips
            //-------------------------------------------------
            int numberTrips = rnd.nextInt(maxNumberOfTrips-1) + 1;
            
            //---------------------------------------
            // Send message selected number of trips
            //---------------------------------------
            for (int t = 0; t < numberTrips; t++)
            {
                //-------------------------------------
                // Get the current client Encoder state
                //-------------------------------------
            	String encryptedEncState = mteStateCacheHelper.Get(Constants.EncoderPrefix + currentConversation);            	
            	if(encryptedEncState == null || encryptedEncState == "") {
            		throw new Exception("Cannot find encryted Encoder State for " + currentConversation);
            	}
            	String encoderState = crypt.decrypt(encryptedEncState, 
            			EncryptionHelper.SHA256(currentConversation, 64), 
            			encIV.toString());
            	
            	//-------------------------------------
                // Restore the Encoder ensure it works
                //-------------------------------------
            	MteEnc encoder = new MteEnc();
            	MteStatus encoderStatus = encoder.restoreStateB64(encoderState);
            	if (encoderStatus != MteStatus.mte_status_success)
                {
                    System.out.println("Error restoring the Encoder mte state for Client " + clientNum +": " + MteBase.getStatusDescription(encoderStatus));
                    throw new Exception("Error restoring the Encoder mte state for Client " + clientNum + ": " + MteBase.getStatusDescription(encoderStatus));
                }
            	            	
            	//-------------------------
                // Encode message to send
                //-------------------------
                String message = "Hello from client " + clientNum + " for the " + (t + 1) + " time.";
                StrStatus encodedPayload = encoder.encodeB64(message);
                if(encodedPayload.status != MteStatus.mte_status_success) {
                	System.out.println("Error encoding the message: " + MteBase.getStatusDescription(encodedPayload.status));
                    throw new Exception("Error restoring the Decoder mte state for Client " + clientNum + ": " + MteBase.getStatusDescription(encodedPayload.status));
                }
                System.out.println("Sending message " + message + "to multi-client server.");
                
                //-----------------------------------------------------------
                // Send encoded message to server, putting clientId in header
                //-----------------------------------------------------------
                String multipleClientResponse = MakeHttpCall(restAPIName + "/api/multiclient", "POST", currentConversation, textContentType, encodedPayload.str);
                
                //----------------------
                // de-serialize response
                //----------------------
                Type multipClientResponseType = new TypeToken<ResponseModel<String>>() {}.getType();
            	ResponseModel<String> serverResponse = gson.fromJson(multipleClientResponse, multipClientResponseType);
            	if(!serverResponse.Success) {
            		if(serverResponse.ResultCode.equalsIgnoreCase(Constants.RC_MTE_STATE_NOT_FOUND)) {
            			//-------------------------------------------------------------------------
                        // the server does not have this client's state - we should "re-handshake"
                        //-------------------------------------------------------------------------
                        boolean handshakeIsSuccessful = HandshakeWithServer(clientNum, clients, currentConversation);
                        if (!handshakeIsSuccessful)
                        {
                            System.out.println("Error from server for client " + clientNum + ": " + serverResponse.Message);
                            throw new Exception("Error from server for client " + clientNum + ": " + serverResponse.Message);
                        }
                        //-----------------------------------------------------------------
                        // break out of this loop so we can contact again after handshake
                        //-----------------------------------------------------------------
                        return;
            		}
            	}
            	
            	//---------------------------------------------------
                // If this was successful save the new Encoder state
                //---------------------------------------------------
            	encoderState = encoder.saveStateB64();
            	encryptedEncState = crypt.encrypt(encoderState,
                        EncryptionHelper.SHA256(currentConversation, 64),
                        encIV.toString());
        		
            	mteStateCacheHelper.Store(Constants.EncoderPrefix + currentConversation, encryptedEncState);
            	
        		//-------------------------------------
                // Get the current client Decoder state
                //-------------------------------------
            	String encryptedDecState = mteStateCacheHelper.Get(Constants.DecoderPrefix + currentConversation);
            		if(encryptedDecState == null || encryptedDecState == "") {
            			throw new Exception("Cannot find encryted Decoder State for " + currentConversation);
            		}
            	String decoderState = crypt.decrypt(encryptedDecState, 
            			EncryptionHelper.SHA256(currentConversation, 64), 
            			encIV.toString());
            	
            	//-------------------------------------
                // Restore the Decoder ensure it works
                //-------------------------------------
            	MteDec decoder = new MteDec();
            	MteStatus decoderStatus = decoder.restoreStateB64(decoderState);
            	if (decoderStatus != MteStatus.mte_status_success)
                {
					String errorMessage = "Error restoring the Decoder mte state for Client " + clientNum +": " + MteBase.getStatusDescription(decoderStatus)
                    System.out.println(errorMessage);
                    throw new Exception(errorMessage);
                }
        		
        		//-----------------------------
                // Decode the incoming message
                //-----------------------------
        		StrStatus decodedMessage = decoder.decodeStrB64(serverResponse.Data);
        		if(decodedMessage.status != MteStatus.mte_status_success) {
					String errorMessage = "Error decoding the message: " + MteBase.getStatusDescription(decodedMessage.status);
                	System.out.println(errorMessage);
                    throw new Exception(errorMessage);
                }
        		
        		//-------------------------
				// Check current seed life
				//-------------------------
				long currentSeed = decoder.getReseedCounter();
				
				if(currentSeed > (maxSeedInterval * .9)) {
					// Uninstantiate the Decoder
					decoderStatus = decoder.uninstantiate();
					if(decoderStatus != MteStatus.mte_status_success)
					{
					    // MTE was not uninstantiated as desired so handle failure appropriately
					    // Below is only an example
					    throw new Exception("Failed to uninstantiate Decoder. Status: "
					        + MteBase.getStatusName(decoderStatus)+ " / "
					        + MteBase.getStatusDescription(decoderStatus));
					}
					// Uninstantiate the Encoder
					encoderStatus = encoder.uninstantiate();
					if(encoderStatus != MteStatus.mte_status_success)
					{
					    // MTE was not uninstantiated as desired so handle failure appropriately
					    // Below is only an example
					    throw new Exception("Failed to uninstantiate Encoder. Status: "
					        + MteBase.getStatusName(encoderStatus)+ " / "
					        + MteBase.getStatusDescription(encoderStatus));
					}
					//-----------------------------
	                // Handshake client with server
	                //-----------------------------
	                boolean handshakeSuccessful = HandshakeWithServer(clientNum, clients, null);
	                if (!handshakeSuccessful)
	                {
	                	System.out.println("Handshake unsuccessful!");
	                    throw new Exception("Handshake unsuccessful!");
	                }
					
				}else {
					//----------------------------------------
	                // If decode is successful save new state 
	                //----------------------------------------
	        		decoderState = decoder.saveStateB64();
	        		encryptedDecState = crypt.encrypt(decoderState, 
	        				EncryptionHelper.SHA256(currentConversation, 64), 
	        				encIV.toString());
	        		mteStateCacheHelper.Store(Constants.DecoderPrefix + currentConversation, encryptedDecState);
				}
        		
        		// Sleep between each call a random amount of time
                // between 10 and 100 milli-seconds
                Thread.sleep(rnd.nextInt(100));
        		
            }// end if statement
            	
    	}catch (Exception ex) {
    		ex.printStackTrace();
    		System.out.println("Exception contacting server: "
    				+ ex.getMessage());
    	}
		return;
    }
    
    /**
     * Handshake with the server and create MTE states
     * @param clientId --> Current client id
     * @param clients --> Client hash map
     * @param currentConversation --> current conversation ID
     * @return
     */
    private static boolean HandshakeWithServer(int clientId, HashMap<Integer, String> clients, String currentConversation)
    {
    	try 
    	{
    		System.out.println("Performing Handshake for Client " + clientId);
    		
    		//--------------------------------
            // create clientId for this client
            //--------------------------------
            HandshakeModel handshake = new HandshakeModel(); 
            handshake.ConversationIdentifier = UUID.randomUUID().toString();
            
            //----------------------------------------------------------
            // If current conversation id passed in update identifier
            //----------------------------------------------------------
            if (currentConversation != null && !currentConversation.isEmpty())
            {
                handshake.ConversationIdentifier = currentConversation;
            }
            
            //-------------------------------------------------------------
            // Add client to dictionary list if this is a new conversation
            //-------------------------------------------------------------
            if(!clients.containsKey(clientId)) {
            	clients.put(clientId, handshake.ConversationIdentifier);
            }
            
            //-------------------------------------------
            // Create eclypses DH containers for handshake
            //-------------------------------------------
            EclypsesECDH encoderEcdh = new EclypsesECDH();
            EclypsesECDH decoderEcdh = new EclypsesECDH();
            
            //-------------------------------------------
            // Get the public key to send to other side
            //-------------------------------------------
            handshake.ClientEncoderPublicKey = encoderEcdh.getDevicePublicKey();
            handshake.ClientDecoderPublicKey = decoderEcdh.getDevicePublicKey();
            
            //-------------------
            // Perform handshake
            //-------------------
            String handshakeString = gson.toJson(handshake);
            String handshakeResponse = MakeHttpCall(restAPIName + "/api/handshake", "POST", handshake.ConversationIdentifier, jsonContentType, handshakeString);
            
            //---------------------------------------
            // Deserialize the result from handshake
            //---------------------------------------
            Type handshakeResponseType = new TypeToken<ResponseModel<HandshakeModel>>() {}.getType();
            ResponseModel<HandshakeModel> response =
            		gson.fromJson(handshakeResponse, handshakeResponseType);
            
            //---------------------------------------
            // If handshake was not successful end
            //---------------------------------------
            if (!response.Success)
            {
            	System.out.println("Error making DH handshake for Client " + clientId+ ": " +response.Message);
                return false;
            }
            
            //----------------------
            // Create shared secret
            //----------------------
            var encoderSharedSecret = encoderEcdh.createSharedSecret(response.Data.ClientEncoderPublicKey);
            var decoderSharedSecret = decoderEcdh.createSharedSecret(response.Data.ClientDecoderPublicKey);
            
            //----------------------------------------------------------
            // Create and store MTE Encoder and Decoder for this Client
            //----------------------------------------------------------
            
            ResponseModel<Void> mteResponse = CreateMteStates(response.Data.ConversationIdentifier,
            		encoderSharedSecret, 
            		decoderSharedSecret, 
            		Long.parseLong(response.Data.Timestamp));
            
            //----------------------------------------------------------
            // Clear container to ensure key is different for each client
            //----------------------------------------------------------
            encoderEcdh = null;
            decoderEcdh = null;
            
            //-----------------------------------------
            // If there was an error break out of loop
            //-----------------------------------------
            if (!mteResponse.Success)
            {
                System.out.println("Error creating mte states for Client " + clientId + ": " + response.Message);
                return false;
            }
            		
            return true;
    	}catch (Exception ex) {
    		ex.printStackTrace();
    		throw ex;
    	}
    }
    
    /**
     * Create Initial MTE states and save to cache
     * @param personal --> current conversation id
     * @param encoderEntropy --> Encoder entropy
     * @param decoderEntropy --> Decoder entropy
     * @param nonce --> MTE nonce
     * @return
     */
    private static ResponseModel<Void> CreateMteStates(String personal, byte[] encoderEntropy, byte[] decoderEntropy, long nonce) {
    	ResponseModel<Void> response = new ResponseModel<Void>();
    	try
    	{   
    		//--------------------------
    		// Create encryption helper
    		//--------------------------
    		EncryptionHelper crypt = new EncryptionHelper();
    		
    		//--------------------
            // Create MTE Encoder 
            //--------------------
    		MteEnc encoder = new MteEnc();
    		encoder.setEntropy(encoderEntropy);
    		encoder.setNonce(nonce);
    		MteStatus status = encoder.instantiate(personal);
    		if (status != MteStatus.mte_status_success)
            {
                System.out.println("Error creating Encoder: Status: " + MteBase.getStatusName(status) + " / " + MteBase.getStatusDescription(status));
                response.Message =
                		"Error creating Encoder: Status: " + MteBase.getStatusName(status) + " / " + MteBase.getStatusDescription(status);
                response.ResultCode = Constants.RC_MTE_ENCODE_EXCEPTION;
                response.Success = false;
                return response;
            }
    		//--------------------------------------
    		// Get the Max Seed Interval if NOT set
    		//--------------------------------------
    		if (maxSeedInterval <= 0)
            {
    			maxSeedInterval = MteBase.getDrbgsReseedInterval(encoder.getDrbg());
            }
    		
    		//------------------------
            // Save and encrypt state
            //------------------------
    		String encoderState = encoder.saveStateB64();
    		String encryptedEncState = crypt.encrypt(encoderState,
                    EncryptionHelper.SHA256(personal, 64),
                    encIV.toString());
    		mteStateCacheHelper.Store(Constants.EncoderPrefix + personal, encryptedEncState);
    		
    		//--------------------
            // Create MTE Decoder
            //--------------------
    		MteDec decoder = new MteDec();
    		decoder.setEntropy(decoderEntropy);
    		decoder.setNonce(nonce);
    		status = decoder.instantiate(personal);
    		if (status != MteStatus.mte_status_success)
            {
                System.out.println("Error creating Decoder: Status: " + MteBase.getStatusName(status) + " / " + MteBase.getStatusDescription(status));
                response.Message =
                		"Error creating Decoder: Status: " + MteBase.getStatusName(status) + " / " + MteBase.getStatusDescription(status);
                response.ResultCode = Constants.RC_MTE_DECODE_EXCEPTION;
                response.Success = false;
                return response;
            }
    		
    		//------------------------
            // Save and encrypt state
            //------------------------
    		String decoderState = decoder.saveStateB64();
    		String encryptedDecState = crypt.encrypt(decoderState, 
    				EncryptionHelper.SHA256(personal, 64), 
    				encIV.toString());
    		mteStateCacheHelper.Store(Constants.DecoderPrefix + personal, encryptedDecState);

    		response.Success = true;
    		response.ResultCode = Constants.RC_SUCCESS;
    		response.Message = Constants.STR_SUCCESS;
    		
    	}catch(Exception ex) {
    		response.Message = "Exception creating MTE state. Ex: " + ex.getMessage();
            response.ResultCode = Constants.RC_MTE_ENCODE_EXCEPTION;
            response.Success = false;
    	}
    	return response;
    }
    
    
    /**
     * Make the Http Call
     * @param connectionUrl --> Connection URL
     * @param connectionMethod --> Connection Method
     * @param clientId --> Current client id
     * @param contentType --> http content type
     * @param payload --> http call payload
     * @return
     */
    private static String MakeHttpCall(String connectionUrl, String connectionMethod, String clientId, String contentType, String payload) {
    	String returnPayload = "";
    	try {
    		//-----------------------------------------
            // Set URI and other default Http settings
            //-----------------------------------------
    		URL url = new URL(connectionUrl);
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		
    		//---------------------------
    		// Check if we have a payload
    		//---------------------------
    		if(payload != null && !payload.isEmpty()) {
    			conn.setDoOutput(true);	
    		}    		
    		conn.setRequestMethod(connectionMethod);
    		conn.setRequestProperty("Content-Type", contentType);
    		
    		//-------------------------
            // Add client id to header 
            //-------------------------
    		conn.setRequestProperty(Constants.ClientIdHeader, clientId);
    		
    		OutputStream os = conn.getOutputStream();
    		os.write(payload.getBytes());
    		os.flush();
    		
    		//--------------------------------------------------
    		// If we did not get a good connection return error
    		//--------------------------------------------------
    		if (conn.getResponseCode() < HttpURLConnection.HTTP_OK || conn.getResponseCode() > 299) {
    		
    			//------------------------------------------------------
    			// Create error response to send back to calling method
    			//------------------------------------------------------
    			ResponseModel<Object> errorResponse = new ResponseModel<Object>();
    			errorResponse.Data = null;
    			errorResponse.Message = "Failed : HTTP error code : "
    				+ conn.getResponseCode();
    			errorResponse.ResultCode = Constants.RC_HTTP_ERROR;
    			returnPayload = gson.toJson(errorResponse);
    			return returnPayload;
    		}

    		//-------------------------
    		// Read in the inputStream
    		//-------------------------
    		BufferedReader br = new BufferedReader(new InputStreamReader(
    				(conn.getInputStream())));
    		
    		//--------------------
    		// Assign the payload
    		//--------------------
    		returnPayload = br.readLine();    		
					
    		//---------------------------
    		// Disconnect the connection
    		//---------------------------
    		conn.disconnect();
    		
    		} catch (MalformedURLException e) {

    			e.printStackTrace();
    			ResponseModel<Object> errorResponse = new ResponseModel<Object>();
    			errorResponse.Data = null;
    			errorResponse.Message = "MalformedURLException in MakeHtteCall : "
    				+ e.getMessage();
    			errorResponse.ResultCode = Constants.RC_HTTP_EXCEPTION;
    			returnPayload = gson.toJson(errorResponse);

    		} catch (IOException e) {

    			e.printStackTrace();
    			ResponseModel<Object> errorResponse = new ResponseModel<Object>();
    			errorResponse.Data = null;
    			errorResponse.Message = "IOException in MakeHttpCall : "
    				+ e.getMessage();
    			errorResponse.ResultCode = Constants.RC_HTTP_EXCEPTION;
    			returnPayload = gson.toJson(errorResponse);

    		}
    	return returnPayload;
    }
}

