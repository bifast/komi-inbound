package bifast.inbound;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
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
import bifast.inbound.model.ChannelTransaction;
import bifast.inbound.model.CorebankTransaction;
import bifast.inbound.model.CreditTransfer;
import bifast.inbound.repository.ChannelTransactionRepository;
import bifast.inbound.repository.CorebankTransactionRepository;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.inbound.service.FlattenIsoMessageService;
import bifast.library.iso20022.custom.BusinessMessage;

@ActiveProfiles("lcl")
@CamelSpringBootTest
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
//@MockEndpoints("direct:isoadpt-credit")
public class ReversalCTTest {

	@Autowired FlattenIsoMessageService flatMsgService;
	@Autowired ProducerTemplate producerTemplate;
	@Autowired TestUtilService testUtilService;
	@Autowired AppHeaderService appHeaderService;
	@Autowired private CreditTransferRepository ctRepo;
	@Autowired private ChannelTransactionRepository chnlRepo;
	@Autowired private CorebankTransactionRepository cbRepo;

//	@EndpointInject("mock:direct:isoadpt-credit")
//	MockEndpoint mockCredit;
	
	private String endToEndId = "";

	@Test
    @Order(1)    
	public void revCT() throws Exception {
		// init sample data
		sampleData1();

		String strRev = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220726FASTIDJA011H9920322602\",\"MsgDefIdr\":\"pacs.008.001.08\",\"CreDt\":\"2022-07-25T20:58:05Z\"},\"Document\":{\"FIToFICstmrCdtTrf\":{\"GrpHdr\":{\"CreDtTm\":\"2022-07-26T03:58:04.023\",\"MsgId\":\"20220726CENAIDJA0111127615611\",\"NbOfTxs\":\"1\",\"SttlmInf\":{\"SttlmMtd\":\"CLRG\"}},\"CdtTrfTxInf\":[{\"SplmtryData\":[{\"Envlp\":{\"Dtl\":{\"Dbtr\":{\"Tp\":\"01\"},\"RltdEndToEndId\":\"" + endToEndId + "\"}}}],\"PmtId\":{\"EndToEndId\":\"20220726CENAIDJA011O9900379830\",\"TxId\":\"20220726SIHBIDJ1010O0214234343\",\"ClrSysRef\":\"001\"},\"PmtTpInf\":{\"LclInstrm\":{\"Prtry\":\"99\"},\"CtgyPurp\":{\"Prtry\":\"01199\"}},\"IntrBkSttlmDt\":\"2022-07-26\",\"ChrgBr\":\"DEBT\",\"Dbtr\":{\"Nm\":\"SULISTIYOWATI\",\"Id\":{\"PrvtId\":{\"Othr\":[{\"Id\":\"3674064910600006\"}]}}},\"DbtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"112211333\"}},\"Tp\":{\"Prtry\":\"SVGS\"}},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"CENAIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{\"Nm\":\"SULISTIYOWATI\"},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"2012102710197\"}},\"Tp\":{\"Prtry\":\"SVGS\"}},\"RmtInf\":{\"Ustrd\":[\"0\"]},\"IntrBkSttlmAmt\":{\"Value\":80000.00,\"Ccy\":\"IDR\"}}]}}}}";
		Object ret = producerTemplate.sendBody("direct:receive", ExchangePattern.InOut, strRev);
		BusinessMessage bm = testUtilService.deSerializeBusinessMessage((String) ret);
		assertEquals("ACTC", bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts());
			
//		mockCredit.expectedMessageCount(1);
//		
		String sttl = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJI\"}}}},\"BizMsgIdr\":\"20220812FASTIDJA010R0200000810\",\"MsgDefIdr\":\"pacs.002.001.10\",\"BizSvc\":\"STTL\",\"CreDt\":\"2022-08-12T10:24:38Z\"},\"Document\":{\"FIToFIPmtStsRpt\":{\"GrpHdr\":{\"MsgId\":\"20220812FASTIDJA01000000832\",\"CreDtTm\":\"2022-08-12T17:24:38.867\"},\"OrgnlGrpInfAndSts\":[{\"OrgnlMsgId\":\"20220812SIHBIDJ101060708001\",\"OrgnlMsgNmId\":\"pacs.008.001.08\"}],\"TxInfAndSts\":[{\"OrgnlEndToEndId\":\"20220726CENAIDJA011O9900379830\",\"OrgnlTxId\":\"20220812SIHBIDJ101060708001\",\"TxSts\":\"ACSC\",\"StsRsnInf\":[{\"Rsn\":{\"Prtry\":\"U000\"}}],\"ClrSysRef\":\"001\",\"OrgnlTxRef\":{\"Dbtr\":{\"Pty\":{\"Nm\":\"ANDRIAN S\"}},\"DbtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"112211333\"}},\"Tp\":{\"Prtry\":\"CACC\"}},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"CENAIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{\"Pty\":{\"Nm\":\"SULISTIYOWATI\"}},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"2012102710197\"}}}},\"SplmtryData\":[{\"Envlp\":{\"Dtl\":{\"DbtrAgtAcct\":{\"Id\":{\"Othr\":{\"Id\":\"01234567\"}}},\"CdtrAgtAcct\":{\"Id\":{\"Othr\":{\"Id\":\"567890\"}}}}}}]}]}}}}";

		Object retSttl = producerTemplate.sendBody("direct:receive", ExchangePattern.InOut, sttl);

		List<CreditTransfer> lct2 = ctRepo.findAllByEndToEndId("20220726CENAIDJA011O9900379830");
		assertEquals(1, lct2.size());
		
		int ctr = 0;
		boolean found = false;
		System.out.println("Akan looping");

		CreditTransfer ct = null;
		while (!found && ctr < 20) {
			ctr = ctr+1;
			TimeUnit.SECONDS.sleep(3);
			ct = ctRepo.findById(lct2.get(0).getId()).orElse(null);
			if (ct.getCbStatus().equals("DONE"))  found = true;
		}
		assertEquals("SUCCESS", ct.getCallStatus());
		assertEquals("RECEIVED", ct.getSettlementConfBizMsgIdr());
		assertEquals("DONE", ct.getCbStatus());
		
//		List<Exchange> lEx = mockCredit.getReceivedExchanges();
//		CreditRequest terima = (CreditRequest) lEx.get(0).getMessage().getBody();
//		Assertions.assertEquals("100000.00", terima.getAmount());
//		Assertions.assertEquals("2500.00", terima.getFeeTransfer());

		List<CorebankTransaction> lcb = cbRepo.findByTransactionTypeAndKomiTrnsId("DebitReversal", ct.getKomiTrnsId());
		assertEquals(1, lcb.size());
		
//		mockCredit.assertIsSatisfied();
	}

	@Test
    @Order(2)    
	public void revCTReject() throws Exception {
		// init sample data
		sampleData2();
		List<CreditTransfer> lct = ctRepo.findAllByEndToEndId(endToEndId);
		Assertions.assertEquals(1, lct.size());

		String strRev = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220726FASTIDJA011H9920322602\",\"MsgDefIdr\":\"pacs.008.001.08\",\"CreDt\":\"2022-07-25T20:58:05Z\"},\"Document\":{\"FIToFICstmrCdtTrf\":{\"GrpHdr\":{\"CreDtTm\":\"2022-07-26T03:58:04.023\",\"MsgId\":\"20220726BMRIIDJA0111127615611\",\"NbOfTxs\":\"1\",\"SttlmInf\":{\"SttlmMtd\":\"CLRG\"}},\"CdtTrfTxInf\":[{\"SplmtryData\":[{\"Envlp\":{\"Dtl\":{\"Dbtr\":{\"Tp\":\"01\"},\"RltdEndToEndId\":\"" + endToEndId + "\"}}}],\"PmtId\":{\"EndToEndId\":\"20220726BMRIIDJA011O9900379830\",\"TxId\":\"20220726SIHBIDJ1010O0214234343\",\"ClrSysRef\":\"001\"},\"PmtTpInf\":{\"LclInstrm\":{\"Prtry\":\"99\"},\"CtgyPurp\":{\"Prtry\":\"01199\"}},\"IntrBkSttlmDt\":\"2022-07-26\",\"ChrgBr\":\"DEBT\",\"Dbtr\":{\"Nm\":\"SULISTIYOWATI\",\"Id\":{\"PrvtId\":{\"Othr\":[{\"Id\":\"3674064910600006\"}]}}},\"DbtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"112211333\"}},\"Tp\":{\"Prtry\":\"SVGS\"}},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"BMRIIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{\"Nm\":\"SULISTIYOWATI\"},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"3602103332330\"}},\"Tp\":{\"Prtry\":\"SVGS\"}},\"RmtInf\":{\"Ustrd\":[\"0\"]},\"IntrBkSttlmAmt\":{\"Value\":90000.00,\"Ccy\":\"IDR\"}}]}}}}";
		Object ret = producerTemplate.sendBody("direct:receive", ExchangePattern.InOut, strRev);
		BusinessMessage bm = testUtilService.deSerializeBusinessMessage((String) ret);
		Assertions.assertEquals("RJCT", bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts());
		Assertions.assertEquals("62", bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getStsRsnInf().get(0).getRsn().getPrtry());
	}

	private void sampleData1 () {
		endToEndId = "20220812SIHBIDJ1010O0260708001";
		String komiId = "20220812O60708001";
		
		ChannelTransaction chnlTrns = chnlRepo.findById(komiId).orElse(new ChannelTransaction());
		chnlTrns.setAmount(new BigDecimal(80000));
		chnlTrns.setCallStatus("SUCCESS");
		chnlTrns.setChannelId("MB");
		chnlTrns.setChannelRefId("TES220726035711682");
		chnlTrns.setElapsedTime(Long.valueOf(5301));
		chnlTrns.setKomiTrnsId(komiId);
		chnlTrns.setMsgName("CTReq");
		chnlTrns.setRecptBank("CENAIDJA");
		chnlTrns.setRequestTime(LocalDateTime.now());
		chnlTrns.setResponseCode("ACTC");
		
		chnlTrns.setTextMessage("{\"CreditTransferRequest\":{\"NoRef\":\"TES220726035711682\",\"TerminalId\":\"MOBILE0001\",\"RecipientBank\":\"CENAIDJA\",\"CategoryPurpose\":\"02\",\"Amount\":\"80000.00\",\"FeeTransfer\":\"2500.00\",\"DebtorName\":\"SULISTIYOWATI\",\"DebtorAccountNumber\":\"2012102710197\",\"DebtorAccountType\":\"SVGS\",\"DebtorId\":\"1002222\",\"DebtorType\":\"01\",\"CreditorName\":\"ANDRIAN S\",\"CreditorAccountNumber\":\"112211333\",\"CreditorAccountType\":\"CACC\",\"PaymentInformation\":\"\"}}");
		chnlRepo.save(chnlTrns);
		
		List<CreditTransfer> lct = ctRepo.findAllByEndToEndId(endToEndId);
		for (CreditTransfer ct : lct ) ctRepo.delete(ct);
		List<CreditTransfer> lct2 = ctRepo.findAllByEndToEndId("20220726CENAIDJA011O9900379830");
		for (CreditTransfer ct : lct2 ) ctRepo.delete(ct);
		
		CreditTransfer ct = new CreditTransfer();
		
		ct.setAmount(new BigDecimal(80000));
		ct.setCallStatus("SUCCESS");
		ct.setCbStatus("DONE");
		ct.setCihubElapsedTime((long) 5072);
		ct.setCihubRequestDT(LocalDateTime.now());
		ct.setCrdtTrnRequestBizMsgIdr("20220812SIHBIDJ1010O0260708001");
		ct.setCrdtTrnResponseBizMsgIdr("20220812FASTIDJA010R0200000705");
		ct.setCreateDt(LocalDateTime.now());
		ct.setCreditorAccountNumber("112211333");
		ct.setCreditorAccountType("CACC");
		ct.setDebtorAccountNumber("2012102710197");
		ct.setDebtorAccountType("SVGS");
		ct.setEndToEndId(endToEndId);
		ct.setKomiTrnsId(komiId);
		ct.setLastUpdateDt(LocalDateTime.now());
		ct.setMsgType("Credit Transfer");
		ct.setOriginatingBank("SIHBIDJ1");
		ct.setPsCounter(0);
		ct.setRecipientBank("CENAIDJA");
		ct.setResponseCode("ACTC");
		ct.setReasonCode("U000");
		ct.setSettlementConfBizMsgIdr("RECEIVED");

		ct.setFullRequestMessage("eJyVU9tO4zAQ/RXkZ6jG7kLTvjlxW4xKWjWGFVR9KOkFtEkaOS6CrfrvO7YTRFcgwFIu9sycOXNm\r\n"
				+ "vCfhrrquNqS3J7wsL5fa/g38W8ql+z4VsqhM4Xdj8+isdkcSeRlKcUXJAdcpUdtvxg14ojCO13Hh\r\n"
				+ "01/kIG1ywoAxCChroIHCGNgFdCAAoOSUoKdYrb1zuUirFkCAD21BgNZIr4SpYc4gOKNMQbd3Tnu/\r\n"
				+ "uvcEU4ltustXhfFE1XYgo8rkOloapdf2cKgbFRyljxm9o+MSqvw4Jb3wKVvnFNAnfhiv1UuFPjYi\r\n"
				+ "MSbLZeGyuf9rY9NEo+mQWDU8F/XiXGZ7MsmNl7BfLNUWX5+xOtYJAb5ij8kQXJU1mVGa2Ybp3G4m\r\n"
				+ "2uhXjAdm3SKzeZ3sdHlkoWCNaJWF0eEfVwvPnbS3i2y3Qh9MA4DdsUKlNkiKqcN71JvQdlD0Q4VG\r\n"
				+ "8WCc5rEVMrkZSZyPu/FvriQaffUT/Wzej9KsniXLAhc5zN0sWSSepqaZtf8mjwFlFFgHdeh2HHl1\r\n"
				+ "VFRyO0zIG87G/OACuM59Iybqx7wZfh/zVjmPxVTy+CQhDdrnlVDKGKXtdvuDKiIeRe54irPjm3uD\r\n"
				+ "jcWwGQEyR0NSZjn6ioVZOCn7xXPmIITJ3KduiMUl4C/43El8+AcPWTcF\r\n"
				+ "");
		
		ct.setFullResponseMsg("eJyNkm9rwjAQxr+K3GuVS4ba+S5anRmbStu92fCF2NbJ2ljaKDrxu++S2DHEsQXay5/L/R6eywkG\r\n"
				+ "u+q5WkP/BKIoJnFpZmP3lzK2caOkqrRyq5l+t6dmBWMRRtJ/FHCm0YRo+897oZwM6B673BtsPkmD\r\n"
				+ "NHDgyDl6jNelkWGAHM3oYQeaQJl+krrkYrmq2oicPtZmCLZUuF/R0fBpJALaGJaJry91W+i1GI/w\r\n"
				+ "vt9h/Q6+ArH97WqXJ0o75dF2LOe5DnUVFHbroaxNsQpvCzSD9VinxkU55TV+EFnXEdt3nBvorFyr\r\n"
				+ "jEpLlQoVEw36bye3e42prSJMlxzwiATN79RpbpMvPnjWB/TgvKBmHG5UH6k42tLvN8QM+TWF6vyt\r\n"
				+ "JzpYDIhhNKSl8a+i7qeWTFNj4LzU5ZFyXsgtY4KIY525HEBYGM0XYJCk9qkoXQ4+Qq2z/KqFxuhY\r\n"
				+ "27bM9dGEqbFcTP1AimkjBPuswiLLCekv9dLqGKl9VphkX2cm1CWCKlba6Uf3JEmMCV+aZeS9\r\n"
				+ "");
		
		ctRepo.save(ct);
		
		List<CorebankTransaction> lcb = cbRepo.findByTransactionTypeAndKomiTrnsId("Debit", komiId);
		for (CorebankTransaction cb : lcb) cbRepo.delete(cb);
		
		CorebankTransaction cb = new CorebankTransaction();
		cb.setCstmAccountName("SULISTIYOWATI");
		cb.setCstmAccountNo("2012102710197");
		cb.setCstmAccountType("SVGS");
		cb.setDateTime("2022-08-12T11:02:20.244");
		cb.setDebitAmount(new BigDecimal(80000));
		cb.setFeeAmount(new BigDecimal(2500));
		cb.setKomiNoref("TES220726035711682");
		cb.setKomiTrnsId(komiId);
		cb.setOrgnlChnlNoref("TES220726035711682");
		cb.setReason("U000");
		cb.setResponse("ACTC");
		cb.setTransactionType("Debit");
		cb.setTrnsDate("20220812");
		cb.setUpdateTime(LocalDateTime.now());
		cb.setFullTextRequest("{\"transactionId\":\"000001\",\"noRef\":\"TES220726035711682\",\"merchantType\":\"6017\",\"terminalId\":\"MOBILE0001\",\"dateTime\":\"2022-08-12T16:51:49.244\",\"originalNoRef\":\"TES220726035711682\",\"originalDateTime\":\"2022-08-12T16:51:49.244\",\"categoryPurpose\":\"02\",\"debtorName\":\"SULISTIYOWATI\",\"debtorType\":\"01\",\"debtorId\":\"1002222\",\"debtorAccountNumber\":\"2012102710197\",\"debtorAccountType\":\"SVGS\",\"amount\":\"80000.00\",\"feeTransfer\":\"2500.00\",\"recipientBank\":\"CENAIDJA\",\"creditorName\":\"ANDRIAN S\",\"creditorAccountNumber\":\"112211333\",\"creditorAccountType\":\"CACC\",\"paymentInformation\":\"\"}");
		cbRepo.save(cb);
	}

	private void sampleData2 () {
		endToEndId = "20220729SIHBIDJ1010O0281261003";
		String komiId = "20220729O81261003";
		ChannelTransaction chnlTrns = chnlRepo.findById(komiId).orElse(new ChannelTransaction());
		chnlTrns.setAmount(new BigDecimal(110000));
		chnlTrns.setCallStatus("SUCCESS");
		chnlTrns.setChannelId("MB");
		chnlTrns.setChannelRefId("MB22004050507");
		chnlTrns.setElapsedTime(Long.valueOf(300));
		chnlTrns.setKomiTrnsId(komiId);
		chnlTrns.setMsgName("CTReq");
		chnlTrns.setRecptBank("BMRIIDJA");
		chnlTrns.setRequestTime(LocalDateTime.now());
		chnlTrns.setResponseCode("RJCT");
		chnlTrns.setTextMessage("{\"CreditTransferRequest\":{\"NoRef\":\"MB22004050507\",\"TerminalId\":\"MOBILE0001\",\"CategoryPurpose\":\"01\",\"DebtorName\":\"ROBY MEDIKA\",\"DebtorType\":\"01\",\"DebtorId\":\"3175024712440003\",\"DebtorAccountNumber\":\"3602103332330\",\"DebtorAccountType\":\"SVGS\",\"DebtorResidentialStatus\":\"01\",\"DebtorTownName\":\"0395\",\"Amount\":\"110000.00\",\"FeeTransfer\":\"2500.00\",\"RecipientBank\":\"BMRIIDJA\",\"CreditorName\":\"\",\"CreditorType\":\"01\",\"CreditorId\":\"25666057\",\"CreditorAccountNumber\":\"112211333\",\"CreditorAccountType\":\"CACC\",\"CreditorResidentialStatus\":\"01\",\"CreditorTownName\":\"0300\",\"CreditorProxyId\":\"\",\"CreditorProxyType\":\"\",\"PaymentInformation\":\"PRODIS MESSAGE ERROR REVERSAL\"}}");
		chnlRepo.save(chnlTrns);
		
		List<CreditTransfer> lct = ctRepo.findAllByEndToEndId(endToEndId);
		for (CreditTransfer ct : lct ) ctRepo.delete(ct);
		
		CreditTransfer ct = new CreditTransfer();
		ct.setAmount(new BigDecimal(100000));
		
		ct.setCallStatus("SUCCESS");
		ct.setCihubElapsedTime(Long.valueOf(1000));
		ct.setCihubRequestDT(LocalDateTime.now());
		ct.setCrdtTrnRequestBizMsgIdr("20220729SIHBIDJ1010O0281261002");
		ct.setCrdtTrnResponseBizMsgIdr("20220729FASTIDJA010R0200001228");
		ct.setCreateDt(LocalDateTime.now());
		ct.setCreditorAccountNumber("112211333");
		ct.setCreditorAccountType("CACC");
		ct.setDebtorAccountNumber("3602103332330");
		ct.setEndToEndId(endToEndId);
		ct.setKomiTrnsId(komiId);
		ct.setMsgType("Credit Transfer");
		ct.setOriginatingBank("SIHBIDJ1");
		ct.setRecipientBank("BMRIIDJA");
		ct.setResponseCode("ACTC");
		ct.setReasonCode("U000");
		ct.setSettlementConfBizMsgIdr("RECEIVED");
		ctRepo.save(ct);
		
		CorebankTransaction cb = new CorebankTransaction();
		cb.setCstmAccountName("SULISTIYOWATI");
		cb.setCstmAccountNo(komiId);
		cb.setCstmAccountType(komiId);
		cb.setDateTime(komiId);
		cb.setDebitAmount(null);
		cb.setFeeAmount(null);
		cb.setFullTextRequest(komiId);
		cb.setKomiNoref(komiId);
		cb.setKomiTrnsId(komiId);
		cb.setOrgnlChnlNoref(komiId);
		cb.setOrgnlDateTime(komiId);
		cb.setReason("U000");
		cb.setResponse("ACTC");
		cb.setTransactionType("Debit");
		cb.setTrnsDate("20220812");
		cb.setUpdateTime(LocalDateTime.now());
	}

}
