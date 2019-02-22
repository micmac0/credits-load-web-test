package org.credits.load.loadtest.services;

import org.credits.load.loadtest.DoSendCsThread;
import org.credits.load.loadtest.util.NodesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class SendCreditsService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SendCreditsService.class);
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	NodesProperties nodesProperties;

	public void executeAsynchronously() {

		taskExecutor.execute(new Runnable() {

			@Override

			public void run() {
				try {

					for (int counter = 0; counter < nodesProperties.getNodes().size(); counter++) {
						DoSendCsThread doSendCsThread = applicationContext.getBean(DoSendCsThread.class);
						doSendCsThread.setNodeConfigNumber(counter);
						doSendCsThread.setNbSend(nodesProperties.getNbTrxThread());
						taskExecutor.execute(doSendCsThread);

					}
				} catch (Exception e) {
					LOGGER.error("error", e);
				}
			}

		});

	}

}
