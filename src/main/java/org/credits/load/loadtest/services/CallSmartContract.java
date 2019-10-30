package org.credits.load.loadtest.services;

import org.credits.load.loadtest.DoSendCsThread;
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
public class CallSmartContract {
	private static final Logger LOGGER = LoggerFactory.getLogger(CallSmartContract.class);
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	SmartContractProperties smartContractProperties;

	public void executeAsynchronously() {

		taskExecutor.execute(new Runnable() {

			@Override

			public void run() {
				try {
					for (int counter = 0; counter < smartContractProperties.getExecutors().size(); counter++) {
						DoSendSmartContractThread smartContractThreadCall = new DoSendSmartContractThread();
						smartContractThreadCall.setCallNumber(counter);
						smartContractThreadCall.setSmartContractProperties(smartContractProperties);

						taskExecutor.execute(smartContractThreadCall);

					}
//					for (int counter = 0; counter < 3; counter++) {
//						DoSendSmartContractThread smartContractThreadCall = new DoSendSmartContractThread();
//						smartContractThreadCall.setSmartContractProperties(smartContractProperties);
//						smartContractThreadCall.setCallNumber(counter);
//						taskExecutor.execute(smartContractThreadCall);
//					}
				} catch (Exception e) {
					LOGGER.error("error", e);
				}
			}

		});

	}

}
