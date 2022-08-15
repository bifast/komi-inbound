package bifast.inbound.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;


@Entity(name="KC_CHANNEL")
public class Channel {

	@Id
	@Column(length=15)
	private String channelId;
	
	@Column(length=100)
	private String channelName;
	
	@Column(length=100)
	private String secretKey;
	
	@Column(length=10)
	private String channelType;
	
	private String merchantCode;
	
	private BigDecimal dailyLimitAmount;
	private BigDecimal transactionLimitAmount;
	
	private LocalDateTime createDt;
	private LocalDateTime modifDt;

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getChannelType() {
		return channelType;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public String getMerchantCode() {
		return Optional.ofNullable(merchantCode).orElse("");
	}

	public void setMerchantCode(String merchantCode) {
		this.merchantCode = merchantCode;
	}

	public BigDecimal getDailyLimitAmount() {
		return dailyLimitAmount;
	}

	public void setDailyLimitAmount(BigDecimal dailyLimitAmount) {
		this.dailyLimitAmount = dailyLimitAmount;
	}

	public BigDecimal getTransactionLimitAmount() {
		return transactionLimitAmount;
	}

	public void setTransactionLimitAmount(BigDecimal transactionLimitAmount) {
		this.transactionLimitAmount = transactionLimitAmount;
	}

	public LocalDateTime getCreateDt() {
		return createDt;
	}

	public void setCreateDt(LocalDateTime createDt) {
		this.createDt = createDt;
	}

	public LocalDateTime getModifDt() {
		return modifDt;
	}

	public void setModifDt(LocalDateTime modifDt) {
		this.modifDt = modifDt;
	}

	
}
