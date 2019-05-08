package org.credits.load.loadtest.util;

import java.math.BigDecimal;

public class Fee {

	public static final long BIAS = 18;
	public static final int NBITS_MANTISSA = 11;
	public static final int NBITS_EXP = 5;
	public static final int NBITS_SIGN = 1;
	public static final long PREFIX = (long) Math.pow(2, NBITS_MANTISSA - 1);

	private boolean sign;
	private long exponent;
	private long mantissa;
	private short fee;
	private String feeBinaryString;
	private BigDecimal bigDecimalAmount;

	public boolean isSign() {
		return sign;
	}

	public void setSign(boolean sign) {
		this.sign = sign;
	}

	public long getExponent() {
		return exponent;
	}

	public void setExponent(long exponent) {
		this.exponent = exponent;
	}

	public long getMantissa() {
		return mantissa;
	}

	public void setMantissa(long mantissa) {
		this.mantissa = mantissa;
	}

	public short getFee() {
		return fee;
	}

	public void setFee(short fee) {
		this.fee = fee;
	}

	public String getFeeBinaryString() {
		return feeBinaryString;
	}

	public void setFeeBinaryString(String feeBinaryString) {
		this.feeBinaryString = feeBinaryString;
	}

	public Fee(BigDecimal amount) throws IllegalArgumentException {
		bigDecimalAmount = amount;
		double x = amount.doubleValue();
		sign = x < 0 ? false : true;

		double exp = Math.log10(x);
		if (exp >= 0) {
			exp = Math.floor(exp + 0.5);
		} else {
			exp = Math.floor(exp - 0.5);
		}
		double m = x / Math.pow(10, exp);
		if (m > 1) {
			m = m / 10;
			exp++;
		}

		exponent = (long) exp;
		exponent += BIAS;

		mantissa = (long) Math.floor(m * PREFIX);
		if (mantissa == PREFIX) {
			mantissa = PREFIX - 1;
		}

		StringBuilder f = new StringBuilder(sign ? "0" : "1");
		f.append(Long.toBinaryString(exponent));
		f.append(Long.toBinaryString(mantissa));
		feeBinaryString = f.toString();

		if (f.length() > (NBITS_MANTISSA + NBITS_EXP + NBITS_SIGN)) {
			throw new IllegalArgumentException("too long binary number " + feeBinaryString);
		}

		fee = Short.valueOf(feeBinaryString, 2);
	}

	public BigDecimal getBigDecimalAmount() {
		return bigDecimalAmount;
	}

	public void setBigDecimalAmount(BigDecimal bigDecimalAmount) {
		this.bigDecimalAmount = bigDecimalAmount;
	}

}
