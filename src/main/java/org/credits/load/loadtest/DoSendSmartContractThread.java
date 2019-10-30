package org.credits.load.loadtest;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.credits.load.loadtest.util.Fee;
import org.credits.load.loadtest.util.SmartContractProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.credits.client.node.crypto.Ed25519;
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

import com.credits.general.thrift.generated.Variant;
import com.credits.general.util.GeneralConverter;


@Component
@Scope("prototype")
public class DoSendSmartContractThread implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoSendSmartContractThread.class);

	SmartContractProperties smartContractProperties;

	private int counter;

	@Override
	public void run() {

		try {
			doSend();
		} catch (Exception e) {
			LOGGER.error("error", e);
		}
	}

	private void doSend() {
		Integer maxCount = 0;
		Long id = 0L;

		String source = smartContractProperties.getExecutors().get(counter).getFromPublic();
		String target = smartContractProperties.getExecutors().get(counter).getSmAddress();
		String pk = smartContractProperties.getExecutors().get(counter).getFromPrivate();
		

//		if(this.counter==1) {
//			source="77AKUt793QSHDsBw3dajJmFiWNsPFs8nQeKpz4hZsS5A";
//			pk = "4FMsncuC3EeLLi7J5BtnrUJX2WRUTk5yfprSKWhF3hPcZUDRpDJhtdaXFk4ZxrvUiYkWwCYCVHRnp1nQRYoHR2zx";
//		} else if(this.counter==2) {
//			source="CKZ7wL4Pp8t8deUUFD4WzrEb2G3EycGbJdnJbpX9XWFj";
//			pk = "3jmdWb12G5CrwQxH4KesFujUyz7ZLz5KFAegK4ZxUdMThr65ugWkrJ8K8w4PhtYALnV1U4F4rhpH6H9cZh25biPj";
//		}
		
		byte[] sourceByte = GeneralConverter.decodeFromBASE58(source);
		byte[] targetByte = GeneralConverter.decodeFromBASE58(target);

		Date now = new Date();

		Fee maxFee = new Fee(new BigDecimal("10"));

		try {
			TTransport transport = new TSocket(smartContractProperties.getExecutors().get(counter).getNodeAddress(),
					smartContractProperties.getExecutors().get(counter).getNodePort());
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
				NodeApiService nodeApiService = NodeApiServiceImpl.getInstance(
						smartContractProperties.getExecutors().get(counter).getNodeAddress(),
						smartContractProperties.getExecutors().get(counter).getNodePort());
				for (int i = 0; i < smartContractProperties.getExecutors().get(counter).getNbExecution(); i++) {
					try {
					
					SmartContractData smartContractData = nodeApiService.getSmartContract(target);
					String code = smartContractData.getSmartContractDeployData().getSourceCode();

					List<Variant> params = new ArrayList<Variant>();
					smartContractProperties.getExecutors().get(counter).getParams().forEach( p -> {
						Variant param = new Variant();
						if("STRING".equals(p.getType())) {

							param.setFieldValue(Variant._Fields.V_STRING, p.getValue());
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
					String methodName = smartContractProperties.getExecutors().get(counter).getMethod();

					smartContractData.setGetterMethod(false);
					smartContractData.setMethod(methodName);
					smartContractData.setParams(params);

					SmartContractInvocationData smartContractInvocData = new SmartContractInvocationData(
							smartContractData.getSmartContractDeployData(), methodName, params, null, smartContractProperties.getExecutors().get(counter).getRunLocally());
					
					if(smartContractInvocData.getUsedContracts() == null) {
						smartContractInvocData.setUsedContracts(new ArrayList<ByteBuffer>());
					}
					byte[] smartContractBytes = NodeClientUtils.serializeByThrift(smartContractInvocData);

					TransactionFlowData tStruct = new TransactionFlowData(id, sourceByte, targetByte, BigDecimal.ZERO,
							maxFee.getFee(), smartContractBytes, null, null);

					byte[] privateKeyByteArr1;
					privateKeyByteArr1 = GeneralConverter.decodeFromBASE58(pk);
					PrivateKey privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr1);
					SignUtils.signTransaction(tStruct, privateKey);

//		        TransactionFlowData transactionData=new TransactionFlowData(id, source, target, amount, offeredMaxFee16Bits, currency, smartContractBytes, commentBytes, signature);
//

					SmartContractTransactionFlowData smartContractDataTrxFlow = new SmartContractTransactionFlowData(
							tStruct, smartContractInvocData);
					smartContractDataTrxFlow.setType(TransactionTypeData.TT_SmartExecute);
					LOGGER.info("send transaction");
					
					TransactionFlowResultData result = nodeApiService
							.smartContractTransactionFlow(smartContractDataTrxFlow);

					Optional<Variant> var = result.getContractResult();
					
					//String strVar = var.get().getV_string();
					//LOGGER.info(strVar);
					if(var.isPresent())
						LOGGER.info("Thread : "+Thread.currentThread().getId()+" : "+result.getMessage()+"\nResult from smartcontract : "+var.get().toString());
					
					}catch(Exception e) {
						LOGGER.error("erreur in sending ", e);
					}
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

	public void setCallNumber(int counter) {
		this.counter = counter;
		
	}

}
