package bifast.inbound.config;

public class Debitrev {
	private long retryInterval;
	private int retry;
	public long getRetryInterval() {
		return retryInterval;
	}
	public void setRetryInterval(long retryInterval) {
		this.retryInterval = retryInterval;
	}
	public int getRetry() {
		return retry;
	}
	public void setRetry(int retry) {
		this.retry = retry;
	}
	
	
}
