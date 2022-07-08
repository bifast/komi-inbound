package bifast.inbound.credittransfer;


import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bifast.inbound.accountenquiry.IsoAERequestPrc;
import bifast.inbound.credittransfer.processor.CheckSAFStatusProcessor;
import bifast.inbound.credittransfer.processor.CreditTransferProcessor;
import bifast.inbound.credittransfer.processor.SaveCreditTransferProcessor;
import bifast.inbound.processor.DuplicateTransactionValidation;
import bifast.inbound.service.JacksonDataFormatService;
import bifast.library.iso20022.custom.BusinessMessage;

@Component
public class CreditTransferRoute extends RouteBuilder {
	@Autowired private CheckSAFStatusProcessor checkSafStatus;
	@Autowired private CreditTransferProcessor creditTransferProcessor;
	@Autowired private DuplicateTransactionValidation duplicationTrnsValidation;
	@Autowired private JacksonDataFormatService jdfService;
	@Autowired private SaveCreditTransferProcessor saveCreditTransferProcessor;
	@Autowired private IsoAERequestPrc isoAERequestPrc;

	@Override
	public void configure() throws Exception {
		JacksonDataFormat businessMessageJDF = jdfService.wrapRoot(BusinessMessage.class);
		
		onException(Exception.class)
			.log("Route level onException")
			.log(LoggingLevel.ERROR, "${exception.stacktrace}")
			.marshal(businessMessageJDF)
			.setHeader("hdr_tmp", simple("${body}"))
			.marshal().zipDeflater().marshal().base64()
			.setProperty("prop_toBI_jsonzip", simple("${body}"))
			.setBody(simple("${header.hdr_tmp}"))
			.log(LoggingLevel.DEBUG,"komi.ct","${body}")
			.handled(true)
			.to("seda:save_ct?exchangePattern=InOnly")
			.removeHeaders("*")
		;
		
		from("direct:crdttransfer").routeId("komi.ct")
			.process(duplicationTrnsValidation)
			
			// cek apakah SAF atau bukan
			// check saf
			.process(checkSafStatus)
			
			.log(LoggingLevel.DEBUG,"komi.ct", 
					"[${exchangeProperty.prop_process_data.inbMsgName}:${exchangeProperty.prop_process_data.endToEndId}] Status SAF: ${header.ct_saf}")
			
			// if SAF = NO/NEW --> call CB, set CBSTS = ACTC/RJCT
			.filter().simple("${header.ct_saf} in 'NO,NEW'")

				// proses sebagai AE request
				.process(isoAERequestPrc)
				.to("direct:isoadpt")
		
				.choice()
					.when().simple("${body.class} endsWith 'FaultPojo'")
						.setHeader("ct_cbsts", constant("ERROR"))
					.endChoice()
					.otherwise()
						.setHeader("ct_cbsts", simple("${body.status}"))
					.endChoice()
				.end()
			.end()
					
			.process(creditTransferProcessor)
			
			// versi gzip nya unt dicatat di table
			.setHeader("ct_tmpbody", simple("${body}"))
			.marshal(businessMessageJDF)
			.marshal().zipDeflater().marshal().base64()
			.setProperty("prop_toBI_jsonzip", simple("${body}"))
			.setBody(simple("${header.ct_tmpbody}"))

			.log(LoggingLevel.DEBUG, "komi.ct", 
					"[${exchangeProperty.prop_process_data.inbMsgName}:${exchangeProperty.prop_process_data.endToEndId}] saf ${header.ct_saf}, cb_sts ${header.ct_cbsts}")

			//if SAF=old/new and CBSTS=RJCT --> reversal
			//jika saf dan corebank error, ct proses harus diulang: reversal = UNDEFINED
			.filter().simple("${header.ct_saf} != 'NO'")
				.choice()
					.when().simple("${header.ct_cbsts} == 'RJCT'")
						.log("[${exchangeProperty.prop_process_data.inbMsgName}:${exchangeProperty.prop_process_data.endToEndId}] Harus CT Reversal.")
						.setHeader("hdr_reversal", constant("YES"))
					.endChoice()
					.when().simple("${header.ct_cbsts} == 'ERROR'")
						.log("[${exchangeProperty.prop_process_data.inbMsgName}:${exchangeProperty.prop_process_data.endToEndId}] Apakah CT Reversal?")
						.setHeader("hdr_reversal", constant("UNDEFINED"))
					.endChoice()
				.end()
			.end()
	
			.to("seda:save_ct?exchangePattern=InOnly")
		
			.removeHeaders("ct_*")

		;
	
		from("seda:save_ct?concurrentConsumers=5").routeId("savect")
			.setExchangePattern(ExchangePattern.InOnly)
			.process(saveCreditTransferProcessor)
		;

	}
}
