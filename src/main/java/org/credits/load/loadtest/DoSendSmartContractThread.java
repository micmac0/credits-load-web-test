package org.credits.load.loadtest;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.credits.load.loadtest.util.SmartContractProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.credits.client.node.pojo.SmartContractData;
import com.credits.client.node.pojo.SmartContractInvocationData;
import com.credits.client.node.pojo.SmartContractTransactionFlowData;
import com.credits.client.node.pojo.TransactionFlowData;
import com.credits.client.node.pojo.TransactionFlowResultData;
import com.credits.client.node.pojo.TransactionTypeData;
import com.credits.client.node.service.NodeApiService;
import com.credits.client.node.service.NodeApiServiceImpl;
import com.credits.client.node.thrift.generated.API;
import com.credits.client.node.thrift.generated.API.Client;
import com.credits.client.node.thrift.generated.API.Client.Factory;
import com.credits.client.node.thrift.generated.WalletTransactionsCountGetResult;
import com.credits.client.node.util.NodeClientUtils;
import com.credits.client.node.util.SignUtils;
import com.credits.common.exception.CreditsCommonException;
import com.credits.common.utils.Converter;
import com.credits.common.utils.Fee;
import com.credits.crypto.Ed25519;
import com.credits.general.thrift.generated.Variant;
import com.credits.leveldb.client.exception.LevelDbClientException;

@Component
@Scope("prototype")
public class DoSendSmartContractThread implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoSendSmartContractThread.class);

	SmartContractProperties smartContractProperties;

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

		String source = smartContractProperties.getExecute().getFromPublic();
		String target = smartContractProperties.getExecute().getSmAddress();
		String pk = smartContractProperties.getExecute().getFromPrivate();
		byte[] sourceByte = Converter.decodeFromBASE58(source);
		byte[] targetByte = Converter.decodeFromBASE58(target);

		Date now = new Date();

		Fee maxFee = new Fee(new BigDecimal("1"));

		try {
			TTransport transport = new TSocket(smartContractProperties.getExecute().getNodeAddress(),
					smartContractProperties.getExecute().getNodePort());
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
				for (int i = 0; i < smartContractProperties.getExecute().getNbExecution(); i++) {
					NodeApiService nodeApiService = NodeApiServiceImpl.getInstance(
							smartContractProperties.getExecute().getNodeAddress(),
							smartContractProperties.getExecute().getNodePort());
					SmartContractData smartContractData = nodeApiService.getSmartContract(target);
					String code = smartContractData.getSmartContractDeployData().getSourceCode();

					List<Variant> params = new ArrayList<Variant>();
					smartContractProperties.getExecute().getParams().forEach( p -> {
						Variant param = new Variant();
						if("STRING".equals(p.getType())) {
							String hachString ="";
							for (int j=0; j<500;j++) {
								hachString+=p.getValue()+"\n";
							}
							param.setFieldValue(Variant._Fields.V_STRING, hachString);
//							param.setFieldValue(Variant._Fields.V_STRING, p.getValue());
						} else if("INTEGER".equals(p.getType())) {
							param.setFieldValue(Variant._Fields.V_INT, new Integer(p.getValue()));
						} else if("DOUBLE".equals(p.getType())) {
							param.setFieldValue(Variant._Fields.V_DOUBLE, new Double(p.getValue()));
						} else if("BOOLEAN".equals(p.getType())) {
							param.setFieldValue(Variant._Fields.V_BOOLEAN, new Boolean(p.getValue()));
						} else if("FLOAT".equals(p.getType())) {
							param.setFieldValue(Variant._Fields.V_FLOAT, new Double(p.getValue()));
						} else if("LONG".equals(p.getType())) {
							param.setFieldValue(Variant._Fields.V_LONG, new Long(p.getValue()));
						}
						params.add(param);
					});
					String methodName = smartContractProperties.getExecute().getMethod();

					smartContractData.setGetterMethod(false);
					smartContractData.setMethod(methodName);
					smartContractData.setParams(params);

					SmartContractInvocationData smartContractInvocData = new SmartContractInvocationData(
							smartContractData.getSmartContractDeployData(), methodName, params, null, smartContractProperties.getExecute().getRunLocally());
					byte[] smartContractBytes = NodeClientUtils.serializeByThrift(smartContractInvocData);

					TransactionFlowData tStruct = new TransactionFlowData(id, sourceByte, targetByte, BigDecimal.ZERO,
							maxFee.getFee(), smartContractBytes, null);

					byte[] privateKeyByteArr1;
					privateKeyByteArr1 = Converter.decodeFromBASE58(pk);
					PrivateKey privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr1);
					SignUtils.signTransaction(tStruct, privateKey);

//		        TransactionFlowData transactionData=new TransactionFlowData(id, source, target, amount, offeredMaxFee16Bits, currency, smartContractBytes, commentBytes, signature);
//
					LOGGER.info(Converter.bytesToHex(tStruct.getSignature()));
					SmartContractTransactionFlowData smartContractDataTrxFlow = new SmartContractTransactionFlowData(
							tStruct, smartContractInvocData);
					smartContractDataTrxFlow.setType(TransactionTypeData.TT_SmartExecute);
					LOGGER.info("send transaction");
					TransactionFlowResultData result = nodeApiService
							.smartContractTransactionFlow(smartContractDataTrxFlow);
					LOGGER.info("wait response");
					Optional<Variant> var = result.getContractResult();
					//String strVar = var.get().getV_string();
					//LOGGER.info(strVar);
					LOGGER.info("end response");
					id++;
					// nodeApiService.transactionFlow(smartContractDataTrxFlow);
				}
				transport.close();
				LOGGER.info("thread  ended");
			}
		} catch (Throwable e) {
			LOGGER.error("erreur in sending ", e);
		}
	}

	public void setSmartContractProperties(SmartContractProperties smartContractProperties) {
		this.smartContractProperties = smartContractProperties;
	}

}
