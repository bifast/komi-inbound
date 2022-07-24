package bifast.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonProcessingException;

import bifast.inbound.iso20022.AppHeaderService;
import bifast.inbound.isoservice.Pacs008MessageService;
import bifast.inbound.isoservice.Pacs008Seed;
import bifast.inbound.isoservice.SettlementHeaderService;
import bifast.inbound.isoservice.SettlementMessageService;
import bifast.inbound.model.CreditTransfer;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatPacs008Pojo;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.inbound.service.FlattenIsoMessageService;
import bifast.inbound.service.RefUtils;
import bifast.library.iso20022.custom.BusinessMessage;
import bifast.library.iso20022.custom.Document;
import bifast.library.iso20022.head001.BusinessApplicationHeaderV01;

@CamelSpringBootTest
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class PaymentCreditRejectTest {

	@Autowired FlattenIsoMessageService flatMsgService;
	@Autowired ProducerTemplate producerTemplate;
	@Autowired TestUtilService utilService;
	@Autowired AppHeaderService appHeaderService;
	@Autowired private Pacs008MessageService pacs008MessageService;
	@Autowired private CreditTransferRepository ctRepo;
	@Autowired private SettlementHeaderService sttlHeaderService;
	@Autowired private SettlementMessageService sttlBodyService;

	
	private ProcessDataPojo setProcessData (BusinessMessage bm) throws JsonProcessingException {	
		ProcessDataPojo processData = new ProcessDataPojo();
		processData.setTextDataReceived(utilService.serializeBusinessMessage(bm));
		processData.setStartTime(Instant.now());
		processData.setKomiTrnsId(RefUtils.genKomiTrnsId());
		processData.setBiRequestMsg(bm);
		FlatPacs008Pojo flat008 = flatMsgService.flatteningPacs008(bm); 
		processData.setBiRequestFlat(flat008);

		return processData;
	}

	private static String endToEndId = null;

	static final BusinessMessage ctReq = new BusinessMessage();

	@Test
    @Order(4)    
	public void postCT() throws Exception {
		BusinessMessage newCT = buildCTRequest();
		ctReq.setAppHdr(newCT.getAppHdr());
		ctReq.setDocument(newCT.getDocument());
		ProcessDataPojo processData = setProcessData(ctReq);
		processData.setInbMsgName("CrdTrn");
		endToEndId = ctReq.getDocument().getFiToFICstmrCdtTrf().getCdtTrfTxInf().get(0).getPmtId().getEndToEndId();
		
		Exchange ex = new DefaultExchange(producerTemplate.getCamelContext());
		ex.setProperty("prop_process_data", processData);
		ex.setProperty("msgName", "CrdTrn");
		ex = producerTemplate.send("direct:crdttransfer", ex);
		Object ret = ex.getMessage().getBody(Object.class);
		
		TimeUnit.SECONDS.sleep(1);
		List<CreditTransfer> lCt = ctRepo.findAllByEndToEndId(endToEndId);
		CreditTransfer ct = null;
		if (lCt.size()>0) ct = lCt.get(0);

		Assertions.assertInstanceOf(BusinessMessage.class, ret);
		BusinessMessage bm = (BusinessMessage) ret;
		Assertions.assertNotNull(bm.getDocument().getFiToFIPmtStsRpt());
		Assertions.assertEquals(bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts(), "ACTC");
		Assertions.assertNotNull(ct);
		Assertions.assertEquals(ct.getCallStatus(), "SUCCESS");
		Assertions.assertEquals(ct.getCbStatus(), "PENDING");
		Assertions.assertEquals(ct.getSettlementConfBizMsgIdr(), "WAITING");
	}

	@Test
    @Order(5)    
	public void postSttl() throws Exception {
		String bizMsgId = utilService.genRfiBusMsgId("010", "02", "INDOIDJA");
		String msgId = utilService.genMessageId("010", "INDOIDJA");

		BusinessMessage settlementConf = new BusinessMessage();

		settlementConf.setAppHdr(sttlHeaderService.getAppHdr("010", bizMsgId));
		settlementConf.setDocument(new Document());
		settlementConf.getDocument().setFiToFIPmtStsRpt(sttlBodyService.SettlementConfirmation(msgId, ctReq));

		ProcessDataPojo processData = setProcessData(ctReq);
		processData.setInbMsgName("Settl");
		processData.setBiRequestFlat(flatMsgService.flatteningPacs002(settlementConf)); 

		Exchange ex = new DefaultExchange(producerTemplate.getCamelContext());
		ex.setProperty("prop_process_data", processData);
		ex.setProperty("msgName", "Settl");
		ex = producerTemplate.send("direct:settlement", ex);

		List<CreditTransfer> lCt = ctRepo.findAllByEndToEndId(endToEndId);
		CreditTransfer ct = null;
		if (lCt.size()>0) ct = lCt.get(0);

		Assertions.assertNotNull(ct);
		Assertions.assertEquals(ct.getSettlementConfBizMsgIdr(), "RECEIVED");
		
		CreditTransfer ct2 = null;
		int ctr = 0;
		boolean found = false;
		while (!found && ctr < 10) {
			ctr = ctr+1;
			TimeUnit.SECONDS.sleep(5);
			ct2 = ctRepo.findById(lCt.get(0).getId()).orElse(null);
			if (ct2.getCbStatus().equals("DONE")) found = true;
		}
		Assertions.assertEquals(ct2.getCbStatus(), "DONE");

	}
	
	private BusinessMessage buildCTRequest() throws Exception {
		Pacs008Seed seedCreditTrn = new Pacs008Seed();
		String bizMsgId = utilService.genRfiBusMsgId("010", "01", "BMNDIDJA" );
		String msgId = utilService.genMessageId("010", "BMNDIDJA");
		seedCreditTrn.setBizMsgId(bizMsgId);
		seedCreditTrn.setMsgId(msgId);
		seedCreditTrn.setAmount(new BigDecimal(100000));
		seedCreditTrn.setCategoryPurpose("01");
		seedCreditTrn.setChannel("01");
		seedCreditTrn.setCrdtAccountNo("3604107554096");		
		seedCreditTrn.setCrdtAccountType("CACC");
		seedCreditTrn.setCrdtName("Johari");
		seedCreditTrn.setDbtrAccountNo("2001000");
		seedCreditTrn.setDbtrAccountType("SVGS");
		seedCreditTrn.setDbtrName("Antonio");
		seedCreditTrn.setDbtrId("9999333339");
		seedCreditTrn.setDbtrType("01"); 
		seedCreditTrn.setDbtrResidentStatus("01");
		seedCreditTrn.setDbtrTownName("0300");
		seedCreditTrn.setOrignBank("BMNDIDJA");
		seedCreditTrn.setRecptBank("SIHBIDJ1");
		seedCreditTrn.setPaymentInfo("");
		seedCreditTrn.setTrnType("010");
		
		BusinessMessage busMsg = new BusinessMessage();
		BusinessApplicationHeaderV01 hdr = new BusinessApplicationHeaderV01();
		hdr = appHeaderService.getAppHdr("pacs.008.001.08", bizMsgId);
		busMsg.setAppHdr(hdr);
		Document doc = new Document();
		doc.setFiToFICstmrCdtTrf(pacs008MessageService.creditTransferRequest(seedCreditTrn));
		busMsg.setDocument(doc);
		return busMsg;
	}


}
