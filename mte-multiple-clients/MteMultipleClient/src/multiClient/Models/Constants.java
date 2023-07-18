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

public class Constants {

	//----------------
    // Cache settings
    //----------------
    public static int ExpireMinutes = 1440;
    public static String IVKey = "63137d61-992e-434e-9fcf-81fec2e599de";
    public static String EncoderPrefix = "ENC_";
    public static String DecoderPrefix = "DEC_";
	
	//----------------------
    // MTE Client ID header
    //----------------------
    public static String ClientIdHeader = "x-client-id";
    
    //-------------------
    // result codes
    //-------------------
    public static String STR_SUCCESS = "SUCCESS";
    public static String RC_SUCCESS = "000";
    public static String RC_VALIDATION_ERROR = "100";
    public static String RC_MTE_ENCODE_EXCEPTION = "104";
    public static String RC_MTE_DECODE_EXCEPTION = "112";
    public static String RC_MTE_STATE_CREATION = "114";
    public static String RC_MTE_STATE_RETRIEVAL = "115";
    public static String RC_MTE_STATE_SAVE = "116";
    public static String RC_MTE_STATE_NOT_FOUND = "117";
    public static String RC_HTTP_ERROR = "200";
    public static String RC_HTTP_EXCEPTION = "201";
    
}
