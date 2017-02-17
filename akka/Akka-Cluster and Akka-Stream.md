# *Cluster*
## Contents
* [A simple cluster example](#1)
* [Terms and behaviors introduction](#2)
* [Cluster singleton](#3)
* [Cluster aware routers](#4)
* [Distributed publish subscribe](#5)
* [Cluster client](#6)
* [Remoting (codename Artery)](#7)
* [Cluster persistance with Cassandra](#8)

## <a name="1"></a>A Simple Cluster Example
###Sample
****
```scala
import akka.actor._
import akka.cluster._

object Example {
	def main(args: Array[String]): Unit = {
		val config = ConfigFactory.load("configuration")
		val system = ActorSystem("ClusterSystem", config)
		val actor = system.actorOf(Prop[MyActor],"actorName")
		actor ! Message
	}
}

```
###Configuration
****
```
akka {
  actor {
    provider = cluster
  }
  remote {
    netty.tcp {
      hostname = "127.0.0.1"
      port = 0
    }
  }
  cluster {
    seed-nodes = [
      "akka.tcp://ClusterSystem@127.0.0.1:2551",
      "akka.tcp://ClusterSystem@127.0.0.1:2552"]
  }
}
```
###Dependency
****
```
"com.typesafe.akka" %% "akka-cluster" % "2.4.16"
```
## <a name="2"></a>Terms And Behaviors Introduction
**node**

A logical member of a cluster. There could be multiple nodes on a physical machine. Defined by a hostname:port:uid tuple.

**cluster**

A set of nodes joined together through the membership service.

**leader**

A single node in the cluster that acts as the leader. Managing cluster convergence and membership state transitions.


![cluster](http://doc.akka.io/docs/akka/2.4/_images/member-states.png)

###*Member States*
****
**joining**

* transient state when joining a cluster

**weakly up**

* transient state while network split (only if akka.cluster.allow-weakly-up-members=on)

**up** 

* normal operating state

**leaving / exiting**

* states during graceful removal

**down**

* marked as down (no longer part of cluster decisions)

**removed**

tombstone state (no longer a member)

###*User Actions*
****
**join**

* join a single node to a cluster - can be explicit or automatic on startup if a node to join have been specified in the configuration

**leave**

* tell a node to leave the cluster gracefully

**down**

* mark a node as down

###*Leader Actions*
****
* joining -> up

* exiting -> removed

### Subscribe To Member Event
****
```scala
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.Actor
 
class SimpleClusterListener extends Actor {
 
  val cluster = Cluster(context.system)
 
  // subscribe to cluster changes, re-subscribe when restart 
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
    //MemberEvent = MemberJoined, MemberWeaklyUp, MemberUp, MemberLeft, MemberExited, MemberRemoved
    //ReachabilityEvent = UnreachableMember, ReachableMember
  }
  override def postStop(): Unit = cluster.unsubscribe(self)
 
  def receive = {
    case MemberUp(member) => println(member.address)
    case UnreachableMember(member) => println(member)
    case MemberRemoved(member, previousStatus) =>
      println("Member is Removed: {} after {}",
        member.address, previousStatus)
    case _: MemberEvent => // ignore
  }
}
```
###Dependencies
****
```
"com.typesafe.akka" %% "akka-cluster" % "2.4.16"
```

## <a name="3"></a>Cluster Singleton
###Sample
****
```scala
import akka.actor._
import akka.cluster._
import akka.cluster.singleton._

object Singleton {
	def main(args: Array[String]): Unit = {
		val config = ConfigFactory.load("configuration")
		val system = ActorSystem("systemName", config)

		//#create-singleton-manager
		system.actorOf(ClusterSingletonManager.props(singletonProps = 		Props[MySingleton],
      		terminationMessage = PoisonPill,
      		settings = ClusterSingletonManagerSettings(system).withRole("seed")),
      		name = "singletonActor")
		//#create-singleton-manager

		//#singleton-proxy
		val proxy = system.actorOf(ClusterSingletonProxy.props(singletonManagerPath = "/user/singletonActor",
      		settings = ClusterSingletonProxySettings(system).withRole("seed")),
      		name = "proxyActor")
		//#singleton-proxy

		proxy ! Message
	}
}
```
###Configuration
****
```
akka.cluster.singleton {
  # The actor name of the child singleton actor.
  singleton-name = "singleton"
  
  # Singleton among the nodes tagged with specified role.
  # If the role is not specified it's a singleton among all nodes in the cluster.
  role = ""
  
  # When a node is becoming oldest it sends hand-over request to previous oldest, 
  # that might be leaving the cluster. This is retried with this interval until 
  # the previous oldest confirms that the hand over has started or the previous 
  # oldest member is removed from the cluster (+ akka.cluster.down-removal-margin).
  hand-over-retry-interval = 1s
  
  # The number of retries are derived from hand-over-retry-interval and
  # akka.cluster.down-removal-margin (or ClusterSingletonManagerSettings.removalMargin),
  # but it will never be less than this property.
  min-number-of-hand-over-retries = 10
}

akka.cluster.singleton-proxy {
  # The actor name of the singleton actor that is started by the ClusterSingletonManager
  singleton-name = ${akka.cluster.singleton.singleton-name}
  
  # The role of the cluster nodes where the singleton can be deployed. 
  # If the role is not specified then any node will do.
  role = ""
  
  # Interval at which the proxy will try to resolve the singleton instance.
  singleton-identification-interval = 1s
  
  # If the location of the singleton is unknown the proxy will buffer this
  # number of messages and deliver them when the singleton is identified. 
  # When the buffer is full old messages will be dropped when new messages are
  # sent via the proxy.
  # Use 0 to disable buffering, i.e. messages will be dropped immediately if
  # the location of the singleton is unknown.
  # Maximum allowed buffer size is 10000.
  buffer-size = 1000 
}
```
###Dependency
****
```
"com.typesafe.akka" %% "akka-cluster-tools" % "2.4.16"
```
## <a name="4"></a>Cluster Aware Routers
###Sample
****
**Pool type**

```scala
import akka.cluster.routing.ClusterRouterPool
import akka.cluster.routing.ClusterRouterPoolSettings
import akka.routing.ConsistentHashingPool
 
class router extend Actor {
	val workerRouter = context.actorOf(
  		ClusterRouterPool(ConsistentHashingPool(0), ClusterRouterPoolSettings(
    	totalInstances = 100, maxInstancesPerNode = 3,
    	allowLocalRoutees = false, useRole = None)).props(Props[StatsWorker]),
  		name = "workerRouter")
}
```
**Group type**

```scala
import akka.cluster.routing.ClusterRouterGroup
import akka.cluster.routing.ClusterRouterGroupSettings
import akka.routing.ConsistentHashingGroup

class router extend Actor {
	val workerRouter = context.actorOf(
  		ClusterRouterGroup(ConsistentHashingGroup(Nil),
  		ClusterRouterGroupSettings(
    	totalInstances = 100, routeesPaths = List("/user/statsWorker"),
    	allowLocalRoutees = true, useRole = Some("compute"))).props(),
  		name = "workerRouter")
}
```
###Configuration
****
**Pool type**

```
akka.actor.deployment {
  /statsService/singleton/workerRouter {
      router = consistent-hashing-pool
      cluster {
        enabled = on
        max-nr-of-instances-per-node = 3
        allow-local-routees = on
        use-role = compute
      }
    }
}
```

**Group type**

```
akka.actor.deployment {
  /statsService/workerRouter {      
      router = consistent-hashing-group
      routees.paths = ["/user/statsWorker"]
      cluster {
        enabled = on
        allow-local-routees = on
        use-role = compute
      }
    }
}
```

## <a name="5"></a>Distributed Publish Subscribe(same cluster)

###Sample
****
```scala
import akka.actor._
import akka.cluster._
import akka.cluster.pubsub._

class PubActor extend Actor {
	import DistributedPubSubMediator._
	
	val mediator = DistributedPubSub(context.system).mediator

	def receive = {
		case msg => 
			// Send Mode
			mediator ! Send(path = "/user/destination", msg, false)

			// Publish Mode
      		mediator ! Publish("Topic", msg)
   }
}
class SubActor extend Actor {
	import DistributedPubSubMediator._
	
	val mediator = DistributedPubSub(context.system).mediator
	
	// correspond to Send Mode
	mediator ! Put(self)
	
	// correspond to Publish Mode
	mediator ! Subscribe("Topic", self)
	
	def receive = {
		case msg => println(msg)
	}
}
object PubSub {
	def main(args: Array[String]): Unit = {
		val config = ConfigFactory.load("configuration")
		val system = ActorSystem("systemName", config)
		
		val pActor = system.actorOf(Prop[PubActor],"pubActor")
		val sActor = system.actorOf(Prop[SubActor],"destination")
		
		pActor ! Message
	}
}
```
###Configuration
****
```
akka.cluster.pub-sub {
  # Actor name of the mediator actor, /system/distributedPubSubMediator
  name = distributedPubSubMediator
 
  # Start the mediator on members tagged with this role.
  # All members are used if undefined or empty.
  role = ""
 
  # The routing logic to use for 'Send'
  # Possible values: random, round-robin, broadcast
  routing-logic = random
 
  # How often the DistributedPubSubMediator should send out gossip information
  gossip-interval = 1s
 
  # Removed entries are pruned after this duration
  removed-time-to-live = 120s
 
  # Maximum number of elements to transfer in one message when synchronizing the registries.
  # Next chunk will be transferred in next round of gossip.
  max-delta-elements = 3000
  
  # The id of the dispatcher to use for DistributedPubSubMediator actors. 
  # If not specified default dispatcher is used.
  # If specified you need to define the settings of the actual dispatcher.
  use-dispatcher = ""
}
```
###Dependency
****
```
"com.typesafe.akka" %% "akka-cluster-tools" % "2.4.16"
```
## <a name="6"></a>Cluster Client(between cluster)
###Sample
****
```scala
import akka.cluster.client.{ClusterClientReceptionist, ClusterClientSettings, ClusterClient}
import akka.cluster.client.ClusterClient.{Send, SendToAll, Publish}
object ServerA {
	def main(args: Array[String]): Unit = {
		val serviceA = Node1System.actorOf(Props[Service], "serviceA")
  		ClusterClientReceptionist(Node1System).registerService(serviceA)
  	}
}
object ServerB {
	def main(args: Array[String]): Unit = {
		val serviceB = Node2System.actorOf(Props[Service], "serviceB")
  		ClusterClientReceptionist(Node2System).registerService(serviceB)
	}
}
object Client {
	def main(args: Array[String]): Unit = {
	//====== also available in configuration ======
		val initialContacts = Set(
  ActorPath.fromString("akka.tcp://OtherSys@host1:2552/system/receptionist"),
  ActorPath.fromString("akka.tcp://OtherSys@host2:2553/system/receptionist"))
  	//=============================================
		val c = Node3System.actorOf(ClusterClient.props(
    	ClusterClientSettings(Node3System).withInitialContacts(initialContacts)), "client")
  		c ! ClusterClient.Send("/user/serviceA", "hello", localAffinity = true)
  		c ! ClusterClient.SendToAll("/user/serviceB", "hi")
  		
  		c ! ClusterClient.Publish("topic", message)
	}
}
```
###Configuration
****
**Settings for the ClusterClientReceptionist extension**

```
akka.extensions = ["akka.cluster.client.ClusterClientReceptionist"]
akka.cluster.client.receptionist {
  # Actor name of the ClusterReceptionist actor, /system/receptionist
  name = receptionist
 
  # Start the receptionist on members tagged with this role.
  # All members are used if undefined or empty.
  role = ""
 
  # The receptionist will send this number of contact points to the client
  number-of-contacts = 3
 
  # The actor that tunnel response messages to the client will be stopped
  # after this time of inactivity.
  response-tunnel-receive-timeout = 30s
  
  # The id of the dispatcher to use for ClusterReceptionist actors. 
  # If not specified default dispatcher is used.
  # If specified you need to define the settings of the actual dispatcher.
  use-dispatcher = ""
 
  # How often failure detection heartbeat messages should be received for
  # each ClusterClient
  heartbeat-interval = 2s
 
  # Number of potentially lost/delayed heartbeats that will be
  # accepted before considering it to be an anomaly.
  # The ClusterReceptionist is using the akka.remote.DeadlineFailureDetector, which
  # will trigger if there are no heartbeats within the duration
  # heartbeat-interval + acceptable-heartbeat-pause, i.e. 15 seconds with
  # the default settings.
  acceptable-heartbeat-pause = 13s
 
  # Failure detection checking interval for checking all ClusterClients
  failure-detection-interval = 2s
}
```
**Settings for the ClusterClient**

```
akka.extensions = ["akka.cluster.client.ClusterClientReceptionist"]
akka.cluster.client {
  # Actor paths of the ClusterReceptionist actors on the servers (cluster nodes)
  # that the client will try to contact initially. It is mandatory to specify
  # at least one initial contact. 
  # Comma separated full actor paths defined by a string on the form of
  # "akka.tcp://system@hostname:port/system/receptionist"
  initial-contacts = ["akka.tcp://OtherSys@host1:2552/system/receptionist", 
  					"akka.tcp://OtherSys@host1:2553/system/receptionist"]
  
  # Interval at which the client retries to establish contact with one of 
  # ClusterReceptionist on the servers (cluster nodes)
  establishing-get-contacts-interval = 3s
  
  # Interval at which the client will ask the ClusterReceptionist for
  # new contact points to be used for next reconnect.
  refresh-contacts-interval = 60s
  
  # How often failure detection heartbeat messages should be sent
  heartbeat-interval = 2s
  
  # Number of potentially lost/delayed heartbeats that will be
  # accepted before considering it to be an anomaly.
  # The ClusterClient is using the akka.remote.DeadlineFailureDetector, which
  # will trigger if there are no heartbeats within the duration 
  # heartbeat-interval + acceptable-heartbeat-pause, i.e. 15 seconds with
  # the default settings.
  acceptable-heartbeat-pause = 13s
  
  # If connection to the receptionist is not established the client will buffer
  # this number of messages and deliver them the connection is established.
  # When the buffer is full old messages will be dropped when new messages are sent
  # via the client. Use 0 to disable buffering, i.e. messages will be dropped
  # immediately if the location of the singleton is unknown.
  # Maximum allowed buffer size is 10000.
  buffer-size = 1000
 
  # If connection to the receiptionist is lost and the client has not been
  # able to acquire a new connection for this long the client will stop itself.
  # This duration makes it possible to watch the cluster client and react on a more permanent
  # loss of connection with the cluster, for example by accessing some kind of
  # service registry for an updated set of initial contacts to start a new cluster client with.
  # If this is not wanted it can be set to "off" to disable the timeout and retry
  # forever.
  reconnect-timeout = off
}
```
###Dependencies
****
```
"com.typesafe.akka" %% "akka-cluster-tools" % "2.4.16"
```
## <a name="7"></a>Remoting (codename Artery)
###Configuration
****
```
akka {
	actor {
		provider = remote
   }
  remote {
    artery {
      enabled = on
      canonical.hostname = "127.0.0.1"
      canonical.port = 25520
    }
  }
```
###Dependency
****
```
"com.typesafe.akka" %% "akka-remote" % "2.4.16"
```
## <a name="8"></a>Cluster Persistance with Cassandra

###Sample
****
```
import akka.persistence._

class CassandraActor extend PersistentActor
{
	var State: Int = 0
	def receiveCommand = {
    case "snap" =>
      saveSnapshot(State)
    case e =>
      persist(e){ event =>
        State += 1
        context.system.eventStream.publish(event)
      }
   def receiveRecover = {
    case SnapshotOffer(_, snapshot: Int) =>
      State = snapshot
    case event => State += 1
}
```
###Configuration
****
```
akka.persistence.journal.plugin = "cassandra-journal"
akka.persistence.snapshot-store.plugin = "cassandra-snapshot-store"
```
###Dependencies
****
```
"com.typesafe.akka" %% "akka-persistence" % "2.4.16",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.22",
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.3",
  "com.datastax.cassandra" % "cassandra-driver-mapping" % "3.1.3",
  "com.datastax.cassandra" % "cassandra-driver-extras" % "3.1.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.16",
  "ch.qos.logback" % "logback-classic" % "1.1.8"
```

# *Stream*
## Contents
* [A simple stream example](#b1)
* [Terms and behaviors introduction](#b2)
* [Working with flows](#b3)
* [Working with graphs](#b4)
* [Integrating with actors](#b5)
* [Dynamic stream handling](#b6)

## <a name="b1"></a>A Simple Stream Example
###Sample
****
```scala
import akka.stream._
import akka.stream.scaladsl._

object Go {
	def main(args: Array[String]): Unit = {
		implicit val system = ActorSystem("QuickStart")
		implicit val materializer = ActorMaterializer()
		val source: Source[Int, NotUsed] = Source(1 to 100)
		source.runForeach(i => println(i))(materializer)
		val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
 
		val result: Future[IOResult] = factorials
    		.map(num => ByteString(s"$num\n"))
    		.runWith(FileIO.toPath(Paths.get("factorials.txt")))
	}
}
```


###Dependency
****
```
"com.typesafe.akka" %% "akka-stream" % "2.4.16",
```
## <a name="b2"></a>Terms And Behaviors Introduction
###*Concepts*
****

**Stream**

* An active process that involves moving and transforming data.

**Element**

* An element is the processing unit of streams. All operations transform and transfer elements from upstream to downstream. Buffer sizes are always expressed as number of elements independently form the actual size of the elements.

**Back-pressure**

* A means of flow-control, a way for consumers of data to notify a producer about their current availability, effectively slowing down the upstream producer to match their consumption speeds. In the context of Akka Streams back-pressure is always understood as non-blocking and asynchronous.

**Non-Blocking**

* Means that a certain operation does not hinder the progress of the calling thread, even if it takes long time to finish the requested operation.

**Graph**

* A description of a stream processing topology, defining the pathways through which elements shall flow when the stream is running.

**Processing Stage**

* The common name for all building blocks that build up a Graph. Examples of a processing stage would be operations like map(), filter(), custom GraphStage s and graph junctions like Merge or Broadcast.
 
**Stream Materialization**

* When constructing flows and graphs in Akka Streams think of them as preparing a blueprint, an execution plan. Stream materialization is the process of taking a stream description (the graph) and allocating all the necessary resources it needs in order to run. In the case of Akka Streams this often means starting up Actors which power the processing, but is not restricted to that—it could also mean opening files or socket connections etc.—depending on what the stream needs.

###*Defining and running streams*
****
**Source**

* A processing stage with exactly one output, emitting data elements whenever downstream processing stages are ready to receive them.

**Sink**

* A processing stage with exactly one input, requesting and accepting data elements possibly slowing down the upstream producer of elements
 
**Flow**

* A processing stage which has exactly one input and output, which connects its up- and downstreams by transforming the data elements flowing through it.
 
**RunnableGraph**

* A Flow that has both ends "attached" to a Source and Sink respectively, and is ready to be run().



## <a name="b3"></a>Working With Flows
###Sample
****

```scala
import akka.stream._
import akka.stream.scaladsl._

object Go {
	def main(args: Array[String]): Unit = {
		implicit val system = ActorSystem("Sys")
		implicit val materializer = ActorMaterializer()
		implicit val ec = system.dispatcher
		
		val source = Source(1 to 10)
		val sink = Sink.fold[Int, Int](0)(_ + _)
 
		// connect the Source to the Sink, obtaining a RunnableGraph
		val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)
 
		// materialize the flow and get the value of the FoldSink
		val sum: Future[Int] = runnable.run()
	}
}

```
###General usage
****

```scala
// Create a source from an Iterable
Source(List(1, 2, 3))
 
// Create a source from a Future
Source.fromFuture(Future.successful("Hello Streams!"))
 
// Create a source from a single element
Source.single("only one element")
 
// an empty source
Source.empty
 
// Sink that folds over the stream and returns a Future
// of the final result as its materialized value
Sink.fold[Int, Int](0)(_ + _)
 
// Sink that returns a Future as its materialized value,
// containing the first element of the stream
Sink.head
 
// A Sink that consumes a stream without doing anything with the elements
Sink.ignore
 
// A Sink that executes a side-effecting call for every element of the stream
Sink.foreach[String](println(_))

// Explicitly creating and wiring up a Source, Sink and Flow
Source(1 to 6).via(Flow[Int].map(_ * 2)).to(Sink.foreach(println(_)))
 
// Starting from a Source
val source = Source(1 to 6).map(_ * 2)
source.to(Sink.foreach(println(_)))
 
// Starting from a Sink
val sink: Sink[Int, NotUsed] = Flow[Int].map(_ * 2).to(Sink.foreach(println(_)))
Source(1 to 6).to(sink)
 
// Broadcast to a sink inline
val otherSink: Sink[Int, NotUsed] =
  Flow[Int].alsoTo(Sink.foreach(println(_))).to(Sink.ignore)
Source(1 to 6).to(otherSink)
```
###Combining materialized values
****
```scala
// An source that can be signalled explicitly from the outside
val source: Source[Int, Promise[Option[Int]]] = Source.maybe[Int]
 
// A flow that internally throttles elements to 1/second, and returns a Cancellable
// which can be used to shut down the stream
val flow: Flow[Int, Int, Cancellable] = throttler
 
// A sink that returns the first element of a stream in the returned Future
val sink: Sink[Int, Future[Int]] = Sink.head[Int]
 
// By default, the materialized value of the leftmost stage is preserved
val r1: RunnableGraph[Promise[Option[Int]]] = source.via(flow).to(sink)
 
// Simple selection of materialized values by using Keep.right
val r2: RunnableGraph[Cancellable] = source.viaMat(flow)(Keep.right).to(sink)
val r3: RunnableGraph[Future[Int]] = source.via(flow).toMat(sink)(Keep.right)
 
// Using runWith will always give the materialized values of the stages added
// by runWith() itself
val r4: Future[Int] = source.via(flow).runWith(sink)
val r5: Promise[Option[Int]] = flow.to(sink).runWith(source)
val r6: (Promise[Option[Int]], Future[Int]) = flow.runWith(source, sink)
 
// Using more complex combinations
val r7: RunnableGraph[(Promise[Option[Int]], Cancellable)] =
  source.viaMat(flow)(Keep.both).to(sink)
 
val r8: RunnableGraph[(Promise[Option[Int]], Future[Int])] =
  source.via(flow).toMat(sink)(Keep.both)
 
val r9: RunnableGraph[((Promise[Option[Int]], Cancellable), Future[Int])] =
  source.viaMat(flow)(Keep.both).toMat(sink)(Keep.both)
 
val r10: RunnableGraph[(Cancellable, Future[Int])] =
  source.viaMat(flow)(Keep.right).toMat(sink)(Keep.both)
 
// It is also possible to map over the materialized values. In r9 we had a
// doubly nested pair, but we want to flatten it out
val r11: RunnableGraph[(Promise[Option[Int]], Cancellable, Future[Int])] =
  r9.mapMaterializedValue {
    case ((promise, cancellable), future) =>
      (promise, cancellable, future)
  }
 
// Now we can use pattern matching to get the resulting materialized values
val (promise, cancellable, future) = r11.run()
 
// Type inference works as expected
promise.success(None)
cancellable.cancel()
future.map(_ + 3)
 
// The result of r11 can be also achieved by using the Graph API
val r12: RunnableGraph[(Promise[Option[Int]], Cancellable, Future[Int])] =
  RunnableGraph.fromGraph(GraphDSL.create(source, flow, sink)((_, _, _)) { implicit builder => (src, f, dst) =>
    import GraphDSL.Implicits._
    src ~> f ~> dst
    ClosedShape
  })
```

## <a name="b4"></a>Working With Graphs

Graphs are built from simple Flows which serve as the linear connections within the graphs as well as junctions which serve as fan-in and fan-out points for Flows. Thanks to the junctions having meaningful types based on their behaviour and making them explicit elements these elements should be rather straightforward to use.

**Fan-out**

* Broadcast[T] – (1 input, N outputs) given an input element emits to each output
* Balance[T] – (1 input, N outputs) given an input element emits to one of its output ports
* UnzipWith[In,A,B,...] – (1 input, N outputs) takes a function of 1 input that given a value for each input emits N output elements (where N <= 20)
* UnZip[A,B] – (1 input, 2 outputs) splits a stream of (A,B) tuples into two streams, one of type A and one of type B

**Fan-in**

* Merge[In] – (N inputs , 1 output) picks randomly from inputs pushing them one by one to its output
* MergePreferred[In] – like Merge but if elements are available on preferred port, it picks from it, otherwise randomly from others
* ZipWith[A,B,...,Out] – (N inputs, 1 output) which takes a function of N inputs that given a value for each input emits 1 output element
* Zip[A,B] – (2 inputs, 1 output) is a ZipWith specialised to zipping input streams of A and B into an (A,B) tuple stream
* Concat[A] – (2 inputs, 1 output) concatenates two streams (first consume one, then the second one)

###Sample
****
```scala
import akka.stream._
import akka.stream.scaladsl._

object Go {
	def main(args: Array[String]): Unit = {
		implicit val system = ActorSystem("Sys")
		implicit val materializer = ActorMaterializer()
		implicit val ec = system.dispatcher
		
		val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
		  import GraphDSL.Implicits._
			val in = Source(1 to 10)
			val out = Sink.ignore
		 
			val bcast = builder.add(Broadcast[Int](2))
			val merge = builder.add(Merge[Int](2))
		 
			val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
		 
			in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
						bcast ~> f4 ~> merge
			ClosedShape
		})
		
		g.run()
	}
}
```
###Constructing and combining Partial Graphs
****
```scala
import akka.stream._
import akka.stream.scaladsl._

object Go {
	def main(args: Array[String]): Unit = {
		implicit val system = ActorSystem("Sys")
		implicit val materializer = ActorMaterializer()
		implicit val ec = system.dispatcher
		
val pickMaxOfThree = GraphDSL.create() { implicit b =>
  import GraphDSL.Implicits._
 
  val zip1 = b.add(ZipWith[Int, Int, Int](math.max _))
  val zip2 = b.add(ZipWith[Int, Int, Int](math.max _))
  zip1.out ~> zip2.in0
 
  UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
}
 
val resultSink = Sink.head[Int]
 
val g = RunnableGraph.fromGraph(GraphDSL.create(resultSink) { implicit b => sink =>
  import GraphDSL.Implicits._
 
  // importing the partial graph will return its shape (inlets & outlets)
  val pm3 = b.add(pickMaxOfThree)
 
  Source.single(1) ~> pm3.in(0)
  Source.single(2) ~> pm3.in(1)
  Source.single(3) ~> pm3.in(2)
  pm3.out ~> sink.in
  ClosedShape
})
 
val max: Future[Int] = g.run()
Await.result(max, 300.millis) should equal(3)
	}
}
```
###Partial Graphs
****

```scala
//==========Source==========
val pairs = Source.fromGraph(GraphDSL.create() { implicit b =>
  import GraphDSL.Implicits._
 
  // prepare graph elements
  val zip = b.add(Zip[Int, Int]())
  def ints = Source.fromIterator(() => Iterator.from(1))
 
  // connect the graph
  ints.filter(_ % 2 != 0) ~> zip.in0
  ints.filter(_ % 2 == 0) ~> zip.in1
 
  // expose port
  SourceShape(zip.out)
})

val firstPair: Future[(Int, Int)] = pairs.runWith(Sink.head)
//==========Sink==========
val pairs2 = Sink.fromGraph(GraphDSL.create() { implicit b =>
  import GraphDSL.Implicits._
 
  // prepare graph elements
  val broadcast = b.add(Broadcast[Int](2))
  def endPoint = Sink.ignore
 
  // connect the graph
  broadcast.out(0).map(identity) ~> endPoint
  broadcast.out(1).map(_.toString) ~> endPoint
 
  // expose port
  SinkShape(broadcast.in)
})

val secondPair: Future[(Int, Int)] = pairs2.runWith(Source(List(1))
//==========Flow==========
val pairUpWithToString =
  Flow.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
 
    // prepare graph elements
    val broadcast = b.add(Broadcast[Int](2))
    val zip = b.add(Zip[Int, String]())
 
    // connect the graph
    broadcast.out(0).map(identity) ~> zip.in0
    broadcast.out(1).map(_.toString) ~> zip.in1
 
    // expose ports
    FlowShape(broadcast.in, zip.out)
  })
 
pairUpWithToString.runWith(Source(List(1)), Sink.head)
```
###Combining Sources and Sinks with simplified API
****
```scala
val sourceOne = Source(List(1))
val sourceTwo = Source(List(2))
val merged = Source.combine(sourceOne, sourceTwo)(Merge(_))
 
val mergedResult: Future[Int] = merged.runWith(Sink.fold(0)(_ + _))
========================================================================
val sendRmotely = Sink.actorRef(actorRef, "Done")
val localProcessing = Sink.foreach[Int](_ => /* do something usefull */ ())
 
val sink = Sink.combine(sendRmotely, localProcessing)(Broadcast[Int](_))
 
Source(List(0, 1, 2)).runWith(sink)
```

**Advanced usage**

http://doc.akka.io/docs/akka/2.4/scala/stream/stream-graphs.html

## <a name="b5"></a>Integrating With Actors

###Integrating with Actors
****
For piping the elements of a stream as messages to an ordinary actor you can use ask in a mapAsync or use Sink.actorRefWithAck.

Messages can be sent to a stream with Source.queue or via the ActorRef that is materialized by Source.actorRef.

```scala
import akka.pattern.ask
implicit val askTimeout = Timeout(5.seconds)
val words: Source[String, NotUsed] =
  Source(List("hello", "hi"))
 
words.mapAsync(parallelism = 5)(elem => (ref ? elem).mapTo[String])
  // continue processing of the replies from the actor
  .map(_.toLowerCase)
  .runWith(Sink.ignore)
  
================================================================
class Translator extends Actor {
  def receive = {
    case word: String =>
      // ... process message
      val reply = word.toUpperCase
      sender() ! reply // reply to the ask
  }
}
```
###Stream to Actor(Sink)
****
```scala
val goal: Sink[StatsJob, NotUsed]= Sink.actorRefWithAck(targetActor, StatsJob("go"), StatsJob("next"), StatsJob("noData"))
val jobSender: Source[StatsJob, NotUsed] = Source.repeat(StatsJob("stream to actor")) take 10
val runG: RunnableGraph[NotUsed] = jobSender to goal
runG.run()
```
###Stream from Actor(Source)
****
```scala
val origin: ActorRef = Source.actorRef[StatsJob](100, OverflowStrategy.dropNew).map(println(_)).to(Sink.ignore).run()

system.scheduler.schedule(0.millis, 1000.millis, origin, StatsJob("origin: actor to stream"))
    
```

###Actor via Stream to Actor
****
```scala
val a2a: ActorRef = Source.actorRef[StatsJob](100, OverflowStrategy.dropNew).map(println(_)).
      to(Sink.actorRefWithAck(targetActor, StatsJob("go"), StatsJob("next"), StatsJob("noData"))).run()
system.scheduler.schedule(0.millis, 1000.millis, a2a, StatsJob("a2a: actor to actor"))
```

## <a name="b6"></a>Dynamic Stream Handling

###Switch
****

* UniqueKillSwitch

	UniqueKillSwitch allows to control the completion of one materialized Graph of FlowShape. Refer to the below for usage examples.

```scala
val countingSrc = Source(Stream.from(1)).delay(1.second, DelayOverflowStrategy.backpressure)
val lastSnk = Sink.last[Int]
 
val (killSwitch, last) = countingSrc
  .viaMat(KillSwitches.single)(Keep.right)
  .toMat(lastSnk)(Keep.both)
  .run()
 
doSomethingElse()
 
killSwitch.shutdown()
```

###MergeHub
****

A MergeHub allows to implement a dynamic fan-in junction point in a graph where elements coming from different producers are emitted in a First-Comes-First-Served fashion. 

```scala
// A simple consumer that will print to the console for now
val consumer = Sink.foreach(println)
 
// Attach a MergeHub Source to the consumer. This will materialize to a
// corresponding Sink.
val runnableGraph: RunnableGraph[Sink[String, NotUsed]] =
  MergeHub.source[String](perProducerBufferSize = 16).to(consumer)
 
// By running/materializing the consumer we get back a Sink, and hence
// now have access to feed elements into it. This Sink can be materialized
// any number of times, and every element that enters the Sink will
// be consumed by our consumer.
val toConsumer: Sink[String, NotUsed] = runnableGraph.run()
 
// Feeding two independent sources into the hub.
Source.single("Hello!").runWith(toConsumer)
Source.single("Hub!").runWith(toConsumer)

//MergeHub with Actor Input
val a2h: ActorRef = Source.actorRef[StatsJob](100, OverflowStrategy.dropNew).to(toConsumer).run()
a2h ! StatsJob("a2h: actor to Hub")
```

###BroadcastHub
****

A BroadcastHub can be used to consume elements from a common producer by a dynamic set of consumers. 

```scala
// A simple producer that publishes a new "message" every second
val producer = Source.tick(1.second, 1.second, "New message")
 
// Attach a BroadcastHub Sink to the producer. This will materialize to a
// corresponding Source.
// (We need to use toMat and Keep.right since by default the materialized
// value to the left is used)
val runnableGraph: RunnableGraph[Source[String, NotUsed]] =
  producer.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right)
 
// By running/materializing the producer, we get back a Source, which
// gives us access to the elements published by the producer.
val fromProducer: Source[String, NotUsed] = runnableGraph.run()
 
// Print out messages from the producer in two independent consumers
fromProducer.runForeach(msg => println("consumer1: " + msg))
fromProducer.runForeach(msg => println("consumer2: " + msg))
```

###Combining dynamic stages to build a simple Publish-Subscribe service
****
```scala
// Obtain a Sink and Source which will publish and receive from the "bus" respectively.
val (sink, source) =
  MergeHub.source[String](perProducerBufferSize = 16)
    .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
    .run()
// Ensure that the Broadcast output is dropped if there are no listening parties.
// If this dropping Sink is not attached, then the broadcast hub will not drop any
// elements itself when there are no subscribers, backpressuring the producer instead.
source.runWith(Sink.ignore)
// We create now a Flow that represents a publish-subscribe channel using the above
// started stream as its "topic". We add two more features, external cancellation of
// the registration and automatic cleanup for very slow subscribers.
val busFlow: Flow[String, String, UniqueKillSwitch] =
  Flow.fromSinkAndSource(sink, source)
    .joinMat(KillSwitches.singleBidi[String, String])(Keep.right)
    .backpressureTimeout(3.seconds)
    
val switch: UniqueKillSwitch =
  Source.repeat("Hello world!")
    .viaMat(busFlow)(Keep.right)
    .to(Sink.foreach(println))
    .run()
 
// Shut down externally
switch.shutdown()
==================================================================
//Pub-Sub with actor in and actor out
val s: Sink[StatsJob, NotUsed]= Sink.actorRefWithAck(copy, StatsJob("go"), StatsJob("next"), StatsJob("noData"))
val busFlow: Flow[StatsJob, StatsJob, UniqueKillSwitch] = Flow.fromSinkAndSource(sink2, source2)
      .viaMat(KillSwitches.single)(Keep.right)
      .backpressureTimeout(3.seconds)
val a2aWithPubSub: ActorRef = Source.actorRef[StatsJob](100, OverflowStrategy.dropNew)
      .via(busFlow)
      .to(s).run()
a2aWithPubSub ! StatsJob("a2aWithPubSub: Pub-Sub with actor in and actor out")
```