package bifast.inbound.corebank;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import bifast.inbound.config.Config;
import bifast.inbound.corebank.isopojo.DebitReversalRequest;
import bifast.inbound.corebank.isopojo.DebitReversalResponse;
import bifast.inbound.model.CorebankTransaction;
import bifast.inbound.model.FaultClass;
import bifast.inbound.model.StatusReason;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.processor.EnrichmentAggregator;
import bifast.inbound.repository.CorebankTransactionRepository;
import bifast.inbound.repository.FaultClassRepository;
import bifast.inbound.repository.StatusReasonRepository;
import bifast.inbound.service.JacksonDataFormatService;

@Component
public class CbDebitReversalRoute extends RouteBuilder{
	@Autowired private Config config;
	@Autowired private EnrichmentAggregator enrichmentAggregator;
	@Autowired private JacksonDataFormatService jdfService;
	@Autowired private CorebankTransactionRepository cbRepo;
	@Autowired private FaultClassRepository faultClassRepo;
	@Autowired private StatusReasonRepository statusReasonRepo;

//	private static Logger logger = LoggerFactory.getLogger(DebitReversalRoute.class);

	@Override
	public void configure() throws Exception {

		JacksonDataFormat debitReversalRequestJDF = jdfService.basic(DebitReversalRequest.class);
		JacksonDataFormat debitReversalResponseJDF = jdfService.basic(DebitReversalResponse.class);
	
		onException(HttpOperationFailedException.class).onWhen(simple("${exception.statusCode} == '504'")) 
			.routeId("komi_debitrev_excp")
			.maximumRedeliveries(config.getDebitrev().getRetry())
			.redeliveryDelay(config.getDebitrev().getRetryInterval())
			.log(LoggingLevel.ERROR, "[${header.cb_msgname}:${exchangeProperty[prop_process_data.endToEndId]}] Call CB Timeout(504).")
	    	.process(this::faultDebitReversal).marshal(debitReversalResponseJDF)
			.continued(true);

		onException(Exception.class)
			.log(LoggingLevel.ERROR, "[$header.cb_msgname}:${exchangeProperty[prop_process_data.endToEndId]}] Call Corebank Error.")
			.log(LoggingLevel.ERROR, "${exception.stacktrace}")
	    	.process(this::faultDebitReversal).marshal(debitReversalResponseJDF)
			.continued(true)
			;

		from("direct:isoadpt-debitreversal").routeId("komi.cb.debitrev")
			.marshal(debitReversalRequestJDF)

			//submit reversal
			.log(LoggingLevel.DEBUG, "komi.cb.debitrev", "[${header.cb_msgname}:"
					+ "${exchangeProperty[prop_process_data.endToEndId]}] POST {{komi.url.isoadapter.reversal}}")
			.log("[${header.cb_msgname}:${exchangeProperty[prop_process_data.endToEndId]}] Request ISOAdapter: ${body}")
			
			.removeHeaders("hdr_*")
			.setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
			.setHeader("HttpMethod", constant("POST"))
			
			.enrich()
				.simple("{{komi.url.isoadapter.reversal}}?bridgeEndpoint=true")
				.aggregationStrategy(enrichmentAggregator)

			.convertBodyTo(String.class)
			.log("[${header.cb_msgname}:${exchangeProperty[prop_process_data.endToEndId]}] Response ISOAdapter: ${body}")
			.unmarshal(debitReversalResponseJDF)

			.wireTap("direct:savecbdebitrevr")
		;
		
		from("direct:savecbdebitrevr").routeId("komi.savecbdebitrevr")
			.log(LoggingLevel.DEBUG, "komi.savecbdebitrevr", 
				"[${header.cb_msgname}:${exchangeProperty[prop_process_data.endToEndId]}] akan save Debit Reversal ke CB-log")
			.process(this::saveTblDebitReversal)
		;
		
	}
	
	public void saveTblDebitReversal(Exchange exchange) throws Exception {
		ProcessDataPojo processData = exchange.getProperty("prop_process_data", ProcessDataPojo.class);
		String komiTrnsId = processData.getKomiTrnsId();
		DebitReversalRequest cbRequest = (DebitReversalRequest) processData.getCorebankRequest();

		CorebankTransaction cbTrns = new CorebankTransaction();
		cbTrns.setCreditAmount(new BigDecimal(cbRequest.getAmount()));
		cbTrns.setCstmAccountName(cbRequest.getDebtorName());
		cbTrns.setCstmAccountNo(cbRequest.getDebtorAccountNumber());
		cbTrns.setCstmAccountType(cbRequest.getDebtorAccountType());
		cbTrns.setDateTime(cbRequest.getDateTime());
		cbTrns.setFeeAmount(new BigDecimal(cbRequest.getFeeTransfer()));
		
	    ObjectMapper mapper = new ObjectMapper();
		cbTrns.setFullTextRequest(mapper.writeValueAsString(cbRequest));

		cbTrns.setKomiNoref(cbRequest.getNoRef());
		cbTrns.setKomiTrnsId(komiTrnsId);

		cbTrns.setOrgnlChnlNoref(cbRequest.getOriginalNoRef());
		cbTrns.setOrgnlDateTime(cbRequest.getOriginalDateTime());
		cbTrns.setTransactionType("DebitReversal");
		cbTrns.setTrnsDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
		cbTrns.setUpdateTime(LocalDateTime.now());
		
		DebitReversalResponse resp = exchange.getMessage().getBody(DebitReversalResponse.class);
		cbTrns.setReason(resp.getReason());
		cbTrns.setResponse(resp.getStatus());

		cbRepo.save(cbTrns);	
	}
		
	public void faultDebitReversal(Exchange exchange) throws Exception {
		
		Object objException = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Object.class);
		String exceptionClassName = objException.getClass().getName();
		Optional<FaultClass> oFaultClass = faultClassRepo.findByExceptionClass(exceptionClassName);

		int statusCode = 500;
		try {
			Method getStatusCode = objException.getClass().getMethod("getStatusCode");
			statusCode = (int) getStatusCode.invoke(objException);
		} catch(NoSuchMethodException noMethodE) {}

		DebitReversalResponse resp = new DebitReversalResponse();

		if (statusCode == 504) {
			resp.setStatus("RJCT");
			resp.setReason("U900");
			resp.setAdditionalInfo(statusReasonRepo.findById("K000").orElse(new StatusReason()).getDescription());
		}

		else if (oFaultClass.isPresent()) {
			resp.setStatus("KSTS");
			resp.setReason (oFaultClass.get().getReason());
			resp.setAdditionalInfo(statusReasonRepo.findById(oFaultClass.get().getReason()).orElse(new StatusReason()).getDescription());

		}
		else {
			resp.setStatus("RJCT");
			resp.setReason("U220");
			resp.setAdditionalInfo(statusReasonRepo.findById("U220").orElse(new StatusReason()).getDescription());
		}

		exchange.getMessage().setBody(resp, DebitReversalResponse.class);

	}

}
