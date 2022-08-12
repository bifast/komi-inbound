package bifast.inbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.EndpointInject;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import bifast.inbound.iso20022.AppHeaderService;
import bifast.inbound.isoservice.Pacs008MessageService;
import bifast.inbound.isoservice.Pacs008Seed;
import bifast.inbound.isoservice.SettlementHeaderService;
import bifast.inbound.isoservice.SettlementMessageService;
import bifast.inbound.model.CreditTransfer;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.inbound.service.FlattenIsoMessageService;
import bifast.library.iso20022.custom.BusinessMessage;
import bifast.library.iso20022.custom.Document;
import bifast.library.iso20022.head001.BusinessApplicationHeaderV01;

@ActiveProfiles("lcl")
@CamelSpringBootTest
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest (
	properties = { 
//		"komi.url.isoadapter.accountinquiry=mock://komi.url.isoadapter.accountinquiry" 
//		"komi.url.isoadapter=http://localhost:9006/mock/adapter" 		
	}
)
@MockEndpoints
public class PaymentNormalTest {

	@Autowired FlattenIsoMessageService flatMsgService;
	@Autowired ProducerTemplate producerTemplate;
	@Autowired TestUtilService testUtilService;
	@Autowired AppHeaderService appHeaderService;
	@Autowired private Pacs008MessageService pacs008MessageService;
	@Autowired private CreditTransferRepository ctRepo;
	@Autowired private SettlementHeaderService sttlHeaderService;
	@Autowired private SettlementMessageService sttlBodyService;

//	@EndpointInject(value = "mock:direct:cb_ae")
//	MockEndpoint mockae;
	
	@EndpointInject(value = "mock://http://localhost:9006/mock/adapter/accountinquiry")
//	@EndpointInject(value = "mock://komi.url.isoadapter.accountinquiry")
	MockEndpoint mockaeurl;

//	@EndpointInject(value = "mock:direct:portalnotif")
//	MockEndpoint mockportal;

//    @Configuration
//    static class TestConfig {
//        @Bean
//        RoutesBuilder route() {
//            return new RouteBuilder() {
//                @Override
//                public void configure() throws Exception {
//                    from("mock:komi.url.isoadapter.accountinquiry")
//                		.log("${body}")
//                    	.to("mock:test");
//                }
//            };
//        }
//    }
    
//	@Test
    @Order(1)    
	public void postAE() throws Exception {
		String strAEReq = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220806FASTIDJA510H9972006258\",\"MsgDefIdr\":\"pacs.008.001.08\",\"CreDt\":\"2022-08-05T17:05:04Z\"},\"Document\":{\"FIToFICstmrCdtTrf\":{\"GrpHdr\":{\"CreDtTm\":\"2022-08-06T00:05:03.979\",\"MsgId\":\"20220806BMRIIDJA5101271696568\",\"NbOfTxs\":\"1\",\"SttlmInf\":{\"SttlmMtd\":\"CLRG\"}},\"CdtTrfTxInf\":[{\"PmtId\":{\"EndToEndId\":\"20220806BMRIIDJA510O0220538096\",\"TxId\":\"20220806BMRIIDJA5101271696568\",\"ClrSysRef\":\"001\"},\"PmtTpInf\":{\"CtgyPurp\":{\"Prtry\":\"51099\"}},\"IntrBkSttlmDt\":\"2022-08-06\",\"ChrgBr\":\"DEBT\",\"Dbtr\":{},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"BMRIIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"2782807801222\"}}},\"IntrBkSttlmAmt\":{\"Value\":1510000.00,\"Ccy\":\"IDR\"}}]}}}}";

		mockaeurl.expectedMessageCount(1);
//		mockaeurl.allMessages().body().isInstanceOf(AccountEnquiryRequest.class);
//		mockae.expectedBodyReceived().simple("${body.class} endsWith 'AccountEnquiryRequest' ");
		
		Object ret = producerTemplate.sendBody("direct:receive", ExchangePattern.InOut, strAEReq);
		
		mockaeurl.assertIsSatisfied();
		
	}

	static final BusinessMessage ctReq = new BusinessMessage();
	private static String endToEndId = null;

	@Test
    @Order(2)    
	public void postCT() throws Exception {
		BusinessMessage newCT = buildCTRequest();
		ctReq.setAppHdr(newCT.getAppHdr());
		ctReq.setDocument(newCT.getDocument());
		endToEndId = ctReq.getDocument().getFiToFICstmrCdtTrf().getCdtTrfTxInf().get(0).getPmtId().getEndToEndId();
		
		String strCTReq = testUtilService.serializeBusinessMessage(ctReq);
		Object ret = producerTemplate.sendBody("direct:receive", ExchangePattern.InOut, strCTReq);
		BusinessMessage bm = testUtilService.deSerializeBusinessMessage((String) ret);

		TimeUnit.SECONDS.sleep(1);
		List<CreditTransfer> lCt = ctRepo.findAllByEndToEndId(endToEndId);
		CreditTransfer ct = null;
		if (lCt.size()>0) ct = lCt.get(0);

		Assertions.assertNotNull(bm.getDocument().getFiToFIPmtStsRpt());
		Assertions.assertEquals(bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts(), "ACTC");
		Assertions.assertNotNull(ct);
		Assertions.assertEquals(ct.getCallStatus(), "SUCCESS");
		Assertions.assertEquals(ct.getCbStatus(), "PENDING");
		Assertions.assertEquals(ct.getSettlementConfBizMsgIdr(), "WAITING");
		
	}

	@Test
    @Order(3)    
	public void postSttl() throws Exception {
		String bizMsgId = testUtilService.genRfiBusMsgId("010", "02", "INDOIDJA");
		String msgId = testUtilService.genMessageId("010", "INDOIDJA");

		BusinessMessage settlementConf = new BusinessMessage();

		settlementConf.setAppHdr(sttlHeaderService.getAppHdr("010", bizMsgId));
		settlementConf.setDocument(new Document());
		settlementConf.getDocument().setFiToFIPmtStsRpt(sttlBodyService.SettlementConfirmation(msgId, ctReq));
		String strSettl = testUtilService.serializeBusinessMessage(settlementConf);

		producerTemplate.sendBody("direct:receive", strSettl);

		List<CreditTransfer> lCt = ctRepo.findAllByEndToEndId(endToEndId);
		CreditTransfer ct = null;
		if (lCt.size()>0) ct = lCt.get(0);

		Assertions.assertNotNull(ct);
		Assertions.assertEquals(ct.getSettlementConfBizMsgIdr(), "RECEIVED");
		
		CreditTransfer ct2 = null;
		int ctr = 0;
		boolean found = false;
		while (!found && ctr < 20) {
			ctr = ctr+1;
			TimeUnit.SECONDS.sleep(3);
			ct2 = ctRepo.findById(lCt.get(0).getId()).orElse(null);
			if (ct2.getCbStatus().equals("DONE")) found = true;
		}
		Assertions.assertEquals(ct2.getCbStatus(), "DONE");

	}
	
	
	private BusinessMessage buildCTRequest() throws Exception {
		Pacs008Seed seedCreditTrn = new Pacs008Seed();
		String bizMsgId = testUtilService.genRfiBusMsgId("010", "01", "BMNDIDJA" );
		String msgId = testUtilService.genMessageId("010", "BMNDIDJA");
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
