package bifast.inbound;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

import bifast.inbound.iso20022.AppHeaderService;
import bifast.inbound.model.ChannelTransaction;
import bifast.inbound.model.CreditTransfer;
import bifast.inbound.repository.ChannelTransactionRepository;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.inbound.service.FlattenIsoMessageService;
import bifast.library.iso20022.custom.BusinessMessage;

@ActiveProfiles("lcl")
@CamelSpringBootTest
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class ReversalCTTest {

	@Autowired FlattenIsoMessageService flatMsgService;
	@Autowired ProducerTemplate producerTemplate;
	@Autowired TestUtilService testUtilService;
	@Autowired AppHeaderService appHeaderService;
	@Autowired private CreditTransferRepository ctRepo;
	@Autowired private ChannelTransactionRepository chnlRepo;

	private String endToEndId = "";

	@Test
    @Order(1)    
	public void revCT() throws Exception {
		// init sample data
		endToEndId = "20220729SIHBIDJ1010O0281261002";
		sampleDataInit();
		List<CreditTransfer> lct = ctRepo.findAllByEndToEndId(endToEndId);
		Assertions.assertEquals(1, lct.size());

		String strRev = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220726FASTIDJA011H9920322602\",\"MsgDefIdr\":\"pacs.008.001.08\",\"CreDt\":\"2022-07-25T20:58:05Z\"},\"Document\":{\"FIToFICstmrCdtTrf\":{\"GrpHdr\":{\"CreDtTm\":\"2022-07-26T03:58:04.023\",\"MsgId\":\"20220726BMRIIDJA0111127615611\",\"NbOfTxs\":\"1\",\"SttlmInf\":{\"SttlmMtd\":\"CLRG\"}},\"CdtTrfTxInf\":[{\"SplmtryData\":[{\"Envlp\":{\"Dtl\":{\"Dbtr\":{\"Tp\":\"01\"},\"RltdEndToEndId\":\"" + endToEndId + "\"}}}],\"PmtId\":{\"EndToEndId\":\"20220726BMRIIDJA011O9900379830\",\"TxId\":\"20220726SIHBIDJ1010O0214234343\",\"ClrSysRef\":\"001\"},\"PmtTpInf\":{\"LclInstrm\":{\"Prtry\":\"99\"},\"CtgyPurp\":{\"Prtry\":\"01199\"}},\"IntrBkSttlmDt\":\"2022-07-26\",\"ChrgBr\":\"DEBT\",\"Dbtr\":{\"Nm\":\"SULISTIYOWATI\",\"Id\":{\"PrvtId\":{\"Othr\":[{\"Id\":\"3674064910600006\"}]}}},\"DbtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"112211333\"}},\"Tp\":{\"Prtry\":\"SVGS\"}},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"BMRIIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{\"Nm\":\"SULISTIYOWATI\"},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"3602103332330\"}},\"Tp\":{\"Prtry\":\"SVGS\"}},\"RmtInf\":{\"Ustrd\":[\"0\"]},\"IntrBkSttlmAmt\":{\"Value\":100000.00,\"Ccy\":\"IDR\"}}]}}}}";
		Object ret = producerTemplate.sendBody("direct:receive", ExchangePattern.InOut, strRev);
		BusinessMessage bm = testUtilService.deSerializeBusinessMessage((String) ret);
		Assertions.assertEquals("ACTC", bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts());
			
	}

//	@Test
    @Order(4)    
	public void revCTReject() throws Exception {
		// init sample data
		endToEndId = "20220729SIHBIDJ1010O0281261003";
		sampleDataInit();
		List<CreditTransfer> lct = ctRepo.findAllByEndToEndId(endToEndId);
		Assertions.assertEquals(1, lct.size());

		String strRev = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220726FASTIDJA011H9920322602\",\"MsgDefIdr\":\"pacs.008.001.08\",\"CreDt\":\"2022-07-25T20:58:05Z\"},\"Document\":{\"FIToFICstmrCdtTrf\":{\"GrpHdr\":{\"CreDtTm\":\"2022-07-26T03:58:04.023\",\"MsgId\":\"20220726BMRIIDJA0111127615611\",\"NbOfTxs\":\"1\",\"SttlmInf\":{\"SttlmMtd\":\"CLRG\"}},\"CdtTrfTxInf\":[{\"SplmtryData\":[{\"Envlp\":{\"Dtl\":{\"Dbtr\":{\"Tp\":\"01\"},\"RltdEndToEndId\":\"" + endToEndId + "\"}}}],\"PmtId\":{\"EndToEndId\":\"20220726BMRIIDJA011O9900379830\",\"TxId\":\"20220726SIHBIDJ1010O0214234343\",\"ClrSysRef\":\"001\"},\"PmtTpInf\":{\"LclInstrm\":{\"Prtry\":\"99\"},\"CtgyPurp\":{\"Prtry\":\"01199\"}},\"IntrBkSttlmDt\":\"2022-07-26\",\"ChrgBr\":\"DEBT\",\"Dbtr\":{\"Nm\":\"SULISTIYOWATI\",\"Id\":{\"PrvtId\":{\"Othr\":[{\"Id\":\"3674064910600006\"}]}}},\"DbtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"112211333\"}},\"Tp\":{\"Prtry\":\"SVGS\"}},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"BMRIIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{\"Nm\":\"SULISTIYOWATI\"},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"3602103332330\"}},\"Tp\":{\"Prtry\":\"SVGS\"}},\"RmtInf\":{\"Ustrd\":[\"0\"]},\"IntrBkSttlmAmt\":{\"Value\":90000.00,\"Ccy\":\"IDR\"}}]}}}}";
		Object ret = producerTemplate.sendBody("direct:receive", ExchangePattern.InOut, strRev);
		BusinessMessage bm = testUtilService.deSerializeBusinessMessage((String) ret);
		Assertions.assertEquals("RJCT", bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts());
		Assertions.assertEquals("62", bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getStsRsnInf().get(0).getRsn().getPrtry());
	}

	private void sampleDataInit () {
		ChannelTransaction chnlTrns = chnlRepo.findById("20220729O81261002").orElse(new ChannelTransaction());
		chnlTrns.setAmount(new BigDecimal(100000));
		chnlTrns.setCallStatus("SUCCESS");
		chnlTrns.setChannelId("MB");
		chnlTrns.setChannelRefId("MB22004050506");
		chnlTrns.setElapsedTime(Long.valueOf(300));
		chnlTrns.setKomiTrnsId("20220729O81261002");
		chnlTrns.setMsgName("CTReq");
		chnlTrns.setRecptBank("BMRIIDJA");
		chnlTrns.setRequestTime(LocalDateTime.now());
		chnlTrns.setResponseCode("ACTC");
		chnlTrns.setTextMessage("{\"CreditTransferRequest\":{\"NoRef\":\"MB22004050506\",\"TerminalId\":\"MOBILE0001\",\"CategoryPurpose\":\"01\",\"DebtorName\":\"ROBY MEDIKA\",\"DebtorType\":\"01\",\"DebtorId\":\"3175024712440003\",\"DebtorAccountNumber\":\"3602103332330\",\"DebtorAccountType\":\"SVGS\",\"DebtorResidentialStatus\":\"01\",\"DebtorTownName\":\"0395\",\"Amount\":\"100000.00\",\"FeeTransfer\":\"2500.00\",\"RecipientBank\":\"BMRIIDJA\",\"CreditorName\":\"\",\"CreditorType\":\"01\",\"CreditorId\":\"25666057\",\"CreditorAccountNumber\":\"112211333\",\"CreditorAccountType\":\"CACC\",\"CreditorResidentialStatus\":\"01\",\"CreditorTownName\":\"0300\",\"CreditorProxyId\":\"\",\"CreditorProxyType\":\"\",\"PaymentInformation\":\"PRODIS MESSAGE ERROR REVERSAL\"}}");
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
		ct.setKomiTrnsId("20220729O81261002");
		ct.setMsgType("Credit Transfer");
		ct.setOriginatingBank("SIHBIDJ1");
		ct.setRecipientBank("BMRIIDJA");
		ct.setResponseCode("ACTC");
		ct.setReasonCode("U000");
		ct.setSettlementConfBizMsgIdr("RECEIVED");
		ctRepo.save(ct);
	}

}
