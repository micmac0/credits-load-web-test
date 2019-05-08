package org.credits.load.loadtest;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.credits.load.loadtest.util.Fee;
import org.credits.load.loadtest.util.NodesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.credits.client.node.crypto.Ed25519;
import com.credits.client.node.pojo.TransactionFlowData;
import com.credits.client.node.pojo.TransactionFlowResultData;
import com.credits.client.node.service.NodeApiService;
import com.credits.client.node.service.NodeApiServiceImpl;
import com.credits.client.node.thrift.generated.API;
import com.credits.client.node.thrift.generated.API.Client;
import com.credits.client.node.thrift.generated.API.Client.Factory;
import com.credits.client.node.thrift.generated.Amount;
import com.credits.client.node.thrift.generated.AmountCommission;
import com.credits.client.node.thrift.generated.Transaction;
import com.credits.client.node.thrift.generated.TransactionFlowResult;
import com.credits.client.node.thrift.generated.TransactionType;
import com.credits.client.node.thrift.generated.WalletTransactionsCountGetResult;
import com.credits.client.node.util.NodePojoConverter;
import com.credits.client.node.util.SignUtils;
import com.credits.general.util.GeneralConverter;


@Component
@Scope("prototype")
public class DoSendCsThread implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoSendCsThread.class);

	private Integer nodeConfigNumber;
	private Integer nbSend;
	private Integer nbTrxResyncTrxId;
	private Integer timeBeforeResyncTrxId;

	@Autowired
	NodesProperties nodesProperties;

	@Override
	public void run() {

		try {
			doSend();
		} catch (Exception e) {
			LOGGER.error("error", e);
		}
	}

	private void doSend(){

		Long id = 0L;
		String source = nodesProperties.getNodes().get(nodeConfigNumber).getFromPublicKey();
		String target = nodesProperties.getNodes().get(nodeConfigNumber).getToPublicKey();
		String pk = nodesProperties.getNodes().get(nodeConfigNumber).getFromPrivateKey();
		byte[] sourceByte = GeneralConverter.decodeFromBASE58(source);
		byte[] targetByte = GeneralConverter.decodeFromBASE58(target);
		Integer incrementFactor = nodesProperties.getIncrementFactor();

		Long previousId = -1L;

		Fee maxFee = new Fee(new BigDecimal("1"));

		try {
			Integer waitTime = nodesProperties.getNodes().get(nodeConfigNumber).getTimeTrxWaitMs();
			TTransport transport = new TSocket(nodesProperties.getNodes().get(nodeConfigNumber).getAddress(),
					nodesProperties.getNodes().get(nodeConfigNumber).getPort());
			Factory clientFactory = new Client.Factory();
			TProtocol protocol = new TBinaryProtocol(transport);
			API.Client client = clientFactory.getClient(protocol);
			transport.open();
			LOGGER.info("thread {} sending configuration : {} trx will be submited per round with {}ms delay between trx,  {}ms between each round", nodeConfigNumber, nbTrxResyncTrxId,
					nodesProperties.getNodes().get(nodeConfigNumber).getTimeTrxWaitMs(),
					timeBeforeResyncTrxId);
			
			if (transport.isOpen()) {
				WalletTransactionsCountGetResult transId = client.WalletTransactionsCountGet(ByteBuffer.wrap(sourceByte));
				if (transId != null)
					id = transId.lastTransactionInnerId + incrementFactor;
				else
					id = 0L;
				if (previousId == -1L) {
					LOGGER.info("thread {} have last id : {}", nodeConfigNumber, id);

				} else {
					LOGGER.info("thread {} have last id : {}", nodeConfigNumber, id);
				}						
				NodeApiService nodeApiService = NodeApiServiceImpl.getInstance(
						nodesProperties.getNodes().get(nodeConfigNumber).getAddress(),
						nodesProperties.getNodes().get(nodeConfigNumber).getPort());

					for (int j = 0; j < nbSend; j++) {

						BigDecimal amountDecimal = new BigDecimal("0.0001");
						Amount amount = NodePojoConverter.bigDecimalToAmount(amountDecimal);
						Amount balance = NodePojoConverter.bigDecimalToAmount(new BigDecimal("1"));








						TransactionFlowData tStruct = new TransactionFlowData(id, sourceByte, targetByte, amountDecimal,
								maxFee.getFee(), null, null);

						byte[] privateKeyByteArr1;
						privateKeyByteArr1 = GeneralConverter.decodeFromBASE58(pk);
						PrivateKey privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr1);
						SignUtils.signTransaction(tStruct, privateKey);
						

						// LOGGER.info(Converter.bytesToHex(tStruct.getBytes()));

						
						TransactionFlowResultData res2 = nodeApiService.transactionFlow(tStruct);
						
						//TransactionFlowResult res2 = client.TransactionFlow(transaction);
						 LOGGER.info(res2.getMessage());

						Thread.currentThread().sleep(waitTime);

						id += incrementFactor;
					}


				}
			
			transport.close();
			LOGGER.info("thread {} ended", nodeConfigNumber);
		} catch (Throwable e) {
			LOGGER.error("erreur in sending thread number {}", nodeConfigNumber, e);
		}
	}

	public Integer getNodeConfigNumber() {
		return nodeConfigNumber;
	}

	public void setNodeConfigNumber(Integer nodeConfigNumber) {
		this.nodeConfigNumber = nodeConfigNumber;
	}

	public void setNbSend(Integer nbSend) {
		this.nbSend = nbSend;
	}



}
