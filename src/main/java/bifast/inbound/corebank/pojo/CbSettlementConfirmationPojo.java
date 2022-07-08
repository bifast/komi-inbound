package bifast.inbound.corebank.pojo;

public class CbSettlementConfirmationPojo {
	
	private String transactionId;
	private String dateTime;
	private String merchantId;
	private String terminalId;
	private String noRef;
	private String originalNoRef;
	private String status;
	private String reason;
	private String additionalInfo;
	
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	public String getDateTime() {
		return dateTime;
	}
	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	public String getMerchantId() {
		return merchantId;
	}
	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}
	public String getTerminalId() {
		return terminalId;
	}
	public void setTerminalId(String terminalId) {
		this.terminalId = terminalId;
	}
	public String getNoRef() {
		return noRef;
	}
	public void setNoRef(String noRef) {
		this.noRef = noRef;
	}
	public String getOriginalNoRef() {
		return originalNoRef;
	}
	public void setOriginalNoRef(String originalNoRef) {
		this.originalNoRef = originalNoRef;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public String getAdditionalInfo() {
		return additionalInfo;
	}
	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
	
	

}
