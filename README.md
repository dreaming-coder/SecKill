
秒杀系统相信很多人见过，比如京东或者淘宝的秒杀，小米手机的秒杀，那么秒杀系统的后台是如何实现的呢？我们如何设计一个秒杀系统呢？对于秒杀系统应该考虑哪些问题？如何设计出健壮的秒杀系统？

当然，鉴于本人的菜鸡属性，目前的知识储备只够实现一个基于 SpringBoot + Mybatis-Plus + MySQL + Redis + RabbitMQ + Themleaf + HTML + CSS + JavaScript 的秒杀案例，因为主要考虑后端，前端界面会很简洁（就是很丑啦）。

本文中的内容最终项目代码可能有出入，因为 bug 太多，调到后面记不过来了，一切以项目代码为准，亲测跑通 \^-\^

项目地址：[https://github.com/dreaming-coder/SecKill](https://github.com/dreaming-coder/SecKill)

# 1. 秒杀应该考虑哪些问题

## 1.1 超卖问题

分析秒杀的业务场景，最重要的有一点就是超卖问题，假如备货只有 100 个，但是最终超卖了 200 个，一般来讲秒杀系统的价格都比较低，如果超卖将严重影响公司的财产利益，因此首当其冲的就是解决商品的超卖问题。

## 1.2 高并发

秒杀具有时间短、并发量大的特点，秒杀持续时间只有几分钟，而一般公司都为了制造轰动效应，会以极低的价格来吸引用户，因此参与抢购的用户会非常的多。短时间内会有大量请求涌进来，后端如何防止并发过高造成缓存击穿或者失效，击垮数据库都是需要考虑的问题。

## 1.3 接口防刷

现在的秒杀大多都会出来针对秒杀对应的软件，这类软件会模拟不断向后台服务器发起请求，一秒几百次都是很常见的，如何防止这类软件的重复无效请求，防止不断发起的请求也是需要我们针对性考虑的。

## 1.4 秒杀 url

对于普通用户来讲，看到的只是一个比较简单的秒杀页面，在未达到规定时间，秒杀按钮是灰色的，一旦到达规定时间，灰色按钮变成可点击状态。这部分是针对小白用户的，如果是稍微有点电脑功底的用户，会通过 <kbd>F12</kbd>看浏览器的 network 看到秒杀的 url，通过特定软件去请求也可以实现秒杀，或者提前知道秒杀 url 的人，一请求就直接实现秒杀了。

## 1.5 数据库设计

秒杀有把我们服务器击垮的风险，如果让它与我们的其他业务使用在同一个数据库中，耦合在一起，就很有可能牵连和影响其他的业务。如何防止这类问题发生，就算秒杀发生了宕机、服务器卡死问题，也应该让他尽量不影响线上正常进行的业务。

## 1.6 大量请求问题

按照 1.2 的考虑，就算使用缓存还是不足以应对短时间的高并发的流量的冲击。如何承载这样巨大的访问量，同时提供稳定低时延的服务保证，是需要面对的一大挑战。我们来算一笔账，假如使用的是 redis 缓存，单台 redis 服务器可承受的 QPS 大概是 4 W 左右，如果一个秒杀吸引的用户量足够多的话，单 QPS 可能达到几十万，单体 redis 还是不足以支撑如此巨大的请求量。缓存会被击穿，直接渗透到数据库，从而击垮 MySQL，后台会将会大量报错。

# 2. 秒杀系统的设计和技术方案

## 2.1 秒杀系统数据库设计

针对 1.5 提出的秒杀数据库的问题，因此应该单独设计一个秒杀数据库 `dB_sec_kill`，防止因为秒杀活动的高并发访问拖垮整个网站。

这里字段类型有点随意，实际开发还得考虑准确性和效率

- `sec_goods`

  这里是参与秒杀的商品表，每一行代表一个活动

  |     字段名     |     类型     |      描述      |
  | :------------: | :----------: | :------------: |
  |       id       |     INT      |  自增（主键）  |
  |    good_id     | VARCHAR(12)  |  活动商品 id   |
  |   good_name    | VARCHAR(60)  |   活动商品名   |
  |  origin_price  | DECIMAL(5,2) |    原来价格    |
  | discount_price | DECIMAL(5,2) |    活动价格    |
  |     stock      |     INT      | 参与活动的库存 |
  |   start_time   |   DATETIME   |  活动开始时间  |
  |    end_time    |   DATETIME   |  活动结束时间  |

- `sec_orders`

  秒杀订单表，每一行代表一个订单

  |   字段名    |    类型     |                     描述                      |
  | :---------: | :---------: | :-------------------------------------------: |
  |  order_id   | VARCHAR(18) |                订单 id（主键）                |
  |    phone    |  CHAR(11)   |               手机号（unique）                |
  |    email    | VARCHAR(40) |                 用户电子邮箱                  |
  |   good_id   | VARCHAR(12) |                    商品 id                    |
  | create_time |  DATETIME   |                 订单创建时间                  |
  |   status    |     INT     | 0 表示订单完成， 1 表示待付款，2 表示订单取消 |

## 2.2 秒杀 url 的设计

为了避免有程序访问经验的人通过下单页面 url 直接访问后台接口来秒杀货品，我们需要将秒杀的 url 实现动态化，即使是开发整个系统的人都无法在秒杀开始前知道秒杀的 url。

思路：

1. 在进行秒杀之前，先请求一个服务端地址，`/seckillpath` 这个地址，用来获取秒杀地址，在服务端生成一个地址作为 `pathId` 存入缓存，（缓存过期时间 60 s），然后将这个地址返回给前端
2. 获得该 `pathId`，后 前端在用这个 `pathId` 拼接在 url 上作为参数，去请求 `/seckill` 服务
3. 后端接收到这个 `pathId` 参数，并且与缓存中的 `pathId` 比较

如果通过比较，进行秒杀逻辑，如果不通过，抛出业务异常，非法请求。

## 2.3 精简 SQL

典型的一个场景是在进行扣减库存的时候，传统的做法是先查询库存，再去 update。这样的话需要两个 SQL，而实际上一个 SQL 我们就可以完成的。可以用这样的做法：`update sec_goods set stock = stock - 1 where good_id = {#goods_id} and stock > 0;` 这样的话，就可以保证库存不会超卖并且一次更新库存。

## 2.4 Redis 预减库存

很多请求进来，都需要后台查询库存，这是一个频繁读的场景。可以使用 Redis 来预减库存，在秒杀开始前可以在 Redis 设值，比如`redis.set(goodsId,100)`，这里预放的库存为 100 可以设值为常量)，每次下单成功之后，`Integer stock = (Integer)redis.get(goosId);` 然后判断 `stock` 的值，如果小于常量值就减去 1；不过注意当取消的时候，需要增加库存，增加库存的时候也得注意不能大于之间设定的总库存数(查询库存和扣减库存需要原子操作，此时可以借助 Lua 脚本)，下次下单再获取库存的时候，直接从 Redis 里面查就可以了。

## 2.5 接口限流

秒杀最终的本质是数据库的更新，但是有很多大量无效的请求，我们最终要做的就是如何把这些无效的请求过滤掉，防止渗透到数据库。限流的话，需要入手的方面很多：

### 2.5.1 前端限流

首先第一步就是通过前端限流，用户在秒杀按钮点击以后发起请求，那么在接下来的 5 秒是无法点击(通过设置按钮为disable)。这一小举措开发起来成本很小，但是很有效。

### 2.5.2 同一个用户xx秒内重复请求直接拒绝

具体多少秒需要根据实际业务和秒杀的人数而定，一般限定为 10 秒。具体的做法就是通过 Redis 的键过期策略，首先对每个请求都从`String value = redis.get(userId);` 如果获取到这个 value 为空或者为 null，表示它是有效的请求，然后放行这个请求。如果不为空表示它是重复性请求，直接丢掉这个请求。如果有效，采用 `redis.setexpire(userId,value,10)`，value可以是任意值，一般放业务属性比较好，这个是设置以 userId 为 key，10 秒的过期时间(10 秒后，key 对应的值自动为 null)。

### 2.5.3 令牌桶算法限流

接口限流的策略有很多，我们这里采用令牌桶算法。令牌桶算法的基本思路是每个请求尝试获取一个令牌，后端只处理持有令牌的请求，生产令牌的速度和效率我们都可以自己限定，guava 提供了 `RateLimter` 的 API 供我们使用。以下做一个简单的例子，注意需要引入guava。

```java
public class TestRateLimiter {

    public static void main(String[] args) {
        //1秒产生1个令牌
        final RateLimiter rateLimiter = RateLimiter.create(1);
        for (int i = 0; i < 10; i++) {
            //该方法会阻塞线程，直到令牌桶中能取到令牌为止才继续向下执行。
            double waitTime= rateLimiter.acquire();
            System.out.println("任务执行" + i + "等待时间" + waitTime);
        }
        System.out.println("执行结束");
    }
}
```

上面代码的思路就是通过 `RateLimiter` 来限定我们的令牌桶每秒产生 1 个令牌(生产的效率比较低)，循环 10 次去执行任务。`acquire() ` 会阻塞当前线程直到获取到令牌，也就是如果任务没有获取到令牌，会一直等待。那么请求就会卡在我们限定的时间内才可以继续往下走，这个方法返回的是线程具体等待的时间。执行如下：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165126369.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)


可以看到任务执行的过程中，第 1 个是无需等待的，因为已经在开始的第 1 秒生产出了令牌。接下来的任务请求就必须等到令牌桶产生了令牌才可以继续往下执行。如果没有获取到就会阻塞(有一个停顿的过程)。不过这个方式不太好，因为用户如果在客户端请求，如果较多的话，直接后台在生产 token 就会卡顿(用户体验较差)，它是不会抛弃任务的，我们需要一个更优秀的策略：**如果超过某个时间没有获取到，直接拒绝该任务**。接下来再来个案例：

```java
public class TestRateLimiter2 {

    public static void main(String[] args) {
        final RateLimiter rateLimiter = RateLimiter.create(1);

        for (int i = 0; i < 10; i++) {
            long timeOut = (long) 0.5;
            boolean isValid = rateLimiter.tryAcquire(timeOut, TimeUnit.SECONDS);
            System.out.println("任务" + i + "执行是否有效:" + isValid);
            if (!isValid) {
                continue;
            }
            System.out.println("任务" + i + "在执行");
        }
        System.out.println("结束");
    }
}
```

其中用到了 `tryAcquire()` 方法，这个方法的主要作用是设定一个超时的时间，如果在指定的时间内**预估(注意是预估并不会真实的等待)，**如果能拿到令牌就返回 true，如果拿不到就返回 false。然后我们让无效的直接跳过，这里设定每秒生产 1 个令牌，让每个任务尝试在 0.5 秒获取令牌，如果获取不到，就直接跳过这个任务(放在秒杀环境里就是直接抛弃这个请求)；程序实际运行如下：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165135423.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)


只有第 1 个获取到了令牌，顺利执行了，下面的基本都直接抛弃了，因为 0.5 秒内，令牌桶( 1 秒 1 个)来不及生产就肯定获取不到返回false了。

**这个限流策略的效率有多高呢？假如我们的并发请求是 400 万瞬间的请求，将令牌产生的效率设为每秒20个，每次尝试获取令牌的时间是 0.05 秒，那么最终测试下来的结果是，每次只会放行 4 个左右的请求，大量的请求会被拒绝，这就是令牌桶算法的优秀之处。**

## 2.6 异步下单

为了提升下单的效率，并且防止下单服务的失败。需要将下单这一操作进行异步处理。最常采用的办法是使用队列，队列最显著的三个优点：**异步、削峰、解耦**。这里可以采用 RabbitMQ，在后台经过了限流、库存校验之后，流入到这一步骤的就是有效请求，然后发送到队列里，队列接受消息，异步下单。下完单，入库没有问题可以用邮件通知用户秒杀成功。假如失败的话，可以采用补偿机制，重试。

## 2.7 整体流程图

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165144505.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)


# 3. web 界面设计

个人觉得，前面数据库决定好了，下一步应该是前端静态界面了（我对前端真鸡儿恐惧，不会搞），数据库和前端界面（包括 JavaScript 之类的）确定了，后面剩下的不就是按照需求写 controller、service 和 repository 了吗？

后台逻辑反而比前端设计简单多了！

下面看看我们这个秒杀案例需要几张页面，注意，我们只是为了实现秒杀系统的整个流程，像登录、注册、管理等暂不考虑，只涉及能反映该秒杀功能的最少页面

- 404

- 秒杀商品列表（作为主页 index.html）

  倒计时、抢购按钮、自动更新商品库存、抢购价格

  订单查询按钮（根据 session，优先查询本地 session 的包含的用户的订单，没有则需要弹窗给出必要的查询信息）

- 订单详情列表

  下单商品、价格、取消/支付/稍后


## 3.1 index.html

示意图如下：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165156656.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165204207.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)


## 3.2 order.html

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165213158.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165220287.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)


## 3.3 404.html

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165229801.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)


# 4. 后台系统实现

此次想基于 IDEA 尝试使用多模块开发，大致分为 repository、service、controller、redis、rabbit 五个模块，首选创建项目工程：

1、创建一个 Spring Initializer 项目

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165240746.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165247979.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)


删除多余内容，如下图所示：

![在这里插入图片描述](https://img-blog.csdnimg.cn/2021060416525673.png#pic_center)


2、创建子模块 repository

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165302950.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165309464.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165317727.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)


3、创建其他子模块

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165324234.png#pic_center)


4、修改父工程 pom 文件

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165331191.png#pic_center)


5、修改子模块 pom 文件

5.1、修改 repository 的 pom 文件

```xml
<parent>
    <groupId>com.example</groupId>
    <artifactId>Seckill</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
```

5.2、修改 service 的 pom 文件

```xml
<parent>
    <groupId>com.example</groupId>
    <artifactId>Seckill</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
<dependency>
    <groupId>com.example</groupId>
    <artifactId>repository</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

5.3、修改 controller 的 pom 文件

```xml
<parent>
    <groupId>com.example</groupId>
    <artifactId>Seckill</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
<dependency>
    <groupId>com.example</groupId>
    <artifactId>service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

5.4、修改 rabbit 的 pom 文件

```xml
<parent>
    <groupId>com.example</groupId>
    <artifactId>Seckill</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
<dependency>
    <groupId>com.example</groupId>
    <artifactId>repository</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.example</groupId>
    <artifactId>redis</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

5.5、修改 redis 的 pom 文件

```xml
<parent>
    <groupId>com.example</groupId>
    <artifactId>Seckill</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
```

## 4.1 repository

### 4.1.1 创建表

```sql
DROP TABLE IF EXISTS `sec_goods`;
CREATE TABLE `sec_goods` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id，主键，因为可能一个商品有多个活动',
  `good_id` varchar(12) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '活动商品 id',
  `origin_price` decimal(8,2) DEFAULT NULL COMMENT '原始价格',
  `discount_price` decimal(8,2) DEFAULT NULL COMMENT '活动价格',
  `stock` int DEFAULT NULL COMMENT '参与活动商品的库存',
  `start_time` datetime DEFAULT NULL COMMENT '活动开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '活动结束时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `sec_orders`;
CREATE TABLE `sec_orders` (
  `order_id` varchar(18) COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单id',
  `phone` char(11) COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号',
  `email` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '电子邮箱',
  `good_id` varchar(12) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '活动商品 id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '订单创建时间',
  `status` char(1) NOT NULL COMMENT '订单状态：0表示完成，1表示待付款，2表示订单取消',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 4.1.2 编写配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seckill
    username: root
    password: root
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

```java
package com.example.repository.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author ice
 * @create 2021-05-27 09:18:38
 */
@Configuration
public class DruidConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource") // 和配置文件中的属性绑定，避免重复写 setXXX()
    public DataSource dataSourceOne(){
        return new DruidDataSource();
    }
}
```

### 4.1.3 编写实体类

```java
package com.example.repository.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author ice
 * @create 2021-05-26 22:00:44
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sec_goods")
public class Good implements Serializable {
    private static final long serialVersionUID = -8595790565352087597L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String goodId;
    private BigDecimal originPrice;
    private BigDecimal discountPrice;
    private Long stock;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime  startTime;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime endTime;
}
```

```java
package com.example.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author ice
 * @create 2021-05-26 22:00:52
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sec_orders")
public class Order implements Serializable {
    private static final long serialVersionUID = -1594951159465127479L;
    @TableId(type = IdType.ASSIGN_UUID)
    private String orderId;

    private String phone;
    private String email;
    private String goodId;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createTime;
    private String status;
}
```

### 4.1.4 编写 Mapper 接口

暂时先继承 `BaseMapper`，后序看情况再添加方法

```java
package com.example.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.example.repository.entity.Good;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @author ice
 * @create 2021-05-27 09:37:39
 */
@Mapper
public interface GoodMapper extends BaseMapper<Good> {
    @Update("update sec_goods set stock = stock - 1 ${ew.customSqlSegment}")
    int deliver(@Param(Constants.WRAPPER) Wrapper<Good> wrapper);
}
```

```java
package com.example.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.repository.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author ice
 * @create 2021-05-27 11:00:26
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
```

## 4.2 redis

### 4.2.1 编写配置

```yaml
spring:
  redis:
    host: 127.0.0.1                # Redis服务器地址
    port: 6379                     # Redis服务器连接端口
    database: 0                    # Redis数据库索引（默认为0）
    timeout: 5000                  # 连接超时时间（毫秒）
    lettuce:
      pool:
        max-active: 20             # 接池最大连接数（使用负值表示没有限制）
        max-wait: -1               # 最大阻塞等待时间(负数表示没限制)
        max-idle: 5                # 连接池中的最大空闲连接
        min-idle: 0                # 连接池中的最小空闲连接
```

```java
package com.example.redis.config;

import com.example.redis.service.LimitService;
import com.example.redis.service.PathService;
import com.example.redis.service.StockService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import java.time.Duration;

/**
 * @author ice
 * @create 2021-05-27 20:55:27
 */
@SuppressWarnings({"rawtypes", "unchecked", "DuplicatedCode"})
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        template.setConnectionFactory(factory);
        //key序列化方式
        template.setKeySerializer(redisSerializer);
        //value序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);
        //value hashmap序列化
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        return template;
    }


    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        //解决查询缓存转换异常的问题
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        // 配置序列化（解决乱码的问题）,过期时间600秒
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(600))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();
        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    @DependsOn({"checkStockScript","redisTemplate"})
    public StockService redisService(){
        return new StockService();
    }

    @Bean
    @DependsOn({"pathScript","redisTemplate"})
    public PathService pathService(){
        return new PathService();
    }

    @Bean
    @DependsOn("redisTemplate")
    public LimitService limitService(){
        return new LimitService();
    }

    @Bean
    public DefaultRedisScript<Boolean> checkStockScript(){
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("checkStock.lua")));
        script.setResultType(Boolean.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> pathScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("dynamicPath.lua")));
        script.setResultType(Long.class);
        return script;
    }

}
```

### 4.2.2 编写 StockService

用于获取库存，如还有则减一，需要保证原子性，利用 Lua 脚本执行

```lua
local stock = redis.call('get',KEYS[1])
if tonumber(stock) > 0 then
    redis.call('DECR', KEYS[1])
    return true
else
    return false
end
```

```java
package com.example.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-29 18:39:57
 */
public class StockService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DefaultRedisScript<Boolean> script;

    public void initStock(Map<String, Object> map) {
        map.forEach(
                (goodId, stock) -> {
                    String key = goodId + ":stock";
                    redisTemplate.opsForValue().set(key, stock);
                }
        );
    }

    public void decrementStock(String goodId) {
        String key = goodId + ":stock";
        redisTemplate.opsForValue().decrement(key, 1);
    }

    public void incrementStock(String goodId) {
        String key = goodId + ":stock";
        redisTemplate.opsForValue().increment(key, 1);
    }

    public Boolean checkStock(String goodId){
        String key = goodId + ":stock";
        List<String> keyList = new ArrayList<>();
        keyList.add(key);
        return redisTemplate.execute(script, keyList);
    }
}
```

### 4.2.3 编写 PathService

用于动态获取 `pathId`，接口防刷，因为是先判断再获取，没有要创建的，也要原子性，所以也用 Lua 脚本执行

```lua
local function getRandom(n,seed)
    math.randomseed(seed)
    local t = {
        "0","1","2","3","4","5","6","7","8","9"
    }
    local s = ""
    for i =1, n do
        s = s .. t[math.random(#t)]
    end;
    return s
end;

local path = redis.call('GET', KEYS[1])

if not path then
    path = tonumber(getRandom(8, ARGV[1]))
    redis.call('SETEX',KEYS[1],60, path)
    return path
else
    return tonumber(path)
end
```

```java
package com.example.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-29 20:01:38
 */
public class PathService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DefaultRedisScript<Long> script;

    public Long getDynamicPath(String goodId){
        String key = goodId + ":path";
        List<String> keyList = new ArrayList<>();
        keyList.add(key);
        return redisTemplate.execute(script, keyList,randomSeed());
    }

    private static long randomSeed(){
        long temp = System.currentTimeMillis();
        long seed = 0L;
        while (temp > 0) {
            seed *= 10;
            seed += temp % 10;
            temp /= 10;
        }
        return seed % 100000000;
    }
}
```

### 4.2.4 编写 LimitService

用来防止短时间内（10 s）相同用户重复请求

```java
package com.example.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-29 22:30:08
 */
public class LimitService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Boolean checkRepeatLimit(String goodId, String phone, String email) {
        String key = goodId + ":" + phone;
        return redisTemplate.opsForValue().setIfAbsent(key, email, Duration.ofSeconds(10));
    }

}
```

## 4.3 rabbit

### 4.3.1 超时订单处理流程

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210604165347629.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2RyZWFtaW5nX2NvZGVy,size_16,color_FFFFFF,t_70#pic_center)


### 4.3.1 编写配置

新建用户 `seckill`，以及虚拟主机 `/seckill`

```yaml
spring:
  rabbitmq:
    host: 118.25.151.78
    port: 5672
    virtual-host: /seckill
    username: seckill
    password: seckill
```

### 4.3.2 编写配置类

```java
package com.example.rabbit.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-30 19:52:59
 */
@Configuration
public class RabbitMQConfiguration {
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 定义交换机

    @Bean
    public Exchange orderEventExchange() {
        return new TopicExchange("order-event-exchange", true, false);
    }

    // 死信队列

    @Bean
    public Queue seckillOrderDelayQueue() {
        //死信队列的属性
        Map<String, Object> arguments = new HashMap<>();
        //指定order-event-exchange为死信交换机，消息过期后将会投到死信交换机
        arguments.put("x-dead-letter-exchange", "order-event-exchange");
        //指定死信的路由key
        arguments.put("x-dead-letter-routing-key", "seckill.order.process");
        //ttl time to live 消息存活时间，为了测试，设置为10s
        arguments.put("x-message-ttl", 10000);
        //队列名，是否持久，是否排他，是否自动删除
        return new Queue("seckill.order.delay.queue", true, false, false, arguments);
    }



    @Bean
    // 处理订单队列，根据订单表中订单状态决定库存释放还是减一入库
    public Queue seckillOrderProcessQueue() {
        return new Queue("seckill.order.process.queue", true, false, false);
    }

    @Bean
    // 支付队列，修改订单状态为 0
    public Queue seckillOrderPayQueue() {
        return new Queue("seckill.order.pay.queue", true, false, false);
    }

    @Bean
    // 支付队列，修改订单状态为 2
    public Queue seckillOrderCancelQueue() {
        return new Queue("seckill.order.cancel.queue", true, false, false);
    }


    // 绑定交换机和队列
    @Bean
    public Binding seckillOrderCreateBinding() {
        return new Binding("seckill.order.delay.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "seckill.order.create", null);
    }

    @Bean
    public Binding seckillOrderProcessBinding() {
        return new Binding("seckill.order.process.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "seckill.order.process", null);
    }
    @Bean
    public Binding seckillOrderPayBinding() {
        return new Binding("seckill.order.pay.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "seckill.order.pay", null);
    }

    @Bean
    public Binding seckillOrderCancelBinding() {
        return new Binding("seckill.order.cancel.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange", "seckill.order.cancel", null);
    }

}
```

### 4.3.3 注册监听方法

```java
package com.example.rabbit.listener;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.repository.entity.Order;
import com.example.repository.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-31 19:49:00
 */

@Component
@Slf4j
@RabbitListener(queues = "seckill.order.pay.queue")
public class OrderPayListener {

    private OrderMapper orderMapper;

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @RabbitHandler
    public void consumer(Order order, Message message, Channel channel) {
        // 根据传入的 order 中的 orderId 定位到确定的订单，将其状态修改为 0
        try {
            log.info("付款订单id：{}", order.getOrderId());
            UpdateWrapper<Order> wrapper = new UpdateWrapper<>();
            wrapper.eq("order_id", order.getOrderId()).eq("status", "1");
            Order update = new Order();
            update.setStatus("0");
            int affectedRows = orderMapper.update(update, wrapper);
            if (affectedRows > 0) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                throw new RuntimeException();
            }
        } catch (IOException e) {
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (RuntimeException runtimeException) {
            log.error("付款失败");
            try {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

```java
package com.example.rabbit.listener;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.repository.entity.Order;
import com.example.repository.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-31 19:54:34
 */
@Component
@Slf4j
@RabbitListener(queues = "seckill.order.cancel.queue")
public class OrderCancelListener {

    private OrderMapper orderMapper;

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @RabbitHandler
    public void consumer(Order order, Message message, Channel channel) {
        // 根据传入的 order 中的 orderId 定位到确定的订单，将其状态修改为 2
        try {
            log.info("取消订单id：{}", order.getOrderId());
            UpdateWrapper<Order> wrapper = new UpdateWrapper<>();
            wrapper.eq("order_id", order.getOrderId()).eq("status", "1");
            Order update = new Order();
            update.setStatus("2");
            int affectedRows = orderMapper.update(update, wrapper);
            if (affectedRows > 0) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                throw new RuntimeException();
            }
        } catch (IOException e) {
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (RuntimeException runtimeException) {
            log.error("取消订单失败");
            try {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

```java
package com.example.rabbit.listener;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.redis.service.StockService;
import com.example.repository.entity.Good;
import com.example.repository.entity.Order;
import com.example.repository.mapper.GoodMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-05-31 19:51:18
 */
@Component
@Slf4j
@RabbitListener(queues = "seckill.order.process.queue")
public class OrderProcessListener {

    private GoodMapper goodMapper;

    private StockService stockService;

    @Autowired
    public void setGoodMapper(GoodMapper goodMapper) {
        this.goodMapper = goodMapper;
    }

    @Autowired
    public void setStockService(StockService stockService) {
        this.stockService = stockService;
    }

    @RabbitHandler
    public void consumer(Order order, Message message, Channel channel) {
        try {
            String status = order.getStatus();
            if ("0".equals(status)) {
                UpdateWrapper<Good> wrapper = new UpdateWrapper<>();
                wrapper.eq("good_id",order.getGoodId()).gt("stock",0);
                int affectedRows = goodMapper.deliver(wrapper);
                if (affectedRows > 0) {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                } else {
                    throw new RuntimeException();
                }
            } else {
                stockService.incrementStock(order.getGoodId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }
        } catch (IOException e) {
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }catch (RuntimeException runtimeException) {
            log.error("商品出库失败");
            try {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

## 4.5 service

### 4.5.1 编写配置

```java
package com.example.service.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 21:38:15
 */
@Configuration
public class ServiceConfiguration {

    @Bean
    @SuppressWarnings("UnstableApiUsage")
    public RateLimiter rateLimiter(){
        return RateLimiter.create(10);
    }
}
```

### 4.5.2 编写数据传输类

```java
package com.example.service.pojo;

import com.example.repository.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 20:12:27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OrderInfoDTO {
    private Order order;
    private String goodName;
    private BigDecimal deal;
}
```

### 4.5.3 编写返回值类型枚举类

```java
package com.example.service.type;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 21:21:37
 */
public enum ReturnType {
    Success,
    TimeLimitError,
    PurchaseLimitError,
    StockOutError,
    SeckillFailError
}
```

### 4.5.4 编写 GoodService

```java
package com.example.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.redis.service.StockService;
import com.example.repository.entity.Good;
import com.example.repository.mapper.GoodMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 18:09:35
 */
@Service
public class GoodService {

    private GoodMapper goodMapper;
    private StockService stockService;

    @Autowired
    public void setGoodMapper(GoodMapper goodMapper) {
        this.goodMapper = goodMapper;
    }

    @Autowired
    public void setStockService(StockService stockService) {
        this.stockService = stockService;
    }

    // 商品页面展示
    public List<Good> goodList() {
        return goodMapper.selectList(null);
    }

    // 商品库存查询
    public Long getStock(String goodId) {
        Long stock = stockService.getStock(goodId);
        if (-1 == stock) {
            QueryWrapper<Good> wrapper = new QueryWrapper<>();
            wrapper.select("stock").eq("good_id", goodId);
            stock = goodMapper.selectOne(wrapper).getStock();
        }
        return stock;
    }


```

### 4.5.5 编写 OrderService

```java
package com.example.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.repository.entity.Good;
import com.example.repository.entity.Order;
import com.example.repository.mapper.GoodMapper;
import com.example.repository.mapper.OrderMapper;
import com.example.service.pojo.OrderInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 18:11:08
 */
@Service
public class OrderService {

    private GoodMapper goodMapper;

    private OrderMapper orderMapper;

    @Autowired
    public void setGoodMapper(GoodMapper goodMapper) {
        this.goodMapper = goodMapper;
    }

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    // 查询订单的信息，在订单详情展示
    public OrderInfoDTO queryOrderInfo(String goodId, String phone) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("phone", phone).eq("good_id", goodId).ne("status", "2");
        Order order = orderMapper.selectOne(orderQueryWrapper);
        QueryWrapper<Good> goodQueryWrapper = new QueryWrapper<>();
        goodQueryWrapper.eq("good_id", goodId);
        Good good = goodMapper.selectOne(goodQueryWrapper);
        return new OrderInfoDTO(order, good.getGoodName(), good.getDiscountPrice());
    }
}
```

### 4.5.6 编写 PayService

```java
package com.example.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.repository.entity.Order;
import com.example.repository.mapper.OrderMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 20:55:32
 */
@Service
public class PayService {
    private RabbitTemplate rabbitTemplate;
    private OrderMapper orderMapper;

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public void pay(String goodId, String phone) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("phone", phone).eq("good_id", goodId).eq("status", "1");
        Order order = orderMapper.selectOne(orderQueryWrapper);
        rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.pay", order);
    }

    public void later(String goodId, String phone) {

    }

    public void cancel(String goodId, String phone) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("phone", phone).eq("good_id", goodId).eq("status", "1");
        Order order = orderMapper.selectOne(orderQueryWrapper);
        rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.cancel", order);
    }
}
```

### 4.5.7 编写 SecKillService

```java
package com.example.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.redis.service.LimitService;
import com.example.redis.service.StockService;
import com.example.repository.entity.Good;
import com.example.repository.entity.Order;
import com.example.repository.mapper.GoodMapper;
import com.example.repository.mapper.OrderMapper;
import com.example.service.type.ReturnType;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 18:07:21
 */
@Service
@SuppressWarnings("UnstableApiUsage")
public class SecKillService {
    private RateLimiter rateLimiter;
    private LimitService limitService;
    private StockService stockService;
    private GoodMapper goodMapper;
    private OrderMapper orderMapper;
    private RabbitTemplate rabbitTemplate;

    @Autowired
    public void setRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Autowired
    public void setLimitService(LimitService limitService) {
        this.limitService = limitService;
    }

    @Autowired
    public void setStockService(StockService stockService) {
        this.stockService = stockService;
    }

    @Autowired
    public void setGoodMapper(GoodMapper goodMapper) {
        this.goodMapper = goodMapper;
    }

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public ReturnType seckill(String goodId, String phone, String email) {
        Boolean flag = limitService.checkRepeatLimit(goodId, phone, email);
        if (!flag) {
            return ReturnType.TimeLimitError;
        }
        Boolean isEnough = stockService.checkStock(goodId);
        if (!isEnough) {
            return ReturnType.StockOutError;
        }
        if (rateLimiter.tryAcquire()) {
            QueryWrapper<Good> wrapper = new QueryWrapper<>();
            wrapper.select("stock").eq("good_id", goodId);
            Long stock = goodMapper.selectOne(wrapper).getStock();
            if (stock == null || stock <= 0) {
                stockService.resetStock(goodId);
                return ReturnType.StockOutError;
            }
            // 判断该用户是否抢购过了
            QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
            orderQueryWrapper.eq("phone", phone).eq("good_id", goodId).ne("status", "2");
            List<Order> orders = orderMapper.selectList(orderQueryWrapper);
            if (orders.size() != 0) {
                stockService.incrementStock(goodId);
                return ReturnType.PurchaseLimitError;
            }

            // 一切就绪，开始生成订单，默认就是待支付状态，status = 1
            Order order = new Order();
            order.setPhone(phone);
            order.setEmail(email);
            order.setGoodId(goodId);
            order.setStatus("1");
            int insert = orderMapper.insert(order);
            if (insert > 0) {
                // 将该订单送入死信队列，等待处理
                rabbitTemplate.convertAndSend("order-event-exchange", "seckill.order.create", order);
            } else {
                stockService.incrementStock(goodId);
                return ReturnType.SeckillFailError;
            }
        } else {
            stockService.incrementStock(goodId);
            return ReturnType.SeckillFailError;
        }
        return ReturnType.Success;
    }
}
```

## 4.6 controller

这个太复杂了，原谅我是在不想记录，因为踩的坑太多了….记不过来，想了解自己看代码去吧

# 5. 打包

这是个好问题，多模块开发的打包部署，每个子模块和父模块的 pom 文件必须写对，参照我这个项目的 pom 写就行，而且是先打包父项目，然后按照子模块的依赖关系顺序打包，从没有依赖其他同级模块的 pom 开始打包。























