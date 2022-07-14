package bifast.inbound.notification;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import bifast.inbound.accountenquiry.AEPortalLogProcessor;
import bifast.inbound.service.JacksonDataFormatService;

@Component
public class NotificationRoute extends RouteBuilder{
	@Autowired private AEPortalLogProcessor portalLogProcessor;
	@Autowired private EventNotificationProcessor eventNotifProcessor;
	@Autowired private ProxyNotifProcessor proxyNotifProc;
	@Autowired private JacksonDataFormatService jdfService;

	@Override
	public void configure() throws Exception {

		JacksonDataFormat portalJdf = jdfService.wrapRoot(PortalApiPojo.class);

		onException(Exception.class)
			.handled(true)
			.maximumRedeliveries(2).redeliveryDelay(5000)
    		.log(LoggingLevel.ERROR, "komi.portalnotif", "Error Log-notif ${body}")
    		.log(LoggingLevel.ERROR, "${exception.stacktrace}")
			;

		from("direct:proxynotif").routeId("komi.prxnotif")
			.log("Proxy Port Notification")
			.process(proxyNotifProc)
		
		;

		from("direct:eventnotif").routeId("komi.eventnotif")
			.log("Ada notifikasi")
			.process(eventNotifProcessor)
			
			//TODO info notification
		;
		
		
		from("seda:portalnotif").routeId("komi.portalnotif")
		
			.process(portalLogProcessor)
			.marshal(portalJdf)
//			.log(LoggingLevel.DEBUG, "komi.portalnotif", "Notif ke portal: ${body}")
			//TODO notifikasi ke customer
			.removeHeaders("hdr_*")
			
			.process(new Processor() {
				public void process(Exchange exchange) throws Exception {
		            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				}
			})

			.setHeader("HttpMethod", constant("POST"))
			
			.doTry()

				.to("rest:post:?host={{komi.url.portalapi}}&bridgeEndpoint=true")
			.endDoTry()
	    	.doCatch(Exception.class)
	    		.log(LoggingLevel.ERROR, "komi.portalnotif", "Error Log-notif ${body}")
	    		.log(LoggingLevel.ERROR, "${exception.stacktrace}")
			.end()


		;

		
	}

}
