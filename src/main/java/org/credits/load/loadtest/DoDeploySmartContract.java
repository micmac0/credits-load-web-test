package org.credits.load.loadtest;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.credits.load.loadtest.util.Fee;
import org.credits.load.loadtest.util.SmartContractProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.credits.client.node.crypto.Ed25519;
import com.credits.client.node.pojo.SmartContractDeployData;
import com.credits.client.node.pojo.SmartContractInvocationData;
import com.credits.client.node.pojo.SmartContractTransactionFlowData;
import com.credits.client.node.pojo.TokenStandartData;
import com.credits.client.node.pojo.TransactionFlowData;
import com.credits.client.node.pojo.TransactionFlowResultData;
import com.credits.client.node.service.NodeApiService;
import com.credits.client.node.service.NodeApiServiceImpl;
import com.credits.client.node.thrift.generated.API;
import com.credits.client.node.thrift.generated.API.Client;
import com.credits.client.node.thrift.generated.API.Client.Factory;
import com.credits.client.node.thrift.generated.WalletTransactionsCountGetResult;
import com.credits.client.node.util.NodeClientUtils;
import com.credits.client.node.util.SignUtils;
import com.credits.general.classload.ByteCodeContractClassLoader;
import com.credits.general.pojo.ByteCodeObjectData;
import com.credits.general.thrift.generated.Variant;
import com.credits.general.util.GeneralConverter;
import com.credits.general.util.compiler.InMemoryCompiler;
import com.credits.general.util.compiler.model.CompilationPackage;
import com.credits.wallet.desktop.utils.SmartContractsUtils;
import com.credits.wallet.desktop.utils.sourcecode.SourceCodeUtils;
import com.credits.wallet.desktop.utils.sourcecode.building.CompilationResult;
import com.credits.wallet.desktop.utils.sourcecode.building.SourceCodeBuilder;

@Component
@Scope("prototype")
public class DoDeploySmartContract implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoDeploySmartContract.class);

	SmartContractProperties smartContractProperties;

	@Override
	public void run() {

		try {
			doSend();
		} catch (Exception e) {
			LOGGER.error("error", e);
		}
	}

	private void doSend()  {
		Integer maxCount = 0;
		Long id = 0L;

		String source = smartContractProperties.getDeploy().getFromPublic();
		byte[] sourceByte = GeneralConverter.decodeFromBASE58(source);
		String pk = smartContractProperties.getDeploy().getFromPrivate();
		String smartContractSourceFile = smartContractProperties.getDeploy().getSourceFile();

		Date now = new Date();

		Fee maxFee = new Fee(new BigDecimal("1"));

		try {
			
			String smartContractSourceCode = SourceCodeUtils.normalizeSourceCode(new String(Files.readAllBytes(Paths.get(smartContractSourceFile))));
			
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
					NodeApiService nodeApiService = NodeApiServiceImpl.getInstance(
						smartContractProperties.getExecute().getNodeAddress(),
						smartContractProperties.getExecute().getNodePort());
			        
					long transactionId = id;	
			        String method = new String();
					List<Variant> params = new ArrayList<Variant>();
					InMemoryCompiler compiler = new InMemoryCompiler();
					CompilationPackage compilationPackage = compiler.compileSourceCode(smartContractSourceCode);
					CompilationResult compileResult = SourceCodeBuilder.compileSmartSourceCode(smartContractSourceCode);


					
                    List<ByteCodeObjectData> byteCodeObjectDataList =
                            GeneralConverter.compilationPackageToByteCodeObjects(compilationPackage);

                        Class<?> contractClass = compileSmartContractByteCode(byteCodeObjectDataList);
                        

                        SmartContractDeployData smartContractDeployData =
                            new SmartContractDeployData(smartContractSourceCode, byteCodeObjectDataList, TokenStandartData.NotAToken);

			       
			       // SmartContractDeployData smartContractDeployData = new SmartContractDeployData(smartContractSourceCode, null, TokenStandartData.CreditsBasic);
			        byte[] smAddress = SmartContractsUtils.generateSmartContractAddress(sourceByte, transactionId, byteCodeObjectDataList);

					SmartContractInvocationData scData = new SmartContractInvocationData(smartContractDeployData, method, params, null, false);
			        byte[] smartContractBytes = NodeClientUtils.serializeByThrift(scData);
			        TransactionFlowData tStruct = new TransactionFlowData(transactionId, sourceByte, smAddress, BigDecimal.ZERO, 
			        		maxFee.getFee(),smartContractBytes, null);
			        
					
			        byte[] privateKeyByteArr1;
					privateKeyByteArr1 = GeneralConverter.decodeFromBASE58(pk);
					PrivateKey privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr1);
					SignUtils.signTransaction(tStruct, privateKey);
			        

					//LOGGER.info(Converter.bytesToHex(tStruct.getSignature()));
					LOGGER.info(scData.getSmartContractDeployData().getSourceCode());
			        SmartContractTransactionFlowData smartContractFlowData = new SmartContractTransactionFlowData(tStruct,scData);


			        
			        TransactionFlowResultData result = nodeApiService.smartContractTransactionFlow(smartContractFlowData);
			        LOGGER.info(result.getMessage());

				transport.close();
				LOGGER.info("thread  ended");
			}
		} catch (Throwable e) {
			LOGGER.error("erreur in sending ", e);
		}
	}
	
    private static Class<?> compileSmartContractByteCode(List<ByteCodeObjectData> smartContractByteCodeData) {
        ByteCodeContractClassLoader classLoader = new ByteCodeContractClassLoader();
        Class<?> contractClass = null;
        for (ByteCodeObjectData compilationUnit : smartContractByteCodeData) {
            Class<?> tempContractClass = classLoader.loadClass(compilationUnit.getName(), compilationUnit.getByteCode());
            if (!compilationUnit.getName().contains("$")) {
                contractClass = tempContractClass;
            }
        }
        return contractClass;
    }

	public void setSmartContractProperties(SmartContractProperties smartContractProperties) {
		this.smartContractProperties = smartContractProperties;
	}

}
