package bifast.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import bifast.inbound.repository.CorebankTransactionRepository;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.library.iso20022.custom.BusinessMessage;

@ActiveProfiles("lcl")
@CamelSpringBootTest
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest 
public class PaymentNormalTest {

	@Autowired ProducerTemplate producerTemplate;
	@Autowired TestUtilService testUtilService;
	@Autowired private CreditTransferRepository ctRepo;
	@Autowired private CorebankTransactionRepository cbRepo;

	static final BusinessMessage ctReq = new BusinessMessage();
	private static String endToEndId = "20220812BMRIIDJA010O0225400000";

	@Test
    @Order(2)    
	public void postCT() throws Exception {

		String strCTReq = initSampleData1();
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
		String strSettl = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220812FASTIDJA010H9979318950\",\"MsgDefIdr\":\"pacs.002.001.10\",\"BizSvc\":\"STTL\",\"CreDt\":\"2022-08-11T17:05:38Z\"},\"Document\":{\"FIToFIPmtStsRpt\":{\"GrpHdr\":{\"MsgId\":\"20220812SIHBIDJ10101342655968\",\"CreDtTm\":\"2022-08-12T00:05:38.695\"},\"OrgnlGrpInfAndSts\":[{\"OrgnlMsgId\":\"20220812BMRIIDJA01025406656\",\"OrgnlMsgNmId\":\"pacs.008.001.08\"}],\"TxInfAndSts\":[{\"OrgnlEndToEndId\":\"" + endToEndId + "\",\"OrgnlTxId\":\"20220812BMRIIDJA0101342655946\",\"TxSts\":\"ACSC\",\"StsRsnInf\":[{\"Rsn\":{\"Prtry\":\"U000\"}}],\"ClrSysRef\":\"001\",\"OrgnlTxRef\":{\"IntrBkSttlmDt\":\"2022-08-12\",\"Dbtr\":{\"Pty\":{\"Nm\":\"RENALDI DAYANUN\"}},\"DbtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"1510010707591\"}}},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"BMRIIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{\"Pty\":{\"Nm\":\"RENALDI DAYANUN\"}},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"4612808586016\"}}}},\"SplmtryData\":[{\"Envlp\":{\"Dtl\":{\"DbtrAgtAcct\":{\"Id\":{\"Othr\":{\"Id\":\"520008000980\"}}},\"CdtrAgtAcct\":{\"Id\":{\"Othr\":{\"Id\":\"523564000980\"}}}}}}]}]}}}}";

		producerTemplate.sendBody("direct:receive", strSettl);

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
		assertEquals("DONE", ct2.getCbStatus());

		List<CorebankTransaction> lcb = cbRepo.findByTransactionTypeAndKomiTrnsId("Credit", ct2.getKomiTrnsId());
		assertEquals(1, lcb.size());
		cbRepo.delete(lcb.get(0));
		
	}
	
    private String initSampleData1() {
		
		List<CreditTransfer> lct = ctRepo.findAllByEndToEndId(endToEndId);
		for (CreditTransfer ct : lct) ctRepo.delete(ct);
		
		String str = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220812FASTIDJA010H9997528158\",\"MsgDefIdr\":\"pacs.008.001.08\",\"CreDt\":\"2022-08-11T17:05:37Z\"},\"Document\":{\"FIToFICstmrCdtTrf\":{\"GrpHdr\":{\"CreDtTm\":\"2022-08-12T00:05:31.836\",\"MsgId\":\"20220812BMRIIDJA0101342655946\",\"NbOfTxs\":\"1\",\"SttlmInf\":{\"SttlmMtd\":\"CLRG\"}},\"CdtTrfTxInf\":[{\"PmtId\":{\"EndToEndId\":\"" + endToEndId + "\",\"TxId\":\"20220812BMRIIDJA0101342655946\",\"ClrSysRef\":\"001\"},\"PmtTpInf\":{\"LclInstrm\":{\"Prtry\":\"02\"},\"CtgyPurp\":{\"Prtry\":\"01099\"}},\"IntrBkSttlmDt\":\"2022-08-12\",\"ChrgBr\":\"DEBT\",\"Dbtr\":{\"Nm\":\"RENALDI DAYANUN\",\"Id\":{\"PrvtId\":{\"Othr\":[{\"Id\":\"7201110410000001\"}]}}},\"DbtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"1510010707591\"}},\"Tp\":{\"Prtry\":\"CACC\"}},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"BMRIIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{\"Nm\":\"RENALDI DAYANUN\"},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"4612808586016\"}},\"Tp\":{\"Prtry\":\"CACC\"}},\"RmtInf\":{\"Ustrd\":[\"Lainnya\"]},\"IntrBkSttlmAmt\":{\"Value\":37500.00,\"Ccy\":\"IDR\"}}]}}}}";
		return str;
    }
	

}
