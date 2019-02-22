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
import com.credits.client.node.thrift.generated.TransactionType;
import com.credits.client.node.thrift.generated.WalletBalanceGetResult;
import com.credits.client.node.thrift.generated.WalletTransactionsCountGetResult;
import com.credits.common.exception.CreditsCommonException;
import com.credits.common.utils.Converter;
import com.credits.common.utils.TransactionStruct;
import com.credits.crypto.Ed25519;
import com.credits.leveldb.client.exception.LevelDbClientException;

@Component
@Scope("prototype")
public class DoSendCsThread implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoSendCsThread.class);

	private Integer nodeConfigNumber;
	private Integer nbSend;

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

		try {
			TTransport transport = new TSocket(nodesProperties.getNodes().get(nodeConfigNumber).getAddress(),
					nodesProperties.getNodes().get(nodeConfigNumber).getPort());
			Factory clientFactory = new Client.Factory();
			TProtocol protocol = new TBinaryProtocol(transport);
			API.Client client = clientFactory.getClient(protocol);
			transport.open();
			if (transport.isOpen()) {
				WalletBalanceGetResult balanceGet = client.WalletBalanceGet(ByteBuffer.wrap(sourceByte));
				WalletTransactionsCountGetResult transId = client.WalletTransactionsCountGet(ByteBuffer.wrap(sourceByte));
				if (transId != null)
					id = transId.lastTransactionInnerId + 1;
				else
					id = 0L;
				LOGGER.info("thread {} have last id : {}", nodeConfigNumber, id);

				for (int i = 0; i < (nbSend / 1000) + 1; i++) {
					if (nbSend / 1000 > 0)
						maxCount = 1000;
					else
						maxCount = nbSend % 1000;
					List<Transaction> transactionToSend = new ArrayList<>();
					for (int j = 0; j < maxCount; j++) {

						BigDecimal amountDecimal = new BigDecimal("0.0001");
						Amount amount = Converter.bigDecimalToAmount(amountDecimal);
						Amount balance = Converter.bigDecimalToAmount(new BigDecimal("0.1"));
						byte currency = 1;

						AmountCommission fee = new AmountCommission((short) 18431);
						long timeCreation = new Date().getTime();
						TransactionType type = null;// TransactionType.TT_Normal;
						Transaction transaction = new Transaction();
						transaction.setId(id);
						transaction.setAmount(amount);
						transaction.setBalance(balance);
						transaction.setCurrency(currency);

						transaction.setFee(fee);
						transaction.setTimeCreation(timeCreation);
						// transaction.setType(type);

						transaction.setSource(sourceByte);
						transaction.setTarget(targetByte);
						TransactionStruct tStruct = new TransactionStruct(id, source, target, amountDecimal, (short) 18431, currency, null);

						byte[] privateKeyByteArr1;
						privateKeyByteArr1 = Converter.decodeFromBASE58(pk);
						PrivateKey privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr1);
						byte[] signature = Ed25519.sign(tStruct.getBytes(), privateKey);
						transaction.setSignature(signature);

						// TransactionFlowResult res2 = client.TransactionFlow(transaction);
						transactionToSend.add(transaction);

						id++;
					}
					for (int k = 0; k < transactionToSend.size(); k++) {
						client.TransactionFlow(transactionToSend.get(k));

					}
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
