package bifast.inbound.corebank.pojo;

public class CreditReversalRequestPojo {

	private String transactionId;
	private Long orignlCBId;
	private String orgnlCBDt;
	private String accountNumber;
	private String accountType;
	private String amount;
	private String fee;
	private String debtorName;
	private String paymentInfo;
	private String requestTime;
	
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	public Long getOrignlCBId() {
		return orignlCBId;
	}
	public void setOrignlCBId(Long orignlCBId) {
		this.orignlCBId = orignlCBId;
	}
	public String getOrgnlCBDt() {
		return orgnlCBDt;
	}
	public void setOrgnlCBDt(String orgnlCBDt) {
		this.orgnlCBDt = orgnlCBDt;
	}
	public String getAccountNumber() {
		return accountNumber;
	}
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
	public String getAccountType() {
		return accountType;
	}
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public String getFee() {
		return fee;
	}
	public void setFee(String fee) {
		this.fee = fee;
	}
	public String getDebtorName() {
		return debtorName;
	}
	public void setDebtorName(String debtorName) {
		this.debtorName = debtorName;
	}
	public String getPaymentInfo() {
		return paymentInfo;
	}
	public void setPaymentInfo(String paymentInfo) {
		this.paymentInfo = paymentInfo;
	}
	public String getRequestTime() {
		return requestTime;
	}
	public void setRequestTime(String requestTime) {
		this.requestTime = requestTime;
	}

	
	
}
