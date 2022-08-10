package bifast.inbound.credittransfer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import bifast.inbound.credittransfer.processor.CTCorebankRequestProcessor;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatPacs008Pojo;
import bifast.inbound.service.CallRouteService;
import bifast.inbound.service.FlattenIsoMessageService;
import bifast.library.iso20022.custom.BusinessMessage;
import bifast.inbound.credittransfer.processor.CbSettlementRequestProc;

@Component
public class CreditTransferSAFRoute extends RouteBuilder {
	@Autowired private CTCorebankRequestProcessor ctRequestProcessor;
	@Autowired private CbSettlementRequestProc settlementRequestPrc;
	@Autowired private CallRouteService routeService;
	@Autowired private FlattenIsoMessageService flatMsgService;

	@Override
	public void configure() throws Exception {
		
//		JacksonDataFormat businessMessageJDF = jdfService.wrapUnwrapRoot(BusinessMessage.class);

		from("sql:select kct.id , "
				+ "kct.komi_trns_id, "
				+ "kct.req_bizmsgid, "
				+ "kct.response_code, "
				+ "kct.full_request_msg ct_msg, "
				+ "kct.e2e_id "
				+ "from kc_credit_transfer kct "
				+ "where kct.cb_status = 'PENDING' "
				+ "and kct.sttl_bizmsgid = 'RECEIVED' "
				+ "?delay=8000"
//				+ "&sendEmptyMessageWhenIdle=true"
				)
			.routeId("komi.ct.saf")
						
			.setProperty("ctsaf_qryresult", simple("${body}"))
			.log("[CTSAF:${exchangeProperty.ctsaf_qryresult[e2e_id]}] Submit incoming CreditTransfer started.")
			
//			.process(initCTJobProcessor)  // hdr_process_data
			.process(this::initCT)  // hdr_process_data

			.process(ctRequestProcessor)
			// send ke corebank
			.setHeader("cb_msgname", constant("CTSAF"))
			.to("direct:isoadpt-credit")
			
			.log(LoggingLevel.DEBUG, "komi.ct.saf", "[CTSAF:${exchangeProperty.ctsaf_qryresult[e2e_id]}] Selesai call credit account")
			
			.choice()
				.when().simple("${body.status} == 'TIMEOUT'")
					.log(LoggingLevel.DEBUG,"komi.ct.saf", "[CTSAF:${exchangeProperty.ctsaf_qryresult[e2e_id]}] Update CT status TIMEOUT")
					.to("sql:update kc_credit_transfer "
							+ "set cb_status = 'TIMEOUT' "
							+ "where id = :#${exchangeProperty.ctsaf_qryresult[id]}")
				.endChoice()
				.when().simple("${body.status} == 'RJCT'")
					.to("sql:update kc_credit_transfer "
							+ "set cb_status = 'DONE', reversal = 'PENDING' "
							+ "where id = :#${exchangeProperty.ctsaf_qryresult[id]}")
				.endChoice()
				.when().simple("${body.status} == 'ERROR'")
					.log(LoggingLevel.ERROR, "[CTSAF:${exchangeProperty.ctsaf_qryresult[e2e_id]}] error submit credit-account ke cbs")
					.to("sql:update kc_credit_transfer "
							+ "set cb_status = 'ERROR', reversal = 'PENDING' "
							+ "where id = :#${exchangeProperty.ctsaf_qryresult[id]}")
				.endChoice()
				
				.when().simple("${body.status} == 'ACTC'")
					.log(LoggingLevel.DEBUG,"komi.ct.saf", "[CTSAF:${exchangeProperty.ctsaf_qryresult[e2e_id]}] CT corebank Accepted")
					.setProperty("ctsaf_settlement", constant("YES"))
					.to("sql:update kc_credit_transfer "
							+ "set cb_status = 'DONE' "
							+ "where id = :#${exchangeProperty.ctsaf_qryresult[id]}")

					.log(LoggingLevel.DEBUG,"komi.settlement.inbound", "[CTSAF:${exchangeProperty.ctsaf_qryresult[e2e_id]}] Akan post settlement")
					.process(settlementRequestPrc)
					.to("direct:isoadpt-sttl")

				.endChoice()
			.end()
							
		;

	}

	private void initCT(Exchange exchange) throws JsonProcessingException {
		@SuppressWarnings("unchecked")
		HashMap<String, Object> arr = exchange.getProperty("ctsaf_qryresult",HashMap.class);

		BusinessMessage orgnlCTRequest = routeService.decryptBusinessMessage(String.valueOf(arr.get("ct_msg"))); 
		
		ProcessDataPojo processData = new ProcessDataPojo();
		FlatPacs008Pojo flat008 = flatMsgService.flatteningPacs008(orgnlCTRequest); 
		
		processData.setBiRequestFlat(flat008);
	
		processData.setBiRequestMsg(orgnlCTRequest);
		processData.setStartTime(Instant.now());
		processData.setInbMsgName("CTSAF");
		processData.setEndToEndId(flat008.getEndToEndId());
		processData.setKomiTrnsId(String.valueOf(arr.get("komi_trns_id")));
		processData.setReceivedDt(LocalDateTime.now());

		exchange.setProperty("prop_process_data", processData);
		
		exchange.setProperty("pr_komitrnsid", String.valueOf(arr.get("komi_trns_id")));

	}
}
