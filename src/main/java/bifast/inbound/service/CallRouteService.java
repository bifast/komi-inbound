package bifast.inbound.service;


import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.spi.RouteController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.library.iso20022.custom.BusinessMessage;

@Service
public class CallRouteService {
	@Autowired private UtilService utilService;
	
	public BusinessMessage decrypt_unmarshal (Exchange exchange) {
		String strBody = exchange.getMessage().getBody(String.class);
		FluentProducerTemplate template = exchange.getContext().createFluentProducerTemplate();
		BusinessMessage decrBody = template.withBody(strBody).to("direct:decryp_unmarshal").request(BusinessMessage.class);
		return decrBody;
	}
	
	public String encrypt_body (Exchange exchange) {
		FluentProducerTemplate template = exchange.getContext().createFluentProducerTemplate();
		String decrBody = template.withBody(exchange.getMessage().getBody()).to("direct:encrypbody").request(String.class);
		return decrBody;
	}
	
	public String encryptBiRequest (Exchange exchange) throws JsonProcessingException {
		ProcessDataPojo processData = exchange.getProperty("prop_process_data", ProcessDataPojo.class);
		BusinessMessage bm = processData.getBiRequestMsg();
		String str = utilService.serializeBusinessMassage(bm);
		
		FluentProducerTemplate template = exchange.getContext().createFluentProducerTemplate();
		String encrBody = template.withBody(str).to("direct:encrypbody").request(String.class);
		return encrBody;
	}

	public void resumeJobRoute (Exchange exchange, String route) throws Exception {
		RouteController routeCtl = exchange.getContext().getRouteController();
		ServiceStatus serviceSts = routeCtl.getRouteStatus(route);
		
		if (serviceSts.isStopped())
			routeCtl.startRoute(route);
		
		else if (serviceSts.isSuspended())
			routeCtl.resumeRoute(route);
	}

	public Object callRoute (Exchange exchange, String to) {
//		FluentProducerTemplate template = exchange.getContext().createFluentProducerTemplate();
//		Object result = template.to(to).request(Object.class);
		
		CamelContext context = exchange.getContext();
		context.createProducerTemplate().send(to, exchange);
		Object result = exchange.getMessage().getBody(Object.class);

		return result;
	}
}
