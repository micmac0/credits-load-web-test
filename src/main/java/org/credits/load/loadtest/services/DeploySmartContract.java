package org.credits.load.loadtest.services;

import org.credits.load.loadtest.DoDeploySmartContract;
import org.credits.load.loadtest.DoSendSmartContractThread;
import org.credits.load.loadtest.util.NodesProperties;
import org.credits.load.loadtest.util.SmartContractProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class DeploySmartContract {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeploySmartContract.class);


	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	SmartContractProperties smartContractProperties;

	public void executeAsynchronously() {

		taskExecutor.execute(new Runnable() {

			@Override

			public void run() {
				try {

					DoDeploySmartContract smartContractThreadDeploy = new DoDeploySmartContract();
					smartContractThreadDeploy.setSmartContractProperties(smartContractProperties);
					taskExecutor.execute(smartContractThreadDeploy);

				} catch (Exception e) {
					LOGGER.error("error", e);
				}
			}

		});

	}

}
