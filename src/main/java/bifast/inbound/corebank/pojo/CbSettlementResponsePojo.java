package bifast.inbound.corebank.pojo;

public class CbSettlementResponsePojo {

	private String komiTrnsId;
	private String orgnlKomiTrnsId;
	private String status;
	private String reason;
	private String additionalInfo;
	
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
