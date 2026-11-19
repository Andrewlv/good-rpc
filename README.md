<div align="center">

# 📡 Good-RPC

**A hand-written RPC framework built to learn how Dubbo works — JDK dynamic proxies, a custom 17-byte binary protocol, an etcd registry, load balancing, retry & fault tolerance, all pluggable via a home-rolled SPI.**

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Vert.x](https://img.shields.io/badge/Vert.x-4.5.1-782A90?logo=vert.x&logoColor=white)
![etcd](https://img.shields.io/badge/Registry-etcd-419FDE?logo=etcd&logoColor=white)
![Serialization](https://img.shields.io/badge/Serialization-JDK%20%C2%B7%20JSON%20%C2%B7%20Kryo%20%C2%B7%20Hessian-blue)
![GitHub stars](https://img.shields.io/github/stars/Andrewlv/good-rpc?style=social)

</div>

---

## 📖 About

Good-RPC is a from-scratch RPC framework written for learning — an imitation of Dubbo's architecture in miniature. Everything a production RPC gives you for free is implemented here by hand, so you can read every layer: how a consumer turns an interface into a proxy, how a request becomes bytes on the wire, how services register and discover each other, and what happens when a call fails.

The repo deliberately contains **two generations of the framework**:

- **`good-rpc-easy`** — the minimal version (~10 classes): JDK dynamic proxy → JDK serialization → HTTP POST → reflection invoke. One file per concern, ideal first read.
- **`good-rpc-core`** — the full version (~55 classes): custom binary protocol over Vert.x TCP, etcd registry with heartbeats & watching, pluggable serializers, load balancing, retry, fault tolerance, and an SPI mechanism.

`example-provider` / `example-consumer` / `example-common` demonstrate both.

> 🛠️ **Work in progress** — a learning project that evolves chapter by chapter. The core pipeline (proxy → etcd discovery → TCP → invoke → respond) works end-to-end; a few known rough edges remain — see [Known Issues](#️-known-issues) below.

## ✨ Features

- 🔌 **Consumer-side proxies** — `ServiceProxyFactory` hands you a JDK dynamic proxy for any service interface; a `MockServiceProxy` returns default values when `rpc.mock=true` (great for offline development)
- 📦 **Provider-side registry** — `LocalRegistry` maps interface name → implementation class; `HttpServerHandler` deserializes the request and invokes the target method via reflection
- 🧬 **Custom RPC protocol** — a 17-byte header (magic `0x1`, version, serializer, message type, status, snowflake request ID, body length) with encoder/decoder, and Vert.x `RecordParser` framing to handle sticky/half TCP packets
- 🔄 **Four serializers** — JDK, JSON (Jackson), Kryo, Hessian — selected by config key through the SPI loader
- 🗂️ **etcd registry** — jetcd-based service registration with 30 s leases, a 10 s heartbeat renewal cron, watch on service keys that evicts the discovery cache, and an etcd-backed service cache
- ⚖️ **Load balancing** — Round Robin (default), Random, and Consistent Hash with 100 virtual nodes per service
- 🛡️ **Retry & fault tolerance** — retry strategies (none / fixed-interval via `guava-retrying`) wrapping the network call, plus tolerant strategies (failFast / failOver / failBack / failSafe) as the last line of defense
- 🔧 **Home-rolled SPI** — `SpiLoader` reads `key=class` files from `META-INF/rpc/system/` and `META-INF/rpc/custom/` (custom wins), with a singleton instance cache
- ⚙️ **Hutool-based config** — `rpc.*` keys from `application.properties` bind straight onto `RpcConfig` via `Props.toBean`

## 🏛️ Architecture

```
Consumer                                              Provider
────────                                              ────────
ServiceProxyFactory.getProxy(UserService.class)
      │
      ▼
ServiceProxy (JDK dynamic proxy)
      │
      ├─► Registry.serviceDiscovery(serviceKey)      RpcApplication.init()
      │     (etcd prefix query, cached locally;          │
      │      watch DELETE → cache eviction)             registry.init(etcd)
      │                                                  │
      ├─► LoadBalancer.select(methodParams)          registry.register(serviceMetaInfo)
      │     roundRobin | random | consistentHash         (30 s lease, 10 s heartbeat)
      │
      ├─► RetryStrategy.doRetry(...)                 LocalRegistry.register(name, implClass)
      │     no | fixedInterval
      │
      └─► VertxTcpClient.doRequest(...)
            │
            ▼
      ProtocolMessageEncoder ──► TCP ──►  VertxHttpServer  (HTTP, complete path)
      17-byte header + body              VertxTcpServer   (TCP, framing only — see notes)
                                              │
                                              ▼
                                        HttpServerHandler
                                        deserialize → LocalRegistry lookup
                                        → reflection invoke → respond

      on failure: TolerantStrategy
      failFast | failOver | failBack | failSafe
```

### Protocol message

| Offset | Field | Bytes | Values |
| --- | --- | --- | --- |
| 0 | magic | 1 | `0x1` |
| 1 | version | 1 | `0x1` |
| 2 | serializer | 1 | 0=JDK · 1=JSON · 2=Kryo · 3=Hessian |
| 3 | type | 1 | 0=request · 1=response · 2=heartbeat · 3=others |
| 4 | status | 1 | 20=ok · 40=bad request · 50=bad response |
| 5–12 | requestId | 8 | snowflake ID |
| 13–16 | bodyLength | 4 | length of the serialized body |
| 17… | body | n | serialized `RpcRequest` / `RpcResponse` |

## 🧱 Tech Stack

| Category | Technology |
| --- | --- |
| Language | Java 17 |
| Network | Vert.x 4.5.1 — HTTP server + TCP server/client, `RecordParser` framing |
| Registry | etcd via jetcd 0.7.7 (leases, heartbeat, watch) · in-memory `LocalRegistry` |
| Serialization | JDK built-in · Jackson 2.12.3 · Kryo 5.6.0 · Hessian 4.0.66 |
| Load balancing | RoundRobin · Random · ConsistentHash (100 virtual nodes) |
| Fault tolerance | `guava-retrying` 2.0.0 · failFast / failOver / failBack / failSafe |
| SPI | Custom loader over `META-INF/rpc/{system,custom}` |
| Config | Hutool 5.8.16 (`Props.toBean` onto `RpcConfig`) |
| Logging | Lombok `@Slf4j` · Logback 1.3.12 |
| Build & tests | Maven · JUnit 4 |

## 📂 Project Structure

```
good-rpc
├── good-rpc-easy                          # Minimal teaching version — proxy + JDK serialization + HTTP
├── good-rpc-core                          # Full framework
│   └── src/main
│       ├── java/com/andrew/goodrpc
│       │   ├── RpcApplication.java        # Global holder, double-checked singleton init
│       │   ├── config/                    # RpcConfig · RegistryConfig
│       │   ├── constant/                  # RpcConstant — rpc.* prefix, default version
│       │   ├── proxy/                     # ServiceProxy · ServiceProxyFactory · MockServiceProxy
│       │   ├── model/                     # RpcRequest · RpcResponse · ServiceMetaInfo
│       │   ├── protocol/                  # ProtocolMessage + encoder/decoder + enums
│       │   ├── serializer/                # Jdk · Json · Kryo · Hessian + factory
│       │   ├── registry/                  # Registry · LocalRegistry · EtcdRegistry · service cache
│       │   ├── loadbalancer/              # RoundRobin · Random · ConsistentHash + factory
│       │   ├── fault/retry/               # NoRetry · FixedInterval + factory
│       │   ├── fault/tolerant/            # FailFast · FailOver · FailBack · FailSafe + factory
│       │   ├── server/                    # VertxHttpServer · HttpServerHandler
│       │   │   └── tcp/                   # VertxTcpServer · VertxTcpClient · TcpBufferHandlerWrapper
│       │   ├── spi/                       # SpiLoader — key=class files, singleton cache
│       │   └── utils/                     # ConfigUtils — application.properties → bean
│       └── resources/META-INF/rpc         # system/ (built-ins) · custom/ (user overrides)
├── example-common                         # UserService interface + User model
├── example-provider                       # ProviderExample · EasyProviderExample · UserServiceImpl
└── example-consumer                       # ConsumerExample · EasyConsumerExample · UserServiceProxy
```

## 🚀 Quick Start

### Prerequisites

| Requirement | Version | Notes |
| --- | --- | --- |
| JDK | 17 | `good-rpc-core/pom.xml` sets `maven.compiler.source = 17` |
| Maven | 3.6+ | No root aggregator POM — build each module in order |
| etcd | 3.x | Only for the full path (`ProviderExample` / `ConsumerExample`) |

### 1. Build and install the modules

```bash
cd good-rpc-core && mvn clean install
cd ../example-common && mvn clean install
```

`example-provider` and `example-consumer` depend on both — open them in your IDE (they have no runner configured) or build them the same way.

### 2. Start etcd (full path only)

`RegistryTest` expects etcd's client port on `localhost:2379`, and the default `RegistryConfig` address is `http://localhost:2380` — so set it explicitly in the example's `src/main/resources/application.properties`:

```properties
rpc.name=goodrpc
rpc.version=2.0
rpc.mock=false
rpc.serializer=jdk
rpc.registryConfig.address=http://localhost:2379
```

Then run any etcd v3.x exposing the client port, e.g.:

```bash
docker run -d --name etcd -p 2379:2379 \
  quay.io/coreos/etcd:v3.5.5 \
  etcd --listen-client-urls http://0.0.0.0:2379 \
       --advertise-client-urls http://0.0.0.0:2379
```

### 3. Run the provider

`example-provider/…/ProviderExample.main`:

```java
// 1. Init the framework (loads rpc.* config, initializes the etcd registry)
RpcApplication.init();

// 2. Register the service locally (interface name → impl class)
LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

// 3. Register service metadata (host:port from rpc config) into etcd
registry.register(serviceMetaInfo);

// 4. Start the Vert.x server on rpc.serverPort (default 8080)
new VertxTcpServer().doStart(RpcApplication.getRpcConfig().getServerPort());
```

### 4. Run the consumer

`example-consumer/…/ConsumerExample.main`:

```java
// Get a dynamic proxy — no HTTP/TCP code in sight
UserService userService = ServiceProxyFactory.getProxy(UserService.class);

User user = new User();
user.setName("Andrew");
User newUser = userService.getUser(user);   // remote call, via proxy
System.out.println(newUser.getName());
System.out.println(userService.getNumber());
```

The call walks the full pipeline: proxy → etcd discovery → load balancer → retry strategy → protocol encode → TCP → decode → respond. Remaining rough edges are listed under [Known Issues](#️-known-issues).

## 🧩 Optional Modules

<details>
<summary><b>good-rpc-easy — the minimal framework</b></summary>

A complete RPC in ~10 classes: `ServiceProxyFactory` + JDK dynamic proxy, `JdkSerializer`, `LocalRegistry`, and `VertxHttpServer` + `HttpServerHandler`. The consumer POSTs a serialized `RpcRequest` to a hardcoded `http://localhost:8080` (the `todo` marks where the registry would slot in). Read this module top-to-bottom before opening `good-rpc-core`.

</details>

<details>
<summary><b>Mock mode — develop without a provider</b></summary>

Set `rpc.mock=true` (in the example's `application.properties`). `ServiceProxyFactory` then returns `MockServiceProxy` instances that log the invocation and return type-appropriate defaults (`0` for primitives, `null` for objects) — no network, no provider needed.

</details>

<details>
<summary><b>Extending via SPI — plug in your own serializer</b></summary>

Every pluggable component (serializer, registry, load balancer, retry & tolerant strategies) is resolved through `SpiLoader` from `key=class` files:

- Built-ins live in `good-rpc-core/src/main/resources/META-INF/rpc/system/`
- Drop your own implementations in `META-INF/rpc/custom/` — same interface name, same file name — and they **override** the system entries

For example, register a serializer with key `protobuf` in `com.andrew.goodrpc.serializer.Serializer`, then set `rpc.serializer=protobuf`.

</details>

<details>
<summary><b>All rpc.* configuration keys</b></summary>

| Key | Default | Meaning |
| --- | --- | --- |
| `rpc.name` | `good-rpc` | Framework name |
| `rpc.version` | `1.0` | Config version |
| `rpc.serverHost` / `rpc.serverPort` | `localhost` / `8080` | Provider address registered into etcd |
| `rpc.mock` | `false` | Return default values instead of remote calls |
| `rpc.serializer` | `jdk` | `jdk` · `json` · `kryo` · `hessian` |
| `rpc.loadBalancer` | `roundRobin` | `roundRobin` · `random` · `consistentHash` |
| `rpc.retryStrategy` | `no` | `no` · `fixedInterval` |
| `rpc.tolerantStrategy` | `failFast` | `failFast` · `failOver` · `failBack` · `failSafe` |
| `rpc.registryConfig.registry` | `etcd` | Registry type (SPI key) |
| `rpc.registryConfig.address` | `http://localhost:2380` | etcd endpoint |
| `rpc.registryConfig.timeout` | `50000` | Connect timeout (ms) |

</details>

## ⚠️ Known Issues

Honest status of the current tree — read before running the examples:

1. **Registry default port is etcd's peer port** — `RegistryConfig` defaults to `:2380`; point it at the client port (`:2379`) as shown in Quick Start.
2. **ConsistentHash ring accumulates** — `ConsistentHashLoadBalancer.select()` adds virtual nodes on every call without clearing the ring first; fine for reading, worth fixing before benchmarking.
3. **Consumer hangs on TCP connect failure** — `VertxTcpClient` doesn't complete its `responseFuture` when the connect itself fails, so the caller blocks instead of falling through to the tolerant strategy.

> ✅ Recently fixed in this repo: `EtcdRegistry` retains its jetcd client (no more startup NPE), `VertxTcpServer` decodes requests, invokes locally and writes framed responses back (framing now mirrors the consumer's), and `ServiceProxy` passes the correct key to `TolerantStrategyFactory`, making the fault-tolerance layer reachable.

## 🧪 Tests

```bash
cd good-rpc-core && mvn test
```

`src/test` covers the protocol encode/decode round-trip, retry strategies, load balancers, and the etcd registry (`RegistryTest` needs a live etcd on `localhost:2379` — and fix #1 above).

## 🤝 Acknowledgments

The architecture, module layout and many implementation ideas closely follow 程序员鱼皮 (liyupi)'s open-source **yu-rpc** learning framework from the [编程导航知识星球](https://yupi.icu) — a hand-written Dubbo-style RPC built chapter by chapter. Dubbo itself is the design reference throughout. Many thanks for the excellent learning material! 🙏

## ⭐ Support

If reading this codebase helped you understand how RPC frameworks work, consider giving the repo a star — it helps other learners find it.
