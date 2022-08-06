package bifast.inbound;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import bifast.inbound.model.CorebankTransaction;
import bifast.inbound.model.CreditTransfer;
import bifast.inbound.model.Settlement;
import bifast.inbound.repository.CorebankTransactionRepository;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.inbound.repository.SettlementRepository;
import bifast.library.iso20022.custom.BusinessMessage;

@ActiveProfiles("lcl")
@CamelSpringBootTest
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class PaymentCreditTimeoutTest {

	@Autowired private CorebankTransactionRepository cbRepo;
	@Autowired private CreditTransferRepository ctRepo;
	@Autowired private ProducerTemplate producerTemplate;
	@Autowired private SettlementRepository sttlRepo;
	@Autowired private TestUtilService testUtilService;

	static final BusinessMessage ctReq = new BusinessMessage();
	private static String endToEndId = null;
	private static String komiRefId = null;
	
//	@Test
    @Order(21)    
	public void postCT() throws Exception {
		String strCTReq = prepareCTData();
		Object ret = producerTemplate.sendBody("direct:receive", ExchangePattern.InOut, strCTReq);
		BusinessMessage bm = testUtilService.deSerializeBusinessMessage((String) ret);

		TimeUnit.SECONDS.sleep(1);
		List<CreditTransfer> lCt = ctRepo.findAllByEndToEndId(endToEndId);
		CreditTransfer ct = null;
		if (lCt.size()>0) ct = lCt.get(0);

		Assertions.assertNotNull(bm.getDocument().getFiToFIPmtStsRpt());
		Assertions.assertEquals(bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts(), "ACTC");
		Assertions.assertNotNull(ct);
		Assertions.assertEquals("SUCCESS", ct.getCallStatus());
		Assertions.assertEquals("PENDING", ct.getCbStatus());
		Assertions.assertEquals("WAITING", ct.getSettlementConfBizMsgIdr());
		
		komiRefId = ct.getKomiTrnsId();
	}

//	@Test
    @Order(22)    
	public void postSttl() throws Exception {
    	String strSettl = prepareSettlementData();
		producerTemplate.sendBody("direct:receive", strSettl);

		TimeUnit.SECONDS.sleep(2);
		List<CreditTransfer> lCt = ctRepo.findAllByEndToEndId(endToEndId);
		CreditTransfer ct = null;
		if (lCt.size()>0) ct = lCt.get(0);

		Assertions.assertNotNull(ct);
		Assertions.assertEquals( "RECEIVED", ct.getSettlementConfBizMsgIdr());
		
		CreditTransfer ct2 = null;
		int ctr = 0;
		boolean found = false;
		while (!found && ctr < 20) {
			ctr = ctr+1;
			TimeUnit.SECONDS.sleep(3);
			ct2 = ctRepo.findById(lCt.get(0).getId()).orElse(null);
			if (ct2.getCbStatus().equals("DONE")) found = true;
		}
		Assertions.assertEquals( "TIMEOUT", ct2.getCbStatus());
		Assertions.assertNull(ct2.getReversal());
		
		List<CorebankTransaction> lcb = cbRepo.findByTransactionTypeAndKomiTrnsId("Credit", ct2.getKomiTrnsId());
		Assertions.assertTrue(lcb.size()>0);
		CorebankTransaction cb = lcb.get(0);
		Assertions.assertEquals("TIMEOUT", cb.getResponse());
		Assertions.assertEquals("U900", cb.getReason());

	}

	private String prepareCTData() throws Exception {
		endToEndId = "20220805BMRIIDJA010O0225001402"; 
		List<CreditTransfer> lct = ctRepo.findAllByEndToEndId(endToEndId);
		for (CreditTransfer ct : lct) ctRepo.delete(ct);
		
		String strCTReq = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220805FASTIDJA010H9966897081\",\"MsgDefIdr\":\"pacs.008.001.08\",\"CreDt\":\"2022-08-04T17:12:46Z\"},\"Document\":{\"FIToFICstmrCdtTrf\":{\"GrpHdr\":{\"CreDtTm\":\"2022-08-05T00:12:29.246\",\"MsgId\":\"20220805BMRIIDJA0101257427293\",\"NbOfTxs\":\"1\",\"SttlmInf\":{\"SttlmMtd\":\"CLRG\"}},\"CdtTrfTxInf\":[{\"PmtId\":{\"EndToEndId\":\"20220805BMRIIDJA010O0225001402\",\"TxId\":\"20220805BMRIIDJA0101257427293\",\"ClrSysRef\":\"001\"},\"PmtTpInf\":{\"LclInstrm\":{\"Prtry\":\"02\"},\"CtgyPurp\":{\"Prtry\":\"01099\"}},\"IntrBkSttlmDt\":\"2022-08-05\",\"ChrgBr\":\"DEBT\",\"Dbtr\":{\"Nm\":\"PUTU KUSALIA PUCANGA\",\"Id\":{\"PrvtId\":{\"Othr\":[{\"Id\":\"5108042103650002\"}]}}},\"DbtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"1450011680796\"}},\"Tp\":{\"Prtry\":\"CACC\"}},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"BMRIIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{\"Nm\":\"PUTU KUSALIA PUCANGAN\"},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"0112109307961\"}},\"Tp\":{\"Prtry\":\"CACC\"}},\"RmtInf\":{\"Ustrd\":[\"credittimeout\"]},\"IntrBkSttlmAmt\":{\"Value\":600000.00,\"Ccy\":\"IDR\"}}]}}}}";
		return strCTReq;
	}
	
	private String prepareSettlementData() throws Exception {
		List<Settlement> lsttl = sttlRepo.findByOrgnlEndToEndId(endToEndId);
		for (Settlement sttl : lsttl) sttlRepo.delete(sttl);
		
		List<CorebankTransaction> lcb = cbRepo.findByTransactionTypeAndKomiTrnsId("Credit", komiRefId);
		for (CorebankTransaction cb : lcb) cbRepo.delete(cb);
		
		String strSettlement = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220805FASTIDJA010H9923703509\",\"MsgDefIdr\":\"pacs.002.001.10\",\"BizSvc\":\"STTL\",\"CreDt\":\"2022-08-04T17:12:47Z\"},\"Document\":{\"FIToFIPmtStsRpt\":{\"GrpHdr\":{\"MsgId\":\"20220805SIHBIDJ10101257427312\",\"CreDtTm\":\"2022-08-05T00:12:47.244\"},\"OrgnlGrpInfAndSts\":[{\"OrgnlMsgId\":\"20220805BMRIIDJA01025001402\",\"OrgnlMsgNmId\":\"pacs.008.001.08\"}],\"TxInfAndSts\":[{\"OrgnlEndToEndId\":\"20220805BMRIIDJA010O0225001402\",\"OrgnlTxId\":\"20220805BMRIIDJA0101257427293\",\"TxSts\":\"ACSC\",\"StsRsnInf\":[{\"Rsn\":{\"Prtry\":\"U000\"}}],\"ClrSysRef\":\"001\",\"OrgnlTxRef\":{\"IntrBkSttlmDt\":\"2022-08-05\",\"Dbtr\":{\"Pty\":{\"Nm\":\"PUTU KUSALIA PUCANGA\"}},\"DbtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"1450011680796\"}}},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"BMRIIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{\"Pty\":{\"Nm\":\"PUTU KUSALIA PUCANGAN\"}},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"0112109307961\"}}}},\"SplmtryData\":[{\"Envlp\":{\"Dtl\":{\"DbtrAgtAcct\":{\"Id\":{\"Othr\":{\"Id\":\"520008000980\"}}},\"CdtrAgtAcct\":{\"Id\":{\"Othr\":{\"Id\":\"523564000980\"}}}}}}]}]}}}}";
		return strSettlement;
	}

}
