package bifast.inbound;

import static org.junit.jupiter.api.Assertions.*;


import org.apache.camel.EndpointInject;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import bifast.library.iso20022.custom.BusinessMessage;

@ActiveProfiles("lcl")
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest 
@MockEndpoints("direct:*")
public class AccountEnquiryTest {

//	@Autowired FlattenIsoMessageService flatMsgService;
	@Autowired ProducerTemplate template;
	@Autowired TestUtilService utilService;

	@EndpointInject(value = "mock:direct:cb_ae")
	MockEndpoint mockae;
	
//	@EndpointInject(value = "mock://http://localhost:9006/mock/adapter/accountinquiry")
//	@EndpointInject(value = "mock://komi.url.isoadapter.accountinquiry")
//	MockEndpoint mockaeurl;

	@Test
	public void postAE() throws Exception {
		String strAEReq = "{\"BusMsg\":{\"AppHdr\":{\"Fr\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"FASTIDJA\"}}}},\"To\":{\"FIId\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}}},\"BizMsgIdr\":\"20220806FASTIDJA510H9972006258\",\"MsgDefIdr\":\"pacs.008.001.08\",\"CreDt\":\"2022-08-05T17:05:04Z\"},\"Document\":{\"FIToFICstmrCdtTrf\":{\"GrpHdr\":{\"CreDtTm\":\"2022-08-06T00:05:03.979\",\"MsgId\":\"20220806BMRIIDJA5101271696568\",\"NbOfTxs\":\"1\",\"SttlmInf\":{\"SttlmMtd\":\"CLRG\"}},\"CdtTrfTxInf\":[{\"PmtId\":{\"EndToEndId\":\"20220806BMRIIDJA510O0220538096\",\"TxId\":\"20220806BMRIIDJA5101271696568\",\"ClrSysRef\":\"001\"},\"PmtTpInf\":{\"CtgyPurp\":{\"Prtry\":\"51099\"}},\"IntrBkSttlmDt\":\"2022-08-06\",\"ChrgBr\":\"DEBT\",\"Dbtr\":{},\"DbtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"BMRIIDJA\"}}},\"CdtrAgt\":{\"FinInstnId\":{\"Othr\":{\"Id\":\"SIHBIDJ1\"}}},\"Cdtr\":{},\"CdtrAcct\":{\"Id\":{\"Othr\":{\"Id\":\"2782807801222\"}}},\"IntrBkSttlmAmt\":{\"Value\":1510000.00,\"Ccy\":\"IDR\"}}]}}}}";

//		mockaeurl.expectedMessageCount(1);
//		mockaeurl.allMessages().body().isInstanceOf(AccountEnquiryRequest.class);
//		mockae.expectedBodyReceived().simple("${body.class} endsWith 'AccountEnquiryRequest' ");
		
		Object obj = template.sendBody("direct:receive", ExchangePattern.InOut, strAEReq);
		assertNotNull(obj);

		assertInstanceOf(String.class, obj);
		assertDoesNotThrow(() -> {
			utilService.deSerializeBusinessMessage((String) obj);
		});
		
		BusinessMessage bm = utilService.deSerializeBusinessMessage((String) obj);
		
		assertEquals("ACTC", bm.getDocument().getFiToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts());

//		mockaeurl.assertIsSatisfied();
		
	}

}
