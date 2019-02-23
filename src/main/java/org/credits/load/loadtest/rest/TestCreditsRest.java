package org.credits.load.loadtest.rest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.credits.load.loadtest.rest.beans.BalanceRest;
import org.credits.load.loadtest.util.NodesProperties;
import org.credits.load.loadtest.util.NodesProperties.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.credits.client.node.thrift.generated.API;
import com.credits.client.node.thrift.generated.API.Client;
import com.credits.client.node.thrift.generated.API.Client.Factory;
import com.credits.client.node.thrift.generated.WalletBalanceGetResult;
import com.credits.common.utils.Converter;

@RestController
@RequestMapping("/test")
public class TestCreditsRest {
	public static final Logger LOGGER = LoggerFactory.getLogger(TestCreditsRest.class);

	@Autowired
	private NodesProperties nodesProperties;

	@RequestMapping(value = "/balances", method = RequestMethod.GET)
	public ResponseEntity<List<BalanceRest>> getBalances() {
		List<BalanceRest> balancesList = new ArrayList<>();

		try {

			TTransport transport = new TSocket(nodesProperties.getNodes().get(0).getAddress(),
					nodesProperties.getNodes().get(0).getPort());
			Factory clientFactory = new Client.Factory();
			TProtocol protocol = new TBinaryProtocol(transport);
			API.Client client = clientFactory.getClient(protocol);
			transport.open();
			if (transport.isOpen()) {
				Iterator<Node> nodeIt = nodesProperties.getNodes().iterator();
				while (nodeIt.hasNext()) {
					String source = nodeIt.next().getFromPublicKey();
					byte[] sourceByte = Converter.decodeFromBASE58(source);
					BalanceRest balanceRest = new BalanceRest();
					WalletBalanceGetResult balanceGet = client.WalletBalanceGet(ByteBuffer.wrap(sourceByte));
					balanceRest.setPublicKey(source);
					balanceRest.setAmount(Converter.amountToBigDecimal(balanceGet.balance));
					balancesList.add(balanceRest);
				}
			}
		} catch (Throwable e) {
			LOGGER.error("can t get balances", e);
		}
		return new ResponseEntity<List<BalanceRest>>(balancesList, HttpStatus.OK);

	}

}
