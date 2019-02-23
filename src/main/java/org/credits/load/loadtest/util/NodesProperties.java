package org.credits.load.loadtest.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nodes-wallets-definition")
public class NodesProperties {
	private List<Node> nodes = new ArrayList<>();
	private Integer nbTrxThread;

	public List<Node> getNodes() {
		return nodes;
	}

	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

	public Integer getNbTrxThread() {
		return nbTrxThread;
	}

	public void setNbTrxThread(Integer nbTrxThread) {
		this.nbTrxThread = nbTrxThread;
	}

	public static class Node {
		private String address;
		private Integer port;
		private String fromPublicKey;
		private String fromPrivateKey;
		private String toPublicKey;
		private Integer timeTrxWaitMs;

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public Integer getPort() {
			return port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

		public String getFromPublicKey() {
			return fromPublicKey;
		}

		public void setFromPublicKey(String fromPublicKey) {
			this.fromPublicKey = fromPublicKey;
		}

		public String getFromPrivateKey() {
			return fromPrivateKey;
		}

		public void setFromPrivateKey(String fromPrivateKey) {
			this.fromPrivateKey = fromPrivateKey;
		}

		public String getToPublicKey() {
			return toPublicKey;
		}

		public void setToPublicKey(String toPublicKey) {
			this.toPublicKey = toPublicKey;
		}

		public Integer getTimeTrxWaitMs() {
			return timeTrxWaitMs;
		}

		public void setTimeTrxWaitMs(Integer timeTrxWaitMs) {
			this.timeTrxWaitMs = timeTrxWaitMs;
		}

	}
}
