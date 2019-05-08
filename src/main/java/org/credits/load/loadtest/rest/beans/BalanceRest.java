package org.credits.load.loadtest.rest.beans;

import java.math.BigDecimal;

public class BalanceRest {
	private String publicKey;
	private BigDecimal amount;

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

}
