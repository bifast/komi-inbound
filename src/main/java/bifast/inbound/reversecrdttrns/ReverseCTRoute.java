package bifast.inbound.reversecrdttrns;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.accountenquiry.IsoAERequestPrc;
import bifast.inbound.reversecrdttrns.processor.CheckDebitHistoryProcessor;
import bifast.inbound.reversecrdttrns.processor.RevCTAcceptedProcessor;
import bifast.inbound.reversecrdttrns.processor.RevCTRejectResponseProcessor;
import bifast.inbound.reversecrdttrns.processor.SaveRevCTProc;

@Component
public class ReverseCTRoute extends RouteBuilder {
	@Autowired private CheckDebitHistoryProcessor checkDebitHistoryProcessor;
	@Autowired private IsoAERequestPrc isoAERequestPrc;
	@Autowired private RevCTAcceptedProcessor rctAccpProcessor;
	@Autowired private RevCTRejectResponseProcessor rejectResponsePrc;
	@Autowired private SaveRevCTProc saveRevCTProc;
//	@Autowired private JacksonDataFormatService jdfService;

	@Override
	public void configure() throws Exception {
//		JacksonDataFormat bmJDF = jdfService.wrapUnwrapRoot(BusinessMessage.class);

		from("direct:reverct").routeId("komi.reversect")
		
			// cari original transaksi 
			.process(checkDebitHistoryProcessor)
			
			.filter().simple("${exchangeProperty.pr_revCTCheckRsl} != 'DataMatch'")
				.log("[${exchangeProperty.msgName}:${exchangeProperty.prop_process_data.endToEndId}] CT asal tidak ketemu atau tidak sesuai.")
				.process(rejectResponsePrc)
				.wireTap("direct:save_rct")
			.end()
			
			.filter(exchangeProperty("pr_revCTCheckRsl").isEqualTo("DataMatch"))

			.process(isoAERequestPrc)
//			.to("direct:isoadpt")
			.to("direct:cb_ae")

			.setProperty("pr_revCTCheckRsl", simple("${body.status}"))
			.filter().simple("${body.status} != 'ACTC'")
				.process(rejectResponsePrc)
				.wireTap("direct:save_rct")
			.end()
		
			.filter(exchangeProperty("pr_revCTCheckRsl").isEqualTo("ACTC"))

			
			.process(rctAccpProcessor)
			.wireTap("direct:save_rct")
		;

		
		from("direct:save_rct").routeId("komi.saverevct")
			.process(saveRevCTProc)
		;
	
	}
}
