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
import org.credits.load.loadtest.util.NodesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.credits.client.node.thrift.generated.API;
import com.credits.client.node.thrift.generated.API.Client;
import com.credits.client.node.thrift.generated.API.Client.Factory;
import com.credits.client.node.thrift.generated.Amount;
import com.credits.client.node.thrift.generated.AmountCommission;
import com.credits.client.node.thrift.generated.Transaction;
import com.credits.client.node.thrift.generated.TransactionFlowResult;
import com.credits.client.node.thrift.generated.TransactionType;
import com.credits.client.node.thrift.generated.WalletTransactionsCountGetResult;
import com.credits.common.exception.CreditsCommonException;
import com.credits.common.utils.Converter;
import com.credits.common.utils.Fee;
import com.credits.common.utils.TransactionStruct;
import com.credits.crypto.Ed25519;
import com.credits.leveldb.client.exception.LevelDbClientException;

@Component
@Scope("prototype")
public class DoSendCsSequentialThread implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoSendCsSequentialThread.class);

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

	private void doSend() throws LevelDbClientException, CreditsCommonException {
		Integer maxCount = 0;
		Long id = 0L;
		String source = nodesProperties.getNodes().get(nodeConfigNumber).getFromPublicKey();
		String target = nodesProperties.getNodes().get(nodeConfigNumber).getToPublicKey();
		String pk = nodesProperties.getNodes().get(nodeConfigNumber).getFromPrivateKey();
		byte[] sourceByte = Converter.decodeFromBASE58(source);
		byte[] targetByte = Converter.decodeFromBASE58(target);
		Integer incrementFactor = nodesProperties.getIncrementFactor();

		Long previousId = -1L;

		Fee maxFee = new Fee(new BigDecimal("0.1"));

		try {
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

				for (int i = 0; i < (nbSend / nbTrxResyncTrxId) + 1; i++) {

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
					previousId = id;
					if (nbSend / nbTrxResyncTrxId > 0)
						maxCount = nbTrxResyncTrxId;
					else
						maxCount = nbSend % nbTrxResyncTrxId;
					List<Transaction> transactionToSend = new ArrayList<>();
					for (int j = 0; j < maxCount; j++) {

						BigDecimal amountDecimal = new BigDecimal("0.0001");
						Amount amount = Converter.bigDecimalToAmount(amountDecimal);
						Amount balance = Converter.bigDecimalToAmount(new BigDecimal("0.1"));
						byte currency = 1;

						AmountCommission fee = new AmountCommission(maxFee.getFee());
						long timeCreation = new Date().getTime();
						TransactionType type = null;// TransactionType.TT_Normal;
						Transaction transaction = new Transaction();
						transaction.setId(id);
						transaction.setAmount(amount);
						transaction.setBalance(balance);
						transaction.setCurrency(currency);

						transaction.setFee(fee);
						transaction.setTimeCreation(timeCreation);
						transaction.setType(null);

						transaction.setSource(sourceByte);
						transaction.setTarget(targetByte);
						TransactionStruct tStruct = new TransactionStruct(id, source, target, amountDecimal, maxFee.getFee(), currency, null);

						byte[] privateKeyByteArr1;
						privateKeyByteArr1 = Converter.decodeFromBASE58(pk);
						PrivateKey privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr1);
						byte[] signature = Ed25519.sign(tStruct.getBytes(), privateKey);
						transaction.setSignature(signature);
						// LOGGER.info(Converter.bytesToHex(tStruct.getBytes()));

						TransactionFlowResult res2 = client.TransactionFlow(transaction);
						// LOGGER.info(res2.status.getMessage());
						Integer waitTime = nodesProperties.getNodes().get(nodeConfigNumber).getTimeTrxWaitMs();
						Thread.currentThread().sleep(waitTime);

						id += incrementFactor;
					}

					Thread.sleep(timeBeforeResyncTrxId);
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

	public void setNbTrxResyncTrxId(Integer nbTrxResyncTrxId) {
		this.nbTrxResyncTrxId = nbTrxResyncTrxId;
	}

	public void setTimeBeforeResyncTrxId(Integer timeBeforeResyncTrxId) {
		this.timeBeforeResyncTrxId = timeBeforeResyncTrxId;
	}

}