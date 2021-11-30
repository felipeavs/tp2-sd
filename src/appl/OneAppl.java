package appl;
import core.Message;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class OneAppl {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new OneAppl(true);
	}

	public OneAppl(boolean flag){

		// String hostBroker = "10.128.0.20";
		// String hostClient = "10.128.0.20";
		String hostBroker = "localhost";
		String hostClient = "localhost";

		PubSubClient clientA = new PubSubClient(hostClient, 8081);
		PubSubClient clientB = new PubSubClient(hostClient, 8082);
		PubSubClient clientC = new PubSubClient(hostClient, 8083);

		clientA.subscribe(hostBroker, 8080);
		clientB.subscribe(hostBroker, 8080);
		clientC.subscribe(hostBroker, 8080);

		Thread accessOne = new requestAcquire(clientA, "ClientA",  "-acquire-", "X", hostBroker, 8080);
		Thread accessTwo = new requestAcquire(clientB, "ClientB",  "-acquire-", "X", hostBroker, 8080);
		Thread accessThree = new requestAcquire(clientC, "ClientC",  "-acquire-", "X", hostBroker, 8080);

		int seconds = (int) (Math.random()*(10000 - 1000)) + 1000;
		System.out.println("Starting in " + seconds/1000 + " seconds...\n");
		try {
			Thread.currentThread().sleep(seconds);
		}catch (InterruptedException e) {
			e.printStackTrace();
		}


		accessOne.start();
		accessTwo.start();
		accessThree.start();

		try{
			accessOne.join();
			accessTwo.join();
			accessThree.join();
		}catch (Exception ignored){}

		clientA.unsubscribe(hostBroker, 8080);
		clientB.unsubscribe(hostBroker, 8080);
		clientC.unsubscribe(hostBroker, 8080);

		clientA.stopPubSubClient();
		clientB.stopPubSubClient();
		clientC.stopPubSubClient();
	}
}

class requestAcquire extends Thread {
	PubSubClient client;
	String clientName;
	String action;
	String resource;
	String hostBroker;
	int portBroker;

	public requestAcquire(PubSubClient client, String clientName, String action, String resource, String hostBroker, int portBroker) {
		this.client = client;
		this.clientName = clientName;
		this.action = action;
		this.resource = resource;
		this.hostBroker = hostBroker;
		this.portBroker = portBroker;
	}

	public void run() {
		Thread access = new ThreadWrapper(client, clientName.concat(action).concat(resource), hostBroker, portBroker);
		access.start();

		try {
			access.join();
		} catch (Exception ignored) {}


		List<Message> logs = client.getLogMessages();

		//Vetor que guarda todos os aquires do log
		List<String> acquires = new ArrayList<String>();

		//Varredura do log e capturando todas as mensagens que tiverem a mensagem acquire
		Iterator<Message> it = logs.iterator();
		while(it.hasNext()){
			Message log = it.next();
			String content = log.getContent();
			if (content.contains("-acquire-")){
				acquires.add(content);
			}
		}

		System.out.print("\nORDEM DE CHEGADA MANTIDA PELO BROKER: " + acquires + " \n");

		while (!acquires.isEmpty()){

			String firstClient = acquires.get(0);
			//recurso liberado
			boolean hasRelease = false;

			//Enquanto o recurso não for liberado
			while(!hasRelease){
				int randomInterval = getRandomInteger(1000, 10000);
				//Pega o primeiro cliente da lista de qcquire, contem o Meu nome?
				// Se o cliente é o mesmo que está tratando o log
				if(firstClient.contains(clientName)){
					try {
						access = new ThreadWrapper(client, "useX", hostBroker, 8080);
						access.start();
						try {
							access.join();
						} catch (Exception ignored) {}

						System.out.println("___________________________");
						System.out.println(firstClient.split("-")[0] + " pegou o recurso X");

						System.out.println("Aguardando " + randomInterval/1000 + " segundos...\n");
						Thread.currentThread().sleep(randomInterval);

						access = new ThreadWrapper(client, clientName.concat(":release:X"), hostBroker, 8080);
						access.start();
						hasRelease = true;
						try {
							access.join();
						} catch (Exception ignored) {}
					}catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				try{
					Thread.currentThread().sleep(randomInterval);
					hasRelease = true;
				}catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (!acquires.isEmpty()){
				acquires.remove(0);
			}
		}
	}

	public int getRandomInteger(int minimum, int maximum){
		return ((int) (Math.random()*(maximum - minimum))) + minimum;
	}

}

class ThreadWrapper extends Thread{
	PubSubClient c;
	String msg;
	String host;
	int port;

	public ThreadWrapper(PubSubClient c, String msg, String host, int port){
		this.c = c;
		this.msg = msg;
		this.host = host;
		this.port = port;
	}

	public void run(){
		c.publish(msg, host, port);
	}
}


/*

package appl;

import java.util.Iterator;
import java.util.List;

import core.Message;

public class OneAppl {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new OneAppl(true);
	}
	
	public OneAppl(){
		PubSubClient client = new PubSubClient();
		client.startConsole();
	}
	
	public OneAppl(boolean flag){
		PubSubClient joubert = new PubSubClient("localhost", 8082);
		PubSubClient debora = new PubSubClient("localhost", 8083);
		PubSubClient jonata = new PubSubClient("localhost", 8084);
		
		joubert.subscribe("localhost", 8080);
		Thread accessOne = new ThreadWrapper(joubert, "Joubert:acquire:var X", "localhost", 8080);
		
		debora.subscribe("localhost", 8080);
		jonata.subscribe("localhost", 8080);
						
		Thread accessTwo = new ThreadWrapper(debora, "Debora:acquire:var X", "localhost", 8080);
		Thread accessThree = new ThreadWrapper(jonata, "Jonata:acquire:var X", "localhost", 8080);
		accessOne.start();
		accessTwo.start();
		accessThree.start();
		
		try{
			accessTwo.join();
			accessOne.join();
			accessThree.join();
		}catch (Exception e){
			
		}
		
		List<Message> logJoubert = joubert.getLogMessages();
		List<Message> logDebora = debora.getLogMessages();
		List<Message> logJonata = jonata.getLogMessages();
		
		Iterator<Message> it = logJoubert.iterator();
		System.out.print("Log Joubert itens: ");
		while(it.hasNext()){
			Message aux = it.next();
			System.out.print(aux.getContent() + aux.getLogId() + " | ");
		}
		System.out.println();
		
		it = logJonata.iterator();
		System.out.print("Log Jonata itens: ");
		while(it.hasNext()){
			Message aux = it.next();
			System.out.print(aux.getContent() + aux.getLogId() + " | ");
		}
		System.out.println();
		
		it = logDebora.iterator();
		System.out.print("Log Debora itens: ");
		while(it.hasNext()){
			Message aux = it.next();
			System.out.print(aux.getContent() + aux.getLogId() + " | ");
		}
		System.out.println();

		Thread accessFour = new ThreadWrapper(debora, "Debora:release:var X", "localhost", 8080);
		accessFour.start();

		try{
			accessFour.join();
		}catch (Exception e){

		}

		logJoubert = joubert.getLogMessages();
		logDebora = debora.getLogMessages();
		logJonata = jonata.getLogMessages();

		it = logJoubert.iterator();
		System.out.print("Log Joubert itens - 2nd time: ");
		while(it.hasNext()){
			Message aux = it.next();
			System.out.print(aux.getContent() + aux.getLogId() + " | ");
		}
		System.out.println();

		it = logJonata.iterator();
		System.out.print("Log Jonata itens - 2nd time: ");
		while(it.hasNext()){
			Message aux = it.next();
			System.out.print(aux.getContent() + aux.getLogId() + " | ");
		}
		System.out.println();

		it = logDebora.iterator();
		System.out.print("Log Debora itens - 2nd time: ");
		while(it.hasNext()){
			Message aux = it.next();
			System.out.print(aux.getContent() + aux.getLogId() + " | ");
		}
		System.out.println();
		
		joubert.unsubscribe("localhost", 8080);
		debora.unsubscribe("localhost", 8080);
		jonata.unsubscribe("localhost", 8080);
		
		joubert.stopPubSubClient();
		debora.stopPubSubClient();
		jonata.stopPubSubClient();
	}
	
	class ThreadWrapper extends Thread{
		PubSubClient c;
		String msg;
		String host;
		int port;
		
		public ThreadWrapper(PubSubClient c, String msg, String host, int port){
			this.c = c;
			this.msg = msg;
			this.host = host;
			this.port = port;
		}
		public void run(){
			c.publish(msg, host, port);
		}
	}

}
*/