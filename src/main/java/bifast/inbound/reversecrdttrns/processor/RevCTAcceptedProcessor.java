package bifast.inbound.reversecrdttrns.processor;

import java.time.format.DateTimeFormatter;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.iso20022.AppHeaderService;
import bifast.inbound.pojo.Pacs002Seed;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatPacs008Pojo;
import bifast.inbound.service.Pacs002MessageService;
import bifast.inbound.service.UtilService;
import bifast.library.iso20022.custom.BusinessMessage;
import bifast.library.iso20022.custom.Document;
import bifast.library.iso20022.head001.BusinessApplicationHeaderV01;
import bifast.library.iso20022.pacs002.FIToFIPaymentStatusReportV10;

@Component
public class RevCTAcceptedProcessor implements Processor {

	@Autowired private AppHeaderService appHdrService;
	@Autowired private Pacs002MessageService pacs002Service;
	@Autowired private UtilService utilService;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Override
	public void process(Exchange exchange) throws Exception {
		
		ProcessDataPojo processData = exchange.getProperty("prop_process_data", ProcessDataPojo.class);
		FlatPacs008Pojo flatRequest = (FlatPacs008Pojo) processData.getBiRequestFlat();
		
		BusinessMessage reqBusMesg = exchange.getProperty("prop_frBIobj", BusinessMessage.class);
//		CreditTransferTransaction39 biReq =  reqBusMesg.getDocument().getFiToFICstmrCdtTrf().getCdtTrfTxInf().get(0);

		String msgType = processData.getBiRequestMsg().getAppHdr().getBizMsgIdr().substring(16, 19);

		String bizMsgId = utilService.genRfiBusMsgId(msgType, processData.getKomiTrnsId());
		String msgId = utilService.genMsgId(msgType, processData.getKomiTrnsId());

		Pacs002Seed resp = new Pacs002Seed();
		resp.setMsgId(msgId);
		resp.setCreditorAccountIdType(flatRequest.getCreditorAccountType());

		
		resp.setStatus("ACTC");
		resp.setReason("U000");
		

		resp.setCreditorResidentialStatus(flatRequest.getCreditorResidentialStatus());  // 01 RESIDENT
		resp.setCreditorTown(flatRequest.getCreditorTownName());  
		resp.setCreditorType(flatRequest.getCreditorType());
		if (null != flatRequest.getCreditorId())
			resp.setCreditorId(flatRequest.getCreditorId());
			
		if (null != flatRequest.getCreditorName())
			resp.setCreditorName(flatRequest.getCreditorName());
		
		FIToFIPaymentStatusReportV10 respMsg = pacs002Service.creditTransferRequestResponse(resp, reqBusMesg);
		Document doc = new Document();
		doc.setFiToFIPmtStsRpt(respMsg);
		
//		String orignBank = flatRequest.getDebtorAgentId();

		BusinessApplicationHeaderV01 appHdr = appHdrService.getAppHdr("pacs.002.001.10", bizMsgId);
		appHdr.setBizSvc("CLEAR");
		
		BusinessMessage respBusMesg = new BusinessMessage();
		respBusMesg.setAppHdr(appHdr);
		respBusMesg.setDocument(doc);
		
		processData.setBiResponseMsg(respBusMesg);
		exchange.setProperty("prop_process_data", processData);
		exchange.getIn().setBody(respBusMesg);

	}

}
