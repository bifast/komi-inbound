package bifast.inbound.corebank;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import bifast.inbound.corebank.isopojo.CreditRequest;
import bifast.inbound.corebank.isopojo.CreditResponse;
import bifast.inbound.model.CorebankTransaction;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.processor.EnrichmentAggregator;
import bifast.inbound.repository.CorebankTransactionRepository;
import bifast.inbound.service.JacksonDataFormatService;

@Component
public class CbCreditRoute extends RouteBuilder {
	@Autowired private JacksonDataFormatService jdfService;
	@Autowired private EnrichmentAggregator enrichmentAggregator;
	@Autowired private CorebankTransactionRepository cbRepo;

	private static Logger logger = LoggerFactory.getLogger(CbCreditRoute.class);

	@Override
	public void configure() throws Exception {
		JacksonDataFormat creditRequestJDF = jdfService.basic(CreditRequest.class);
		JacksonDataFormat creditResponseJDF = jdfService.basic(CreditResponse.class);

		onException(HttpOperationFailedException.class).onWhen(simple("${exception.statusCode} == '504'"))
			.log(LoggingLevel.ERROR, "[${header.cb_msgname}:${header.cb_e2eid}] Corebank TIMEOUT: \n ${exception.message}")
			.process(this::cbCreditFailed).marshal(creditResponseJDF)
			.continued(true);

		onException(Exception.class)
			.log(LoggingLevel.ERROR, "[${header.cb_msgname}:${header.cb_e2eid}] Call CB Error.")
			.log(LoggingLevel.ERROR, "${exception.stacktrace}")
			.process(this::cbCreditFailed).marshal(creditResponseJDF)
			.continued(true);

		from("direct:isoadpt-credit").routeId("komi.cb.credit")

			.setHeader("cb_msgname", simple("${exchangeProperty[prop_process_data.inbMsgName]}"))
			.setHeader("cb_e2eid", simple("${exchangeProperty[prop_process_data.endToEndId]}"))
			.setProperty("cb_request", simple("${body}"))
			
			.log(LoggingLevel.DEBUG,"komi.cb.credit", "[${header.cb_msgname}:${header.cb_e2eid}] Terima di corebank: ${body}")
			
			.setHeader("cb_requestName", constant("credit"))
			.setHeader("cb_url", simple("{{komi.url.isoadapter.credit}}"))
			.marshal(creditRequestJDF)

			.setProperty("cb_request_str", simple("${body}"))
			
	 		.log("[${header.cb_msgname}:${header.cb_e2eid}] POST ${header.cb_url}")
	 		.log("[${header.cb_msgname}:${header.cb_e2eid}] CB Request: ${body}")

			.setHeader("HttpMethod", constant("POST"))
			.setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
			.enrich().simple("${header.cb_url}?bridgeEndpoint=true").aggregationStrategy(enrichmentAggregator)
			.convertBodyTo(String.class)
			.log("[${header.cb_msgname}:${header.cb_e2eid}] CB response: ${body}")
			
			.unmarshal(creditResponseJDF)
			
			.process(this::saveCreditTransaction)
		;
	}
	
	private void saveCreditTransaction(Exchange exchange) {
//		String msgName = exchange.getMessage().getHeader("cb_msgname", String.class);
		ProcessDataPojo processData = exchange.getProperty("prop_process_data", ProcessDataPojo.class);
		String msgName = processData.getInbMsgName();
		
		String komiTrnsId = exchange.getProperty("pr_komitrnsid", String.class);
		String strRequest = exchange.getProperty("cb_request_str", String.class);
		CreditResponse cbResponse = exchange.getMessage().getBody(CreditResponse.class);

		logger.debug("[" + msgName + ":" + processData.getEndToEndId() + "] Akan save table corebank_transaction");

		CorebankTransaction corebankTrans = new CorebankTransaction();

		corebankTrans.setUpdateTime(LocalDateTime.now());
		corebankTrans.setFullTextRequest(strRequest);
		corebankTrans.setTrnsDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
		corebankTrans.setKomiTrnsId(komiTrnsId);

		corebankTrans.setReason(cbResponse.getReason());
		corebankTrans.setResponse(cbResponse.getStatus());

		CreditRequest req = (CreditRequest) exchange.getProperty("cb_request", Object.class);
		
		corebankTrans.setCreditAmount(new BigDecimal(req.getAmount()));
		corebankTrans.setCstmAccountName(req.getCreditorName());
		corebankTrans.setCstmAccountNo(req.getCreditorAccountNumber());
		corebankTrans.setCstmAccountType(req.getCreditorAccountType());
		corebankTrans.setDateTime(req.getDateTime());
		corebankTrans.setFeeAmount(new BigDecimal(0));
		
		corebankTrans.setKomiNoref(req.getNoRef());
		corebankTrans.setOrgnlChnlNoref(req.getOriginalNoRef());
		corebankTrans.setOrgnlDateTime(req.getOriginalDateTime());
					
		corebankTrans.setTransactionType("Credit");
		cbRepo.save(corebankTrans);
	}

	private void cbCreditFailed (Exchange exchange) throws IllegalAccessException, 
															IllegalArgumentException, 
															InvocationTargetException {
		
		Object objException = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Object.class);

		int statusCode = 500;
		try {
			Method getStatusCode = objException.getClass().getMethod("getStatusCode");
			statusCode = (int) getStatusCode.invoke(objException);
		} catch(NoSuchMethodException noMethodE) {}

		CreditResponse resp = new CreditResponse();
		if (statusCode == 504) {
			resp.setStatus("TIMEOUT");
			resp.setReason("U900");
		}
		else {
			resp.setStatus("ERROR");
			resp.setReason("U901");
		}

		exchange.getMessage().setBody(resp, CreditResponse.class);
	}
}
