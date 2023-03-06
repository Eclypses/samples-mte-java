package handshake.Models;

import com.google.gson.annotations.SerializedName;

public class ResponseModel<T> {
	  @SerializedName(value="Data")
	  public T Data;
	  public String Message;
	  public Boolean Success;
	  public String ResultCode;
	  public String ExceptionUid;
	  public String access_token;
	  
	  public ResponseModel(T data)
	  {
	    Data = data;
	    this.Message = "Success";
	    this.ResultCode = "000";
	    this.Success = true;
	    this.ExceptionUid = "";
	    this.access_token = "";
	  }

	  public ResponseModel()
	  {
	    this.Message = "Success";
	    this.ResultCode = "000";
	    this.Success = true;
	    this.ExceptionUid = "";
	    this.access_token = "";
	    
	  }

	  
	  public <M> ResponseModel<M> ReturnDataWithResponseModel(ResponseModel<T> inResponse, M data){
		    ResponseModel<M> response = new ResponseModel<M>(data);
		    response.Message = inResponse.Message;
		    response.ResultCode = inResponse.ResultCode;
		    response.Success = inResponse.Success;
		    response.access_token = inResponse.access_token;
		    response.ExceptionUid = inResponse.ExceptionUid;
		    return response;
	  } 
	}
