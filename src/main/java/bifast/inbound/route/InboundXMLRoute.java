package bifast.inbound.route;

import javax.xml.bind.JAXBContext;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.stereotype.Component;

import bifast.library.iso20022.custom.BusinessMessage;


@Component
public class InboundXMLRoute extends RouteBuilder {


	@Override
	public void configure() throws Exception {
		JaxbDataFormat jaxb = new JaxbDataFormat();
		JAXBContext con = JAXBContext.newInstance(BusinessMessage.class);
		jaxb.setContext(con);
		
		restConfiguration()
			.component("servlet")
		;
			
		rest("/xml")
			.post("/service")
				.consumes("application/xml")
				.to("direct:parsexml")
		;

	

		from("direct:parsexml").routeId("komi.xmlEndpoint")
			.convertBodyTo(String.class)
			.setHeader("hdr_inputformat", constant("xml"))

			// simpan msg inbound compressed
			.setHeader("hdr_tmp", simple("${body}"))
			.marshal().zipDeflater()
			.marshal().base64()
			.setHeader("hdr_frBI_jsonzip", simple("${body}"))
			.setBody(simple("${header.hdr_tmp}"))

			.unmarshal(jaxb)	

			.to("direct:receive")

			.filter().simple("${header.hdr_msgType} !in 'SETTLEMENT, PROXYNOTIF'")
				.marshal(jaxb) 
				// simpan outbound compress
				.setHeader("hdr_tmp", simple("${body}"))
				.marshal().zipDeflater()
				.marshal().base64()
				.setHeader("hdr_toBI_jsonzip", simple("${body}"))
				.setBody(simple("${header.hdr_tmp}"))
			.end()
//			.to("seda:logandsave?exchangePattern=InOnly")

			.log("[${header.hdr_frBIobj.appHdr.msgDefIdr}:${header.hdr_frBIobj.appHdr.bizMsgIdr}] completed.")
			.removeHeaders("*")
		
		;


	}
}
