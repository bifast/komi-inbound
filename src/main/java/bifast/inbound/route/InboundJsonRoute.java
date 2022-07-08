package bifast.inbound.route;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.service.JacksonDataFormatService;
import bifast.library.iso20022.custom.BusinessMessage;


@Component
public class InboundJsonRoute extends RouteBuilder {
	@Autowired private JacksonDataFormatService jdfService;

	@Override
	public void configure() throws Exception {
		JacksonDataFormat jsonBusinessMessageDataFormat = jdfService.wrapUnwrapRoot(BusinessMessage.class);

		
		restConfiguration()
			.component("servlet")
		;
			
		rest("/json")
			.post("/service")
				.consumes("application/json")
				.to("direct:parsejson")

			.post("/test")
				.consumes("application/json")
				.to("direct:testae")
		;


		from("direct:parsejson").routeId("komi.jsonEndpoint")
			.convertBodyTo(String.class)
			.setHeader("hdr_inputformat", constant("json"))

			.log(LoggingLevel.DEBUG,"komi.jsonEndpoint", "-------****------")
			.log("Terima: ${body}")
			
			// simpan msg inbound compressed
			.setHeader("hdr_tmp", simple("${body}"))
			.marshal().zipDeflater()
			.marshal().base64()
			.setHeader("hdr_frBI_jsonzip", simple("${body}"))
			.setProperty("prop_frBI_jsonzip", simple("${body}"))
			.setBody(simple("${header.hdr_tmp}"))
			
			.unmarshal(jsonBusinessMessageDataFormat)  // ubah ke pojo BusinessMessage
			.setProperty("prop_frBIobj", simple("${body}"))
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
