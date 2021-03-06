package pl.edu.pw.elka.community.finding.application.model.algoritms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import pl.edu.pw.elka.community.finding.algorithms.FastNewman;
import pl.edu.pw.elka.community.finding.algorithms.GrivanNewman;
import pl.edu.pw.elka.community.finding.algorithms.Louvain;
import pl.edu.pw.elka.community.finding.application.controller.events.Event;
import pl.edu.pw.elka.community.finding.application.controller.events.EventName;
import pl.edu.pw.elka.community.finding.application.controller.events.EventsBlockingQueue;
import pl.edu.pw.elka.community.finding.application.model.graph.structure.Edge;
import pl.edu.pw.elka.community.finding.application.model.graph.structure.Node;
import pl.edu.pw.elka.community.finding.application.model.tests.Output;
import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.algorithms.cluster.VoltageClusterer;
import edu.uci.ics.jung.graph.Graph;

/**
 * Manager for community finding algorithms. Manages tests, chose correct parameters etc.
 * 
 * @author Wojciech Kaczorowski
 * 
 */
public class AlgorithmManager {

	/**
	 * Type of algorithm actually use.
	 */
	private AlgorithmType algorithmType;

	private EventsBlockingQueue blockingQueue;

	public AlgorithmManager(EventsBlockingQueue blockingQueue) {
		this.blockingQueue = blockingQueue;
	}

	/**
	 * Method managing work for algorithm.
	 * 
	 * @param graph
	 * @param param
	 * @return
	 */
	public int computeSingle(Graph<Node, Edge> graph, int param) {
		int numberGroups;
		long time = System.currentTimeMillis();
		System.out.println(algorithmType);
		switch (algorithmType) {
		case LOUVAIN:
			numberGroups = louvain(graph);
			break;

		case GRIVAN_NEWMAN:
			numberGroups = grivanNewman(graph, param);
			break;

		case IMPROVED_GRIVAN_NEWMAN:
			numberGroups = improvedGrivanNewman(graph, param);
			break;

		case WU_HUBERMAN:
			numberGroups = wuHuberman(graph, param);
			break;

		case FAST_NEWMAM:
			numberGroups = fastNewman(graph);
			break;

		default:
			numberGroups = 0;
			break;
		}

		long timeTotal = System.currentTimeMillis() - time;
		System.out.println("Calculation time: " + timeTotal + " ms");
		blockingQueue.add(new Event(EventName.REFRESH_VIEW));
		return numberGroups;
	}

	public Collection<Output> computeAll(Properties properties, Graph<Node, Edge> graph) {
		ArrayList<Output> outputs = new ArrayList<Output>(4);

		System.out.println("LV:" + properties);
		Louvain<Node, Edge> louvain = new Louvain<Node, Edge>(null);
		Output louvainOut = new Output();
		long louvainTime = System.currentTimeMillis();
		louvainOut.setCommunities(louvain.getCommunities(graph));
		louvainOut.setTime(System.currentTimeMillis() - louvainTime);
		louvainOut.addProperty("algorithmType", "LV");
		louvainOut.addProperty(properties);
		louvainOut.calculateModularity(graph);
		outputs.add(louvainOut);

		System.out.println("FN:" + properties);
		FastNewman<Node, Edge> fastNewman = new FastNewman<Node, Edge>();
		Output fastNewmanOut = new Output();
		long fastNewmanTime = System.currentTimeMillis();
		fastNewmanOut.setCommunities(fastNewman.getCommunities(graph));
		fastNewmanOut.setTime(System.currentTimeMillis() - fastNewmanTime);
		fastNewmanOut.addProperty("algorithmType", "FN");
		fastNewmanOut.addProperty(properties);
		fastNewmanOut.calculateModularity(graph);
		outputs.add(fastNewmanOut);

		int clusterCandidates = (int) Math.ceil((louvainOut.getCommunities().size() + fastNewmanOut.getCommunities().size()) / 2.0);
		if (properties.get("comm") != null) {
			clusterCandidates = Integer.valueOf((String) properties.get("comm"));
		}
		System.out.println("WH:" + properties);
		VoltageClusterer<Node, Edge> wuHuberman = new VoltageClusterer<Node, Edge>(graph, clusterCandidates);
		Output wuHubermanOut = new Output();
		long wuHubermanTime = System.currentTimeMillis();
		wuHubermanOut.setCommunities(wuHuberman.cluster(clusterCandidates));
		wuHubermanOut.setTime(System.currentTimeMillis() - wuHubermanTime);
		wuHubermanOut.addProperty("algorithmType", "WH");
		wuHubermanOut.addProperty(properties);
		wuHubermanOut.calculateModularity(graph);
		outputs.add(wuHubermanOut);

		System.out.println("GN:" + properties);
		GrivanNewman<Node, Edge> grivanNewman = new GrivanNewman<Node, Edge>(clusterCandidates);
		Output grivanNewmanOut = new Output();
		long grivanNewmanTime = System.currentTimeMillis();
		grivanNewmanOut.setCommunities(grivanNewman.getCommunities(graph));
		grivanNewmanOut.setTime(System.currentTimeMillis() - grivanNewmanTime);
		grivanNewmanOut.addProperty("algorithmType", "GN");
		grivanNewmanOut.addProperty(properties);
		grivanNewmanOut.calculateModularity(graph);
		outputs.add(grivanNewmanOut);

		return outputs;
	}

	private int louvain(Graph<Node, Edge> graph) {
		Louvain<Node, Edge> algorithm = new Louvain<Node, Edge>(null);
		int groupCounter = 0;
		for (Set<Node> set : algorithm.getCommunities(graph)) {
			for (Node n : set) {
				n.setGroup(String.valueOf(groupCounter));
			}
			++groupCounter;
		}
		return groupCounter;

	}

	private int improvedGrivanNewman(Graph<Node, Edge> graph, int groupsNumber) {
		GrivanNewman<Node, Edge> algorithm = new GrivanNewman<>(groupsNumber);
		int groupCounter = 0;
		for (Set<Node> set : algorithm.getCommunities(graph)) {
			for (Node n : set) {
				n.setGroup(String.valueOf(groupCounter));
			}
			++groupCounter;
		}
		return groupCounter;
	}

	private int grivanNewman(Graph<Node, Edge> graph, int numEdgesToRemove) {
		EdgeBetweennessClusterer<Node, Edge> algorithm = new EdgeBetweennessClusterer<Node, Edge>(numEdgesToRemove);
		int groupCounter = 0;
		for (Set<Node> set : algorithm.transform(graph)) {
			for (Node n : set) {
				n.setGroup(String.valueOf(groupCounter));
			}
			++groupCounter;
		}
		return groupCounter;
	}

	private int wuHuberman(Graph<Node, Edge> graph, int clusterCandidates) {
		VoltageClusterer<Node, Edge> algorithm = new VoltageClusterer<Node, Edge>(graph, clusterCandidates);
		int groupCounter = 0;
		for (Set<Node> set : algorithm.cluster(clusterCandidates)) {
			for (Node n : set) {
				n.setGroup(String.valueOf(groupCounter));
			}
			++groupCounter;
		}
		return groupCounter;
	}

	private int fastNewman(Graph<Node, Edge> graph) {
		FastNewman<Node, Edge> algorithm = new FastNewman<Node, Edge>();
		int groupCounter = 0;
		for (Set<Node> set : algorithm.getCommunities(graph)) {
			for (Node n : set) {
				n.setGroup(String.valueOf(groupCounter));
			}
			++groupCounter;
		}
		return groupCounter;
	}

	public AlgorithmType getAlgorithmType() {
		return algorithmType;
	}

	public void setAlgorithmType(AlgorithmType algorithmType) {
		this.algorithmType = algorithmType;
	}

}
