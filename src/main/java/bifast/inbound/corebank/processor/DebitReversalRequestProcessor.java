package bifast.inbound.corebank.processor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import bifast.inbound.corebank.isopojo.DebitReversalRequest;
import bifast.inbound.model.CorebankTransaction;
import bifast.inbound.model.CreditTransfer;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatPacs008Pojo;
import bifast.inbound.repository.CorebankTransactionRepository;
import bifast.inbound.repository.CreditTransferRepository;

@Component
public class DebitReversalRequestProcessor implements Processor{

	@Autowired private CorebankTransactionRepository cbRepo;
	@Autowired private CreditTransferRepository ctRepo;

	@Override
	public void process(Exchange exchange) throws Exception {
		ProcessDataPojo processData = exchange.getProperty("prop_process_data", ProcessDataPojo.class);
		FlatPacs008Pojo biReq = (FlatPacs008Pojo) processData.getBiRequestFlat();
		
		List<CreditTransfer> lct = ctRepo.findAllByEndToEndId(biReq.getOrgnlEndToEndId());

		String strCbRequest = null;
		DebitReversalRequest reversalReq = new DebitReversalRequest();

		if (lct.size()>0) { 
			CreditTransfer ct = lct.get(0);
		
			List<CorebankTransaction> lcb = cbRepo.findByTransactionTypeAndKomiTrnsId("Debit", ct.getKomiTrnsId());
			CorebankTransaction cb = null;
			if (lcb.size()>0) cb = lcb.get(0);
			strCbRequest = cb.getFullTextRequest();

		    ObjectMapper mapper = new ObjectMapper();
		    reversalReq = mapper.readValue(strCbRequest, DebitReversalRequest.class);
		}
		
		String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
		reversalReq.setDateTime(dateTime);

		processData.setCorebankRequest(reversalReq);
		exchange.getMessage().setBody(reversalReq);
	}

}
