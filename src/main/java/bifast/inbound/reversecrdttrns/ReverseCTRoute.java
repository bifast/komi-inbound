package bifast.inbound.reversecrdttrns;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.accountenquiry.IsoAERequestPrc;
import bifast.inbound.reversecrdttrns.processor.CheckDebitHistoryProcessor;
import bifast.inbound.reversecrdttrns.processor.RevCTAcceptedProcessor;
import bifast.inbound.reversecrdttrns.processor.RevCTRejectResponseProcessor;
import bifast.inbound.reversecrdttrns.processor.SaveRevCTProc;
import bifast.inbound.service.JacksonDataFormatService;
import bifast.library.iso20022.custom.BusinessMessage;

@Component
public class ReverseCTRoute extends RouteBuilder {
	@Autowired private CheckDebitHistoryProcessor checkDebitHistoryProcessor;
	@Autowired private IsoAERequestPrc isoAERequestPrc;
	@Autowired private RevCTAcceptedProcessor rctAccpProcessor;
	@Autowired private RevCTRejectResponseProcessor rejectResponsePrc;
	@Autowired private SaveRevCTProc saveRevCTProc;
	@Autowired private JacksonDataFormatService jdfService;

	@Override
	public void configure() throws Exception {
		JacksonDataFormat bmJDF = jdfService.wrapUnwrapRoot(BusinessMessage.class);

		from("direct:reverct").routeId("komi.reversect")
		
			// cari original transaksi 
			.process(checkDebitHistoryProcessor)
			
			.filter().simple("${exchangeProperty.pr_revCTCheckRsl} != 'DataMatch'")
				.log("[${exchangeProperty.msgName}:${exchangeProperty.prop_process_data.endToEndId}] CT asal tidak ketemu atau tidak sesuai.")
				.to("direct:rctreject")
				//TODO lapor admin
			.end()
			
			.filter(exchangeProperty("pr_revCTCheckRsl").isEqualTo("DataMatch"))

			.process(isoAERequestPrc)
			.to("direct:isoadpt")

			.setProperty("pr_revCTCheckRsl", simple("${body.status}"))
			.filter().simple("${body.status} != 'ACTC'")
				.to("direct:rctreject")
				//TODO lapor admin
			.end()
		
			.filter(exchangeProperty("pr_revCTCheckRsl").isEqualTo("ACTC"))

			.process(rctAccpProcessor)
			.setHeader("tmpBody", simple("${body}"))
			.marshal(bmJDF).marshal().zipDeflater().marshal().base64()
			.setProperty("prop_toBI_jsonzip", simple("${body}"))
			.setBody(simple("${header.tmpBody}"))

			.to("seda:save_rct?exchangePattern=InOnly")
					
			
		;

		from("direct:rctreject").routeId("komi.rctreject")
			.process(rejectResponsePrc)
			.setHeader("tmpBody", simple("${body}"))
			.marshal(bmJDF).marshal().zipDeflater().marshal().base64()
			.setProperty("prop_toBI_jsonzip", simple("${body}"))
			.setBody(simple("${header.tmpBody}"))
			.to("seda:save_rct?exchangePattern=InOnly")
		;
		
		from("seda:save_rct").routeId("komi.saverevct")
			.setExchangePattern(ExchangePattern.InOnly)
			.process(saveRevCTProc)
		;
	
	}
}
