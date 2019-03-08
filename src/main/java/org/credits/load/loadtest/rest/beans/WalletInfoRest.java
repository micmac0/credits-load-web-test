package org.credits.load.loadtest.rest.beans;

public class WalletInfoRest {
	private String publicKey;
	private Double balance;
	private Long lastTrxId;

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public Double getBalance() {
		return balance;
	}

	public void setBalance(Double balance) {
		this.balance = balance;
	}

	public Long getLastTrxId() {
		return lastTrxId;
	}

	public void setLastTrxId(Long lastTrxId) {
		this.lastTrxId = lastTrxId;
	}

}
