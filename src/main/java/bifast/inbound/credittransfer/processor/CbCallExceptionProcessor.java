package bifast.inbound.credittransfer.processor;

import java.lang.reflect.Method;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import bifast.inbound.corebank.pojo.CbCreditRequestPojo;
import bifast.inbound.corebank.pojo.CbCreditResponsePojo;

@Component
public class CbCallExceptionProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		Object objException = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Object.class);
		String exceptionClassName = objException.getClass().getName();		
		
		int statusCode = 500;	
		try {
			Method getStatusCode = objException.getClass().getMethod("getStatusCode");
			statusCode = (int) getStatusCode.invoke(objException);
		} catch(NoSuchMethodException noMethodE) {}
		
		String callStatus = "";
		String reason = "";
		if (exceptionClassName.equals("java.net.SocketTimeoutException")) {
			callStatus = "TIMEOUT";
		}
		else if (statusCode == 504) {
			callStatus = "TIMEOUT";
			reason = "U900";
		}
		else {
			callStatus = "ERROR";
			reason = "U901";
		}
		
		String errorInfo= "Check error log";
		try {
			Method getMessage = objException.getClass().getMethod("getMessage");
			errorInfo = (String) getMessage.invoke(objException);
			errorInfo = objException.getClass().getSimpleName() + ": " + errorInfo;
			if (errorInfo.length()>250)
				errorInfo = errorInfo.substring(0,249);
		}
		catch(NoSuchMethodException noMethodE) {
			errorInfo = "Check error log";
		}
		
		CbCreditRequestPojo cbReq = exchange.getMessage().getHeader("post_crdt_request", CbCreditRequestPojo.class);
		CbCreditResponsePojo crdtResponse = new CbCreditResponsePojo();
		crdtResponse.setAccountNumber(cbReq.getCreditorAccountNumber());
		crdtResponse.setStatus(callStatus);
		crdtResponse.setReason(reason);
		crdtResponse.setAdditionalInfo(errorInfo);

		exchange.getMessage().setBody(crdtResponse);

	}

}
