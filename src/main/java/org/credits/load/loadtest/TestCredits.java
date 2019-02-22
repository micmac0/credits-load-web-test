package org.credits.load.loadtest;

import org.credits.load.loadtest.services.SendCreditsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableScheduling
public class TestCredits implements CommandLineRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCredits.class);

	@Autowired
	SendCreditsService sendCreditsService;

	public static void main(String[] args) {
		// launch app
		SpringApplication.run(TestCredits.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		// TODO Auto-generated method stub
		LOGGER.info("Hey start APP");
		// doSend();
		sendCreditsService.executeAsynchronously();

	}

	// // }
	// // @SpringBootApplication
	// // @RestController
	// // @EnableScheduling
	// // public class TestCredits implements CommandLineRunner {
	//
	// //
	//
	// //
	// // @Override
	// // public void run(String... args) throws Exception {
	// //
	// // // TODO Auto-generated method stub
	// // LOGGER.info("Hey start APP");
	// // // test2();
	// //
	// // // SpringApplication.exit(null, null);
	// // }
	// //
	// private static void doSend() throws LevelDbClientException, CreditsCommonException, TTransportException {
	// TSocket socket = new TSocket("beltegeuse", 9090);
	// TProtocol prot = new TBinaryProtocol(socket);
	// API.Client client = new API.Client(prot);
	//
	// byte[] sourceByte = Converter.decodeFromBASE58(source);
	// byte[] targetByte = Converter.decodeFromBASE58(target);
	// long id = new Date().getTime();
	// BigDecimal AmountDecimal = new BigDecimal("0.01");
	// Amount amount = Converter.bigDecimalToAmount(AmountDecimal);
	// Amount balance = Converter.bigDecimalToAmount(new BigDecimal("0.01"));
	// byte currency = 1;
	//
	// AmountCommission fee = new AmountCommission((short) 18431);
	// long timeCreation = new Date().getTime();
	// TransactionType type = null; // TransactionType.TT_Normal;
	// Transaction transaction = new Transaction();
	// transaction.setId(id);
	// transaction.setAmount(amount);
	// transaction.setBalance(balance);
	// transaction.setCurrency(currency);
	//
	// transaction.setFee(fee);
	// transaction.setTimeCreation(timeCreation);
	// transaction.setType(type);
	//
	// transaction.setSource(ByteBuffer.wrap(sourceByte));
	// transaction.setTarget(ByteBuffer.wrap(targetByte));
	//
	// try {
	// socket.open();
	// WalletTransactionsCountGetResult transId = client.WalletTransactionsCountGet(ByteBuffer.wrap(sourceByte));
	// if (transId != null)
	// id = transId.lastTransactionInnerId + 1;
	// else
	// id = 1;
	// transaction.setId(id);
	//
	// byte[] privateKeyByteArr1;
	// privateKeyByteArr1 = Converter.decodeFromBASE58(pk);
	// PrivateKey privateKey = Ed25519.bytesToPrivateKey(privateKeyByteArr1);
	// TransactionStruct tStruct = new TransactionStruct(id, source, target, AmountDecimal, (short) 18431, currency, null);
	// System.out.println(Converter.bytesToHex(tStruct.getBytes()));
	// byte[] signature2 = Ed25519.sign(tStruct.getBytes(), privateKey);
	// transaction.setSignature(signature2);
	//
	// TransactionFlowResult res = client.TransactionFlow(transaction);
	// String msgHexa = res.getStatus().message.split(" ")[1];
	// System.out.println(res.roundNum);
	// System.out.println(msgHexa);
	// System.out.println(res.getStatus().getCode());
	//
	// } catch (Throwable e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } finally {
	// socket.close();
	// }
	// }

}
