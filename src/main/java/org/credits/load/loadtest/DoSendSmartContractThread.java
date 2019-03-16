package org.credits.load.loadtest;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.security.PrivateKey;

import org.apache.thrift.TBase;
import org.apache.thrift.TSerializer;
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

import com.credits.client.executor.thrift.generated.ContractExecutor;
import com.credits.client.node.thrift.generated.API;
import com.credits.client.node.thrift.generated.API.Client;
import com.credits.client.node.thrift.generated.API.Client.Factory;
import com.credits.client.node.thrift.generated.Amount;
import com.credits.client.node.thrift.generated.AmountCommission;
import com.credits.client.node.thrift.generated.SmartContractGetResult;
import com.credits.client.node.thrift.generated.SmartContractInvocation;
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
public class DoSendSmartContractThread implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoSendSmartContractThread.class);

	private Integer nodeConfigNumber;

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
		String target = "59Cb9zLbWADLLsXwzFLeaphjcYHqEGKc2XNj434X7THt";
		String pk = nodesProperties.getNodes().get(nodeConfigNumber).getFromPrivateKey();
		byte[] sourceByte = Converter.decodeFromBASE58(source);
		byte[] targetByte = Converter.decodeFromBASE58(target);

		Fee maxFee = new Fee(new BigDecimal("0"));

		try {
			TTransport transport = new TSocket(nodesProperties.getNodes().get(nodeConfigNumber).getAddress(),
					nodesProperties.getNodes().get(nodeConfigNumber).getPort());
			Factory clientFactory = new Client.Factory();
			TProtocol protocol = new TBinaryProtocol(transport);
			API.Client client = clientFactory.getClient(protocol);
			transport.open();

			WalletTransactionsCountGetResult transId = client.WalletTransactionsCountGet(ByteBuffer.wrap(sourceByte));
			if (transId != null)
				id = transId.lastTransactionInnerId + 1;
			else
				id = 0L;
			if (transport.isOpen()) {
				SmartContractGetResult smart = client.SmartContractGet(ByteBuffer.wrap(Converter.decodeFromBASE58(target)));
				ContractExecutor.Client clientSM = new ContractExecutor.Client(protocol);

				Transaction transaction = new Transaction();
				Amount amount = new Amount(0, 0);

				transaction.setId(id);
				transaction.setSource(ByteBuffer.wrap(sourceByte));
				transaction.setTarget(ByteBuffer.wrap(targetByte));
				transaction.setAmount(amount);
				AmountCommission fee = new AmountCommission(maxFee.getFee());
				transaction.setFee(fee);
				transaction.setCurrency((byte) 1);
				SmartContractInvocation smartContractInvoc = new SmartContractInvocation("hello", null, false);
				transaction.setSmartContract(smartContractInvoc);
				transaction.setSmartContractIsSet(true);
				transaction.setType(TransactionType.TT_SmartExecute);

				TSerializer serializer = new TSerializer();
				TBase base = smartContractInvoc;
				byte[] smSerialized = serializer.serialize(base);

				TransactionStruct tStruct = new TransactionStruct(id, source, target, BigDecimal.ZERO, maxFee.getFee(), (byte) 1, smSerialized);
				byte[] privateKeyByteArr1;
				privateKeyByteArr1 = Converter.decodeFromBASE58(pk);
				PrivateKey privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr1);
				byte[] signature = Ed25519.sign(tStruct.getBytes(), privateKey);
				transaction.setSignature(signature);

				TransactionFlowResult res = client.TransactionFlow(transaction);
				System.out.println(res.getStatus().message);
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

	public void setNodesProperties(NodesProperties nodesProperties) {
		this.nodesProperties = nodesProperties;
	}

}
