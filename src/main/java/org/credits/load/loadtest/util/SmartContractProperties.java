package org.credits.load.loadtest.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "smart-contract")
public class SmartContractProperties {
	private Deploy deploy;
	private Execute execute;

	public Deploy getDeploy() {
		return deploy;
	}

	public void setDeploy(Deploy deploy) {
		this.deploy = deploy;
	}

	public Execute getExecute() {
		return execute;
	}

	public void setExecute(Execute execute) {
		this.execute = execute;
	}

	public static class Deploy {
		private String fromPublic;
		private String fromPrivate;
		private String sourceFile;

		public String getFromPublic() {
			return fromPublic;
		}

		public void setFromPublic(String from) {
			this.fromPublic = from;
		}

		public String getSourceFile() {
			return sourceFile;
		}

		public void setSourceFile(String sourceFile) {
			this.sourceFile = sourceFile;
		}

		public String getFromPrivate() {
			return fromPrivate;
		}

		public void setFromPrivate(String fromPrivate) {
			this.fromPrivate = fromPrivate;
		}

	}

	public static class Execute {
		private String smAddress;
		private String fromPublic;
		private String fromPrivate;
		private Boolean runLocally;
		private String nodeAddress;
		private Integer nodePort;
		private Integer nbExecution;
		private Integer timeWaitMs;
		private String method;
		private List<SmConfigParam> params = new ArrayList<SmartContractProperties.Execute.SmConfigParam>();

		public String getSmAddress() {
			return smAddress;
		}

		public void setSmAddress(String smAddress) {
			this.smAddress = smAddress;
		}

		public String getFromPublic() {
			return fromPublic;
		}

		public void setFromPublic(String fromPublic) {
			this.fromPublic = fromPublic;
		}

		public String getFromPrivate() {
			return fromPrivate;
		}

		public void setFromPrivate(String fromPrivate) {
			this.fromPrivate = fromPrivate;
		}

		public Boolean getRunLocally() {
			return runLocally;
		}

		public void setRunLocally(Boolean runLocally) {
			this.runLocally = runLocally;
		}

		public String getNodeAddress() {
			return nodeAddress;
		}

		public void setNodeAddress(String nodeAddress) {
			this.nodeAddress = nodeAddress;
		}

		public Integer getNodePort() {
			return nodePort;
		}

		public void setNodePort(Integer nodePort) {
			this.nodePort = nodePort;
		}

		public Integer getNbExecution() {
			return nbExecution;
		}

		public void setNbExecution(Integer nbExecution) {
			this.nbExecution = nbExecution;
		}

		public Integer getTimeWaitMs() {
			return timeWaitMs;
		}

		public void setTimeWaitMs(Integer timeWaitMs) {
			this.timeWaitMs = timeWaitMs;
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public List<SmConfigParam> getParams() {
			return params;
		}

		public void setParams(List<SmConfigParam> params) {
			this.params = params;
		}

		public static class SmConfigParam {
			private String value;
			private String type;

			public String getValue() {
				return value;
			}

			public void setValue(String value) {
				this.value = value;
			}

			public String getType() {
				return type;
			}

			public void setType(String type) {
				this.type = type;
			}
		}

	}
}
