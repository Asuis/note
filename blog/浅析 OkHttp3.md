https://juejin.im/entry/58fef752a22b9d0065b23dcc

OkHttp3
前言

做React Native的时候遇到业务线反馈的一个Bug：在使用Charles做代理的时候，将reactTimeout值改小的时候，有时候会发现在Charles没有捕获到Http请求的时候，仍然返回数据了。这是一个比较诡异的问题，出现问题的原因可能有以下两点：

    Http请求通过缓存直接返回;
    Http请求并未通过设置的代理请求。
    针对这个问题，用了STFW没有找到什么明确的答案，既然如此，那就直接从源码着手分析，通过一些笔记的整理，就有了这篇文章。由于精力能力有限，对于OkHttp3的分析不会在细节处深入，如果有错误指出烦请拍砖，共同进步。
    （另源码分析基于OkHttp3-3.4.1版本）

概述

本文会先简单说下OkHttp3的工作流程，然后介绍OkHttp3的一些核心类（如连接池StreamAllocation以及各式各样的Interceptor），接着从源码角度分析一次HTTP请求在OkHttp3中所经历的过程，在不同的Interceptor(拦截器)可以看到一些OkHttp3设计的一些巧妙思想，最后对上述分析做个简单的总结。

    Okhttp3是Square公司开源的强大高效的Java网络请求库，具有以下特性：

        支持Http2/SPDY；
        默认启用长连接，使用连接池管理，支持Cache(目前仅支持GET请求的缓存)；
        路由节点管理，提升访问速度；
        透明的Gzip处理，节省网络流量。
        灵活的拦截器，行为类似Java EE的Filter或者函数编程中的Map。

旅程开始

OkHttp3支持同步和异步两种请求方式，异步请求会经过Dispatcher在线程池中执行。同步请求没有线程池这一个过程，由于同步请求很简单，这里仅分析异步请求方式。OkHttp3一次完整的请求过程是从构造一个Request对象开始，接着调用OkHttpClient.newCall()返回一个RealCall对象并调用RealCall.enqueue()方法，最后会进入Dispacher.enqueue()方法中，这里会将RealCall对象放入线程池中调度执行。

OkHttp3请求流程图
OkHttp3的核心类

这部分会简单介绍一些OkHttp3的核心类，这些核心类共同支持了OkHttp3的一些基础功能。粗略的分成了Interceptor(拦截器)、Router(路由)和Stream(流)三部分，OkHttpClient类是初始化时候就配置的，比较简单就不说了。

OkHttp3核心类
Interceptor(拦截器)

OkHttp3的Interceptor是Request -> Response请求过程中的一个"节点"单位，通过一连串有序的Interceptor拦截器"节点"组成一条加工链，加工链中的任意一个"节点"都可以去拦截加工Request和Response。OkHttp默认提供了一套完善的Interceptor集合，当然也支持自定义一个Interceptor来实现一个上传/下载的进度更新器或者黑白名单拦截等等个性化的功能。

    阅读OkHttp3的源码建议从Interceptor开始。

OkHttp默认提供了如下Interceptor：

    RetryAndFollowUpInterceptor：默认情况下位于OkHttp3加工链的首位，顾名思义，具有失败-重试机制，支持页面重定向和一些407之类的代理验证等，此外负责StreamAllocation对象的创建(稍后介绍)。

    BridgeInterceptor：桥拦截器，配置Request的Headers头信息：读取Cookie，默认启用Gzip，默认加入Keep-Alive长连接，如果不想让OkHttp3擅自使用长连接，只需在Request的Header中预设Connection字段即可。

    CacheInterceptor: 管理OkHttp3的缓存，目前仅支持GET类型的缓存，使用文件形式的Lru缓存管理策略，CacheStrategy类负责了缓存相关的策略管理。

    ConnectInterceptor：OkHttp3打开一个Socket连接的地方，OkHttp3相关的Router路由切换策略也可以从这里开始跟踪。

    CallServerInterceptor：处于OkHttp3加工链的末尾，通过HttpStream往Socket中写入Request报文信息，并回读Response报文信息。

    OkHttp3的拦截器执行顺序依次是：自定义Interceptors(暂且称作A) -> RetryAndFollowUpInterceptor -> BridgeInterceptor -> CacheInterceptor -> ConnectInterceptor -> 自定义NetInterceptors(暂且称作B) -> CallServerInterceptor

        B仅在非WebSocket情况下被调用。
        A与B的区别是，A能拦截所有类型的请求，包括缓存命中的请求；而B仅拦截非WebSocket的情况下产生真正网络访问的请求。因此在B上做网络上传和下载进度的监听器是比较合适的。

Stream(流相关类)

OkHttp3并没有直接操作Socket，而是通过okio库进行了封装，okio库的设计也是非常赞的，它的Sink对应输入流，Source对应输出流，okio已经实现了与之对应的缓冲相关的包装类，采用了Segment切片和循环链表结构实现缓冲处理，有兴趣还是可以看看okio的源码。

    StreamAllocation：OkHttp3管理物理连接的对象，负责连接流的创建关闭等管理工作，通过池化物理连接来减少Hand-shake握手过程以提升请求效率，另外StreamAllocation通过持有RouteSelector对象切换路由。关于路由切换，这里有个场景，在Android系统中，如果你配置了代理，当代理服务器访问超时的时候，OkHttp3在进行请求重试时候会切换到下个代理或者采用无代理直连形式请求。因此并非设置了代理，OkHttp3就会"老实"的跟着你的规则走。这也是本文一开始提到的问题产生的原因。

    HttpStream： 这是一个抽象类，其子类实现了各类网络协议流格式。 HttpStream在OkHttp3中有两个实现类Http1xStream和Http2xStream，Http1xStream实现了HTTP/1.1协议流，Http2xStream则实现了HTTP/2和SPDY协议流。

    ConnectionPool：OkHttp连接池，由OkHttpClient持有该对象，ConnectionPool持有一个0核心线程数的线程池(与Executors.newCachedThreadPool()提供的线程池行为完全一样)用于清理一些超时的RealConnection连接对象，持有一个Deque对象缓存OkHttp3的RealConnection连接对象。

Router(路由)

OkHttp3要求每个连接都需要指明一个Router路由对象，当然这个Router路由对象可以是直连类型的，意即你不使用任何的代理服务器。当尝试使用某个路由请求失败的时候，OkHttp3会在允许请求重试的情况下通过RouterSelector切换到下个路由继续请求，并将失败的路由记录到黑名单中，这样在OkHttp3重复请求一个目标地址的时候能够优先选择成功的路由进行网络请求。

    Router：包含代理与Socket地址信息。
    RouteDatabase：记录请求失败的Router路由对象的"黑名单"。
    RouteSelector：负责指派Router路由。持有RouteDatabase对象。

源码角度解析OkHttp3请求

分析查看源码一般抓主干，本文也不例外，我们接下来就直接从代码来看OkHttp3是如何进行一次完整的网络请求。限于篇幅，仅仅分析异步情况下的网络请求，同步方式的网络请求更加简单，核心部分都是一样的。另外为了方便阅读，部分无关语句会被省略。

//所在类 okhttp3.RealCall

@Override public void enqueue(Callback responseCallback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    client.dispatcher().enqueue(new AsyncCall(responseCallback));
  }

在调用client.newCall(request).enqueue(...)方法开始请求之后，就会进入上面的enqueue()方法，可以看到最后是调用了Dispatcher.enqueue()，继续跟踪源码到如下地方：

//所在类 okhttp3.Dispatcher

synchronized void enqueue(AsyncCall call) {
    if (runningAsyncCalls.size() < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) {
      runningAsyncCalls.add(call);
      executorService().execute(call);
    } else {
      readyAsyncCalls.add(call);
    }
  }

可以看到OkHttp3使用了一个线程池来执行这个AsyncCall，AsyncCall本质上是一个Runnable对象，最后会调用AsyncCall.execute()方法。

//所在类 okhttp3.RealCall.AsyncCall

    @Override protected void execute() {
      boolean signalledCallback = false;
      try {
        Response response = getResponseWithInterceptorChain();
        if (retryAndFollowUpInterceptor.isCanceled()) {
          signalledCallback = true;
          responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
        } else {
          signalledCallback = true;
          responseCallback.onResponse(RealCall.this, response);
        }
      } catch (IOException e) {
        ...
      } finally {
        client.dispatcher().finished(this);
      }
    }

通过getResponseWithInterceptorChain()获取Response报文，继续跟踪下去。

//所在类 okhttp3.RealCall

private Response getResponseWithInterceptorChain() throws IOException {
    // Build a full stack of interceptors.
    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.addAll(client.interceptors());
    interceptors.add(retryAndFollowUpInterceptor);
    interceptors.add(new BridgeInterceptor(client.cookieJar()));
    interceptors.add(new CacheInterceptor(client.internalCache()));
    interceptors.add(new ConnectInterceptor(client));
    if (!retryAndFollowUpInterceptor.isForWebSocket()) {
      interceptors.addAll(client.networkInterceptors());
    }
    interceptors.add(new CallServerInterceptor(
        retryAndFollowUpInterceptor.isForWebSocket()));

    Interceptor.Chain chain = new RealInterceptorChain(
        interceptors, null, null, null, 0, originalRequest);
    return chain.proceed(originalRequest);
  }

OkHttp3将Interceptor(拦截器)放到一个集合里，通过自增递归的方式调用RealInterceptorChain.proceed()方法依次执行集合里的每一个Interceptor(拦截器)，这个Chain的传递过程刚开始看有点绕，这里简单画个流程图方便理解。

RealInterceptor关键源码如下：

// 所在类 okhttp3.internal.http.RealInterceptorChain

public RealInterceptorChain(List<Interceptor> interceptors, StreamAllocation streamAllocation,
      HttpStream httpStream, Connection connection, int index, Request request) {
    this.interceptors = interceptors;
    this.connection = connection;
    this.streamAllocation = streamAllocation;
    this.httpStream = httpStream;
    this.index = index;
    this.request = request;
  }

public Response proceed(Request request, StreamAllocation streamAllocation, HttpStream httpStream,
      Connection connection) throws IOException {

    // 省略部分源码
    ...

    // Call the next interceptor in the chain.
    RealInterceptorChain next = new RealInterceptorChain(
        interceptors, streamAllocation, httpStream, connection, index + 1, request);
    Interceptor interceptor = interceptors.get(index);
    Response response = interceptor.intercept(next);

    // Confirm that the next interceptor made its required call to chain.proceed().
    if (httpStream != null && index + 1 < interceptors.size() && next.calls != 1) {
      throw new IllegalStateException("network interceptor " + interceptor
          + " must call proceed() exactly once");
    }

    // Confirm that the intercepted response isn't null.
    if (response == null) {
      throw new NullPointerException("interceptor " + interceptor + " returned null");
    }

    return response;
  }

RealInterceptorChain的构造函数中的index和interceptors是匹配的，用来索引接下来需要执行的Intercetpor拦截器。
通过调用interceptor.intercept(next)方法，在各个Intercetpor拦截器里又能通过next参数继续调用proceed()方法，完成递归操作。
接下来我们按照执行顺序依次看下这些interceptors具体都干了什么，从RetryAndFollowUpInterceptor开始。

// 所在类 okhttp3.internal.http.RetryAndFollowUpInterceptor

@Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();

    streamAllocation = new StreamAllocation(
        client.connectionPool(), createAddress(request.url()));

    int followUpCount = 0;
    Response priorResponse = null;
    while (true) {
      if (canceled) {
        streamAllocation.release();
        throw new IOException("Canceled");
      }

      Response response = null;
      boolean releaseConnection = true;
      try {
        response = ((RealInterceptorChain) chain).proceed(request, streamAllocation, null, null);
        releaseConnection = false;
      } catch (RouteException e) {
        // The attempt to connect via a route failed. The request will not have been sent.
        if (!recover(e.getLastConnectException(), true, request)) throw e.getLastConnectException();
        releaseConnection = false;
        continue;
      } catch (IOException e) {
        // An attempt to communicate with a server failed. The request may have been sent.
        if (!recover(e, false, request)) throw e;
        releaseConnection = false;
        continue;
      } finally {
        // We're throwing an unchecked exception. Release any resources.
        if (releaseConnection) {
          streamAllocation.streamFailed(null);
          streamAllocation.release();
        }
      }

      // Attach the prior response if it exists. Such responses never have a body.
      if (priorResponse != null) {
        response = response.newBuilder()
            .priorResponse(priorResponse.newBuilder()
                .body(null)
                .build())
            .build();
      }

      Request followUp = followUpRequest(response);

      if (followUp == null) {
        if (!forWebSocket) {
          streamAllocation.release();
        }
        return response;
      }

      closeQuietly(response.body());

      if (++followUpCount > MAX_FOLLOW_UPS) {
        streamAllocation.release();
        throw new ProtocolException("Too many follow-up requests: " + followUpCount);
      }

      if (followUp.body() instanceof UnrepeatableRequestBody) {
        throw new HttpRetryException("Cannot retry streamed HTTP body", response.code());
      }

      if (!sameConnection(response, followUp.url())) {
        streamAllocation.release();
        streamAllocation = new StreamAllocation(
            client.connectionPool(), createAddress(followUp.url()));
      } else if (streamAllocation.stream() != null) {
        throw new IllegalStateException("Closing the body of " + response
            + " didn't close its backing stream. Bad interceptor?");
      }

      request = followUp;
      priorResponse = response;
    }
  }

代码有点长，不过主要还是做了这几个操作，首先StreamAllocation对象在这里被创建，接着调用proceed()方法执行了一次请求，并拿到一个Response报文，在followUpRequest()方法中对Response报文进行了各种判断(验证了407，判断需不需要重定向等)确定是否需要再次请求，如果需要持续请求会在followUpRequest()返回一个新的Request对象并重新请求。followUpRequest()的代码有点长，可以自行查阅源码，这里就不贴了。继续看执行的下一个拦截器BridgeInterceptor。

// 所在类 okhttp3.internal.http.BridgeInterceptor 

@Override public Response intercept(Chain chain) throws IOException {
    Request userRequest = chain.request();
    Request.Builder requestBuilder = userRequest.newBuilder();

    RequestBody body = userRequest.body();
    if (body != null) {
      MediaType contentType = body.contentType();
      if (contentType != null) {
        requestBuilder.header("Content-Type", contentType.toString());
      }

      long contentLength = body.contentLength();
      if (contentLength != -1) {
        requestBuilder.header("Content-Length", Long.toString(contentLength));
        requestBuilder.removeHeader("Transfer-Encoding");
      } else {
        requestBuilder.header("Transfer-Encoding", "chunked");
        requestBuilder.removeHeader("Content-Length");
      }
    }

    if (userRequest.header("Host") == null) {
      requestBuilder.header("Host", hostHeader(userRequest.url(), false));
    }

    if (userRequest.header("Connection") == null) {
      requestBuilder.header("Connection", "Keep-Alive");
    }

    // If we add an "Accept-Encoding: gzip" header field we're responsible for also decompressing
    // the transfer stream.
    boolean transparentGzip = false;
    if (userRequest.header("Accept-Encoding") == null) {
      transparentGzip = true;
      requestBuilder.header("Accept-Encoding", "gzip");
    }

    List<Cookie> cookies = cookieJar.loadForRequest(userRequest.url());
    if (!cookies.isEmpty()) {
      requestBuilder.header("Cookie", cookieHeader(cookies));
    }

    if (userRequest.header("User-Agent") == null) {
      requestBuilder.header("User-Agent", Version.userAgent());
    }

    Response networkResponse = chain.proceed(requestBuilder.build());

    HttpHeaders.receiveHeaders(cookieJar, userRequest.url(), networkResponse.headers());

    Response.Builder responseBuilder = networkResponse.newBuilder()
        .request(userRequest);

    if (transparentGzip
        && "gzip".equalsIgnoreCase(networkResponse.header("Content-Encoding"))
        && HttpHeaders.hasBody(networkResponse)) {
      GzipSource responseBody = new GzipSource(networkResponse.body().source());
      Headers strippedHeaders = networkResponse.headers().newBuilder()
          .removeAll("Content-Encoding")
          .removeAll("Content-Length")
          .build();
      responseBuilder.headers(strippedHeaders);
      responseBuilder.body(new RealResponseBody(strippedHeaders, Okio.buffer(responseBody)));
    }

    return responseBuilder.build();
  }

可以看出来BridgeInterceptor对Request和Response报文加工的具体步骤，默认对Request报文增加了gzip头信息，并在Response报文中对gzip进行解压缩处理。另外CookieJar类也是在这里处理的。接下来就是CacheInterceptor拦截器。

// 所在类 okhttp3.internal.cache.CacheInterceptor

@Override public Response intercept(Chain chain) throws IOException {
    Response cacheCandidate = cache != null
        ? cache.get(chain.request())
        : null;

    long now = System.currentTimeMillis();

    CacheStrategy strategy = new CacheStrategy.Factory(now, chain.request(), cacheCandidate).get();
    Request networkRequest = strategy.networkRequest;
    Response cacheResponse = strategy.cacheResponse;

    if (cache != null) {
      cache.trackResponse(strategy);
    }

    if (cacheCandidate != null && cacheResponse == null) {
      closeQuietly(cacheCandidate.body()); // The cache candidate wasn't applicable. Close it.
    }

    // If we're forbidden from using the network and the cache is insufficient, fail.
    if (networkRequest == null && cacheResponse == null) {
      return new Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(504)
          .message("Unsatisfiable Request (only-if-cached)")
          .body(EMPTY_BODY)
          .sentRequestAtMillis(-1L)
          .receivedResponseAtMillis(System.currentTimeMillis())
          .build();
    }

    // If we don't need the network, we're done.
    if (networkRequest == null) {
      return cacheResponse.newBuilder()
          .cacheResponse(stripBody(cacheResponse))
          .build();
    }

    Response networkResponse = null;
    try {
      networkResponse = chain.proceed(networkRequest);
    } finally {
      // If we're crashing on I/O or otherwise, don't leak the cache body.
      if (networkResponse == null && cacheCandidate != null) {
        closeQuietly(cacheCandidate.body());
      }
    }

    // If we have a cache response too, then we're doing a conditional get.
    if (cacheResponse != null) {
      if (validate(cacheResponse, networkResponse)) {
        Response response = cacheResponse.newBuilder()
            .headers(combine(cacheResponse.headers(), networkResponse.headers()))
            .cacheResponse(stripBody(cacheResponse))
            .networkResponse(stripBody(networkResponse))
            .build();
        networkResponse.body().close();

        // Update the cache after combining headers but before stripping the
        // Content-Encoding header (as performed by initContentStream()).
        cache.trackConditionalCacheHit();
        cache.update(cacheResponse, response);
        return response;
      } else {
        closeQuietly(cacheResponse.body());
      }
    }

    Response response = networkResponse.newBuilder()
        .cacheResponse(stripBody(cacheResponse))
        .networkResponse(stripBody(networkResponse))
        .build();

    if (HttpHeaders.hasBody(response)) {
      CacheRequest cacheRequest = maybeCache(response, networkResponse.request(), cache);
      response = cacheWritingResponse(cacheRequest, response);
    }

    return response;
  }

CacheInterceptor.intercept()方法也挺长的，首先从cache缓存中获取一个匹配的Response报文并赋给cacheCandidate变量，cache是一个InternalCache对象，里面持有DiskLruCache这个对象，以文件流的形式存储Response报文，采用LRU原则管理这些缓存；接着使用CacheStrategy.Factory工厂类生成一个缓存策略类CacheStrategy，通过该类拿到两个关键变量networkRequest和cacheResponse，这里针对cacheCandidate、networkRequest和cacheResponse这三个变量的赋值情况依次进行了以下处理：

    cacheCandidate不为空，cacheResponse为空，说明缓存过期，将cacheCandidate从cache中清除；
    networkRequest和cacheResponse同时为空，说明Request要求只使用缓存，而缓存并不存在或者已经失效，直接返回504的错误报文，请求结束；
    networkRequest为空，说明cacheResponse不为空，命中缓存，直接返回cacheResponse报文；
    未命中缓存，开启网络请求，继续执行下一个Interceptor拦截器。

分析完Request，对请求回来的Response报文处理就很简单了，就是针对Response报文情况决定是否使用缓存特性。

    OkHttp3的Cache相关策略可参考RFC 2616, 14.9

当缓存未命中时候OkHttp3就开始执行真正的网络请求，CacheInterceptor的下一个就是ConnectInterceptor拦截器。

// 所在类 okhttp3.internal.connection.ConnectInterceptor

@Override public Response intercept(Chain chain) throws IOException {
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Request request = realChain.request();
    StreamAllocation streamAllocation = realChain.streamAllocation();

    // We need the network to satisfy this request. Possibly for validating a conditional GET.
    boolean doExtensiveHealthChecks = !request.method().equals("GET");
    HttpStream httpStream = streamAllocation.newStream(client, doExtensiveHealthChecks);
    RealConnection connection = streamAllocation.connection();

    return realChain.proceed(request, streamAllocation, httpStream, connection);
  }

ConnectInterceptor做的事情很简单，先获取了在RetryAndFollowUpInterceptor中创建的StreamAllocation对象，接着执行streamAllocation.newStream()打开一个物理连接并返回一个HttpStream的对象，HttpStream在前文提到了是网络协议流(HTTP/1.1、HTTP/2和SPDY)的具体实现。这时候调用realChain.proceed()方法的时候，四个参数均不为空，这是集齐了所有的"龙珠"召唤最终的"神龙"CallServerInterceptor拦截器了。

    在ConnectInterceptor的下一个拦截器并非绝对是CallServerInterceptor，如果有自定义NetInterceptors则会被优先执行，不过绝大部分情况下CallServerInterceptor在最后也是会被调用的。

// 所在类 okhttp3.internal.http.CallServerInterceptor

@Override public Response intercept(Chain chain) throws IOException {
    HttpStream httpStream = ((RealInterceptorChain) chain).httpStream();
    StreamAllocation streamAllocation = ((RealInterceptorChain) chain).streamAllocation();
    Request request = chain.request();

    long sentRequestMillis = System.currentTimeMillis();
    httpStream.writeRequestHeaders(request);

    if (HttpMethod.permitsRequestBody(request.method()) && request.body() != null) {
      Sink requestBodyOut = httpStream.createRequestBody(request, request.body().contentLength());
      BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);
      request.body().writeTo(bufferedRequestBody);
      bufferedRequestBody.close();
    }

    httpStream.finishRequest();

    Response response = httpStream.readResponseHeaders()
        .request(request)
        .handshake(streamAllocation.connection().handshake())
        .sentRequestAtMillis(sentRequestMillis)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build();

    if (!forWebSocket || response.code() != 101) {
      response = response.newBuilder()
          .body(httpStream.openResponseBody(response))
          .build();
    }

    if ("close".equalsIgnoreCase(response.request().header("Connection"))
        || "close".equalsIgnoreCase(response.header("Connection"))) {
      streamAllocation.noNewStreams();
    }

    int code = response.code();
    if ((code == 204 || code == 205) && response.body().contentLength() > 0) {
      throw new ProtocolException(
          "HTTP " + code + " had non-zero Content-Length: " + response.body().contentLength());
    }

    return response;
  }

CallServerInterceptor拦截器里先调用httpStream协议流对象写入Request的Header部分，接着写入Body部分，这样就完成了Request的请求，从httpStream里回读Response报文，并根据情况读取Response的Body部分，当Response响应报文的头信息中Connection字段为close时，将streamAllocation设置成noNewStreams状态，标识其当前Connection对象不再被复用，将在流请求结束之后被回收掉。
总结

通过三四天对OkHttp3源码的阅读，佩服框架设计的巧妙，不光在于类的封装上，里面对设计模式的实践也挺好的。其中Prototype原型模式和Builder建造者模式被广泛使用，关于Interceptor的概念使得全新设计一个私有请求协议不无可能，而okio对于流的封装也是很巧妙的。推荐好好学习下。