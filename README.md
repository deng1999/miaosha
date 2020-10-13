# miaosha
项目简介：本项目主要是模拟应对大并发场景下，如何完成商品的秒杀，以及针对秒杀场景下为应对大并发所做的优化
采用的技术栈：springboot+Mybatis+redis+rabbitmq
1.启动
 访问入口：http://localhost:8080/login/to_login
 初始账号/密码：17856002745/123456
主要分为：用户登录，商品列表，商品详情以及订单详情模块
登录实现：
- 明文密码两次MD5处理

- JSR303参数检验和全局异常处理器

- 分布式Session

### 明文密码两次MD5处理

- 客户端：C_PASS=MD5(明文+固定salt)
- 服务端：S_PASS=MD5(C_PASS+随机salt)

加密：出于安全考虑

第一次 （在前端加密，客户端）：密码加密是（明文密码+固定盐值）生成md5用于传输，目的，由于http是明文传输，当输入密码若直接发送服务端验证，此时被截取将直接获取到明文密码，获取用户信息。

加盐值是为了混淆密码，原则就是明文密码不能在网络上传输。

第二次：在服务端再次加密，当获取到前端发送来的密码后。通过MD5（密码+随机盐值）再次生成密码后存入数据库。

防止数据库被盗的情况下，通过md5反查，查获用户密码。方法是盐值会在用户登陆的时候随机生成，并存在数据库中，这个时候就会获取到。

第二次的目的：
黑客若是同时黑掉数据库，通过解析前端js文件，知道如果md5加密的过程，就知道此时用户的密码。

但是此时我们要是在后端加入随机盐值和传输密码的md5组合，黑客是无法知道通过后端密码加密过程的，从而无法知道密码。
### 分布式Session

在用户登录成功之后，将用户信息存储在redis中，然后生成一个token返回给客户端，这个token为存储在redis中的用户信息的key，这样，当客户端第二次访问服务端时会携带token，首先到redis中获取查询该token对应的用户使用是否存在，这样也就不用每次到数据库中去查询是不是该用户了，从而减轻数据库的访问压力。
接口优化：
- Redis预减库存减少数据库的访问

- 内存标记减少redis访问

- 请求先入队缓冲，异步下单，增强用户体验
- RabbitMQ安装与Spring Boot集成
Redis预减库存减少数据库的访问：
核心思想：减少对数据库的访问。
**秒杀接口优化思路**：减少数据库的访问

- 系统初始化时，将商品库存信息加载到redis中；
- 服务端收到请求后，redis预减库存，如果库存不足，则直接进入下一步；
- 服务端将请求入队，立即返回向客户端返回排队中的信息，提高用户体验；
- 服务端请求出队，生成秒杀订单，减少库存；
- 客户端轮询是否秒杀成功。



 