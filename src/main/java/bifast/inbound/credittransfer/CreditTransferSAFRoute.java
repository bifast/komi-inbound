package bifast.inbound.credittransfer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import bifast.inbound.corebank.isopojo.CreditResponse;
import bifast.inbound.corebank.isopojo.DebitReversalResponse;
import bifast.inbound.corebank.processor.DebitReversalRequestProcessor;
import bifast.inbound.credittransfer.processor.CTCorebankRequestProcessor;
import bifast.inbound.pojo.ProcessDataPojo;
import bifast.inbound.pojo.flat.FlatPacs008Pojo;
import bifast.inbound.repository.CreditTransferRepository;
import bifast.inbound.service.CallRouteService;
import bifast.inbound.service.FlattenIsoMessageService;
import bifast.library.iso20022.custom.BusinessMessage;
import bifast.inbound.model.CreditTransfer;

@Component
public class CreditTransferSAFRoute extends RouteBuilder {

	@Autowired private CallRouteService routeService;
	@Autowired private CreditTransferRepository ctRepo;
	@Autowired private CTCorebankRequestProcessor ctRequestProcessor;
	@Autowired private FlattenIsoMessageService flatMsgService;
	@Autowired private DebitReversalRequestProcessor debitReversalRequestProc;

	@Override
	public void configure() throws Exception {
		
		from("sql:select kct.id , "
				+ "kct.komi_trns_id, "
				+ "kct.req_bizmsgid, "
				+ "kct.response_code, "
				+ "kct.msg_type, "
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
			
			.process(this::initCT)  // hdr_process_data

			.log(LoggingLevel.DEBUG, "komi.ct.saf", 
					"[CTSAF:${exchangeProperty.ctsaf_qryresult[e2e_id]}] Processing ${exchangeProperty.prop_process_data.inbMsgName}")
			
			.choice()
				.when().simple("${exchangeProperty.prop_process_data.inbMsgName} == 'CTSAF' ")
					.to("direct:postcredit")
				.when().simple("${exchangeProperty.prop_process_data.inbMsgName} == 'RevCT' ")
					.to("direct:postdebitreversal")
			;

		from("direct:postdebitreversal").routeId("komi.ctsaf.debitreversal")
			.process(debitReversalRequestProc)
			.setHeader("cb_msgname", constant("RevCT"))
			.to("direct:isoadpt-debitreversal")
			.log(LoggingLevel.DEBUG, "komi.ctsaf.debitreversal", "[RevCT:${exchangeProperty.ctsaf_qryresult[e2e_id]}] Selesai call debit reversal")
			.process(exchange -> {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> arr = exchange.getProperty("ctsaf_qryresult", HashMap.class);
				Long id = (Long) arr.get("id");
				DebitReversalResponse creditResponse = exchange.getMessage().getBody(DebitReversalResponse.class);
				this.update_table_ct(id, creditResponse.getStatus());
			})
		;

		from("direct:postcredit").routeId("komi.ctsaf.credit")
			.process(ctRequestProcessor)
			// send ke corebank
			.setHeader("cb_msgname", constant("CTSAF"))
			.to("direct:isoadpt-credit")
			
			.log(LoggingLevel.DEBUG, "komi.ct.saf", "[CTSAF:${exchangeProperty.ctsaf_qryresult[e2e_id]}] Selesai call credit account")
			
			.process(exchange -> {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> arr = exchange.getProperty("ctsaf_qryresult", HashMap.class);
				Long id = (Long) arr.get("id");
				CreditResponse creditResponse = exchange.getMessage().getBody(CreditResponse.class);
				this.update_table_ct(id, creditResponse.getStatus());
			})
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

		String msgType = String.valueOf((arr.get("msg_type")));
		if (msgType.equals("Credit Transfer")) {
			processData.setInbMsgName("CTSAF");
			processData.setEndToEndId(flat008.getEndToEndId());
		}
		else if (msgType.equals("Reversal Credit Transfer")) {
			processData.setInbMsgName("RevCT");
//			processData.setEndToEndId(flat008.getOrgnlEndToEndId());
			processData.setEndToEndId(flat008.getEndToEndId());
		}
		
		processData.setKomiTrnsId(String.valueOf(arr.get("komi_trns_id")));
		processData.setReceivedDt(LocalDateTime.now());

		exchange.setProperty("prop_process_data", processData);
		
		exchange.setProperty("pr_komitrnsid", String.valueOf(arr.get("komi_trns_id")));
//		exchange.getMessage().setHeader("cb_msgname", "RevCT");
	}

	private void update_table_ct (Long id, String status) {
		Optional<CreditTransfer> oct = ctRepo.findById(id);
		if (oct.isPresent()) {
			CreditTransfer ct = oct.get();
			
			if (status.equals("TIMEOUT"))
				ct.setCbStatus("TIMEOUT");

			else if (status.equals("RJCT")) {
				ct.setCbStatus("DONE");
				ct.setReversal("PENDING");
			}

			else if (status.equals("ERROR")) {
				ct.setCbStatus("ERROR");
				ct.setReversal("PENDING");
			}
			
			else if (status.equals("ACTC")) {
				ct.setCbStatus("DONE");
			}

			ctRepo.save(ct);
		}
	}
}
