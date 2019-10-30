package org.credits.load.loadtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import com.credits.client.node.pojo.TransactionFlowData;
import com.credits.client.node.pojo.TransactionFlowResultData;
import com.credits.client.node.service.NodeApiService;

@Service
@EnableAsync
public class SendTrxAsync {
	private static final Logger LOGGER = LoggerFactory.getLogger(SendTrxAsync.class);
	@Async
	public void asyncDoSendTrx(NodeApiService nodeApiService, TransactionFlowData tStruct) {
		TransactionFlowResultData res2 = nodeApiService.transactionFlow(tStruct);
		LOGGER.info(res2.getMessage()+" round number = "+res2.getRoundNumber());
	}

}
