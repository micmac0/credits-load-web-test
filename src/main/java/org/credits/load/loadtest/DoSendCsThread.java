package org.credits.load.loadtest;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.credits.load.loadtest.util.Fee;
import org.credits.load.loadtest.util.NodesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import com.credits.client.node.crypto.Ed25519;
import com.credits.client.node.pojo.TransactionFlowData;
import com.credits.client.node.pojo.TransactionFlowResultData;
import com.credits.client.node.service.NodeApiService;
import com.credits.client.node.service.NodeApiServiceImpl;
import com.credits.client.node.thrift.generated.API;
import com.credits.client.node.thrift.generated.API.Client;
import com.credits.client.node.thrift.generated.API.Client.Factory;

import com.credits.client.node.thrift.generated.AmountCommission;
import com.credits.client.node.thrift.generated.Transaction;
import com.credits.client.node.thrift.generated.TransactionFlowResult;
import com.credits.client.node.thrift.generated.TransactionType;
import com.credits.client.node.thrift.generated.WalletBalanceGetResult;
import com.credits.client.node.thrift.generated.WalletTransactionsCountGetResult;
import com.credits.client.node.util.NodePojoConverter;
import com.credits.client.node.util.SignUtils;
import com.credits.general.thrift.generated.Amount;
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
	
	@Autowired
	SendTrxAsync sendTrxAsync;

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
		String commentsStr = nodesProperties.getNodes().get(nodeConfigNumber).getComments();
		Integer incrementFactor = nodesProperties.getIncrementFactor();
		byte[] commentsByt = null;
		if(!StringUtils.isBlank(commentsStr)) {
			try {
				commentsByt = commentsStr.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		BigDecimal amountDecimal = new BigDecimal(nodesProperties.getNodes().get(nodeConfigNumber).getAmount());

		Long previousId = -1L;

		Fee maxFee = new Fee(new BigDecimal("10"));

		try {
			Integer waitTime = nodesProperties.getNodes().get(nodeConfigNumber).getTimeTrxWaitMs();
			TTransport transport = new TSocket(nodesProperties.getNodes().get(nodeConfigNumber).getAddress(),
					nodesProperties.getNodes().get(nodeConfigNumber).getPort());
			Factory clientFactory = new Client.Factory();
			TProtocol protocol = new TBinaryProtocol(transport);
			API.Client client = clientFactory.getClient(protocol);
			transport.open();

			
			if (transport.isOpen()) {
//				byte[] custWallet = GeneralConverter.decodeFromBASE58("AhkX6vbhBjRdK8sfyRRUKN1CDyR7eFnkNQh2ePbVpB12");
//				WalletBalanceGetResult resultBalance = client.WalletBalanceGet(ByteBuffer.wrap(custWallet));
//				System.out.println(NodePojoConverter.amountToBigDecimal(resultBalance.balance));


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
				id = id +1;
				NodeApiService nodeApiService = NodeApiServiceImpl.getInstance(
						nodesProperties.getNodes().get(nodeConfigNumber).getAddress(),
						nodesProperties.getNodes().get(nodeConfigNumber).getPort());

					for (int j = 0; j < nbSend; j++) {


						 Amount amount = GeneralConverter.bigDecimalToAmount(amountDecimal);
						 Amount balance = GeneralConverter.bigDecimalToAmount(new BigDecimal("1"));



						TransactionFlowData tStruct = new TransactionFlowData(id, sourceByte, targetByte, amountDecimal,
								maxFee.getFee(), null, commentsByt, null);

						byte[] privateKeyByteArr1;
						privateKeyByteArr1 = GeneralConverter.decodeFromBASE58(pk);
						PrivateKey privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr1);
						SignUtils.signTransaction(tStruct, privateKey);
						

						// LOGGER.info(Converter.bytesToHex(tStruct.getBytes()));

						sendTrxAsync.asyncDoSendTrx(nodeApiService, tStruct);
						



						Thread.currentThread().sleep(waitTime);

						id ++;
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
