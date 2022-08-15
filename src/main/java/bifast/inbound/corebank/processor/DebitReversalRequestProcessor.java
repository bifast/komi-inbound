package bifast.inbound.corebank.processor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import bifast.inbound.corebank.isopojo.DebitReversalRequest;
import bifast.inbound.model.Channel;
import bifast.inbound.model.ChannelTransaction;
import bifast.inbound.model.CreditTransfer;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatPacs008Pojo;
import bifast.inbound.repository.ChannelRepository;
import bifast.inbound.repository.ChannelTransactionRepository;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.inbound.reversecrdttrns.pojo.ChnlCreditTransferRequestPojo;

@Component
public class DebitReversalRequestProcessor implements Processor{

	@Autowired private CreditTransferRepository ctRepo;
	@Autowired private ChannelTransactionRepository channelTrnsRepo;
	@Autowired private ChannelRepository channelRepo;

	@Override
	public void process(Exchange exchange) throws Exception {
		ProcessDataPojo processData = exchange.getProperty("prop_process_data", ProcessDataPojo.class);
		FlatPacs008Pojo biReq = (FlatPacs008Pojo) processData.getBiRequestFlat();
		
		Optional<CreditTransfer> oCrdtTrns = ctRepo.getSuccessByEndToEndId(biReq.getOrgnlEndToEndId());
		
		DebitReversalRequest reversalReq = null;
		if (oCrdtTrns.isPresent())
			reversalReq = getChannelTransaction(oCrdtTrns.get().getKomiTrnsId());		
		
		processData.setCorebankRequest(reversalReq);
		exchange.getMessage().setBody(reversalReq);
	}

	private DebitReversalRequest getChannelTransaction (String komiTrnsId) throws JsonMappingException, 
																				JsonProcessingException {
		DebitReversalRequest reversalReq = new DebitReversalRequest();
		Optional<ChannelTransaction>  oChnlTrns = channelTrnsRepo.findById(komiTrnsId);

		if (oChnlTrns.isPresent()) {
			ChannelTransaction chnReqData = oChnlTrns.get();
			String merchantCode = channelRepo.findById(oChnlTrns.get().getChannelId()).orElse(new Channel()).getMerchantCode();
			String textMsg = chnReqData.getTextMessage();
		    ObjectMapper mapper = new ObjectMapper();
		    mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
		    ChnlCreditTransferRequestPojo chnReq = mapper.readValue(textMsg, ChnlCreditTransferRequestPojo.class);


			reversalReq.setAmount(chnReq.getAmount());
			reversalReq.setCategoryPurpose(chnReq.getCategoryPurpose());
			reversalReq.setCreditorAccountNumber(chnReq.getCrdtAccountNo());
			reversalReq.setCreditorAccountType(chnReq.getCrdtAccountType());
			reversalReq.setCreditorId(chnReq.getCrdtId());
			reversalReq.setCreditorName(chnReq.getCrdtName());
			reversalReq.setCreditorProxyId(chnReq.getCrdtProxyIdValue());
			reversalReq.setCreditorProxyType(chnReq.getCrdtProxyIdType());
			reversalReq.setCreditorResidentStatus(chnReq.getCrdtResidentialStatus());
			reversalReq.setCreditorTownName(chnReq.getCrdtTownName());
			reversalReq.setCreditorType(chnReq.getCrdtType());
			
			reversalReq.setDebtorAccountNumber(chnReq.getDbtrAccountNo());
			reversalReq.setDebtorAccountType(chnReq.getDbtrAccountType());
			reversalReq.setDebtorId(chnReq.getDbtrId());
			reversalReq.setDebtorName(chnReq.getDbtrName());
			reversalReq.setDebtorResidentStatus(chnReq.getDbtrResidentialStatus());
			reversalReq.setDebtorTownName(chnReq.getDbtrTownName());
			reversalReq.setDebtorType(chnReq.getDbtrType());
			reversalReq.setFeeTransfer(chnReq.getFeeTransfer());
			
			reversalReq.setMerchantType(merchantCode);
			reversalReq.setNoRef(chnReq.getChannelRefId());
			
			String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
			reversalReq.setDateTime(dateTime);
			reversalReq.setOriginalDateTime(dateTime);
			
			reversalReq.setOriginalNoRef(chnReq.getChannelRefId());

			reversalReq.setPaymentInformation(chnReq.getPaymentInfo());
			reversalReq.setRecipientBank(chnReq.getRecptBank());
			reversalReq.setTerminalId(chnReq.getTerminalId());
			
			reversalReq.setTransactionId("000001");
			
		}

		return reversalReq;
	}
}
