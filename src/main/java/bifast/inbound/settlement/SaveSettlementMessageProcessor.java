package bifast.inbound.settlement;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.model.CreditTransfer;
import bifast.inbound.model.Settlement;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatPacs002Pojo;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.inbound.repository.SettlementRepository;

@Component
public class SaveSettlementMessageProcessor implements Processor {
	@Autowired private CreditTransferRepository ctRepo;
	@Autowired private SettlementRepository settlementRepo;

//	private static Logger logger = LoggerFactory.getLogger(SaveSettlementMessageProcessor.class);

	public void process(Exchange exchange) throws Exception {
		 
		ProcessDataPojo processData = exchange.getProperty("prop_process_data", ProcessDataPojo.class);
		FlatPacs002Pojo flatSttl = (FlatPacs002Pojo) processData.getBiRequestFlat();

		String fullReqMsg = exchange.getMessage().getHeader("hdr_frBI_jsonzip",String.class);
		
		Settlement sttl = new Settlement();
		sttl.setSettlBizMsgId(flatSttl.getBizMsgIdr());
		sttl.setOrgnlEndToEndId(flatSttl.getOrgnlEndToEndId());
		sttl.setReceiveDate(LocalDateTime.now());
		sttl.setFullMessage(fullReqMsg);
		
		sttl.setDbtrBank(flatSttl.getDbtrAgtFinInstnId());
		sttl.setCrdtBank(flatSttl.getCdtrAgtFinInstnId());

		if (!(null == flatSttl.getCdtrAcctId()))
			sttl.setCrdtAccountNo(flatSttl.getCdtrAcctId());
		if (!(null == flatSttl.getDbtrAcctId()))
			sttl.setDbtrAccountNo(flatSttl.getDbtrAcctId());

		CreditTransfer ct = null;
		Optional<CreditTransfer> oct = ctRepo.getSuccessByEndToEndId(flatSttl.getOrgnlEndToEndId());
		if (oct.isPresent()) {
			ct = oct.get();
			ct.setSettlementConfBizMsgIdr("RECEIVED");
			ctRepo.save(ct);

			sttl.setOrgnlCTBizMsgId(ct.getCrdtTrnRequestBizMsgIdr());
			sttl.setKomiTrnsId(ct.getKomiTrnsId());
		}
		
		settlementRepo.save(sttl);
		
	}
}
