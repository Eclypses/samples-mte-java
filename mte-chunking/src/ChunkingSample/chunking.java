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


package ChunkingSample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.eclypses.mte.*;

public class chunking {
	private static MteMkeEnc mkeEncoder;
	private static MteMkeDec mkeDecoder;
	
	//-------------
	// File names
	//-------------
	private static String pathToEncodedFile = "src/encoded";
	private static String pathToDecodedFile = "src/decoded";
	private static final String defaultExtension = ".txt";
	
	private static final int BUFFER_SIZE = 1024; // 1KB
	
	 /**
	 * Main  
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		try {
			
			// -----------------
			// Buffered input.
			// -----------------
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String fPath = "";
			// ----------------------
			// Prompt for file path
			// ----------------------
			System.out.print("Enter full path to file: \n");
			//----------------------------------------
			// Loop till we get a valid file to encode
			//----------------------------------------
			while(true) {
				//---------------------------------------------
				// Ensure file exists, and update both output 
				// files with correct file extension
				//---------------------------------------------
				fPath = br.readLine();
				File textFile = new File(fPath);
				if(textFile.exists()){
					String filename = textFile.getName();
					String extension = "";
					if (filename.contains(".")) {
					     extension = filename.substring(filename.lastIndexOf("."));
					     pathToEncodedFile = pathToEncodedFile + extension;
					     pathToDecodedFile = pathToDecodedFile + extension;
					}else {
						pathToEncodedFile = pathToEncodedFile + defaultExtension;
						pathToDecodedFile = pathToDecodedFile + defaultExtension;
					}
					break;
				}else {
					System.out.print("Invalid file path, please type in full path to file.\n");
				}
			}
						
			//--------------------
			// Create MKE Encoder
			//--------------------
			mkeEncoder = new MteMkeEnc();

			//------------------------------------------------------
			// Set encoder nonce and identifier
			// These values should be treated like encryption keys
			//------------------------------------------------------
			long nonce = 0;
			String identifier = "demo";

			//------------------------------------------------------
			// Check how long entropy we need, set default all 0's 
			// should be treated like encryption keys - this is just example
			//------------------------------------------------------
			int entropyMinBytes = MteBase.getDrbgsEntropyMinBytes(mkeEncoder.getDrbg());
			StringBuffer outputBuffer = new StringBuffer(entropyMinBytes);
			for (int i = 0; i < entropyMinBytes; i++){
			    outputBuffer.append("0");
			}
			String entropy = outputBuffer.toString();
			entropy.replace(' ', '0');

			//--------------------------------
			// Set MTE values for the Encoder
			//--------------------------------
			mkeEncoder.setEntropy(entropy.getBytes());
			mkeEncoder.setNonce(nonce);

			MteStatus encoderStatus = mkeEncoder.instantiate(identifier);
			if (encoderStatus != MteStatus.mte_status_success)
			{
				throw new Exception("Failed to initialize the MTE encoder engine. Status: "
				        + MteBase.getStatusName(encoderStatus)+ " / " 
				        + MteBase.getStatusDescription(encoderStatus));
			}

			//-----------------------------
			// Initialize chunking session
			//-----------------------------
			encoderStatus = mkeEncoder.startEncrypt();
			if(encoderStatus != MteStatus.mte_status_success)
			{
			    throw new Exception("Failed to start encode chunk. Status: "
			        + MteBase.getStatusName(encoderStatus)+ " / " 
			        + MteBase.getStatusDescription(encoderStatus));
			}

			//---------------------------------------
			// Set input and output file for encode
			//---------------------------------------
			String inputFile = fPath;
			String outputFile = pathToEncodedFile;
			
			//----------------------------------------------
			// Check if output file exists, if so delete it
			//----------------------------------------------
			Path fileToDeletePathEN = Paths.get(outputFile);
			Files.deleteIfExists(fileToDeletePathEN);
			 
			//---------------------------------
			// Create input and output streams
			//---------------------------------
			try (
			    InputStream inputStream = new FileInputStream(inputFile);
			    OutputStream outputStream = new FileOutputStream(outputFile);
			    ) {
			 
					//-------------------------------------
					// Set buffer and create input stream
					//-------------------------------------
			        byte[] buffer = new byte[BUFFER_SIZE];
			        int bytesRead;
			            while ((bytesRead = inputStream.read(buffer)) != -1) {
			            	//------------------------------------------------------------
			                // Encode the data in place - encoded data put back in buffer
			            	//------------------------------------------------------------
			                MteStatus chunkStatus = mkeEncoder.encryptChunk(buffer, 0, bytesRead);
			                if(chunkStatus != MteStatus.mte_status_success)
			                {
			                    throw new Exception("Failed to encode chunk. Status: "
			                        + MteBase.getStatusName(chunkStatus)+ " / " 
			                        + MteBase.getStatusDescription(chunkStatus));
			                }
			                outputStream.write(buffer, 0, bytesRead);
			            }
			            //-----------------------------
			            // Finish the chunking session
			            //-----------------------------
			            MteBase.ArrStatus finalEncodedChunk = mkeEncoder.finishEncrypt();
			            if(finalEncodedChunk.status != MteStatus.mte_status_success)
			            {
			                throw new Exception("Failed to finish encode chunk. Status: "
			                    + MteBase.getStatusName(finalEncodedChunk.status)+ " / " 
			                    + MteBase.getStatusDescription(finalEncodedChunk.status));
			            }
			            //----------------------------------
			            // Append the final data to the file
			            //----------------------------------
			            outputStream.write(finalEncodedChunk.arr);
			 
			        } catch (IOException ex) {
			            ex.printStackTrace();
			        }

			System.out.println("Encoded file " + pathToEncodedFile + " created.");
			//----------------
			// Refill entropy
			//----------------
			outputBuffer = new StringBuffer(entropyMinBytes);
			for (int i = 0; i < entropyMinBytes; i++){
			    outputBuffer.append("0");
			}
			entropy = outputBuffer.toString();
			entropy.replace(' ', '0');
			
			mkeDecoder = new MteMkeDec();
			
			//--------------------------------
			// Set MTE values for the Decoder
			//--------------------------------
			mkeDecoder.setEntropy(entropy.getBytes());
			mkeDecoder.setNonce(nonce);
			
			MteStatus decoderStatus = mkeDecoder.instantiate(identifier);
			if(decoderStatus != MteStatus.mte_status_success)
			{
			    throw new Exception("Failed to initialize Decoder. Status: "
			        + MteBase.getStatusName(decoderStatus)+ " / " 
			        + MteBase.getStatusDescription(decoderStatus));
			}
			
			//-----------------------------
			// Initialize chunking session
			//-----------------------------
			decoderStatus = mkeDecoder.startDecrypt();
			if(decoderStatus != MteStatus.mte_status_success)
			{
			    throw new Exception("Failed to start decode chunk. Status: "
			        + MteBase.getStatusName(decoderStatus)+ " / " 
			        + MteBase.getStatusDescription(decoderStatus));
			}
			
			//----------------------------
			// Set and check decoded file
			//----------------------------
			String finalOutputFile = pathToDecodedFile;
			Path fileToDeletePathDC = Paths.get(finalOutputFile);
			Files.deleteIfExists(fileToDeletePathDC);
			 
			//---------------------------------
			// Create input and output stream
			//---------------------------------
			try (
			    InputStream inputStream = new FileInputStream(outputFile);
			    OutputStream outputStream = new FileOutputStream(finalOutputFile);
			    ) {
			 
					//--------------------------
					// Read in bytes and decode
					//--------------------------
			        byte[] buffer = new byte[BUFFER_SIZE];
			        
			        int bytesRead;
			            while ((bytesRead = inputStream.read(buffer)) != -1) {
			            	
			            	//-------------------------------------------------------
			            	// If bytesRead is equal to buffer do simple decodeChunk
			            	//-------------------------------------------------------
			            	byte[] decodedChunk = new byte[0];
		                    if (bytesRead == buffer.length)
		                    {
		                    	decodedChunk = mkeDecoder.decryptChunk(buffer);
		                    }
		                    else
		                    {
		                        //------------------------------------------
		                        // Find out what the decoded length will be
		                        //------------------------------------------
		                    	int cipherBlock = MteBase.getCiphersBlockBytes(mkeDecoder.getCipher());
		                        int buffBytes = bytesRead - cipherBlock;
		                        //----------------------------------
		                        // Allocate buffer for decoded data
		                        //----------------------------------
		                        decodedChunk = new byte[buffBytes];
		                        int decryptError = mkeDecoder.decryptChunk(buffer, 0, bytesRead, decodedChunk, 0);
		                        if (decryptError < 0)
		                        {
		                        	throw new IllegalArgumentException("decrypt chunk error.");
		                        }
		                    }      
			                
		                    //-----------------------------
			                // Write decoded chunk to file
		                    //-----------------------------
			                outputStream.write(decodedChunk);
			            }
			            //----------------------------
			            // Finish the chunking session
			            //----------------------------
			            MteBase.ArrStatus finalEncodedChunk = mkeDecoder.finishDecrypt();
			            if(finalEncodedChunk.status != MteStatus.mte_status_success)
			            {
			                throw new Exception("Failed to finish decode chunk. Status: "
			                    + MteBase.getStatusName(finalEncodedChunk.status)+ " / " 
			                    + MteBase.getStatusDescription(finalEncodedChunk.status));
			            }
			            //-------------------------------------
			            // Check if there is additional bytes 
			            // If not initialize empty byte array
			            //-------------------------------------
			            if(finalEncodedChunk.arr == null || finalEncodedChunk.arr.length <=0) { finalEncodedChunk.arr = new byte[0]; }
			            
			            //-----------------------------------
			            // Append the final data to the file
			            //-----------------------------------
			            outputStream.write(finalEncodedChunk.arr);
			            
			            System.out.println("Encoded file decoded to " + pathToDecodedFile);
			 
			        } catch (IOException ex) {
			            ex.printStackTrace();
			        }
		}catch(Exception ex) {
			throw ex;
	  }
	}
}
