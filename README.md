# mini-dubbo

简易版 Dubbo —— 保留核心架构，去掉生产级复杂代码，帮助快速理解 Dubbo 核心机制。

## 模块结构

```
mini-dubbo/
├── pom.xml
├── mini-dubbo-common          ← SPI + URL
├── mini-dubbo-remoting        ← Netty Server/Client、编解码、Request/Response
├── mini-dubbo-rpc             ← Invoker + Protocol + Filter + ProxyFactory
├── mini-dubbo-cluster         ← Cluster + Directory + LoadBalance
├── mini-dubbo-registry        ← ZooKeeper 注册中心
├── mini-dubbo-config          ← ServiceConfig + ReferenceConfig + Bootstrap
└── mini-dubbo-demo/
    ├── mini-dubbo-demo-api    ← 接口定义
    ├── mini-dubbo-demo-provider
    └── mini-dubbo-demo-consumer
```

## 模块依赖关系

## 各模块职责

| 模块 | 职责 | 核心类 |
|------|------|--------|
| **common** | SPI 扩展机制 + URL 参数载体 | `@SPI`, `@Adaptive`, `@Activate`, `ExtensionLoader`, `URL` |
| **remoting** | 网络通信 | `NettyServer`, `NettyClient`, `DubboCodec`, `Request`, `Response` |
| **rpc** | RPC 调用核心 | `Invoker`, `Invocation`, `Result`, `Protocol`, `Filter`, `ClusterFilter`, `ProxyFactory` |
| **cluster** | 集群容错 + 负载均衡 | `Cluster`, `Directory`, `LoadBalance`, `FailoverCluster`, `FailfastCluster` |
| **registry** | 注册中心 | `Registry`, `ZookeeperRegistry`, `NotifyListener` |
| **config** | 配置 + 启动编排 | `ServiceConfig`, `ReferenceConfig`, `Bootstrap` |
| **demo** | 示例 | `DemoService`, `DemoServiceImpl` |

## 核心调用链路

### Provider 端

```
ServiceConfig.export()
  → ProxyFactory.getInvoker()        ← 业务对象包装成 Invoker
  → Protocol.export()                ← SPI 选择协议
    → DubboProtocol.export()
      → NettyServer.start()          ← 开启网络监听
  → Registry.register()              ← 注册到 ZooKeeper
```

### Consumer 端

```
ReferenceConfig.get()
  → Protocol.refer()                 ← SPI 选择协议
    → DubboProtocol.refer()
      → NettyClient.connect()        ← 建立连接
  → Cluster.join()                   ← 包装集群容错
    → FailoverClusterInvoker
  → ProxyFactory.getProxy()          ← 生成代理对象
```

### 运行时调用

```
demoService.sayHello("dubbo")
  → InvokerInvocationHandler         ← 代理拦截
    → ClusterFilter 链               ← 集群级 Filter（LoadBalance 之前）
    → FailoverClusterInvoker         ← 集群容错 + 负载均衡
      → Directory.list()             ← 获取可用 Invoker 列表
      → LoadBalance.select()         ← 选一个 Provider
    → Filter 链                      ← 实例级 Filter（LoadBalance 之后）
    → DubboInvoker                   ← 发送 RPC
      → NettyClient → NettyServer    ← 网络传输
      → Provider 处理 → 返回结果
```

## 与真实 Dubbo 的对比

| 组件 | mini-dubbo | 真实 Dubbo |
|------|-----------|-----------|
| SPI | 简化版 ExtensionLoader | 完整版 + ScopeModel 隔离 |
| URL | 简化版（直接 HashMap） | URLAddress + URLParam 压缩存储 |
| 网络 | Netty（简化版） | Netty（完整版 + 心跳/重连/编解码器链） |
| 序列化 | JDK Serializable | Hessian2 / Protobuf / Kryo |
| 注册中心 | ZooKeeper + Curator | ZooKeeper / Nacos / Consul 等 |
| 代理 | JDK Proxy | Javassist |
| Filter | 两级（ClusterFilter + Filter） | 两级 + Wrapper 链 |
| Cluster | failover + failfast | failover/failfast/failsafe/failback/forking/broadcast |
| Directory | StaticDirectory + RegistryDirectory | + ServiceDiscoveryRegistryDirectory |
| 配置 | 简化版 Bootstrap | DubboBootstrap + DefaultApplicationDeployer + 内部模块 |

## 学习路径

1. **common** → 理解 SPI 机制和 URL 的作用
2. **remoting** → 理解网络通信层
3. **rpc** → 理解 Invoker 抽象和 Filter 链
4. **cluster** → 理解集群容错和负载均衡
5. **registry** → 理解服务注册与发现
6. **config** → 理解启动编排和配置组装
7. **demo** → 端到端运行验证
