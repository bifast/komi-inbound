package bifast.inbound.accountenquiry;

import java.time.Duration;
import java.time.Instant;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.corebank.isopojo.AccountEnquiryInboundResponse;
import bifast.inbound.model.AccountEnquiry;
import bifast.inbound.pojo.FaultPojo;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatPacs008Pojo;
import bifast.inbound.repository.AccountEnquiryRepository;
import bifast.library.iso20022.custom.BusinessMessage;

@Component
public class SaveAccountEnquiryProcessor implements Processor {

	@Autowired
	private AccountEnquiryRepository accountEnquiryRepo;

//	@SuppressWarnings("static-access")
	@Override
	public void process(Exchange exchange) throws Exception {
	
		ProcessDataPojo processData = exchange.getProperty("prop_process_data", ProcessDataPojo.class);
		BusinessMessage bmResponse = processData.getBiResponseMsg();
		FlatPacs008Pojo flatRequest = (FlatPacs008Pojo) processData.getBiRequestFlat();

		AccountEnquiry ae = new AccountEnquiry();
		
		String fullRequestMesg = exchange.getProperty("prop_frBI_jsonzip",String.class);
		String fullResponseMesg = exchange.getMessage().getHeader("hdr_toBI_jsonzip", String.class);
			
		ae.setAccountNo(flatRequest.getCreditorAccountNo());
		ae.setAmount(flatRequest.getAmount());
		ae.setReqBizMsgIdr(flatRequest.getBizMsgIdr());
//		ae.setChnlTrxId(null);
		
		ae.setKomiTrnsId(processData.getKomiTrnsId());
		
		long timeElapsed = Duration.between(processData.getStartTime(), Instant.now()).toMillis();
		ae.setElapsedTime(timeElapsed);

		ae.setSubmitDt(processData.getReceivedDt());
		
		ae.setFullRequestMessage(fullRequestMesg);
		ae.setFullResponseMsg(fullResponseMesg);
		
		ae.setRecipientBank(flatRequest.getCreditorAgentId());
		ae.setOriginatingBank(flatRequest.getDebtorAgentId());
		
		ae.setRespBizMsgIdr(bmResponse.getAppHdr().getBizMsgIdr());

		ae.setCallStatus("SUCCESS");
		
		Object oCbResponse = processData.getCorebankResponse();
		if (oCbResponse.getClass().getSimpleName().equals("FaultPojo")) {
			FaultPojo fault = (FaultPojo) oCbResponse;
			ae.setErrorMessage(fault.getErrorMessage());
			ae.setResponseCode("RJCT");
			ae.setReasonCode(fault.getReasonCode());
		}
		else {
			AccountEnquiryInboundResponse aeResponse = (AccountEnquiryInboundResponse) oCbResponse;
			ae.setResponseCode(aeResponse.getStatus());
			ae.setReasonCode(aeResponse.getReason());
		}
			
		accountEnquiryRepo.save(ae);
		
	}

}
