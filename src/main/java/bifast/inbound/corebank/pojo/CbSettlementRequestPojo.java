package bifast.inbound.corebank.pojo;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("SettlementConfirmation")
public class CbSettlementRequestPojo {

	private String komiTrnsId;
	private String orgnlKomiTrnsId;
	
	private String bizMsgId;
	private String msgId;
	private String counterParty;

	public String getKomiTrnsId() {
		return komiTrnsId;
	}
	public void setKomiTrnsId(String komiTrnsId) {
		this.komiTrnsId = komiTrnsId;
	}
	public String getOrgnlKomiTrnsId() {
		return orgnlKomiTrnsId;
	}
	public void setOrgnlKomiTrnsId(String orgnlKomiTrnsId) {
		this.orgnlKomiTrnsId = orgnlKomiTrnsId;
	}
	public String getBizMsgId() {
		return bizMsgId;
	}
	public void setBizMsgId(String bizMsgId) {
		this.bizMsgId = bizMsgId;
	}
	public String getMsgId() {
		return msgId;
	}
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	public String getCounterParty() {
		return counterParty;
	}
	public void setCounterParty(String counterParty) {
		this.counterParty = counterParty;
	}
	

}
