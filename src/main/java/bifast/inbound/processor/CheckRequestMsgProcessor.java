package bifast.inbound.processor;

import java.time.Instant;
import java.time.LocalDateTime;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatAdmi004Pojo;
import bifast.inbound.pojo.flat.FlatPacs002Pojo;
import bifast.inbound.pojo.flat.FlatPacs008Pojo;
import bifast.inbound.pojo.flat.FlatPrxy901Pojo;
import bifast.inbound.service.FlattenIsoMessageService;
import bifast.inbound.service.RefUtils;
import bifast.library.iso20022.custom.BusinessMessage;

@Component
public class CheckRequestMsgProcessor implements Processor {
	@Autowired private FlattenIsoMessageService flatMsgService;
	
	
	private static Logger logger = LoggerFactory.getLogger(CheckRequestMsgProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		BusinessMessage inputMsg = exchange.getMessage().getBody(BusinessMessage.class);

		ProcessDataPojo processData = new ProcessDataPojo();

		String trnType = inputMsg.getAppHdr().getBizMsgIdr().substring(16,19);
			
		if (inputMsg.getAppHdr().getMsgDefIdr().startsWith("pacs.002")) {
			
			FlatPacs002Pojo flat002 = flatMsgService.flatteningPacs002(inputMsg); 
			processData.setBiRequestFlat(flat002);
			exchange.setProperty("flatRequest", flat002);
			exchange.setProperty("end2endid", flat002.getOrgnlEndToEndId());
			
			if (inputMsg.getAppHdr().getBizSvc().equals("STTL")) {
				trnType = "SETTLEMENT";
				processData.setInbMsgName("Settl");
				processData.setEndToEndId(flat002.getOrgnlEndToEndId());
				exchange.setProperty("msgName", "Settl");
			}

		}

		else if (inputMsg.getAppHdr().getMsgDefIdr().startsWith("prxy.901")) {
			trnType = "PROXYNOTIF";
			logger.debug("Akan flattening proxy notif");
			FlatPrxy901Pojo flat = flatMsgService.flatteningPrxy901(inputMsg);
			processData.setBiRequestFlat(flat);
			exchange.setProperty("flatRequest", flat);
			processData.setInbMsgName("PrxNtf");
			exchange.setProperty("msgName", "PrxNtf");
		}
		
		else if (inputMsg.getAppHdr().getMsgDefIdr().startsWith("pacs.008")) {
			FlatPacs008Pojo flat008 = flatMsgService.flatteningPacs008(inputMsg); 
			processData.setBiRequestFlat(flat008);
			exchange.setProperty("flatRequest", flat008);
			processData.setEndToEndId(flat008.getEndToEndId());
			exchange.setProperty("end2endid", flat008.getEndToEndId());
			if (trnType.equals("510")) {
				processData.setInbMsgName("AccEnq");
				exchange.setProperty("msgName", "AccEnq");
			}
			else if ((trnType.equals("010")) || (trnType.equals("110"))) {
				processData.setInbMsgName("CrdTrn");
				exchange.setProperty("msgName", "CrdTrn");
			}
			else if (trnType.equals("011")) {
				processData.setInbMsgName("RevCT");
				exchange.setProperty("msgName", "RevCT");
			}
			
		}

		else if (inputMsg.getAppHdr().getMsgDefIdr().startsWith("admi.004")) {
			FlatAdmi004Pojo flat004 = flatMsgService.flatteningAdmi004(inputMsg);
			processData.setBiRequestFlat(flat004);
			exchange.setProperty("flatRequest", flat004);
			trnType = "EVENTNOTIF";
			processData.setInbMsgName("EvtNtf");
			exchange.setProperty("msgName", "EvtNtf");
		}

//		exchange.getMessage().setHeader("hdr_msgType", trnType);
		
		processData.setBiRequestMsg(inputMsg);
		processData.setStartTime(Instant.now());
//		processData.setInbMesgType(trnType);
		processData.setKomiTrnsId(RefUtils.genKomiTrnsId());
		
		logger.debug("KomiTransId: " + processData.getKomiTrnsId());
		
		processData.setReceivedDt(LocalDateTime.now());
//		processData.setTextDataReceived(null);

		exchange.setProperty("starttime", LocalDateTime.now());
		exchange.setProperty("prop_process_data", processData);
		exchange.setProperty("pr_komitrnsid", processData.getKomiTrnsId());
	
	}
	

}
