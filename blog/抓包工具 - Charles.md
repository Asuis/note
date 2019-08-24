---
 originUrl: 'https://juejin.im/entry/59142013ac502e006c589ad0'
---
 
 抓包 其实很多程序员都不陌生了，但是真正抓过包、分析过的又有几个。
本文将介绍几款简单易用的抓包工具，并针对目前互联网主流的Http和Https网络包进行抓取并分析，同时分享手机抓包的技术
1. Charles、Fiddler和Wireshark
2. Http、Https及其原理
3. 手机抓包
4. Charles的附加功能
一、抓包工具

    Fiddler
        是之前我用Window电脑的时候，特别喜欢用的工具，而且当时的Fiddler足以满足的我简单的抓包工作，现在也应该更新到Fiddler3了，如果用Windows的朋友可以用Fiddler3进行抓包。
    Charles
        自从换了Mac之后我就喜欢上了这个工具，不过Charles在Windows上同样也有。
    Wireshark
        这个我不是怎么经常用，这个抓包工具可以详细的看到网络请求的三次握手，并且可支持spdy、tcp等等的网络协议抓包，当然其他两个是不支持的。

我将以Charles为例分别抓取Http和Https包：
下载
Mac破解版下载地址：download.csdn.net/detail/m694…
Win破解版下载地址：download.csdn.net/detail/m694…
官网链接，需要购买LisenseKey：www.charlesproxy.com/

打开界面如下：

第二、Http、Https包
2.1、Http包
2.1.1 清理Charles列表，让抓包更加清晰

2.1.2 以我的CSDN为例（m694449212）,通过Chrome点击‘我的博客’，抓到需要的包

从中我们过滤出m694449212的博客包，但是这个过滤的过程需要我们去一个个找（当然如果你的经验比较足或者英语比较好的话，可以发现其实就是blog.csdn.net的包）
2.1.3 分析包

2.1.4 Reuqest

其中比较重要的是Cookie,网站为了辨别用户身份、进行 session 跟踪而储存在用户本地终端上的数据（通常经过加密）.
同时Cookie在我们爬虫的时候也是一个必不可少的东西，那么如何自动化获取Cookie呢？后面会讲解到。
2.1.5 Response

获取Cookie,通过我以往的经验:
a. 获取Cookie的时候首先需要保证我们的浏览器环境是干净的，我说的干净其实就是清楚当前浏览器保存的Cookie，并重启浏览器。
b. 重启之后我们访问www.csdn.net，当前Host的Request中就不包含Cookie，那么Cookie在哪呢，其实细心点的会发现Cookie在Response的Headers->set-cookie中，并在下次请求中使用到。
c. 那么当我们登录操作并携带Cookie在请求Headers中，那么登录成功之后该Cookie就会生效。之后我们的所有请求携带该Cookie就会是一个正常的请求，并能拿到需要的结果。
关于某些请求携带sign参数的，后面的文章我会讲解到破解Sign函数（其实有时候不是直接的破解而是函数的Hook,有兴趣的可以提前了解下Android或者iOS的Hook，通过IDA找到sign函数,并使用cycript调用），之后的文章我会以国外的知名App Instagram为例，Hook它的签名函数。

来个美女提提神（图片来自Instagram的Https包数据），继续往下看

2.2、Https包
2.2.1、Https简介
SSL相信大家都不陌生。其实Https就是在Http基础上通过SSL协议进行加密之后的网络传输。
并通过非对称和对称加密算法来对密码和数据进行加密。具体看下图：

1. Client明文将自己支持的一套加密规则、一个随机数(Random_C)发送给服务器.
2. Server返回自己选择的加密规则、CA证书（服务器地址、加密公钥、以及证书颁发机构）、外加一个通过加密规则和HASH算法生成的随机数(Random_S)
3. Client收到Server的消息之后会:

a:验证证书（地址是否是正在访问的和机构是否合法）、
b:自己生成一个随机的密码(Pre_master)并使用CA证书中的加密公钥进行加密(enc_pre_master)、
c:计算出一个对称加密的enc_key,通过Random_C、Random_S、Pre_master、
d:生成握手信息：使用约定好的Hash算法计算握手信息，并通过enc_key和约定好的加密算法对消息进行加密

4. Client将enc_pre_master、加密之后的握手消息发送给Server
5. Server收到消息之后

a: 收到enc_pre_master之后，会通过私钥进行解密（非对称加密算法）得到pre_master
b: 通过pre_masrer、Random_C、Random_S计算得到协商密码 enc_key
c: 通过enc_key解密握手信息，验证HASH是否和客户端发来的一致
d: 生成握手信息同样适用enc_key和约定好的加密算法

6. Server发送握手信息给Client,也就是说Server验证通过了Client,并再次发送消息给Client让其验证自己
7. 客户端拿到握手信息解密，握手结束。客户端解密并计算握手消息的HASH，如果与服务端发来的HASH一致，此时握手过程结束。
8. 正常加密通信，握手成功之后，所有的通信数据将由之前协商密钥enc_key及约定好的算法进行加密解密。
其中Https使用到的加密算法如下：

    非对称加密算法：RSA，DSA/DSS
    对称加密算法：AES，RC4，3DES
    HASH算法：MD5，SHA1，SHA256

2.2.2、 Charles抓取Https原理
Charles本身就是一个协议代理工具，在上篇的Https原理上，客户端和服务器的所有通信都被Charles捕获到。
如下图：

主要步骤如下：
1. Charles捕获Client发送给Server请求，并伪装成客户端向服务器发起握手请求
2. 服务器响应，Charles获取到服务器的CA证书，并用根证书公钥进行解密，获取到服务器的CA证书公钥。然后Charles伪造自己的CA证书，伪装为服务器的CA证书发送给客户端
3. 客户端收到返回之后，和上面讲到的过程一样，证书校验、生成密码、并使用Charles伪装的证书公钥进行加密，并生成 Https通信的协商密码enc_key
4. Charles捕获到Client发来的重要信息，并使用自己伪造的证书私钥将密文解密，获取到enc_key.然后Charles使用服务器之前返回的证书公钥对明文进行加密并发送给服务器
5. 去之前一样，服务器收到消息之后，用私钥解开并建立信任，然后发送加密的握手信息。
6. Charles截获服务器发来的握手密文，并用对称密钥解开，再用自己伪造证书的私钥加密传给客户端
7. 客户端拿到加密信息后，用公钥解开，验证HASH。握手过程正式完成，客户端与服务器端就这样建立了”信任“。

    其实在整个过程中，最重要的就是enc_key,由于Charles从一开始伪造并获取了enc_key，所以在整个通信过程中Charles充当第三者，所有信息对其来讲都是透明的。
    其次就是根证书，这是https一个信任链的开始。这也是Charles伪造的CA证书能获得双方信任的关键。

2.2.3、演示Charles抓取Https

    原理清楚之后，其实操作就很简单了，操作的核心点就是根证书。

    安装根证书（Charles Root Certificate）

    让系统信任该证书

    接下来将需要抓的Https链接加入到CharlesSSL代理规则中，443是Https的默认端口


    当然你也可以像我最后一条一样，使用 *:443 来抓取所有https的包。
    通过浏览器访问自己要抓的链接，这样所有的Https都可以像Http一样明文展示都我们面前。

第三、手机抓包

    手机抓包的原理其实也很简单，让手机和抓包工具处于同一局域网，并将手机的WifiProxy手动代理到电脑的Ip和Charles设置的抓包端口上，具体操作可在网上找到,具体见blog.csdn.net/richer1997/…

我这边主要讲一下手机端Https包的抓取，其实和浏览器的抓取一样：

    首先需要安装Charles的根证书到手机上。

    点击之后，会弹出让你在手机上配置代理到对应Ip和端口，之后通过手机浏览器打开chls.pro/ssl

    使用手机访问该链接之后，会自动被识别为证书，并跳转到：(当然我这里已经是安装过的，未安装的点击右上角安装即可)

    之后就类似与PC端抓Https包原理一样，手机端的证书被作为根证书使用，并通过Charles拿到enc_key.将所有通信过程透明化。

第四、Charles的附加功能

    在我刚开始使用Charles的时候，我只是用来简单的抓抓接口，直到我看到别人使用BurpSuite自定义请求数据并Repeat的时候，我在考虑Charles是否也有这种功能。当然不出我所料，Charles也是支持的。

在对应接口上点击右键，出现菜单，其中我经常使用到的就是Compose、Repeat和RepeatAdvanced

Compose:可直接自定义对应的请求，并执行该请求。这个对我们抓包用处很大。我们可以从中得到该接口的必填参数等等的。
Repeat：很简单就是执行一次重复请求操作
Repeat Advanved：重复请求的高级操作，可自定义重复的次数、每隔多少秒执行。这个功能对于我们的接口的压测是很有用的。
除了这几个我常用的功能，当然Charles还有更多更加实用的功能，如过滤、排序等等。还需要大家去自行使用，发现更多更好、并适用于自己的功能。
小结

    抓包的用处其实很大，有时候可以用来调试我们的接口、有时候也可以用来做一些对工作有益的事，当然并"不建议"用来攻击别人的网络。
