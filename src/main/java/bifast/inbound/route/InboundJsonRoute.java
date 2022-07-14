package bifast.inbound.route;

import java.time.Instant;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.service.JacksonDataFormatService;
import bifast.inbound.service.RefUtils;
import bifast.library.iso20022.custom.BusinessMessage;


@Component
public class InboundJsonRoute extends RouteBuilder {
	@Autowired private JacksonDataFormatService jdfService;

	@Override
	public void configure() throws Exception {
		JacksonDataFormat jsonBusinessMessageDataFormat = jdfService.wrapUnwrapRoot(BusinessMessage.class);

		restConfiguration().component("servlet");
			
		rest("/json")
			.post("/service").consumes("application/json").to("direct:parsejson")
			.post("/test").consumes("application/json").to("direct:testae")
		;


		from("direct:parsejson").routeId("komi.jsonEndpoint")
			.convertBodyTo(String.class)
			.setHeader("hdr_inputformat", constant("json"))

			.log(LoggingLevel.DEBUG,"komi.jsonEndpoint", "-------****------")
			.log("Terima: ${body}")
			
			.process(new Processor() {
				public void process(Exchange exchange) throws Exception {
					ProcessDataPojo processData = new ProcessDataPojo();
					processData.setTextDataReceived(exchange.getMessage().getBody(String.class));
					processData.setStartTime(Instant.now());
					processData.setKomiTrnsId(RefUtils.genKomiTrnsId());
					exchange.setProperty("prop_process_data", processData);
				}
			})
			
			.unmarshal(jsonBusinessMessageDataFormat)  // ubah ke pojo BusinessMessage
			.to("direct:receive")
			
			.filter().simple("${exchangeProperty.prop_process_data.inbMsgName} !in 'Settl, PrxNtf'")
				.marshal(jsonBusinessMessageDataFormat) 
				.log("[${exchangeProperty.prop_process_data.inbMsgName}:${exchangeProperty.prop_process_data.endToEndId}] Response: ${body}")
				// simpan outbound compress
				.setHeader("hdr_tmp", simple("${body}"))
				.setBody(simple("${header.hdr_tmp}"))
			.end()
				
			.log("[${exchangeProperty.prop_process_data.inbMsgName}:${exchangeProperty.prop_process_data.endToEndId}] completed.")
			
			.removeHeaders("*")
			
		;


	}
}
