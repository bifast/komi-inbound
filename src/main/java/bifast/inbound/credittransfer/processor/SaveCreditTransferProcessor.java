package bifast.inbound.credittransfer.processor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.model.CreditTransfer;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatPacs008Pojo;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.inbound.service.CallRouteService;
import bifast.library.iso20022.custom.BusinessMessage;

@Component
public class SaveCreditTransferProcessor implements Processor {

	@Autowired private CallRouteService callRouteService;
	@Autowired private CreditTransferRepository creditTrnRepo;

	@Override
	public void process(Exchange exchange) throws Exception {
		 
		ProcessDataPojo processData = exchange.getProperty("prop_process_data", ProcessDataPojo.class);
		FlatPacs008Pojo flatReq = (FlatPacs008Pojo)processData.getBiRequestFlat();
		
		CreditTransfer ct = new CreditTransfer();

		ct.setKomiTrnsId(processData.getKomiTrnsId());
		
		String fullReqMsg = callRouteService.encryptBiRequest(exchange);
		String fullRespMsg = exchange.getProperty("prop_toBI_jsonzip",String.class);
		
		ct.setFullRequestMessage(fullReqMsg);
		ct.setFullResponseMsg(fullRespMsg);
		
		if (null != processData.getBiResponseMsg()) {
			BusinessMessage respBi = processData.getBiResponseMsg();
			String responseCode = respBi.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts();

			ct.setResponseCode(responseCode);
			ct.setCrdtTrnResponseBizMsgIdr(respBi.getAppHdr().getBizMsgIdr());
			ct.setReasonCode(respBi.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getStsRsnInf().get(0).getRsn().getPrtry());
			ct.setCallStatus("SUCCESS");
			if (responseCode.equals("ACTC")) {
				ct.setCbStatus("PENDING");
				ct.setSettlementConfBizMsgIdr("WAITING");
			}
		}
		
		ct.setCihubRequestDT(processData.getReceivedDt());
		long timeElapsed = Duration.between(processData.getStartTime(), Instant.now()).toMillis();
		ct.setCihubElapsedTime(timeElapsed);

		ct.setCreateDt(LocalDateTime.now());
		ct.setLastUpdateDt(LocalDateTime.now());

//		FIToFICustomerCreditTransferV08 creditTransferReq = rcvBi.getDocument().getFiToFICstmrCdtTrf();
		
		ct.setAmount(flatReq.getAmount());
		ct.setCrdtTrnRequestBizMsgIdr(flatReq.getBizMsgIdr());
		ct.setEndToEndId(flatReq.getEndToEndId());
		ct.setCreditorAccountNumber(flatReq.getCreditorAccountNo());

		if (!(null == flatReq.getCreditorAccountType()))
			ct.setCreditorAccountType(flatReq.getCreditorAccountType());
		
		if (null!=flatReq.getCreditorType()) 
			ct.setCreditorType(flatReq.getCreditorType());

		if (!(null==flatReq.getCreditorId()))
			ct.setCreditorId(flatReq.getCreditorId());
				
		
		ct.setDebtorAccountNumber(flatReq.getDebtorAccountNo());
		ct.setDebtorAccountType(flatReq.getDebtorAccountType());
		
		if (null != flatReq.getDebtorType())
				ct.setDebtorType(flatReq.getDebtorType());

		if (!(null==flatReq.getDebtorId()))
			ct.setDebtorId(flatReq.getDebtorId());

		if (processData.getInbMsgName().equals("CrdTrn"))
			ct.setMsgType("Credit Transfer");
		else
			ct.setMsgType("Reverse CT");
				
		ct.setOriginatingBank(flatReq.getDebtorAgentId());
		ct.setRecipientBank(flatReq.getCreditorAgentId());

		
		String reversal = exchange.getMessage().getHeader("hdr_reversal",String.class);
		ct.setReversal(reversal);
		
		if (null != flatReq.getCpyDplct())
			ct.setCpyDplct(flatReq.getCpyDplct());
		ct.setPsCounter(0);
		
		creditTrnRepo.save(ct);
	}
	
}