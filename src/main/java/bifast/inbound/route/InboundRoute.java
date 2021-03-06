package bifast.inbound.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.processor.CheckRequestMsgProc;


@Component
public class InboundRoute extends RouteBuilder {

	@Autowired private CheckRequestMsgProc checkRequestMsgProcessor;
	

	@Override
	public void configure() throws Exception {
		
		from("direct:receive").routeId("komi.inboundRoute")
			.process(checkRequestMsgProcessor) 
		
			.choice().id("komi.dispatcher")

				.when().simple("${exchangeProperty.msgName} == 'Settl'")   // terima settlement
					.to("direct:settlement")
					.setBody(constant(""))

				.when().simple("${exchangeProperty.msgName} == 'PrxNtf'")  
					.to("direct:proxynotif")
					.setBody(constant(""))

				.when().simple("${exchangeProperty.msgName} == 'AccEnq'")   // terima account enquiry
					.to("direct:accountenq")

				.when().simple("${exchangeProperty.msgName} == 'CrdTrn'")    // terima credit transfer
					.to("direct:crdttransfer")
					.setHeader("hdr_toBIobj", simple("${body}"))

				.when().simple("${exchangeProperty.msgName} == 'RevCT'")     // reverse CT
					.to("direct:reverct")
					.setHeader("hdr_toBIobj", simple("${body}"))

				.when().simple("${exchangeProperty.msgName} == 'MsgRjct'")     // reverse CT
					.to("direct:reverct")
					.setBody(constant(""))

				.otherwise()	
					.log("[Inbound] Message ${exchangeProperty.msgName} tidak dikenal")
			.end()
	
			// kirim log notif ke Portal
			.wireTap("direct:portalnotif")
				
		;


	}
}
